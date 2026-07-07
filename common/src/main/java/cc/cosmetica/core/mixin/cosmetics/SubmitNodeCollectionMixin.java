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
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionMixin implements CosmeticaModelSubmitter {
    @Shadow
    @Final
    public TranslucentFeatureRenderPhase translucentBlocksAndItems;

    @Shadow
    @Final
    public SimpleFeatureRenderPhase solid;

    @Shadow
    @Nullable
    private static RenderType getOutlineRenderType(RenderType renderType) {
        throw new AssertionError("Shadow Fail (getOutlineRenderType by Cosmetica)");
    }

    @Shadow
    @Final
    public SimpleFeatureRenderPhase outline;

    // this is more verbose but should cause less potential conflicts than an @ModifyConstant on tint colour.
    @Override
    public void submitCosmeticaModel(PoseStack stack,
                                     RenderType renderType,
                                     List<BlockStateModelPart> modelPartList,
                                     int packedLight,
                                     int tintColour,
                                     int outlineColour) {
        // based off submitBlockModel
        PoseStack.Pose pose = stack.last().copy();

        if (!renderType.isOutline()) {
            BlockModelFeatureRenderer.Submit submit = new BlockModelFeatureRenderer.Submit(
                    pose, renderType, modelPartList, BlockModelRenderState.EMPTY_TINTS, packedLight, OverlayTexture.NO_OVERLAY, tintColour, null
            );
            if (renderType.hasBlending()) {
                this.translucentBlocksAndItems.submit((TranslucentSubmit)submit);
            } else {
                this.solid.submit(submit);
            }
        }

        if (outlineColour != 0) {
            RenderType outlineRenderType = getOutlineRenderType(renderType);
            if (outlineRenderType != null) {
                this.outline
                        .submit(
                                new BlockModelFeatureRenderer.Submit(
                                        pose, outlineRenderType, modelPartList, BlockModelRenderState.EMPTY_TINTS, 15728880, OverlayTexture.NO_OVERLAY, outlineColour, null
                                )
                        );
            }
        }
    }
}
