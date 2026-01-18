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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.HasCosmeticsRenderState;
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Add the custom layers for rendering cosmetics on humanoid models, and makes australians upside down.
 * 1.21.4+ : Add cosmetics to the entity render state.
 */
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("rawtypes")
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected abstract boolean addLayer(RenderLayer arg);

	@Inject(at = @At("TAIL"), method = "<init>")
	private void init(EntityRendererProvider.Context context, EntityModel entityModel, float f, CallbackInfo ci) {
		if (entityModel instanceof HumanoidModel) {
			this.addLayer(new HumanoidAccessoriesLayer<>((LivingEntityRenderer) (Object) this, context.getEquipmentAssets()));
		}
	}

	// ========== //
	// Aussie RSE //
	// ========== //

	@Inject(at = @At("HEAD"), method = "isEntityUpsideDown", cancellable = true)
	private void checkAustralians(LivingEntity entity, CallbackInfoReturnable<Boolean> info) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(entity);

		if (cosmetics.isPresent() && cosmetics.get().isUpsideDown()) {
			info.setReturnValue(true);
		}
	}

	@Inject(at = @At("RETURN"), method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V")
	private void onExtractRenderState(LivingEntity entity, LivingEntityRenderState renderState, float f, CallbackInfo info) {
		HasCosmeticsRenderState cosmeticsRenderState = (HasCosmeticsRenderState) renderState;
		cosmeticsRenderState.cosmeticacore$extractCosmeticsOf(entity);
	}
}
