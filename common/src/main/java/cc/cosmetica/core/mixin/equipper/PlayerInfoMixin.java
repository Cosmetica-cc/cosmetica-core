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
import cc.cosmetica.core.impl.CosmeticEquipHelper;
import cc.cosmetica.core.impl.CosmeticEquipper;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

/**
 * PlayerInfo contains the persistent info on a player in multiplayer.
 * As multiple PlayerEntities can exist on a player throughout a game service, we store the cosmetics for remote players
 * here.
 * This way if (e.g.) a player dies and respawns, or the player entity unloads, their cosmetics can be reloaded.
 */
@Mixin(PlayerInfo.class)
public class PlayerInfoMixin implements CosmeticEquipper {
    // Implemented by Equip Helper

    @Shadow @Final private GameProfile profile;
    @Unique
    private final CosmeticEquipHelper cosmeticacore$cosmetics = new CosmeticEquipHelper(manager -> manager.getCosmetics(
            new CosmeticManager.Either((PlayerInfo) (Object) this)));

    @Override
    public Optional<Cosmetics> cosmeticacore$getCosmetics() {
        return this.cosmeticacore$cosmetics.getCosmetics();
    }

    @Override
    public void cosmeticacore$refreshCosmetics(CosmeticManager manager) {
        this.cosmeticacore$cosmetics.refreshCosmetics(manager, this.profile.getId(), newCosmetics -> {
            MasterCosmeticManager.post((PlayerInfo) (Object) this, newCosmetics);
        });
    }

    @Override
    public void cosmeticacore$updateCosmetics(@Nullable CosmeticManager manager) {
        this.cosmeticacore$cosmetics.updateCosmetics(manager, newCosmetics -> {
            MasterCosmeticManager.post((PlayerInfo) (Object) this, newCosmetics);
        });
    }

    @Override
    public void cosmeticacore$onEntityRemoved() {
        @Nullable CosmeticManager manager = this.cosmeticacore$cosmetics.getManager().getValue();

        if (manager != null) {
            manager.onRevoke(new CosmeticManager.Either((PlayerInfo) (Object) this));
        }
    }

    // Tick called in ClientLevel.tick, or by RemotePlayer$pollCosmetics (latter not by core alone)
    @Override
    public void cosmeticacore$pollCosmetics() {
        MasterCosmeticManager.pollCosmetics(new CosmeticManager.Either((PlayerInfo) (Object) this), this.cosmeticacore$cosmetics.getManager());
    }
}
