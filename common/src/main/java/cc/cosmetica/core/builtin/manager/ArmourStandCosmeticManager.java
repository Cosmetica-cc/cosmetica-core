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

package cc.cosmetica.core.builtin.manager;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Manages cosmetics for armour stands that represent outfits.
 */
public class ArmourStandCosmeticManager implements CosmeticManager {
	boolean hasOutfit = false;

	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof ArmorStand && hasOutfit; // todo actual has outfit
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return null;
	}

	/**
	 * Get whether something is an outfit id. That is, a dashless UUID.
	 * @param text
	 * @return
	 */
	public static boolean isOutfitId(String text) {
		// Check length (without dashes, a UUID should have exactly 32 characters)
		if (text.length() != 32) {
			return false;
		}

		// Check if all characters are hexadecimal
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!isHexadecimalChar(c)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isHexadecimalChar(char c) {
		return (c >= '0' && c <= '9') ||
				(c >= 'a' && c <= 'f') ||
				(c >= 'A' && c <= 'F');
	}
}
