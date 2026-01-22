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
import com.mojang.blaze3d.font.GlyphInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Add Cosmetica Icon.
 */
@Mixin(Font.PreparedTextBuilder.class)
public abstract class FontStringRenderOutputMixin {
	@Shadow
	float x;

	@Shadow
	float y;

	@Shadow protected abstract void markBackground(float f, float g, float h);

	@Shadow protected abstract void addGlyph(TextRenderable.Styled arg);

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
				this.x -= advance/1.5f;
			}

			// see FontTexture#add
			BakedGlyph glyph = new BakedSheetGlyph(
                    () -> advance,
					GlyphRenderTypes.createForColorTexture(icon.location),
					Minecraft.getInstance().getTextureManager().getTexture(icon.location).getTextureView(),
					// u0 u1 v0 v1
					0, 1, 0, 1,
					// left right up down. See RawGlyph
					0, scale * icon.getWidth(), 0.0f, scale * icon.getHeight()
			);

			this.addGlyph(glyph.createGlyph(
					this.x,
					this.y,
					iconData.transparent ? 0x48FFFFFF : -1,
					0, // no shadow
					style,
					0, 0 // no bold or shadow
			));
			this.markBackground(this.x, this.y, advance);

			// + advance
			this.x += advance;
		}
	}
}
