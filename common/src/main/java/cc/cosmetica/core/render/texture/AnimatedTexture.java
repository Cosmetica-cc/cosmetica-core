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

import cc.cosmetica.core.mixin.texture.NativeImageAccessorMixin;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Base for a texture that supports multiple frames. Does not tick on its own.
 * Implement {@link net.minecraft.client.renderer.texture.Tickable} to animate.
 */
public abstract class AnimatedTexture extends AbstractTexture {
	public AnimatedTexture(int frames) {
		this.frames = frames;
	}

	protected NativeImage image;
	protected int ticksPerFrame = 1;

	private final int frames;
	private int frameHeight;
	private int frame;
	private int tick;

	protected void setupAnimations() {
		if (this.isAnimatable()) {
			this.frame = 0;
			this.frameHeight = this.image.getHeight() / this.frames;

			if (this.frames <= 0) {
				throw new IllegalStateException("Frames cannot be less than one! If you're not using a cape loaded locally, please contact the Cosmetica devs asap. Debug data: frames=" + this.frames + ",frameHeight=" + this.frameHeight + ",frameDelayTicks=" + this.ticksPerFrame + ",width=" + this.image.getWidth() + ",height=" + this.image.getHeight());
			}
		} else {
			this.frameHeight = this.image.getHeight();
		}
	}

	protected void upload() {
		TextureUtil.prepareImage(this.getId(), 0, this.image.getWidth(), this.frameHeight);
		this.image.upload(0, 0, 0, 0, this.frameHeight * this.frame, this.image.getWidth(), this.frameHeight, this.blur, false, false, false);
	}

	protected void doTick() {
		if (this.image != null && ((NativeImageAccessorMixin) (Object) this.image).getPixels() != 0) {
			this.tick = (this.tick + 1) % this.ticksPerFrame;

			if (this.tick == 0) {
				this.frame = (this.frame + 1) % this.frames;

				this.upload();
			}
		}
	}

	/**
	 * Get the raw image being used by this animated texture, including all frames.
	 */
	public NativeImage getRawImage() {
		return this.image;
	}

	public boolean isAnimatable() {
		return this.frames > 1;
	}

	public int getFrameHeight() {
		return this.isAnimatable() ? this.frameHeight : this.getHeight();
	}

	private int getHeight() {
		return this.image == null ? 0 : this.image.getHeight();
	}

	public int getFrameCount() {
		return this.frames;
	}

	@Override
	public String toString() {
		return "AnimatedTexture{" +
				"image=" + image +
				", frameCounterTicks=" + ticksPerFrame +
				", frames=" + frames +
				", frameHeight=" + frameHeight +
				", frame=" + frame +
				", tick=" + tick +
				'}';
	}
}
