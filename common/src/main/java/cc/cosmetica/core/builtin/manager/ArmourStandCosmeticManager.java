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

package cc.cosmetica.core.builtin.manager;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.builtin.OutfitCosmeticsHolder;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Manages cosmetics for armour stands that represent outfits.
 */
public class ArmourStandCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(Either either) {
		if (either.entity == null) {
			return false;
		}
		LivingEntity entity = either.entity;
		return entity instanceof OutfitCosmeticsHolder && ((OutfitCosmeticsHolder) entity).cosmeticacore$getOutfitCosmetics() != null;
	}

	@Override
	public Cosmetics getCosmetics(Either either) {
		assert either.entity != null;
		return ((OutfitCosmeticsHolder) either.entity).cosmeticacore$getOutfitCosmetics();
	}

	@Override
	public void onAssign(Either either) {
		assert either.entity != null;
		final LivingEntity entity = either.entity;

		Cosmetics outfitCosmetics = ((OutfitCosmeticsHolder) entity).cosmeticacore$getOutfitCosmetics();
		WeakReference<LivingEntity> entityRef = new WeakReference<>(entity);

		if (outfitCosmetics != null && outfitCosmetics.getOutfitId().isPresent()) {
			try {
				UUID uuid = UUID.fromString(outfitCosmetics.getOutfitId().get());
				((OutfitCosmeticsHolder) entity).cosmeticacore$setSubscribedID(uuid);

				Logging.getInstance().debug(LoggingCategory.LOOKUP, "Subscribing to outfit updates for {}", uuid);

				CosmeticaAPI.subscribe(
						CosmeticaAPI.SubscriptionEvent.OUTFIT,
						uuid,
						ARMOUR_STAND_MANAGER,
						() -> {
							// this is probably over-cautious. onRevoke() should always be called before the LE is removed
							LivingEntity entity_ = entityRef.get();
							if (entity_ != null) {
								((OutfitCosmeticsHolder) entity_).cosmeticacore$reloadCosmetics();
							}
						});
			} catch (IllegalArgumentException e) {
				Logging.getInstance().warn(
						"Could not subscribe to outfit {}: {}: {}",
						outfitCosmetics.getOutfitId().get(),
						e.getClass().getName(), e.getMessage());
			}
		}
	}

	@Override
	public void onRevoke(Either either) {
		assert either.entity != null;
		UUID uuid = ((OutfitCosmeticsHolder)either.entity).cosmeticacore$getSubscribedID();
		if (uuid != null) {
			Logging.getInstance().debug(LoggingCategory.LOOKUP, "Unsubscribing from outfit updates for {}", uuid);
			CosmeticaAPI.unsubscribe(CosmeticaAPI.SubscriptionEvent.OUTFIT, uuid, ARMOUR_STAND_MANAGER);
		}
	}

	/**
	 * Get whether something is an outfit uuid. Dashed or dashless.
	 * @param uuidString the text to check.
	 * @return the uuid string to use if the string is a valid potential outfit id. Otherwise null.
	 */
	public static @Nullable String isOutfitUuid(String uuidString) {
		if (uuidString.length() == 32) {
			// Add dashes to form a standard UUID format
			uuidString = uuidString.substring(0, 8) + "-" +
					uuidString.substring(8, 12) + "-" +
					uuidString.substring(12, 16) + "-" +
					uuidString.substring(16, 20) + "-" +
					uuidString.substring(20);
		}

		// only 36-length strings are valid uuids
		if (uuidString.length() != 36) {
			return null;
		}

		// Verify if the string is a valid UUID and check if it's version 4
		try {
			UUID uuid = UUID.fromString(uuidString);
			return uuid.version() == 4 ? uuid.toString() : null;
		} catch (IllegalArgumentException e) {
			return null; // Invalid UUID format
		}
	}

	private static final ResourceLocation ARMOUR_STAND_MANAGER = new ResourceLocation("cosmetica", "outfits");
}
