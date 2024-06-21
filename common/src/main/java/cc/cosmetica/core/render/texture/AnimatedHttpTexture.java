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
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * An animated texture that is used by Cosmetica. Adapted from CosmeticIconTexture in Cosmetica 1.
 */
public class AnimatedHttpTexture extends HttpTexture implements Tickable {
	public AnimatedHttpTexture(@Nullable File file, String url, ResourceLocation loadingTexture, int frames, int ticksPerFrame)
			throws IllegalArgumentException {
		super(file, url, loadingTexture, false, null);

		if (frames > 1 && ticksPerFrame == 0) {
			throw new IllegalArgumentException("Animated texture (" + frames + " frames) but ticks per frame is 0!");
		}

		// TODO frames on loading texture?
		this.url = url;
		this.frames = frames;
		this.ticksPerFrame = ticksPerFrame;
	}

	private final int frames;
	private final int ticksPerFrame;
	private int frameHeight;
	private int frame;
	private int tick;
	private NativeImage image;
	private final String url;

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

	@Override
	public void tick() {
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
}
