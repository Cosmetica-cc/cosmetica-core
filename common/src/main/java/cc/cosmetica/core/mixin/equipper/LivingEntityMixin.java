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
import cc.cosmetica.core.impl.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
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

	// Implemented by Equip Helper

	@Unique
	private final CosmeticEquipHelper cosmeticacore$cosmetics = new CosmeticEquipHelper(manager -> manager.getCosmetics(
			new CosmeticManager.Either((LivingEntity) (Object) this)));

	@Override
	public Optional<Cosmetics> cosmeticacore$getCosmetics() {
		return this.cosmeticacore$cosmetics.getCosmetics();
	}

	@Override
	public void cosmeticacore$refreshCosmetics(CosmeticManager manager) {
		this.cosmeticacore$cosmetics.refreshCosmetics(manager, this.getUUID(), newCosmetics -> {
			MasterCosmeticManager.post((LivingEntity) (Object) this, newCosmetics);
		});
	}

	@Override
	public void cosmeticacore$updateCosmetics(CosmeticManager manager) {
		this.cosmeticacore$cosmetics.updateCosmetics(manager, newCosmetics -> {
			MasterCosmeticManager.post((LivingEntity) (Object) this, newCosmetics);
		});
	}

	// Implement By Self

	@Override
	public void cosmeticacore$onEntityRemoved() {
		@Nullable CosmeticManager manager = this.cosmeticacore$cosmetics.getManager().getValue();

		if (manager != null) {
			manager.onRevoke(new CosmeticManager.Either((LivingEntity) (Object) this));
		}
	}

	@Override
	public void cosmeticacore$pollCosmetics() {
		MasterCosmeticManager.pollCosmetics(new CosmeticManager.Either((LivingEntity)(Object)this), this.cosmeticacore$cosmetics.getManager());
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		boolean remotePlayer = (Object)this instanceof RemotePlayer;

		if (!remotePlayer && this.level.isClientSide()) {
			MasterCosmeticManager.pollCosmetics(new CosmeticManager.Either((LivingEntity)(Object)this), this.cosmeticacore$cosmetics.getManager());
		}
	}
}
