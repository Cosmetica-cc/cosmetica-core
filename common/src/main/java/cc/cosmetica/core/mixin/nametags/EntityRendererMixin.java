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

package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.IconSubmitter;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Split from PlayerRendererMixin in 26.1.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            ordinal = 1
    ), method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V")
    protected void onRenderNameTag(
            EntityRenderState state,
            PoseStack stack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            int offset,
            CallbackInfo ci)
    {
        if (state instanceof AvatarRenderState aState) {
            // add lore
            NametagRenderer.submitLore(aState, stack, collector, camera);

            // add nametag icons
            Cosmetics.getCosmetics(aState).ifPresent(c -> {
                NametagConfig iconCosmetic = c.getNametag();
                CachedImage icon = iconCosmetic.getIcon().getImage();

                if (icon.isLoaded()) {
                    ((IconSubmitter) collector.order(0)).cosmeticacore$submitIcon(icon, state.isDiscrete || iconCosmetic.isTransparentIcon(), true);
                }
            });
        }
    }
}
