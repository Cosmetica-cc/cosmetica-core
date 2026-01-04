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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Random;

/**
 * Render utilities for items modified by cosmetica cosmetics.
 */
public class CustomItemRenderer {
    public static void renderShield(PoseStack stack, MultiBufferSource multiBufferSource, ShieldModel model, int i, int j, boolean glint) {
        final Material shieldMaterial = ModelBakery.NO_PATTERN_SHIELD;

        stack.pushPose();
        stack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vertexConsumer = shieldMaterial.sprite()
                .wrap(ItemRenderer.getFoilBufferDirect(multiBufferSource, model.renderType(shieldMaterial.atlasLocation()), true, glint));
        model.handle().render(stack, vertexConsumer, i, j, 1.0F, 1.0F, 1.0F, 1.0F);

        model.plate().render(stack, vertexConsumer, i, j, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer texture = multiBufferSource.getBuffer(RenderType.entityTranslucent(new ResourceLocation("cosmetica-core", "test.png")));

        // draw texture
        ModelPart part = model.plate();

       try {
           Matrix4f m4f = stack.last().pose();
           RandomSource random = RandomSource.create(0);
           ModelPart.Cube cube = part.getRandomCube(random);

           Vector4f vector4f = new Vector4f(cube.minX/16f, cube.minY/16f, cube.minZ/16f - 0.01f, 1.0F);
           vector4f.transform(m4f);
           texture.vertex(vector4f.x(), vector4f.y(), vector4f.z(), 1, 1, 1, 1, 0, 0, j, i, 1, 1, 1);

           vector4f = new Vector4f(cube.maxX/16f, cube.minY/16f, cube.minZ/16f - 0.01f, 1.0F);
           vector4f.transform(m4f);
           texture.vertex(vector4f.x(), vector4f.y(), vector4f.z(), 1, 1, 1, 1, 1, 0, j, i, 1, 1, 1);

           vector4f = new Vector4f(cube.maxX/16f, cube.maxY/16f, cube.minZ/16f - 0.01f, 1.0F);
           vector4f.transform(m4f);
           texture.vertex(vector4f.x(), vector4f.y(), vector4f.z(), 1, 1, 1, 1, 1, 1, j, i, 1, 1, 1);

           vector4f = new Vector4f(cube.minX/16f, cube.maxY/16f, cube.minZ/16f - 0.01f, 1.0F);
           vector4f.transform(m4f);
           texture.vertex(vector4f.x(), vector4f.y(), vector4f.z(), 1, 1, 1, 1, 0, 1, j, i, 1, 1, 1);
       }catch (RuntimeException e) {
           e.printStackTrace();
       }

//        BannerRenderer.renderPatterns(stack, multiBufferSource, i, j, model.plate(), shieldMaterial,
//                false, list, itemStack.hasFoil());

        stack.popPose();
    }
}
