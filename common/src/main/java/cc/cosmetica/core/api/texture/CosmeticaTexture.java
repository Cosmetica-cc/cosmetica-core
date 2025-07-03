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
import cc.cosmetica.core.mixin.texture.NativeImageAccessorMixin;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CosmeticaTexture extends AbstractTexture {
    private CosmeticaTexture(File file, String url, ResourceLocation loadingTexture, @Nullable ResourceLocation errorTexture,
                                    int frames, int ticksPerFrame, Consumer<NativeImage> onFirstUpload)
            throws IllegalArgumentException {
        if (frames > 1 && ticksPerFrame == 0) {
            throw new IllegalArgumentException("Animated texture (" + frames + " frames) but ticks per frame is 0!");
        }

        // properties
        this.cacheFile = file;
        this.url = url;
        this.realFrames = frames;
        this.realTicksPerFrame = ticksPerFrame;
        this.currentFrames = 0;
        this.currentTicksPerFrame = 2;
        this.onFirstUpload = onFirstUpload;
        this.loadingTexture = loadingTexture;
        this.errorTexture = errorTexture;
    }

    private final File cacheFile;
    private final String url;
    private final ResourceLocation loadingTexture, errorTexture;
    private final int realFrames;
    private final int realTicksPerFrame;
    private final Consumer<NativeImage> onFirstUpload;
    @Nullable private CompletableFuture<?> future;

    private int frameHeight;
    private int frame;
    private int currentFrames, currentTicksPerFrame;
    private int tick;
    private NativeImage image;

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        // don't duplicate download requests, silly
        if (this.future != null)
            return;

        // first, load from local cache
        boolean loadedCache = this.loadFromDisk(resourceManager, false);

        if (!loadedCache) {
            // HTTP request (based on HttpTexture.load)
            this.future = CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection = null;
                Logging.getInstance().debug("Downloading cosmetica texture from {} to {}", this.url, this.cacheFile);

                try {
                    httpURLConnection = (HttpURLConnection)new URL(this.url).openConnection(Minecraft.getInstance().getProxy());
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(false);
                    httpURLConnection.connect();
                    if (httpURLConnection.getResponseCode() / 100 == 2) {
                        InputStream rawInputStream = httpURLConnection.getInputStream();

                        if (this.cacheFile == null) {
                            Minecraft.getInstance().execute(() -> {
                                try {
                                    NativeImage directRead = NativeImage.read(readAnyImage(rawInputStream));
                                    this.firstUpload(this.image = directRead, true, realFrames);
                                } catch (IOException e) {
                                    Logging.getInstance().error("Couldn't download cosmetica texture", e);
                                }
                            });
                        } else {
                            FileUtils.copyInputStreamToFile(rawInputStream, this.cacheFile);
                            this.loadFromDisk(resourceManager, true);
                        }
                    }
                } catch (Exception var6) {
                    Logging.getInstance().error("Couldn't download cosmetica texture", var6);
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }, Util.backgroundExecutor());
        }
    }

    private boolean loadFromDisk(ResourceManager resourceManager, boolean done) throws IOException {
        ResourceLocation fallback = done && this.errorTexture != null ? this.errorTexture : this.loadingTexture;

        final boolean usedCache;
        final NativeImage nativeImage;
        final int nextFrames;

        if (this.cacheFile != null && this.cacheFile.isFile()) {
            Logging.getInstance().debug("Loading cosmetica texture from local cache ({})", this.cacheFile);

            FileInputStream fileInputStream = new FileInputStream(this.cacheFile);

            NativeImage nativeImage1 = null;
            try {
                nativeImage1 = NativeImage.read(readAnyImage(fileInputStream));
            } catch (IOException e) {
                Logging.getInstance().error("Error reading cached texture at {}", e, this.cacheFile);
            }

            if (nativeImage1 == null) {
                // we use SimpleTexture-based code to upload the fallback texture
                TextureImage defaultImage = load(resourceManager, fallback);
                nativeImage = defaultImage.image;
                nextFrames = defaultImage.frames;
                usedCache = false;
            } else {
                // success
                nativeImage = nativeImage1;
                nextFrames = realFrames;
                usedCache = true;
            }
        } else {
            // we use SimpleTexture-based code to upload the loading texture
            TextureImage defaultImage = load(resourceManager, fallback);
            nativeImage = defaultImage.image;
            nextFrames = defaultImage.frames;
            usedCache = false;
        }

        Objects.requireNonNull(nativeImage, "NativeImage null? ('impossible' data flow)");
        this.image = nativeImage;

        // upload call
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.firstUpload(nativeImage, usedCache, nextFrames));
        } else {
            this.firstUpload(nativeImage, usedCache, nextFrames);
        }

        return usedCache;
    }

    private void firstUpload(NativeImage image, boolean trueImage, int nextFrames) {
        this.currentTicksPerFrame = trueImage ? this.realTicksPerFrame : 2;
        this.currentFrames = nextFrames;
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
        if (this.currentFrames > 1 && this.image != null && ((NativeImageAccessorMixin) (Object) this.image).getPixels() != 0) {
            this.tick = (this.tick + 1) % this.currentTicksPerFrame;

            if (this.tick == 0) {
                this.frame = (this.frame + 1) % this.currentFrames;
                //Debug.info("Uploading frame {}", this.frame);
                this.upload(this.image, false);
            }
        }
    }

    /**
     * Load the current animation frame.
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
        Logging.getInstance().debug("Closing image {}", this.url);
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
        return this.realFrames;
    }

    /**
     * Convert any input source to PNG.
     * @param imageSource the image source.
     * @return an input stream for a PNG image.
     */
    private static InputStream readAnyImage(InputStream imageSource) throws IOException {
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
            return imageSource;
        } else {
            Logging.getInstance().debug("(Cosmetica Texture) Image is not a PNG. Applying transformation.");
            // Transform other formats to png and flatten animations (especially webp, used by Cosmetica for thumbnails)
            // https://github.com/haraldk/TwelveMonkeys?tab=readme-ov-file#advanced-usage
            // https://codingtechroom.com/question/convert-anime-gif-frames-to-bufferedimage-java
            BufferedImage flattened;

            try (ImageInputStream input = ImageIO.createImageInputStream(imageSource)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new IllegalArgumentException("No reader for input");
                }

                ImageReader reader = readers.next();
                reader.setInput(input);
                final int frames = reader.getNumImages(true);
                BufferedImage image0 = reader.read(0);

                if (frames < 2) {
                    flattened = image0;
                } else {
                    final int w = image0.getWidth();
                    final int h = image0.getHeight();
                    // flatten
                    flattened = new BufferedImage(w, h * frames, BufferedImage.TYPE_INT_ARGB);
                    // Yes, this is the fastest method. It's hardware accelerated!
                    // https://stackoverflow.com/questions/3175820/fastest-way-to-draw-bufferedimages-to-another-bufferedimage
                    Graphics g = flattened.getGraphics();
                    g.drawImage(image0, 0, 0, w, h, null);

                    // draw remaining frames
                    for (int frame = 1; frame < frames; frame++) {
                        g.drawImage(reader.read(frame), 0, frame * h, w, h, null);
                    }
                }
            }

            // write image PNG to byte array and read to get png
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(flattened, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
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
        private Animated(File file, String url, ResourceLocation loadingTexture, @Nullable ResourceLocation errorTexture, int frames, int ticksPerFrame, Consumer<NativeImage>  onLoad) throws IllegalArgumentException {
            super(file, url, loadingTexture, errorTexture, frames, ticksPerFrame, onLoad);
        }

        @Override
        public void tick() {
            this.doTick();
        }
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
        private Consumer<NativeImage> onLoad;
        private boolean autoAnimate = true;
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
         * Set the error texture, should the texture fail to load.
         * @param errorTexture the error texture.
         * @return This Builder instance.
         */
        public Builder failToLoadTexture(@Nullable ResourceLocation errorTexture) {
            this.errorTexture = errorTexture;
            return this;
        }

        /**
         * Sets the number of frames and ticks per frame for the animated texture.
         * Both frames and ticksPerFrame must be positive.
         *
         * @param frames        Number of frames in the animated texture.
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
         * Set whether this texture should automatically animate with multiple frames. On by default.
         * @return This Builder instance.
         */
        public Builder autoAnimate(boolean auto) {
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
        public Builder onLoad(Consumer<NativeImage>  onLoad) {
            this.onLoad = onLoad;
            return this;
        }

        /**
         * Constructs and returns an {@link CosmeticaTexture} instance with the configured parameters.
         * @return An {@link CosmeticaTexture} instance.
         */
        public CosmeticaTexture build() {
            // Create and return AnimatedHttpTexture instance
            if (this.frames > 1 && this.autoAnimate) {
                return new Animated(file, url, loadingTexture, errorTexture, frames, ticksPerFrame, onLoad);
            } else {
                return new CosmeticaTexture(file, url, loadingTexture, errorTexture, frames, ticksPerFrame, onLoad);
            }
        }
    }
}
