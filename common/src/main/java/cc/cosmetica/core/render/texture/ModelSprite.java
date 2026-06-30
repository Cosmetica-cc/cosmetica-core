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

package cc.cosmetica.core.render.texture;

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.impl.Logging;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

import java.util.stream.IntStream;

/**
 * Sprite that references a Cosmetica texture instead of a section of the block atlas.
 */
public class ModelSprite extends TextureAtlasSprite {
	/**
	 * Create a new ModelSprite.
	 * @param location the resource location of the texture this uses.
	 * @param image the image to use.
	 * @param height the height of one frame of the image.
	 * @param frames the number of frames on the image.
	 * @param onClose a callback to run when this sprite is closed.
	 */
	public ModelSprite(Identifier location, GpuTexture image, int height, int frames, Runnable onClose) {
		// textureAtlas, info, mipLevels, uScale (atlasTextureWidth), vScale (atlasTextureHeight), width, height, image
		super(null,
				// dummy data for the animation metadata: we want to handle the animation ourselves.
				new ModelSpriteContents(
						location,
						new FrameSize(image.getWidth(0), height),
						image,
						frames,
						onClose),
//						new AnimationMetadataSection(Optional.empty(), Optional.of(image.getWidth()), Optional.of(height), 69, false)),
				image.getWidth(0), height,
				0, 0, 0
		);

		this.location = location;
	}

	private final Identifier location;

	@Override
	public String toString() {
		return "ModelSprite{" +
				"location=" + this.location +
				", u=[" + this.getU0() + "," + this.getU1() + "]" +
				", v=[" + this.getV0() + ", " + this.getV1() + "]" +
				'}';
	}

	@Override
	public Identifier atlasLocation() {
		// This is now called on 1.21.11!
		// pretend to be block atlas
		return BLOCK_ATLAS;
//		if (CosmeticaCoreExpectPlatform.isDev()) {
//			throw new UnsupportedOperationException("I am a teapot. Tried to call atlas() on cosmetica ModelSprite.");
//		}
//		else {
//			// fix compat with ModelGapFix (modelfix)
//			// pretend to be the block atlas
//			Logging.getInstance().warnOnce("UnsafeAtlasAccess", "A mod called atlas() on a cosmetica ModelSprite. Behaviour could be unpredictable.");
//			return BLOCK_ATLAS;
//		}
	}
	private static final Identifier BLOCK_ATLAS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");

//	@Override
//	public void uploadFirstFrame() {
//		throw new UnsupportedOperationException("I am a teapot. Tried to call uploadFirstFrame() on cosmetica ModelSprite.");
//	}

	private static int getMaximumMipmapLevels(NativeImage image) {
		return log2(Math.min(image.getWidth(), image.getHeight()));
	}

	// Fast, Integer Log2
	private static int log2(int in) {
		int i = 0;

		while (in > 1) {
			i++;
			in >>= 1;
		}

		return i;
	}

	public static class ModelSpriteContents extends SpriteContents {
		public ModelSpriteContents(Identifier resourceLocation, FrameSize frameSize, GpuTexture image, int frames, Runnable onClose) {
			super(resourceLocation, frameSize, image); // Do we need to keep the NativeImage then?
			this.frames = frames;
			this.onClose = onClose;
		}

		private final int frames;
		private final Runnable onClose;

		@Override
		protected int getFrameCount() {
			return this.frames;
		}

		@Override
		public IntList getUniqueFrames() {
			return IntArrayList.toList(IntStream.range(0, getFrameCount()));
		}

		// TODO what are these two close() functions for?
		// This one seems to close the image in vanilla, whereas ticker/close seems to close the interpolation data object
		// the latter does effectively the same thing but whatever texture is currently active in the interpolation data object
		@Override
		public void close() {
			this.onClose.run();
		}
	}
}
