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
import cc.cosmetica.core.util.VP8X;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.HttpResponseException;
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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CosmeticaTexture extends AbstractTexture {
    private CosmeticaTexture(File file, String url, Identifier loadingTexture, @Nullable Identifier errorTexture,
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

        Minecraft.getInstance().execute(this::load);
    }

    private final File cacheFile;
    private final String url;
    private final Identifier loadingTexture;
    private final @Nullable Identifier errorTexture;
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
    private GpuTexture image;
    private NativeImage imageCPU;

    private void load() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        try {
            this.load(resourceManager);
        } catch (IOException e) {
            Logging.getInstance().error("Error loading Cosmetica texture", e);
        }
    }

    private void load(ResourceManager resourceManager) throws IOException {
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
                final int responseCode = httpURLConnection.getResponseCode();

                if (responseCode / 100 == 2) {
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
                    StringBuilder message = new StringBuilder("Error code ")
                            .append(responseCode)
                            .append(" reading texture from ")
                            .append(this.url);
                    try {
                        InputStream errorStream = httpURLConnection.getErrorStream();

                        if (errorStream != null) {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                                message.append('\n');
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    message.append(line).append('\n');
                                }
                            }
                        }
                    } catch (IOException e) {
                        Logging.getInstance().warn("Error reading error message from server? ", e);
                    }
                    throw new HttpResponseException(responseCode, message.toString());
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
        }, BACKGROUND_TASK_EXECUTOR);
    }

    private void loadFromPack(ResourceManager resourceManager, Identifier location) throws IOException {
        // we use SimpleTexture-based code to upload the loading/fallback texture
        TextureImage defaultImage = load(resourceManager, location);
        final NativeImage nativeImage = defaultImage.image;
        final int nextFrames = defaultImage.frames;
        final int nextFrameInc = 1;
        final boolean usedCache = false;

        // upload call
        if (!RenderSystem.isOnRenderThread()) {
            Minecraft.getInstance().execute(() -> this.firstUpload(nativeImage, usedCache, nextFrames, nextFrameInc));
        } else {
            this.firstUpload(nativeImage, usedCache, nextFrames, nextFrameInc);
        }
    }

    private boolean loadCacheFile() throws IOException {
        if (RenderSystem.isOnRenderThread()) {
            Logging.getInstance().warn("(Cosmetica) loadCacheFile called from render thread! May cause lag!");
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
                Minecraft.getInstance().execute(() -> this.firstUpload(nativeImage, true, nextFrames, nextFrameInc));
                return true;
            }
        }

        // failed to load or no cache
        return false;
    }

    private void firstUpload(NativeImage image, boolean trueImage, int nextFrames, int nextFrameInc) {
        GpuDevice gpuDevice = RenderSystem.getDevice();

        // load full texutre on the gpu
        // usage: 5. Bitflags representing allowed usages?
        this.image = gpuDevice.createTexture((String)null, 5, GpuFormat.RGBA8_UNORM, image.getWidth(), image.getHeight(), 1, 1);
        // storing the image twice is easier than trying to hack SpriteContents to use GPU images
        // See note at bottom of method
        this.imageCPU = image;

        // e
        this.currentTicksPerFrame = trueImage ? this.realTicksPerFrame : 2;
        this.currentFrames = nextFrames;
        this.autoFrameInc = nextFrameInc;
        this.frameHeight = this.currentFrames == 0 ? image.getHeight() : image.getHeight() / this.currentFrames;
        this.frame = 0;

        this.texture = gpuDevice.createTexture((String)null, 5, GpuFormat.RGBA8_UNORM, image.getWidth(), this.frameHeight, 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = gpuDevice.createTextureView(this.texture);

        this.upload(false);
        if (trueImage) {
            this.future = null;
            this.onFirstUpload.accept(image);
        }

        // FIXME Good future contribution: Make code work only storing one copy of the image (CPU or GPU) without losing crazy efficiency
        // Would creating and destroying the GPU image for each texture each frame be a big overhead?
//        image.close();
    }

    private void upload(boolean close) {
        if (this.image == null) {
            throw new IllegalStateException("Tried to upload texture but image is null");
        } else if (this.texture == null) {
            Logging.getInstance().warnOnce("texture upload", "Tried to upload texture but no texture");
        } else {
            GpuDevice gpuDevice = RenderSystem.getDevice();
            CommandEncoder gpuCommands = gpuDevice.createCommandEncoder();

            gpuCommands.copyTextureToTexture(
                    this.image, // source
                    this.texture, // destination
                    0,
                    // dest X, Y; source X, Y
                    0, 0,
                    0, this.frameHeight * this.frame,
                    image.getWidth(0), this.frameHeight);

            if (close) {
                this.image.close();
                this.imageCPU.close();
            }
        }
    }

    void doTick() {
        if (this.currentFrames > 1 && this.autoFrameInc >= 1 && this.image != null && !this.image.isClosed()) {
            this.tick = (this.tick + 1) % this.currentTicksPerFrame;

            if (this.tick == 0) {
                this.frame = (this.frame + this.autoFrameInc) % this.currentFrames;
                //Debug.info("Uploading frame {}", this.frame);
                this.upload(false);
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
        if (!RenderSystem.isOnRenderThread())
            throw new IllegalStateException("Not on render thread!");
        this.frame = frame;
        this.upload(false);
    }

    @Override
    public void close() {
        //Debug.info("Closing image on thread {} due to dispose. Are we allowed? {}", Thread.currentThread(), RenderSystem.isOnRenderThreadOrInit());
        Logging.getInstance().debug(LoggingCategory.ASSETS, "Closing image {}", this.url);
        if (this.image != null) {
            this.image.close();
            this.imageCPU.close();

            this.image = null;
            this.imageCPU = null;
        }
        //Debug.info("Disposed of image.");
    }

    // getters
    /**
     * Get the current image object associated with this http texture. This will be the full http texture if loaded,
     * otherwise the loading image.
     * @return the current image this http texture is using.
     */
    public GpuTexture getCurrentImageGpu() {
        return this.image;
    }

    /**
     * Get the current image object associated with this http texture. This will be the full http texture if loaded,
     * otherwise the loading image.
     * @return the current image this http texture is using.
     */
    public NativeImage getCurrentImage() {
        return this.imageCPU;
    }

    public int getFrameHeight() {
        return this.frameHeight;
    }

    public int getFrameCount() {
        return this.tilesheetFrames;
    }

    private static final ExecutorService BACKGROUND_TASK_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
        if (RenderSystem.isOnRenderThread()) {
            Logging.getInstance().warn("Converting image formats on render thread! This will cause lag.");
        }

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
            Optional<int[]> canvasDimensions = VP8X.getWebpDimensions(imageSource);
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

                List<FrameMetaData> metaDataList = null;

                if (reader instanceof FrameMetadataHolder) {
                    Optional<int[]> canvasDimensions2 = ((FrameMetadataHolder) reader).getCanvasDimensions();
                    if (canvasDimensions2.isPresent()) {
                        canvasDimensions = canvasDimensions2;
                    }
                    metaDataList = ((FrameMetadataHolder) reader).getFrameMetadata();
                }

                final int canvasW = canvasDimensions.map(v -> v[0]).orElse(image0.getWidth());
                final int canvasH = canvasDimensions.map(v -> v[1]).orElse(image0.getHeight()) / proportionalHeight;

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

                    // draw remaining frames
                    BufferedImage lastFrame = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
                    for (int frame = 0; frame < frames; frame++) {
                        BufferedImage imageFrame = frame == 0 ? image0 : reader.read(frame);

                        if (metaDataList != null && frame < metaDataList.size()) {
                            FrameMetaData metaData = metaDataList.get(frame);

                            // composite behaviour
                            Graphics2D frameGraphics = lastFrame.createGraphics();
                            if (!metaData.blend) {
                                frameGraphics.setComposite(AlphaComposite.Clear);
                                frameGraphics.fillRect(metaData.bounds.x, metaData.bounds.y, imageFrame.getWidth(), imageFrame.getHeight());
                                frameGraphics.setComposite(AlphaComposite.SrcOver);
                            }
                            frameGraphics.drawImage(imageFrame, metaData.bounds.x, metaData.bounds.y, null);
                            frameGraphics.dispose();
                            imageFrame = lastFrame;
                        }
                        lastFrame = imageFrame;

                        g.drawImage(imageFrame,
                                0, frame * canvasH, imageFrame.getWidth(), frame * canvasH + Math.min(imageFrame.getHeight(), canvasH),
                                0, 0, imageFrame.getWidth(), Math.min(imageFrame.getHeight(), canvasH),
                                null);

                        if (metaDataList != null && frame < metaDataList.size()) {
                            FrameMetaData metaData = metaDataList.get(frame);
                            Graphics2D frameGraphics = lastFrame.createGraphics();

                            if (metaData.dispose) {
                                frameGraphics.setComposite(AlphaComposite.Clear);
                                frameGraphics.fillRect(metaData.bounds.x, metaData.bounds.y, imageFrame.getWidth(), imageFrame.getHeight());
                            }
                            frameGraphics.dispose();
                        }
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
    private static TextureImage load(ResourceManager resourceManager, Identifier resourceLocation) throws IOException {
        TextureImage textureImage = new TextureImage();

        Resource resource = resourceManager.getResourceOrThrow(resourceLocation);
        NativeImage nativeImage;
        try (InputStream stream = resource.open()) {
            nativeImage = NativeImage.read(stream);
        }

        try {
            Optional<AnimationMetadataSection> textureMetadataSection = resource.metadata()
                    .getSection(AnimationMetadataSection.TYPE);

            textureImage.frames = textureMetadataSection.flatMap(AnimationMetadataSection::frames).map(List::size).orElse(1);
        } catch (RuntimeException ex) {
            Logging.getInstance().warn("Failed reading metadata of cosmetica texture: {}", resourceLocation, ex);
        }

        textureImage.image = nativeImage;
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
    private static class Animated extends CosmeticaTexture implements TickableTexture {
        private Animated(File file, String url, Identifier loadingTexture, @Nullable Identifier errorTexture,
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
        private final @NotNull Identifier loadingTexture;

        // Optional fields with default values
        private File file;
        private int frames = 1;
        private int ticksPerFrame = 1;
        private boolean ignoreTilesheet = false;
        private Consumer<NativeImage> onLoad;
        private AutoAnimate autoAnimate = AutoAnimate.AUTO;
        private @Nullable Identifier errorTexture;

        /**
         * Constructs a new Builder instance.
         *
         * @param url The URL to retrieve the texture from.
         */
        public Builder(@NotNull String url, @NotNull Identifier loadingTexture) {
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
        public Builder failToLoadTexture(@Nullable Identifier errorTexture) {
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
