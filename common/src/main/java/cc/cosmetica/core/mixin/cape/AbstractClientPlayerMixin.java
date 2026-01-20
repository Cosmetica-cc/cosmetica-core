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
import cc.cosmetica.core.impl.CapeTextureManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Add custom capes and elytras.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
	@Shadow @org.jetbrains.annotations.Nullable protected abstract PlayerInfo getPlayerInfo();

	public AbstractClientPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	// Capes
	@Unique
	CapeTextureManager cosmeticacore$capeTextureManager = new CapeTextureManager();

	@Inject(at = @At("RETURN"), method = "getSkin", cancellable = true)
	private void addCosmeticaCapes(CallbackInfoReturnable<PlayerSkin> info) {
		@Nullable PlayerInfo playerInfo = this.getPlayerInfo();

		if (playerInfo != null) {
			Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(this);
			info.setReturnValue(this.cosmeticacore$capeTextureManager.getPlayerSkin(cosmetics, info.getReturnValue()));
		}
	}
}
