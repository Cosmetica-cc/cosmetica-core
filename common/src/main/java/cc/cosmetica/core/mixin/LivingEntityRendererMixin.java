/*
 * Copyright 2024 Cosmetica
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

package cc.cosmetica.core.mixin;

import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add the custom layers for rendering cosmetics on humanoid models.
 */
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("rawtypes")
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected abstract boolean addLayer(RenderLayer arg);

	@Inject(at=@At("TAIL"), method="<init>")
	private void init(EntityRenderDispatcher entityRenderDispatcher, EntityModel entityModel, float f, CallbackInfo info) {
		if (entityModel instanceof HumanoidModel) {
			this.addLayer(new HumanoidAccessoriesLayer((LivingEntityRenderer)(Object)this));
		}
	}
}
