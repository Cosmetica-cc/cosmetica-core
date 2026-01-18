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
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Add icons to the tab menu.
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	/// MODIFYING WIDTH ///
	@Inject(method="render",
			at= @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;"),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void capturePlayerInfo(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci, List list, List list2, int j, int k, int l, Iterator var10, PlayerInfo playerInfo) {
		this.cosmeticacore$tempPassInfo = playerInfo;
	}

	@Unique
	private PlayerInfo cosmeticacore$tempPassInfo;

	@Redirect(method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I", ordinal=0))
	private int addIconToWidth(Font instance, FormattedText arg) {
		Level level = Minecraft.getInstance().level;
		float additionalWidth = 0;

		if (level != null && this.cosmeticacore$tempPassInfo.getProfile().id() != null) {
			Player player = level.getPlayerByUUID(this.cosmeticacore$tempPassInfo.getProfile().id());

			if (player != null) {
				Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(player);

				if (cosmetics.isPresent()) {
					CachedImage icon = cosmetics.get().getNametag().getIcon().getImage();

					if (icon.isLoaded()) {
						additionalWidth = icon.getWidth() * (8.0f / icon.getHeight());
					}
				};
			}
		}

		// clear
		this.cosmeticacore$tempPassInfo = null;

		return (int)additionalWidth + instance.width(arg);
	}

	/// RENDERING ICON ///

	@Inject(method = "render",
			at = @At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"),
			locals= LocalCapture.CAPTURE_FAILHARD)
	private void beforeRenderName(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci, List list, List list2, int j, int k, int l, int o, int p, int q, boolean bl, int r, int n, int s, int t, int u, List list3, List list4, int w, int x, int v, int y, int z, int aa, PlayerInfo playerInfo2, PlayerTabOverlay.ScoreDisplayEntry scoreDisplayEntry, GameProfile gameProfile) {
		Level level = Minecraft.getInstance().level;

		if (level != null && gameProfile.id() != null) {
			Player player = level.getPlayerByUUID(gameProfile.id());

			if (player != null) {
				Cosmetics.getCosmetics(player).ifPresent(cosmetics -> {
					NametagConfig nametagConfig = cosmetics.getNametag();
					CachedImage icon = nametagConfig.getIcon().getImage();

					if (icon.isLoaded()) {
						NametagRenderer.prepareIcon(icon, 2, nametagConfig.isTransparentIcon(), false);
					}
				});
			}
		}
	}
}
