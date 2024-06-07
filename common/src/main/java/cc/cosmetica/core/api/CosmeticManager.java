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

package cc.cosmetica.core.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * Handles what cosmetics should exist on a given set of entities.
 * Register these with {@link CosmeticManagers#registerCosmeticManager(int, CosmeticManager)}.
 */
public interface CosmeticManager {
	/**
	 * Get whether this cosmetic manager can and should manage this entity. This is called every tick that the entity
	 * is rendered on the client.
	 * @param entity the entity to query whether it should be managed.
	 * @return whether this entity should be managed by this cosmetic manager.
	 */
	boolean canManage(LivingEntity entity);

	/**
	 * Get the cosmetics to apply to the given entity being managed .
	 * @param entity
	 * @return
	 */
	Cosmetics getCosmetics(LivingEntity entity)

	/**
	 * Called when a living entity is assigned to this cosmetic manager.
	 * @param entity the entity that has been assigned to this cosmetic manager.
	 */
	default void onAssign(LivingEntity entity) {
		// Default behaviour : do nothing
	}

	/**
	 * Called when an entity is assigned to a different cosmetic manager.
	 * @param entity the entity that was being managed by this cosmetic manager.
	 */
	default void onRevoke(LivingEntity entity) {
		// Default behaviour : do nothing
	}
}
