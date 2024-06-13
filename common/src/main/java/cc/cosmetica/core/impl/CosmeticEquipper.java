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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Duck interface for an {@linkplain LivingEntity entity} equipping cosmetics.
 */
public interface CosmeticEquipper {
	/**
	 * Get the Cosmetics worn by this living entity.
	 * @return the cosmetics container on this living entity.
	 */
	Optional<Cosmetics> cosmeticacore$getCosmetics();

	/**
	 * Set the Cosmetics worn by this living entity.
	 * @param cosmetics the cosmetics container on this living entity.
	 */
	void cosmeticacore$setCosmetics(Cosmetics cosmetics);

	/**
	 * Called when the entity is removed from the client level.
	 */
	void cosmeticacore$onEntityRemoved();
}
