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

package cc.cosmetica.core.mixin.network;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.OutfitCosmetics;
import cc.cosmetica.core.builtin.OutfitCosmeticsHolder;
import cc.cosmetica.core.builtin.manager.ArmourStandCosmeticManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(ArmorStand.class)
public abstract class ArmourStandMixin extends LivingEntity implements OutfitCosmeticsHolder {
	protected ArmourStandMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Unique
	private Component cosmeticacore$customName;
	@Unique
	@Nullable
	private CompletableFuture<? extends Cosmetics> cosmeticacore$armourCosmetics;
	@Unique
	@Nullable
	private UUID cosmeticacore$subscribedID;

	@Nullable
	@Override
	public Cosmetics cosmeticacore$getOutfitCosmetics() {
		return this.cosmeticacore$armourCosmetics == null ? null : this.cosmeticacore$armourCosmetics.getNow(null);
	}

	@Override
	public void cosmeticacore$reloadCosmetics() {
		this.cosmeticacore$customName = null;
	}

	@Override
	public @Nullable UUID cosmeticacore$getSubscribedID() {
		return this.cosmeticacore$subscribedID;
	}

	@Override
	public void cosmeticacore$setSubscribedID(UUID uuid) {
		this.cosmeticacore$subscribedID = uuid;
	}

	@Inject(at = @At("RETURN"), method = "tickHeadTurn")
	private void onTick(CallbackInfo ci) {
		if (this.level().isClientSide()) { // don't compute on the server
			Component customName = this.getCustomName();

			if (this.cosmeticacore$customName != customName) { // assuming name instance won't be changed
				// FIXME should this null check be back-ported?
				String text = customName == null ? null : customName.getContents() instanceof PlainTextContents ? ((PlainTextContents) customName.getContents()).text() : null;

				if (text != null) {
					this.cosmeticacore$customName = customName;

					// try look up cosmetics
					String uuid = ArmourStandCosmeticManager.isOutfitUuid(text);

					if (uuid == null) {
						this.cosmeticacore$armourCosmetics = null;
					} else {
						this.cosmeticacore$armourCosmetics = OutfitCosmetics.getAsyncById(uuid);
					}
				} else {
					this.cosmeticacore$customName = null;
					this.cosmeticacore$armourCosmetics = null;
				}
			}
		}
	}
}
