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

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.CosmeticEquipper;
import cc.cosmetica.core.impl.Logging;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * Override CosmeticEquipper methods for RemotePlayer to use player info.
 */
@Mixin(RemotePlayer.class)
public class RemotePlayerMixin extends AbstractClientPlayer implements CosmeticEquipper {
    public RemotePlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Override
    public Optional<Cosmetics> cosmeticacore$getCosmetics() {
        // We should be in game if a RemotePlayer is loaded in vanilla conditions.
        // However, to prevent conflicts with mods that might use RemotePlayer in e.g. GUI rendering:
        if (Minecraft.getInstance().player == null)
            return Optional.empty();

        ClientPacketListener connection = Minecraft.getInstance().player.connection;
        PlayerInfo info = connection.getPlayerInfo(this.getUUID());

        if (info != null) {
            return ((CosmeticEquipper) info).cosmeticacore$getCosmetics();
        } else {
            Logging.getInstance().warnOnce("remotePlayerInfoNull", "Remote Player " + this.getUUID() + " had associated Player Info == null?");
            return Optional.empty();
        }
    }

    @Override
    public void cosmeticacore$updateCosmetics(@Nullable CosmeticManager manager) {
        throw new UnsupportedOperationException("RemotePlayer should be handled via PlayerInfo");
    }

    @Override
    public void cosmeticacore$refreshCosmetics(CosmeticManager manager) {
        throw new UnsupportedOperationException("RemotePlayer should be handled via PlayerInfo");
    }

    @Override
    public void cosmeticacore$onEntityRemoved() {
        // No-op. Wait until player info is removed.
    }

    @Override
    public void cosmeticacore$pollCosmetics() {
        // We should be in game if a RemotePlayer is loaded in vanilla conditions.
        // However, to prevent conflicts with mods that might use RemotePlayer in e.g. GUI rendering:
        if (Minecraft.getInstance().player == null)
            return;

        ClientPacketListener connection = Minecraft.getInstance().player.connection;
        PlayerInfo info = connection.getPlayerInfo(this.getUUID());

        if (info != null) {
            ((CosmeticEquipper) info).cosmeticacore$pollCosmetics();
        }
    }
}
