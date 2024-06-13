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

package cc.cosmetica.core.mixin;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.CosmeticEquipper;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.impl.IdentityCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Implements the {@link CosmeticEquipper} and connects the entity tick to polling cosmetics.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements CosmeticEquipper {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Unique
	private Cosmetics cosmeticacore$cosmetics;

	@Unique
	private final IdentityCache<CosmeticManager> cosmeticacore$manager = new IdentityCache<>();

	@Override
	public Optional<Cosmetics> cosmeticacore$getCosmetics() {
		return Optional.ofNullable(this.cosmeticacore$cosmetics);
	}

	@Override
	public void cosmeticacore$setCosmetics(Cosmetics cosmetics) {
		this.cosmeticacore$cosmetics = cosmetics;
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		if (this.level.isClientSide()) {
			MasterCosmeticManager.pollCosmetics((LivingEntity)(Object)this, this.cosmeticacore$manager);
		}
	}
}
