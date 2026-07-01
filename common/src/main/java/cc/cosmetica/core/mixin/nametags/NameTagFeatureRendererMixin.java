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
import cc.cosmetica.core.impl.NametagRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @Inject(method = "buildGroup",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font$PreparedText;visit(Lnet/minecraft/client/gui/Font$GlyphVisitor;)V"))
    private void onBuildGroup(final FeatureFrameContext context, final List<NameTagFeatureRenderer.Submit> submits, CallbackInfo info,
                              @Local NameTagFeatureRenderer.Submit submit) {
        IconSubmitter.IconSubmission icon = ((IconSubmitter) (Object) submit).cosmeticacore$getPreparedIcon();
        if (icon != null) {
            NametagRenderer.prepareIcon(icon.icon(), icon.transparent(), icon.readjustTextPosition());
        }
    }
}
