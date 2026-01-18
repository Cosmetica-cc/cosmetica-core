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

package cc.cosmetica.core.render;

//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.model.ShieldModel;
//import net.minecraft.client.model.geom.ModelPart;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.entity.ItemRenderer;
//import net.minecraft.client.resources.model.Material;
//import net.minecraft.client.resources.model.ModelBakery;
//import net.minecraft.resources.ResourceLocation;

/**
 * Render utilities for items modified by cosmetica cosmetics.
 */
public class CustomItemRenderer {
    // TODO check if this works
//    public static void renderShield(GuiGraphics graphics, MultiBufferSource multiBufferSource, ShieldModel model, int i, int j, boolean glint) {
//        final Material shieldMaterial = ModelBakery.NO_PATTERN_SHIELD;
//        final var stack = graphics.pose();
//
//        stack.pushPose();
//        stack.scale(1.0F, -1.0F, -1.0F);
//        VertexConsumer vertexConsumer = shieldMaterial.sprite()
//                .wrap(ItemRenderer.getFoilBufferDirect(multiBufferSource, model.renderType(shieldMaterial.atlasLocation()), true, glint));
//        model.handle().render(stack, vertexConsumer, i, j);
//
//        model.plate().render(stack, vertexConsumer, i, j);
//
//        // TODO add glint
//        VertexConsumer texture = multiBufferSource.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("cosmetica-core", "test.png")));
//
//        // draw texture
//        ModelPart part = model.plate();
//
//        try {
//            part.render(stack, texture, i, j);
//        }catch (RuntimeException e) {
//            e.printStackTrace();
//        }

//        BannerRenderer.renderPatterns(stack, multiBufferSource, i, j, model.plate(), shieldMaterial,
//                false, list, itemStack.hasFoil());
//
//        graphics.pose().popPose();
//    }
}
