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

import cc.cosmetica.core.impl.CosmeticEquipper;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface for the cosmetics equipped on an entity.
 */
public interface Cosmetics {
	/**
	 * Get the accessories equipped on this entity.
	 * @return the accessories this entity is equipping.
	 */
	Collection<Accessory> getAccessories();

	/**
	 * Get the container for cosmetics being worn by the given entity.
	 * @param entity the entity for which to get the container.
	 * @return the container.
	 * @param <LE> the type of entity to get the cosmetics for.
	 */
	static <LE extends LivingEntity> Optional<Cosmetics> getCosmetics(LE entity) {
		CosmeticEquipper equipper = (CosmeticEquipper) entity;
		return equipper.cosmetica$getCosmetics();
	}
}
