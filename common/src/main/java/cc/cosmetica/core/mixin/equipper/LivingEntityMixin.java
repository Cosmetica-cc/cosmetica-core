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
import cc.cosmetica.core.impl.IdentityCache;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

/**
 * Implements the {@link CosmeticEquipper} and connects the entity tick to polling cosmetics.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements CosmeticEquipper {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	// allows us to use older cosmetics while newer ones are still loading.
	// this should not get very big.
	@Unique
	private final Queue<Cosmetics> cosmeticacore$cosmetics = new ArrayDeque<>();

	@Unique
	private final IdentityCache<CosmeticManager> cosmeticacore$manager = new IdentityCache<>();

	@Override
	public Optional<Cosmetics> cosmeticacore$getCosmetics() {
		return Optional.ofNullable(this.cosmeticacore$cosmetics.peek());
	}

	@Override
	public void cosmeticacore$updateCosmetics(CosmeticManager manager) {
		if (cosmeticacore$manager.getValue() == manager) {
			if (manager == null) {
				synchronized (this.cosmeticacore$cosmetics) {
					this.cosmeticacore$cosmetics.clear();
				}

				// forward to listeners
				MasterCosmeticManager.post((LivingEntity) (Object) this, null);
			} else {
				// push a new cosmetics
				Cosmetics next = manager.getCosmetics((LivingEntity) (Object) this);
				Logging.getInstance().debug("Next cosmetics " + next);

				synchronized (this.cosmeticacore$cosmetics) {
					this.cosmeticacore$cosmetics.add(next);
				}

				next.enqueue(() -> {
					boolean updated = false;

					synchronized (this.cosmeticacore$cosmetics) {
						// fast-forward to front
						if (this.cosmeticacore$cosmetics.contains(next)) {
							updated = true;

							while (this.cosmeticacore$cosmetics.peek() != next)
								this.cosmeticacore$cosmetics.remove();
						}

						Logging.getInstance().debug("Loaded Cosmetics {}", this.cosmeticacore$cosmetics);
					}

					if (updated) {
						// forward to listeners
						MasterCosmeticManager.post((LivingEntity) (Object) this, next);
					}
				}, () -> {
					synchronized (this.cosmeticacore$cosmetics) {
						this.cosmeticacore$cosmetics.remove(next);
					}
				});
			}
		}
	}

	@Override
	public void cosmeticacore$onEntityRemoved() {
		CosmeticManager manager = this.cosmeticacore$manager.getValue();

		if (manager != null) {
			manager.onRevoke((LivingEntity) (Object) this);
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		if (this.level.isClientSide()) {
			MasterCosmeticManager.pollCosmetics((LivingEntity)(Object)this, this.cosmeticacore$manager);
		}
	}
}
