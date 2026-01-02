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

package cc.cosmetica.core.mixin.cape;

import cc.cosmetica.core.render.NonHumanCapeLayer;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds capes to armour stands.
 */
@Mixin(ArmorStandRenderer.class)
public abstract class ArmourStandRendererMixin extends LivingEntityRenderer<ArmorStand, ArmorStandArmorModel> {
	public ArmourStandRendererMixin(EntityRendererProvider.Context context, ArmorStandArmorModel entityModel, float f) {
		super(context, entityModel, f);
	}

	@Inject(at = @At("RETURN"), method="<init>")
	private void addCapesToArmourStands(EntityRendererProvider.Context context, CallbackInfo ci) {
		// TODO is this right
		this.addLayer(new NonHumanCapeLayer<>(this, context.bakeLayer(ModelLayers.PLAYER)));
	}
}
