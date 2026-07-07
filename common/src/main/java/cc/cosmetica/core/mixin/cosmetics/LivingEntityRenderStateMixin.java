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

package cc.cosmetica.core.mixin.cosmetics;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.HasCosmeticsRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Make Cosmetics accessible.
 */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements HasCosmeticsRenderState {
    @Unique
    private WeakReference<@Nullable Cosmetics> cosmeticacore$cosmetics = new WeakReference<>(null);

    @Override
    public <T extends LivingEntity> void cosmeticacore$extractCosmeticsOf(T entity) {
        Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(entity);
        this.cosmeticacore$cosmetics = new WeakReference<>(cosmetics.orElse(null));
    }

    @Override
    public void cosmeticacore$setCosmetics(@Nullable Cosmetics cosmetics) {
        this.cosmeticacore$cosmetics = new WeakReference<>(cosmetics);
    }

    @Override
    public Optional<Cosmetics> cosmeticacore$getCosmetics() {
        return Optional.ofNullable(this.cosmeticacore$cosmetics.get());
    }
}
