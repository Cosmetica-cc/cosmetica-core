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

package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add Cosmetica Icon.
 */
@Mixin(Font.StringRenderOutput.class)
public class FontStringRenderOutputMixin {
	@Shadow private float x;

	@Shadow @Final private Matrix4f pose;

	@Shadow @Final MultiBufferSource bufferSource;

	@Shadow @Final private boolean seeThrough;

	@Shadow private float y;

	@Shadow @Final private int packedLightCoords;

	@Unique
	private Float cosmeticacore$advance = null;

	@Inject(at = @At("RETURN"), method="<init>")
	private void accept(Font font, MultiBufferSource buf, float initialX, float initialY,
						int colour, boolean dropShadow, Matrix4f matrix4f, boolean seeThrough, int light, CallbackInfo ci) {
		CachedImage icon = NametagRenderer.getPreparedIcon();

		if (icon != null) {
			// see BitmapProvider$Builder.create for how this is scaled
			float scale = 8.0f / icon.getHeight();

			// adjust start position if necessary
			float advance = scale*icon.getWidth() + 1.0f;

			if (NametagRenderer.shouldReadjustNametagPosition()) {
				this.cosmeticacore$advance = advance;
				this.x -= advance/1.5f;
			}

			// see FontTexture#add
			BakedGlyph glyph = new BakedGlyph(
					RenderType.text(icon.location),
					RenderType.textSeeThrough(icon.location),
					// u0 u1 v0 v1
					0, 1, 0, 1,
					// left right up down. See RawGlyph
					0, scale*icon.getWidth(), 3.0f, scale*icon.getHeight() + 3.0f
			);

			VertexConsumer consumer = this.bufferSource.getBuffer(glyph.renderType(this.seeThrough));

			// italic, x, y, pose, vc, r,g,b,a, light
			glyph.render(false, this.x, this.y, this.pose, consumer, 1,1,1,1, this.packedLightCoords);

			// + advance
			this.x += advance;
		}
	}

	@ModifyArg(
			method = "finish",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$Effect;<init>(FFFFFFFFF)V"),
			index = 0
	)
	private float adjustBackgroundStart(float f) {
		if (this.cosmeticacore$advance != null) {
			f -= this.cosmeticacore$advance/1.5f;
		}
		return f;
	}
}
