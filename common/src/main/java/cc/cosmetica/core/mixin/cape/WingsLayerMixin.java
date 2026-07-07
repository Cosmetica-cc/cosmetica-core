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
import cc.cosmetica.core.api.ImageCosmetic;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @Inject(at = @At("HEAD"), method = "getPlayerElytraTexture", cancellable = true)
    private static void addCosmeticEquipperElytras(
            final HumanoidRenderState renderState,
            CallbackInfoReturnable<Identifier> info)
    {
        Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(renderState);
        if (cosmetics.isPresent()) {
            Optional<ImageCosmetic> elytra = cosmetics.get().getElytra();

            if (elytra.isPresent()) {
                info.setReturnValue(elytra.get().getImage().location);
            }
        }
    }
}
