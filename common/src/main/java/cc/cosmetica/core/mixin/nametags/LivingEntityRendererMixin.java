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

package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.impl.NametagRenderer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Third-person nametag.
 * Goes along with {@link InventoryScreenMixin} which disables the third person nametag in the inventory screen.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(
			method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private void shouldShowName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		boolean thirdPerson = Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON;
		if (thirdPerson && NametagRenderer.shouldShowOwnNametag()
				&& entity == Minecraft.getInstance().getCameraEntity()) cir.setReturnValue(Minecraft.renderNames() && !entity.isVehicle());
	}
}
