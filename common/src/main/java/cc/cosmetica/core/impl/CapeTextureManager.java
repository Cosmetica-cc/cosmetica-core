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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.ImageCosmetic;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Manages cape textures.
 */
public class CapeTextureManager {
    @NotNull WeakReference<PlayerSkin> vanillaSkin = new WeakReference<>(null);
    @Nullable PlayerSkin modifiedSkin = null;
    @NotNull WeakReference<Cosmetics> cosmetics = new WeakReference<>(null);

    public PlayerSkin getPlayerSkin(Optional<Cosmetics> cosmetics, PlayerSkin existing) {
        if (cosmetics.isPresent()) {
            final @Nullable PlayerSkin cachedVanilla = this.vanillaSkin.get();

            if (cachedVanilla == existing && cosmetics.get() == this.cosmetics.get()) {
                return modifiedSkin;
            } else {
                Optional<ImageCosmetic> cloak = cosmetics.get().getCloak();
                Optional<ImageCosmetic> elytra = cosmetics.get().getElytra();

                class CosmeticaAssetTexture implements ClientAsset.Texture {
                    CosmeticaAssetTexture(Identifier location) {
                        this.location = location;
                    }

                    private final Identifier location;

                    @Override
                    public Identifier texturePath() {
                        return this.location;
                    }

                    @Override
                    public Identifier id() {
                        return this.location;
                    }
                }

                Identifier cloakLocation =  cloak.isPresent()  ? cloak.get().getImage().location  : MasterCosmeticManager.hideVanillaCapes ? null : existing.cape() == null   ? null : existing.cape().texturePath();
                Identifier elytraLocation = elytra.isPresent() ? elytra.get().getImage().location : MasterCosmeticManager.hideVanillaCapes ? null : existing.elytra() == null ? null : existing.elytra().texturePath();

                PlayerSkin modified = new PlayerSkin(
                        existing.body(),
                        cloakLocation == null ? null : new CosmeticaAssetTexture(cloakLocation),
                        elytraLocation == null ? null : new CosmeticaAssetTexture(elytraLocation),
                        existing.model(),
                        existing.secure()
                );

                this.vanillaSkin = new WeakReference<>(existing);
                this.modifiedSkin = modified;
                this.cosmetics = new WeakReference<>(cosmetics.get());
                return modified;
            }
        } else {
            return existing;
        }
    }
}
