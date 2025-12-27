/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.core.api.texture;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import cc.cosmetica.core.mixin.texture.NativeImageAccessorMixin;
import cc.cosmetica.core.util.VP8X;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CosmeticaTexture extends AbstractTexture {
    private CosmeticaTexture(File file, String url, ResourceLocation loadingTexture, @Nullable ResourceLocation errorTexture,
                                    int frames, int ticksPerFrame, Consumer<NativeImage> onFirstUpload, int heightDivider)
            throws IllegalArgumentException {
        if (frames > 1 && ticksPerFrame == 0) {
            throw new IllegalArgumentException("Animated texture (" + frames + " frames) but ticks per frame is 0!");
        }

        // properties
        this.cacheFile = file;
        this.url = url;
        this.tilesheetFrames = frames;
        this.realTicksPerFrame = ticksPerFrame;
        this.currentFrames = 0;
        this.currentTicksPerFrame = 2;
        this.onFirstUpload = onFirstUpload;
        this.loadingTexture = loadingTexture;
        this.errorTexture = errorTexture;
        this.heightDivider = heightDivider;
    }

    private final File cacheFile;
    private final String url;
    private final ResourceLocation loadingTexture;
    private final @Nullable ResourceLocation errorTexture;
    private final int tilesheetFrames;
    private final int realTicksPerFrame;
    private final Consumer<NativeImage> onFirstUpload;
    private final int heightDivider;
    int tilesheetIncrement = 1;
    @Nullable private CompletableFuture<?> future;

    private int frameHeight;
    private int frame;
    private int currentFrames, currentTicksPerFrame, autoFrameInc;
    private int tick;
    private NativeImage image;

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        // don't duplicate download requests, silly
        if (this.future != null)
            return;

        // first, load loading texture
        this.loadFromPack(resourceManager, this.loadingTexture);

        // HTTP request (based on HttpTexture.load)
        this.future = CompletableFuture.runAsync(() -> {
            // Check cache
            boolean loadedCache = false;
            try {
                loadedCache = this.loadCacheFile();
            } catch (IOException e) {
                Logging.getInstance().error("Failed to load cosmetica texture cache", e);
            }
            if (loadedCache)
                return;

            // failed to load cache - use internet
            HttpURLConnection httpURLConnection = null;
            Logging.getInstance().debug(LoggingCategory.ASSETS, "Downloading cosmetica texture from {} to {}", this.url, this.cacheFile);

            try {
                httpURLConnection = (HttpURLConnection)new URL(this.url).openConnection(Minecraft.getInstance().getProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    InputStream rawInputStream = httpURLConnection.getInputStream();

                    if (this.cacheFile == null) {
                        AnimatedInputStream ais = readToPNG(rawInputStream, this.cacheFile.getName(), this.heightDivider);

                        IOException e_ = null;
                        NativeImage directRead_ = null;
                        try {
                            directRead_ = NativeImage.read(ais.stream);
                        } catch (IOException ex) {
                            e_ = ex;
                        }
                        final IOException e = e_;
                        final NativeImage directRead = directRead_;

                        Minecraft.getInstance().execute(() -> {
                            if (e == null) {
                                this.firstUpload(directRead, true, tilesheetFrames * ais.frames, ais.frames == 1 ? this.tilesheetIncrement : tilesheetFrames);
                            } else {
                                Logging.getInstance().error("Couldn't download cosmetica texture", e);
                                if (this.errorTexture != null) {
                                    try {
                                        this.loadFromPack(resourceManager, this.errorTexture);
                                    } catch (IOException ex) {
                                        Logging.getInstance().error("Couldn't load fallback texture", ex);
                                    }
                                }
                            }
                        });
                    } else {
                        FileUtils.copyInputStreamToFile(rawInputStream, this.cacheFile);
                        this.loadCacheFile();
                    }
                } else {
                    throw new HttpResponseException(httpURLConnection.getResponseCode(), "Reading texture from " + this.url);
                }
            } catch (Exception var6) {
                Logging.getInstance().error("Couldn't download cosmetica texture", var6);
                try {
                    if (this.errorTexture != null) {
                        this.loadFromPack(resourceManager, this.errorTexture);
                    }
                } catch (IOException ex) {
                    Logging.getInstance().error("Couldn't load fallback texture", ex);
                }
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        }, Util.backgroundExecutor());
    }

    private void loadFromPack(ResourceManager resourceManager, ResourceLocation location) throws IOException {
        // we use SimpleTexture-based code to upload the loading/fallback texture
        TextureImage defaultImage = load(resourceManager, location);
        final NativeImage nativeImage = defaultImage.image;
        final int nextFrames = defaultImage.frames;
        final int nextFrameInc = 1;
        final boolean usedCache = false;

        // upload call
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            if (this.image == null) this.image = nativeImage; // just in case
            RenderSystem.recordRenderCall(() -> this.firstUpload(nativeImage, usedCache, nextFrames, nextFrameInc));
        } else {
            this.firstUpload(nativeImage, usedCache, nextFrames, nextFrameInc);
        }
    }

    private boolean loadCacheFile() throws IOException {
        if (RenderSystem.isOnRenderThread()) {
            Logging.getInstance().warn("(Cosmetica) loadFromDisk called from render thread! May cause lag!");
        }

        if (this.cacheFile != null && this.cacheFile.isFile()) {
            Logging.getInstance().debug(LoggingCategory.ASSETS, "Loading cosmetica texture from local cache ({})", this.cacheFile);

            FileInputStream fileInputStream = new FileInputStream(this.cacheFile);

            NativeImage nativeImage1 = null;
            int trueFrames = 1;
            try {
                AnimatedInputStream inputStream = readToPNG(fileInputStream, this.cacheFile.getName(), this.heightDivider);
                nativeImage1 = NativeImage.read(inputStream.stream);
                trueFrames = inputStream.frames;
            } catch (IOException e) {
                Logging.getInstance().error("Error reading cached texture at {}", e, this.cacheFile);
            }

            if (nativeImage1 != null) {
                // success
                final NativeImage nativeImage = nativeImage1;
                final int nextFrames = tilesheetFrames * trueFrames;
                // prioritise the 'true' animation for auto-animation
                final int nextFrameInc = trueFrames == 1 ? this.tilesheetIncrement : tilesheetFrames;

                // upload
                RenderSystem.recordRenderCall(() -> this.firstUpload(nativeImage, true, nextFrames, nextFrameInc));
                return true;
            }
        }

        // failed to load or no cache
        return false;
    }

    private void firstUpload(NativeImage image, boolean trueImage, int nextFrames, int nextFrameInc) {
        this.image = image;
        this.currentTicksPerFrame = trueImage ? this.realTicksPerFrame : 2;
        this.currentFrames = nextFrames;
        this.autoFrameInc = nextFrameInc;
        this.frameHeight = this.currentFrames == 0 ? image.getHeight() : image.getHeight() / this.currentFrames;
        this.frame = 0;
        this.upload(image, false);
        if (trueImage) {
            this.future = null;
            this.onFirstUpload.accept(image);
        }
    }

    private void upload(NativeImage image, boolean close) {
        TextureUtil.prepareImage(this.getId(), 0, image.getWidth(), this.frameHeight);
        image.upload(0, 0, 0, 0, this.frameHeight * this.frame, image.getWidth(), this.frameHeight, this.blur, false, false, close);
    }

    void doTick() {
        if (this.currentFrames > 1 && this.autoFrameInc >= 1 && this.image != null && ((NativeImageAccessorMixin) (Object) this.image).getPixels() != 0) {
            this.tick = (this.tick + 1) % this.currentTicksPerFrame;

            if (this.tick == 0) {
                this.frame = (this.frame + this.autoFrameInc) % this.currentFrames;
                //Debug.info("Uploading frame {}", this.frame);
                this.upload(this.image, false);
            }
        }
    }

    /**
     * Load and upload the given animation frame.
     * @param frame the frame index to load.
     */
    public void loadFrame(int frame) {
        if (frame < 0 || frame >= this.currentFrames)
            throw new IllegalArgumentException("Frame out of bounds for " + this.currentFrames + ": " + frame);
        if (!RenderSystem.isOnRenderThreadOrInit())
            throw new IllegalStateException("Not on render thread or init!");
        this.frame = frame;
        this.upload(this.image, false);
    }

    @Override
    public void close() {
        //Debug.info("Closing image on thread {} due to dispose. Are we allowed? {}", Thread.currentThread(), RenderSystem.isOnRenderThreadOrInit());
        Logging.getInstance().debug(LoggingCategory.ASSETS, "Closing image {}", this.url);
        if (this.image != null) this.image.close();
        //Debug.info("Disposed of image.");
    }

    // getters
    /**
     * Get the current image object associated with this http texture. This will be the full http texture if loaded,
     * otherwise the loading image.
     * @return the current image this http texture is using.
     */
    public NativeImage getCurrentImage() {
        return this.image;
    }

    public int getFrameHeight() {
        return this.frameHeight;
    }

    public int getFrameCount() {
        return this.tilesheetFrames;
    }

    private static class AnimatedInputStream {
        AnimatedInputStream(InputStream stream, int frames) {
            this.stream = stream;
            this.frames = frames;
        }

        final InputStream stream;
        final int frames;
    }

    /**
     * Convert any input source to PNG.
     * @param imageSource the image source.
     * @param str string for debug.
     * @param proportionalHeight the amount to divide the canvas height by per animated frame.
     * @return an input stream for a PNG image.
     */
    private static AnimatedInputStream readToPNG(InputStream imageSource, String str, int proportionalHeight) throws IOException {
        if (!imageSource.markSupported()) {
            // make mark supported by wrapping in buffered input stream
            imageSource = new BufferedInputStream(imageSource);
        }

        // if ^ this ever throws, wrap unsupported streams in buffered input stream
        // as it stands, the method should always be passed a buffered input stream anyway

        imageSource.mark(8);
        byte[] magic = new byte[8];
        boolean png = imageSource.read(magic) == magic.length;

        if (png) {
            png = magic[0] == (byte)0x89
                    && magic[1] == (byte)0x50
                    && magic[2] == (byte)0x4e
                    && magic[3] == (byte)0x47
                    && magic[4] == (byte)0x0d
                    && magic[5] == (byte)0x0a
                    && magic[6] == (byte)0x1a
                    && magic[7] == (byte)0x0a;
        }

        // yaahh rewind time
        imageSource.reset();

        if (png) {
            // NativeImage can read a png
            return new AnimatedInputStream(imageSource, 1);
        } else {
            Logging.getInstance().debug(LoggingCategory.ASSETS, "(Cosmetica Texture) Image is not a PNG. Applying transformation.");

            // If webp, use canvas size instead of frame size
            imageSource.mark(VP8X.MARK_LIMIT);
            Optional<int[]> webpDimensions = VP8X.getWebpDimensions(imageSource);
            imageSource.reset();

            // Transform other formats to png and flatten animations (especially webp, used by Cosmetica for thumbnails)
            // https://github.com/haraldk/TwelveMonkeys?tab=readme-ov-file#advanced-usage
            // https://codingtechroom.com/question/convert-anime-gif-frames-to-bufferedimage-java
            BufferedImage flattened;
            final int frames;

            try (ImageInputStream input = ImageIO.createImageInputStream(imageSource)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new IllegalArgumentException("No reader for input");
                }

                ImageReader reader = readers.next();
                reader.setInput(input);
                frames = reader.getNumImages(true);
                BufferedImage image0 = reader.read(0);

                final int canvasW = webpDimensions.map(v -> v[0]).orElse(image0.getWidth());
                final int canvasH = webpDimensions.map(v -> v[1]).orElse(image0.getHeight()) / proportionalHeight;

                if (frames <= 1) {
                    if (image0.getWidth() == canvasW && image0.getHeight() == canvasH) {
                        flattened = image0;
                    } else {
                        flattened = new BufferedImage(canvasW, canvasH * frames, BufferedImage.TYPE_INT_ARGB);
                        Graphics g = flattened.getGraphics();
                        g.drawImage(image0, 0, 0, image0.getWidth(), image0.getHeight(), null);
                        g.dispose();
                    }
                } else {
                    // flatten
                    flattened = new BufferedImage(canvasW, canvasH * frames, BufferedImage.TYPE_INT_ARGB);
                    // Yes, this is the fastest method. It's hardware accelerated!
                    // https://stackoverflow.com/questions/3175820/fastest-way-to-draw-bufferedimages-to-another-bufferedimage
                    Graphics g = flattened.getGraphics();
                    g.drawImage(image0,
                            0, 0, image0.getWidth(), Math.min(image0.getHeight(), canvasH),
                            0, 0, image0.getWidth(), Math.min(image0.getHeight(), canvasH),
                            null);

                    // draw remaining frames
                    for (int frame = 1, parsedFrame = 1; frame < frames; frame++) {
                        BufferedImage imageFrame = reader.read(frame);
                        g.drawImage(imageFrame,
                                0, parsedFrame * canvasH, imageFrame.getWidth(), parsedFrame * canvasH + Math.min(imageFrame.getHeight(), canvasH),
                                0, 0,                     imageFrame.getWidth(), Math.min(imageFrame.getHeight(), canvasH),
                                null);
                        parsedFrame++;
                    }
                    g.dispose();
                }
            }

            // write image PNG to byte array and read to get png
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(flattened, "png", os);
            // debug
            if (Boolean.getBoolean("cosmetica.debugDumpImageConversions")) {
                new File("./cosmetica-debug").mkdir();
                ImageIO.write(flattened, "png", new File("./cosmetica-debug/" + str + "-"+ frames + ".png"));
            }

            return new AnimatedInputStream(new ByteArrayInputStream(os.toByteArray()), frames);
        }
    }

    // Based on SimpleTexture.TextureImage.load
    private static TextureImage load(ResourceManager resourceManager, ResourceLocation resourceLocation) throws IOException {
        TextureImage textureImage = new TextureImage();

        try (Resource resource = resourceManager.getResource(resourceLocation)) {
            NativeImage nativeImage = NativeImage.read(resource.getInputStream());

            try {
                AnimationMetadataSection textureMetadataSection = resource.getMetadata(AnimationMetadataSection.SERIALIZER);
                if (textureMetadataSection == null)
                    textureMetadataSection = AnimationMetadataSection.EMPTY;
                textureImage.frames = textureMetadataSection.getFrameCount();
            } catch (RuntimeException ex) {
                Logging.getInstance().warn("Failed reading metadata of cosmetica texture: {}", resourceLocation, ex);
            }

            textureImage.image = nativeImage;
        }

        return textureImage;
    }

    private static class TextureImage {
        private NativeImage image;
        private int frames;
    }

    /**
     * Ticking version of CosmeticaTexture.
     * Exists so we can use all the utilities that CosmeticaHttpTexture adds to HttpTexture for static textures, without
     * adding the unnecessary overhead of ticking every static texture (which will be most textures).
     */
    private static class Animated extends CosmeticaTexture implements Tickable {
        private Animated(File file, String url, ResourceLocation loadingTexture, @Nullable ResourceLocation errorTexture,
                         int frames, int ticksPerFrame, Consumer<NativeImage> onLoad, int tilesheetAnimInc, int heightDivider) throws IllegalArgumentException {
            super(file, url, loadingTexture, errorTexture, frames, ticksPerFrame, onLoad, heightDivider);
            this.tilesheetIncrement = tilesheetAnimInc;
        }

        @Override
        public void tick() {
            this.doTick();
        }
    }

    public enum AutoAnimate {
        /**
         * Always animate anything. True animations are prioritised as primary animations over tilesheets.
         */
        ALWAYS,
        /**
         * Always animate, but only if tilesheet frames given is > 1.
         */
        AUTO, // TODO maybe this shouldn't exist. Could cause too much confusion?
        /**
         * Always animate true animations but never tilesheets. Excluding resource pack animations.
         */
        NEVER_TILESHEETS,
        /**
         * Never animate automatically.
         */
        NEVER
    }

    /**
     * Builder class for creating a {@link CosmeticaTexture} instance.
     */
    public static class Builder {
        // Required
        private final @NotNull String url;
        private final @NotNull ResourceLocation loadingTexture;

        // Optional fields with default values
        private File file;
        private int frames = 1;
        private int ticksPerFrame = 1;
        private boolean ignoreTilesheet = false;
        private Consumer<NativeImage> onLoad;
        private AutoAnimate autoAnimate = AutoAnimate.AUTO;
        private @Nullable ResourceLocation errorTexture;

        /**
         * Constructs a new Builder instance.
         *
         * @param url The URL to retrieve the texture from.
         */
        public Builder(@NotNull String url, @NotNull ResourceLocation loadingTexture) {
            Objects.requireNonNull(url, "URL cannot be null");
            Objects.requireNonNull(loadingTexture, "Loading texture cannot be null");
            this.url = url;
            this.loadingTexture = loadingTexture;
            this.errorTexture = null;
        }

        /**
         * Get the URL this builder's texture will point to.
         */
        public String getURL() {
            return this.url;
        }

        /**
         * Set the error texture, should the texture fail to load.
         * @param errorTexture the error texture.
         * @return This Builder instance.
         */
        public Builder failToLoadTexture(@Nullable ResourceLocation errorTexture) {
            this.errorTexture = errorTexture;
            return this;
        }

        /**
         * Sets the number of tilesheet frames and ticks per frame for the animated texture.
         * Both frames and ticksPerFrame must be positive.
         *
         * @param frames        Number of frames in the animated texture's tilesheet. This excludes frames
         *                      from a truly animated image. For a truly animated texture, the final frame
         *                      count is {@code frames * animationFrames}.
         * @param ticksPerFrame Game ticks per frame to show. Unused if there is only one frame.
         * @return This Builder instance.
         * @throws IllegalArgumentException if frames or ticksPerFrame are not positive.
         */
        public Builder frames(int frames, int ticksPerFrame) {
            if (frames <= 0 || ticksPerFrame <= 0) {
                throw new IllegalArgumentException("frames and ticksPerFrame must be positive");
            }

            this.frames = frames;
            this.ticksPerFrame = ticksPerFrame;
            return this;
        }

        /**
         * For non-png textures, ignore the tilesheet when converting to a PNG.
         * @return This Builder instance.
         */
        public Builder ignoreTilesheet(boolean ignore) {
            this.ignoreTilesheet = ignore;
            return this;
        }

        /**
         * Get the current ticks per frame setting of this builder.
         * @return the current ticks per frame setting.
         */
        public int getTicksPerFrame() {
            return this.ticksPerFrame;
        }

        /**
         * Get the current frame count setting of this builder.
         * @return the current frame count setting.
         */
        public int getFrames() {
            return this.frames;
        }

        /**
         * Set whether this texture should automatically animate with multiple frames. AUTO by default.
         * Automatic animations will prioritise true animations over tilesheet animations.
         * @return This Builder instance.
         */
        public Builder autoAnimate(AutoAnimate auto) {
            this.autoAnimate = auto;
            return this;
        }

        /**
         * Sets the file to cache the texture in.
         *
         * @param file The file to cache the texture in.
         * @return This Builder instance.
         */
        public Builder cached(File file) {
            this.file = file;
            return this;
        }

        /**
         * Sets the task to run when the texture is loaded.
         *
         * @param onLoad The task to run when the texture is loaded.
         * @return This Builder instance.
         */
        public Builder onLoad(Consumer<NativeImage> onLoad) {
            this.onLoad = onLoad;
            return this;
        }

        /**
         * Constructs and returns an {@link CosmeticaTexture} instance with the configured parameters.
         * @return An {@link CosmeticaTexture} instance.
         */
        public CosmeticaTexture build() {
            // Create and return AnimatedHttpTexture instance
            if ((this.frames < 2 && this.autoAnimate == AutoAnimate.AUTO) || this.autoAnimate == AutoAnimate.NEVER) {
                return new CosmeticaTexture(file, url, loadingTexture, errorTexture, ignoreTilesheet ? 1 : frames, ticksPerFrame, onLoad, ignoreTilesheet ? frames : 1);
            } else {
                return new Animated(file, url, loadingTexture, errorTexture, ignoreTilesheet ? 1 : frames, ticksPerFrame, onLoad, this.autoAnimate == AutoAnimate.NEVER_TILESHEETS ? 0 : 1, ignoreTilesheet ? frames : 1);
            }
        }
    }
}
