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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.ArmorStandModel;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;

import java.util.Optional;

/**
 * Force arms to show on armour stands that have arm cosmetics.
 */
@Mixin(ArmorStandModel.class)
public abstract class ArmourStandModelMixin extends ArmorStandArmorModel {
	public ArmourStandModelMixin(float f) {
		super(f);
	}

	@Inject(at = @At("RETURN"), method="setupAnim(Lnet/minecraft/world/entity/decoration/ArmorStand;FFFFF)V")
	private void afterSetupAnim(ArmorStand armorStand, float f, float g, float h, float i, float j, CallbackInfo ci) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(armorStand);

		if (cosmetics.isPresent()) {
			for (Accessory accessory : cosmetics.get().getAccessories()) {
				// if any arm cosmetics are used, force enable arms
				if (accessory.getAttachment() == AttachmentEnum.LEFT_ARM ||
						accessory.getAttachment() == AttachmentEnum.RIGHT_ARM) {
					this.leftArm.visible = true;
					this.rightArm.visible = true;
					return;
				}
			}
		}
	}
}
