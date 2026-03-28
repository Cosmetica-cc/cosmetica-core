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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @Inject(method = "renderTranslucent",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
    private void onRenderFontBatch0(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit nameTagSubmit) {
        IconSubmitter.IconSubmission icon = ((IconSubmitter) (Object) nameTagSubmit).cosmeticacore$getPreparedIcon();
        if (icon != null) {
            NametagRenderer.prepareIcon(icon.icon(), icon.transparent(), icon.readjustTextPosition());
        }
    }

    @Inject(method = "renderTranslucent",
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
    private void onRenderFontBatch1(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit nameTagSubmit) {
        IconSubmitter.IconSubmission icon = ((IconSubmitter) (Object) nameTagSubmit).cosmeticacore$getPreparedIcon();
        if (icon != null) {
            NametagRenderer.prepareIcon(icon.icon(), icon.transparent(), icon.readjustTextPosition());
        }
    }
}
