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
import cc.cosmetica.core.api.PlayerCosmetics;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.impl.UUIDs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cosmetics received from the API for yourself. Will use cached cosmetics if none can be obtained from the API.
 */
public class SelfCosmeticManager implements CosmeticManager {
	public SelfCosmeticManager() {
		CosmeticaAPI.subscribe(
				CosmeticaAPI.SubscriptionEvent.PLAYER,
				UUIDs.fromString(Minecraft.getInstance().getUser().getUuid()),
				SELF_MANAGER, () -> ApiCosmeticManager.lookUpGameProfile(Minecraft.getInstance().getUser().getGameProfile()));
	}

	private static PlayerCosmetics cosmetics;

	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof LocalPlayer && cosmetics != null;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return cosmetics;
	}

	public static void set(PlayerCosmetics newCosmetics) {
		cosmetics = newCosmetics;
		MasterCosmeticManager.post(null, newCosmetics);
	}
	// todo detect account switching to change cosmetics

	private static final ResourceLocation SELF_MANAGER = new ResourceLocation("cosmetica", "self");
}
