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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
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
    @Shadow @Final private NameTagFeatureRenderer.Storage nameTagSubmits;

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
    private void onSubmitNametag(PoseStack stack, Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (this.cosmeticacore$preparedIcon != null) {
            IconSubmitter iconSubmitter0 = (IconSubmitter) (Object) ((NameTagFeatureRendererStorageAccessor)this.nameTagSubmits).getNameTagSubmitsNormal().getLast();
            iconSubmitter0.cosmeticacore$submitIcon(this.cosmeticacore$preparedIcon);
            if (bl) {
                IconSubmitter iconSubmitter1 = (IconSubmitter) (Object) ((NameTagFeatureRendererStorageAccessor)this.nameTagSubmits).getNameTagSubmitsSeethrough().getLast();
                iconSubmitter1.cosmeticacore$submitIcon(this.cosmeticacore$preparedIcon);
            }
            this.cosmeticacore$preparedIcon = null;
        }
    }
}
