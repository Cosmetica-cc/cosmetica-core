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

package cc.cosmetica.core.mixin.cape;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Add custom capes and elytras.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
	@Shadow @org.jetbrains.annotations.Nullable protected abstract PlayerInfo getPlayerInfo();

	public AbstractClientPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	// Capes
	@Unique
	@Nullable WeakReference<PlayerSkin> cosmeticacore$vanillaSkin = new WeakReference<>(null);
	@Unique
	@Nullable PlayerSkin cosmeticacore$modifiedSkin = null;

	@Inject(at = @At("RETURN"), method = "getSkin", cancellable = true)
	private void addCosmeticaCapes(CallbackInfoReturnable<PlayerSkin> info) {
		@Nullable PlayerInfo playerInfo = this.getPlayerInfo();

		if (playerInfo != null) {
			Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);

			if (cosmetics.isPresent()) {
				final @Nullable PlayerSkin cachedVanilla = this.cosmeticacore$vanillaSkin.get();
				final PlayerSkin existing = info.getReturnValue();

				if (cachedVanilla == existing) {
					info.setReturnValue(cosmeticacore$modifiedSkin);
				} else {
					Optional<ImageCosmetic> cloak = cosmetics.get().getCloak();
					Optional<ImageCosmetic> elytra = cosmetics.get().getElytra();

					ResourceLocation cloakLocation = cloak.isPresent()   ?  cloak.get().getImage().location : MasterCosmeticManager.hideVanillaCapes ? null : existing.capeTexture();
					ResourceLocation elytraLocation = elytra.isPresent() ? elytra.get().getImage().location : MasterCosmeticManager.hideVanillaCapes ? null : existing.elytraTexture();

					PlayerSkin modified = new PlayerSkin(
							existing.texture(),
							existing.textureUrl(),
							cloakLocation,
							elytraLocation,
							existing.model(),
							existing.secure()
					);

					this.cosmeticacore$vanillaSkin = new WeakReference<>(existing);
					this.cosmeticacore$modifiedSkin = modified;
					info.setReturnValue(modified);
				}
			}
		}
	}
}
