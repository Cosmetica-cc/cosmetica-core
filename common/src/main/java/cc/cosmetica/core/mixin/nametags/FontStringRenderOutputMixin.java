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

package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Add Cosmetica Icon.
 */
@Mixin(Font.StringRenderOutput.class)
public class FontStringRenderOutputMixin {
	@Shadow
    float x;

	@Shadow @Final private Matrix4f pose;

	@Shadow @Final MultiBufferSource bufferSource;

	@Shadow
    float y;

	@Shadow @Final private int packedLightCoords;

	@Shadow @Final private Font.DisplayMode mode;
	@Unique
	private Float cosmeticacore$advance = null;
	@Unique
	private boolean cosmeticacore$drawnIcon = false;

	@Inject(at = @At("HEAD"), method="accept")
	private void accept(int i, Style style, int j, CallbackInfoReturnable<Boolean> cir) {
		// check for first render pass
		if (this.cosmeticacore$drawnIcon) {
			return;
		}
		this.cosmeticacore$drawnIcon = true;

		NametagRenderer.Icon iconData = NametagRenderer.getPreparedIcon();

		if (iconData == null) return;
		CachedImage icon = iconData.image.get();

		if (icon != null) {
			// see BitmapProvider$Builder.create for how this is scaled
			float scale = 8.0f / icon.getHeight();

			// adjust start position if necessary
			float advance = scale*icon.getWidth() + 2.0f;

			if (NametagRenderer.shouldReadjustNametagPosition()) {
				this.cosmeticacore$advance = advance;
				this.x -= advance/1.5f;
			}

			// see FontTexture#add
			BakedGlyph glyph = new BakedGlyph(
					GlyphRenderTypes.createForColorTexture(icon.location),
					// u0 u1 v0 v1
					0, 1, 0, 1,
					// left right up down. See RawGlyph
					0, scale*icon.getWidth(), 0.0f, scale*icon.getHeight()
			);

			VertexConsumer consumer = this.bufferSource.getBuffer(glyph.renderType(this.mode));

			glyph.renderChar(new BakedGlyph.GlyphInstance(
					this.x,
					this.y,
					iconData.transparent ? 0x80FFFFFF : -1,
					0, // no shadow
					glyph,
					style,
					0, 0 // no bold or shadow
			), this.pose, consumer, this.packedLightCoords);

			/*
			// italic, x, y, pose, vc, r,g,b,a, light
			glyph.render(false, this.x, this.y, this.pose, consumer, 1,1,1, iconData.transparent ? (0x20 / 255.0f) : 1, this.packedLightCoords);
			 */

			// + advance
			this.x += advance;
		}
	}

	@Inject(at = @At("RETURN"), method = "finish")
	private void onFinish(float f, CallbackInfoReturnable<Float> cir) {
		// reset captured
		this.cosmeticacore$drawnIcon = false;
	}

	@ModifyArg(
			method = "finish",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$Effect;<init>(FFFFFI)V"),
			index = 0
	)
	private float adjustBackgroundStart(float f) {
		if (this.cosmeticacore$advance != null) {
			f -= this.cosmeticacore$advance/1.5f;
		}
		return f;
	}
}
