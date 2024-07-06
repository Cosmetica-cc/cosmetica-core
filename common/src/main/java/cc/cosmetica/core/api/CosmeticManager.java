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
	 * Get whether this cosmetic manager can and should manage this entity.
	 * @param entity the entity to query whether it should be managed.
	 * @return whether this entity should be managed by this cosmetic manager.
	 * @apiNote This can be called nearly every tick for every LivingEntity on the client, so make it fast and concise!
	 * 		    If this returns true, {@link CosmeticManager#getCosmetics(LivingEntity)} will be called shortly after.
	 */
	boolean canManage(LivingEntity entity);

	/**
	 * Get the cosmetics to apply to the given entity being managed.
	 * @param entity the entity being managed by this.
	 * @return the cosmetics that should be rendered on this living entity. Cannot be null.
	 */
	Cosmetics getCosmetics(LivingEntity entity);

	/**
	 * Called when a living entity is assigned to this cosmetic manager. This is called before {@link CosmeticManager#getCosmetics(LivingEntity)}.
	 * @param entity the entity that has been assigned to this cosmetic manager.
	 */
	default void onAssign(LivingEntity entity) {
		// Default behaviour : do nothing
	}

	/**
	 * Called when an entity is assigned to a different cosmetic manager, or is removed from the world.
	 * @param entity the entity that was being managed by this cosmetic manager.
	 */
	default void onRevoke(LivingEntity entity) {
		// Default behaviour : do nothing
	}
}
