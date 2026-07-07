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

import cc.cosmetica.core.impl.CosmeticaModelSubmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin implements CosmeticaModelSubmitter {
    @Shadow
    public abstract OrderedSubmitNodeCollector order(int par1);

    // this is more verbose but should cause less potential conflicts than an @ModifyConstant on tint colour.
    @Override
    public void submitCosmeticaModel(PoseStack stack,
                                     RenderType renderType,
                                     List<BlockStateModelPart> modelPartList,
                                     int packedLight,
                                     int tintColour,
                                     int outlineColour) {
        OrderedSubmitNodeCollector collector = this.order(0);

        if (collector instanceof CosmeticaModelSubmitter submitter) {
            submitter.submitCosmeticaModel(
                    stack, renderType, modelPartList, packedLight, tintColour, outlineColour
            );
        } else {
            collector.submitBlockModel(
                    stack, renderType, modelPartList,
                    BlockModelRenderState.EMPTY_TINTS,
                    // light, overlay, outline
                    packedLight, OverlayTexture.NO_OVERLAY, outlineColour
            );
        }
    }
}
