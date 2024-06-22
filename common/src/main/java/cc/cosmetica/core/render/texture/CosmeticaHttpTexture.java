/*
 * Copyright 2024 Cosmetica
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

package cc.cosmetica.core.render.texture;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.mixin.texture.NativeImageAccessorMixin;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;

import java.io.File;

/**
 * An animated texture that is used by Cosmetica. Adapted from CosmeticIconTexture in Cosmetica 1.
 * Requires the mixins texture/NativeImageAccessorMixin and texture/HttpTextureMixin
 */
public class CosmeticaHttpTexture extends HttpTexture {
	private CosmeticaHttpTexture(File file, String url, ResourceLocation loadingTexture,
								 int frames, int ticksPerFrame, Runnable onLoad)
			throws IllegalArgumentException {
		super(file, url, loadingTexture, false, onLoad);

		if (frames > 1 && ticksPerFrame == 0) {
			throw new IllegalArgumentException("Animated texture (" + frames + " frames) but ticks per frame is 0!");
		}

		// TODO frames on loading texture?
		this.url = url;
		this.frames = frames;
		this.ticksPerFrame = ticksPerFrame;
	}

	private final String url;
	private final int frames;
	private final int ticksPerFrame;

	private int frameHeight;
	private int frame;
	private int tick;
	private NativeImage image;

	public void firstUpload(NativeImage image, boolean loading) {
		// memory management
		if (this.image != null && ((NativeImageAccessorMixin)(Object)this.image).getPixels() != 0L) {
			//Debug.info("Closing image on thread {} due to load. Are we allowed? {}", Thread.currentThread(), RenderSystem.isOnRenderThreadOrInit());
			this.image.close();
			//Debug.info("Closed image.");
		}

		this.image = image;
		this.frameHeight = image.getHeight() / this.frames;
		this.frame = 0;

		try {
			this.upload(image, !loading);
		} catch (IllegalStateException e) {
			Logging.getInstance().error("Error while uploading icon texture (loading: {}, icon url: {})", e, loading, this.url);
		}
	}

	public void upload(NativeImage image, boolean close) {
		TextureUtil.prepareImage(this.getId(), 0, image.getWidth(), this.frameHeight);
		image.upload(0, 0, 0, 0, this.frameHeight * this.frame, image.getWidth(), this.frameHeight, this.blur, false, false, close);
	}

	void doTick() {
		if (this.frames > 1 && this.image != null && ((NativeImageAccessorMixin) (Object) this.image).getPixels() != 0) {
			this.tick = (this.tick + 1) % ticksPerFrame;

			if (this.tick == 0) {
				this.frame = (this.frame + 1) % this.frames;
				//Debug.info("Uploading frame {}", this.frame);
				this.upload(this.image, false);
			}
		}
	}

	@Override
	public void close() {
		//Debug.info("Closing image on thread {} due to dispose. Are we allowed? {}", Thread.currentThread(), RenderSystem.isOnRenderThreadOrInit());
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
		return this.frames;
	}

	/**
	 * Ticking version of CosmeticaHttpTexture.
	 * Exists so we can use all the utilities that CosmeticaHttpTexture adds to HttpTexture for static textures, without
	 * adding the unnecessary overhead of ticking every static texture (which will be most textures).
	 */
	private static class Animated extends CosmeticaHttpTexture implements Tickable {
		private Animated(File file, String url, ResourceLocation loadingTexture, int frames, int ticksPerFrame, Runnable onLoad) throws IllegalArgumentException {
			super(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
		}

		@Override
		public void tick() {
			this.doTick();
		}
	}

	/**
	 * Builder class for creating an {@link CosmeticaHttpTexture} instance.
	 */
	public static class Builder {
		// Required
		private final String url;
		private final ResourceLocation loadingTexture;

		// Optional fields with default values
		private File file;
		private int frames = 1;
		private int ticksPerFrame = 1;
		private Runnable onLoad;

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
		public Builder onLoad(Runnable onLoad) {
			this.onLoad = onLoad;
			return this;
		}

		/**
		 * Constructs and returns an {@link CosmeticaHttpTexture} instance with the configured parameters.
		 * @return An {@link CosmeticaHttpTexture} instance.
		 */
		public CosmeticaHttpTexture build() {
			// Create and return AnimatedHttpTexture instance
			if (this.frames > 1) {
				return new Animated(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
			} else {
				return new CosmeticaHttpTexture(file, url, loadingTexture, frames, ticksPerFrame, onLoad);
			}
		}
	}
}
