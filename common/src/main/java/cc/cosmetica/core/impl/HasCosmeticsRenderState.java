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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Duck interface to apply and access cosmetics during entity render.
 * Applied to: {@link net.minecraft.client.renderer.entity.state.LivingEntityRenderState}.
 */
public interface HasCosmeticsRenderState {
    /**
     * Set the cosmetics on the render state from an entity.
     * @param entity the entity to copy cosmetics from.
     */
    <T extends LivingEntity> void cosmeticacore$extractCosmeticsOf(T entity);

    /**
     * Directly set the cosmetics on the render state.
     * @param cosmetics the cosmetics on the render state.
     */
    void cosmeticacore$setCosmetics(@Nullable Cosmetics cosmetics);

    /**
     * Get the cosmetics to render for this entity.
     * @return the cosmetics to render for this entity.
     */
    Optional<Cosmetics> cosmeticacore$getCosmetics();
}
