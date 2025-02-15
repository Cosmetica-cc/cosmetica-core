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

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.ImageCosmetic;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Add custom capes and elytras.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
	public AbstractClientPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	// Capes

	@Inject(at = @At("HEAD"), method = "isCapeLoaded", cancellable = true)
	private void isCosmeticaCloakLoaded(CallbackInfoReturnable<Boolean> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);
		info.setReturnValue(cosmetics.isPresent() && cosmetics.get().getCloak().isPresent() && cosmetics.get().getCloak().get().getImage().isLoaded());
	}

	@Inject(at = @At("HEAD"), method = "getCloakTextureLocation", cancellable = true)
	private void addCosmeticaCloaks(CallbackInfoReturnable<ResourceLocation> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);

		if (cosmetics.isPresent()) {
			Optional<ImageCosmetic> cloak = cosmetics.get().getCloak();

			if (cloak.isPresent()) {
				info.setReturnValue(cloak.get().getImage().location); // set the return value to our one
			}
		}
	}
}
