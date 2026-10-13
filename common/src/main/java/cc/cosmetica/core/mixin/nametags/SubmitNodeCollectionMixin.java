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

import cc.cosmetica.core.impl.IconSubmitter;
import cc.cosmetica.core.impl.IconsMixinImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements IconSubmitter {
    @Shadow @Final
    public SimpleFeatureRenderPhase nameTags;

    @Shadow
    @Final
    public TranslucentFeatureRenderPhase seeThrough;

    @Shadow
    @Final
    public SimpleFeatureRenderPhase solid;
    @Unique
    private @Nullable IconSubmission cosmeticacore$preparedIcon = null;

    @Override
    public void cosmeticacore$submitIcon(IconSubmission submission) {
        this.cosmeticacore$preparedIcon = submission;
    }

    @Override
    public @org.jetbrains.annotations.Nullable IconSubmission cosmeticacore$getPreparedIcon() {
        return this.cosmeticacore$preparedIcon;
    }

    @Inject(at = @At("RETURN"), method = "submitNameTag")
    private void onSubmitNametag(PoseStack stack,
                                 Vec3 nameTagAttachment,
                                 int offset,
                                 Component name,
                                 boolean seeThrough,
                                 int lightCoords,
                                 CameraRenderState camera,
                                 CallbackInfo ci) {

        /*
        -> whether to use solid instead of nameTag
        return content instanceof TextFeatureRenderer.Content.Text text
			&& ARGB.alpha(text.color()) == 255
			&& ARGB.alpha(text.backgroundColor()) == 0
			&& RenderSystem.isRenderingLevel;
         */
        // see through submits a nametag with -1, 0
        float backgroundAlpha = Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F);
        float textAlpha = Math.max((backgroundAlpha + 0.75F) * 0.5F, 0.5F);
        int backgroundColor = ARGB.color(backgroundAlpha, -16777216);
        int textColor = ARGB.color(textAlpha, -1);

        boolean useSolidForMain = RenderSystem.isRenderingLevel && (seeThrough || (ARGB.alpha(textColor) == 255 && ARGB.alpha(backgroundColor) == 0));

        boolean clear = IconsMixinImpl.addIcons(
                this.cosmeticacore$preparedIcon,
                useSolidForMain ? this.solid : this.nameTags,
                this.seeThrough,
                seeThrough
        );

        if (clear) {
            this.cosmeticacore$preparedIcon = null;
        }
    }
}
