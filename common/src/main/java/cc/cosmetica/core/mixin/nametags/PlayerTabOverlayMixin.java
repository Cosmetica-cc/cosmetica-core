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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/**
 * Add icons to the tab menu.
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	@Inject(method = "render",
			at =@At(value="INVOKE", target="Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"),
			locals= LocalCapture.CAPTURE_FAILHARD)
	private void beforeRenderName(PoseStack poseStack, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci, ClientPacketListener clientPacketListener, List list, int j, int k, int m, int n, int l, boolean bl, int o, int p, int q, int r, int s, List list2, List list3, int u, int v, int t, int w, int x, int y, PlayerInfo playerInfo2, GameProfile gameProfile) {
		Level level = Minecraft.getInstance().level;

		if (level != null && gameProfile.getId() != null) {
			Player player = level.getPlayerByUUID(gameProfile.getId());

			if (player != null) {
				Cosmetics.getCosmetics(player).ifPresent(cosmetics -> {
					if (cosmetics.getIcon().isLoaded()) {
						NametagRenderer.prepareIcon(cosmetics.getIcon(), 2);
					}
				});
			}
		}
	}
}
