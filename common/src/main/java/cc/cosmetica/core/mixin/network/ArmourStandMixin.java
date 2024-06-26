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

package cc.cosmetica.core.mixin.network;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.builtin.manager.ArmourStandCosmeticManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ArmorStand.class)
public abstract class ArmourStandMixin extends LivingEntity {
	protected ArmourStandMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Unique
	private TextComponent cosmeticacore$customName;
	@Unique
	@Nullable
	private CompletableFuture<Cosmetics> cosmeticacore$armourCosmetics;

	@Inject(at = @At("RETURN"), method = "tick")
	private void onTick(CallbackInfo ci) {
		Component customName = this.getCustomName();

		if (this.cosmeticacore$customName != customName) { // assuming name instance won't be changed
			if (customName instanceof TextComponent && ArmourStandCosmeticManager.isOutfitId(((TextComponent)customName).getText())) {
				this.cosmeticacore$customName = (TextComponent) customName;

				this.cosmeticacore$armourCosmetics = null;//ArmourStandCosmeticManager.getCosmetics;
			} else {
				this.cosmeticacore$customName = null;
				this.cosmeticacore$armourCosmetics = null;
			}
		}
	}
}
