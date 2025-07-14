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

import cc.cosmetica.core.render.CustomItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntityWithoutLevelRenderer.class, priority = 999)
public class BlockEntityWithoutLevelRendererMixin {
    @Shadow @Final private ShieldModel shieldModel;

    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void onRenderByItem(ItemStack itemStack, ItemTransforms.TransformType transformType, PoseStack poseStack,
                                MultiBufferSource multiBufferSource, int i, int j, CallbackInfo info) {
        boolean cosmeticaShield = itemStack.getItem() == Items.SHIELD;
        boolean banner = itemStack.getTagElement("BlockEntityTag") != null;

        if (!banner && cosmeticaShield) {
            CustomItemRenderer.renderShield(poseStack, multiBufferSource, this.shieldModel, i, j, itemStack.hasFoil());
            info.cancel();
        }
    }
}
