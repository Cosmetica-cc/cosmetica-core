package cc.cosmetica.core.render.texture;

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
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CosmeticaTexture extends AbstractTexture {
    private CosmeticaTexture(File file, String url, ResourceLocation loadingTexture,
                                    int frames, int ticksPerFrame, Consumer<NativeImage> onFirstUpload)
            throws IllegalArgumentException {
        if (frames > 1 && ticksPerFrame == 0) {
            throw new IllegalArgumentException("Animated texture (" + frames + " frames) but ticks per frame is 0!");
        }

        // properties
        this.cacheFile = file;
        this.url = url;
        this.realFrames = frames;
        this.currentFrames = 0;
        this.ticksPerFrame = ticksPerFrame;
        this.onFirstUpload = onFirstUpload;
        this.loadingTexture = loadingTexture;
    }

    private final File cacheFile;
    private final String url;
    private final ResourceLocation loadingTexture;
    private final int realFrames;
    private final int ticksPerFrame;
    private final Consumer<NativeImage> onFirstUpload;
    @Nullable private CompletableFuture<?> future;

    private int frameHeight;
    private int frame;
    private int currentFrames;
    private int tick;
    private NativeImage image;

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        // don't duplicate download requests, silly
        if (this.future != null)
            return;

        // first, load from local cache
        boolean loadedCache = this.loadFromDisk(resourceManager);

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

                        // get url file extension
                        String extension;
                        {
                            String[] parts = this.url.split("/");
                            parts = parts[parts.length - 1].split("\\.");
                            if (parts.length == 0)
                                extension = null;
                            else
                                extension = parts[parts.length - 1];
                        }

                        InputStream inputStream;

                        // determine if not png
                        if (extension != null && !"png".equals(extension)) {
                            Logging.getInstance().debug("(Cosmetica Texture) Image is not a PNG (ext {}). Applying transformation", extension);
                            // Transform other formats to png (especially webp, used by Cosmetica for thumbnails)
                            // https://github.com/haraldk/TwelveMonkeys?tab=readme-ov-file#advanced-usage
                            BufferedImage image;

                            try (ImageInputStream input = ImageIO.createImageInputStream(rawInputStream)) {
                                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

                                if (!readers.hasNext()) {
                                    throw new IllegalArgumentException("No reader for input");
                                }

                                ImageReader reader = readers.next();

                                try {
                                    reader.setInput(input);
                                    image = reader.read(0);
                                } finally {
                                    // avoid memory leaks
                                    reader.dispose();
                                }
                            }

                            // write image PNG to byte array and read to get png
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            ImageIO.write(image, "png", os);
                            inputStream = new ByteArrayInputStream(os.toByteArray());
                        } else {
                            // already png
                            inputStream = rawInputStream;
                        }

                        if (this.cacheFile == null) {
                            Minecraft.getInstance().execute(() -> {
                                try {
                                    NativeImage directRead = NativeImage.read(inputStream);
                                    this.firstUpload(this.image = directRead, true, realFrames);
                                } catch (IOException e) {
                                    Logging.getInstance().error("Couldn't download cosmetica texture", e);
                                }
                            });
                        } else {
                            FileUtils.copyInputStreamToFile(inputStream, this.cacheFile);
                            this.loadFromDisk(resourceManager);
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

    private boolean loadFromDisk(ResourceManager resourceManager) throws IOException {
        boolean usedCache;
        NativeImage nativeImage;
        int nextFrames;

        if (this.cacheFile != null && this.cacheFile.isFile()) {
            Logging.getInstance().debug("Loading cosmetica texture from local cache ({})", this.cacheFile);

            nextFrames = realFrames;
            FileInputStream fileInputStream = new FileInputStream(this.cacheFile);
            nativeImage = NativeImage.read(fileInputStream);
            usedCache = true;
        } else {
            // we use SimpleTexture-based code to upload the loading texture
            TextureImage defaultImage = load(resourceManager, this.loadingTexture);
            nativeImage = defaultImage.image;
            nextFrames = defaultImage.frames;
            usedCache = false;
        }

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
            this.tick = (this.tick + 1) % ticksPerFrame;

            if (this.tick == 0) {
                this.frame = (this.frame + 1) % this.currentFrames;
                //Debug.info("Uploading frame {}", this.frame);
                this.upload(this.image, false);
            }
        }
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
        private Animated(File file, String url, ResourceLocation loadingTexture, int frames, int ticksPerFrame, Consumer<NativeImage>  onLoad) throws IllegalArgumentException {
            super(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
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
        private final String url;
        private final ResourceLocation loadingTexture;

        // Optional fields with default values
        private File file;
        private int frames = 1;
        private int ticksPerFrame = 1;
        private Consumer<NativeImage>  onLoad;

        /**
         * Constructs a new Builder instance.
         *
         * @param url The URL to retrieve the texture from.
         */
        public Builder(String url, ResourceLocation loadingTexture) {
            this.url = url;
            this.loadingTexture = loadingTexture;
        }

        /**
         * Sets the number of frames and ticks per frame for the animated texture.
         * Both frames and ticksPerFrame must be positive integers.
         *
         * @param frames       Number of frames in the animated texture.
         * @param ticksPerFrame Game ticks per frame to show.
         * @return This Builder instance.
         * @throws IllegalArgumentException if frames or ticksPerFrame is not positive.
         */
        public Builder frames(int frames, int ticksPerFrame) {
            if (frames <= 0 || ticksPerFrame <= 0) {
                throw new IllegalArgumentException("frames and ticksPerFrame must be positive integers");
            }

            this.frames = frames;
            this.ticksPerFrame = ticksPerFrame;
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
            if (this.frames > 1) {
                return new Animated(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
            } else {
                return new CosmeticaTexture(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
            }
        }
    }
}
