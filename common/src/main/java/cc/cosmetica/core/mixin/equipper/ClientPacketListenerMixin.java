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

package cc.cosmetica.core.mixin.equipper;

import cc.cosmetica.core.impl.CosmeticEquipper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

/**
 * Revoke cosmetic manager when PlayerInfo is removed.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow public abstract @Nullable PlayerInfo getPlayerInfo(UUID uUID);

    @Shadow public abstract Collection<PlayerInfo> getOnlinePlayers();

    @Inject(
            method = "handlePlayerInfo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/social/PlayerSocialManager;removePlayer(Ljava/util/UUID;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onPlayerInfoRemove(ClientboundPlayerInfoPacket packet, CallbackInfo ci,
                                    Iterator var2, ClientboundPlayerInfoPacket.PlayerUpdate playerUpdate) {
        if (packet.getAction() == ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER) {
            PlayerInfo info = this.getPlayerInfo(playerUpdate.getProfile().getId());
            if (info != null) {
                ((CosmeticEquipper) info).cosmeticacore$onEntityRemoved();
            }
        }
    }

    @Inject(
            method = "cleanup",
            at = @At("HEAD")
    )
    private void onDisconnect(CallbackInfo ci) {
        for (PlayerInfo info : this.getOnlinePlayers()) {
            ((CosmeticEquipper) info).cosmeticacore$onEntityRemoved();
        }
    }
}
