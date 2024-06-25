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

package cc.cosmetica.core.mixin.cape;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
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
		info.setReturnValue(cosmetics.isPresent() && cosmetics.get().getCloak().isLoaded());
	}

	@Inject(at = @At("HEAD"), method = "getCloakTextureLocation", cancellable = true)
	private void addCosmeticaCloaks(CallbackInfoReturnable<ResourceLocation> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);

		if (cosmetics.isPresent()) {
			info.setReturnValue(cosmetics.get().getCloak().location); // set the return value to our one
		}
	}

	// Elytra
	@Inject(at = @At("HEAD"), method = "isElytraLoaded", cancellable = true)
	private void isCosmeticaElytraLoaded(CallbackInfoReturnable<Boolean> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);
		// we want to make sure elytra is always controlled by us (so cloak can't be misused as elytra)
		info.setReturnValue(cosmetics.isPresent());
	}

	@Inject(at = @At("HEAD"), method = "getElytraTextureLocation", cancellable = true)
	private void addCosmeticaElytras(CallbackInfoReturnable<ResourceLocation> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);

		if (cosmetics.isPresent()) {
			CachedImage image = cosmetics.get().getElytra();
			info.setReturnValue(image.isLoaded() ? image.location : COSMETICACORE$WINGS_LOCATION); // set the return value to our one
		}
	}

	// ElytraLayer#WINGS_LOCATION
	@Unique
	private static final ResourceLocation COSMETICACORE$WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");
}
