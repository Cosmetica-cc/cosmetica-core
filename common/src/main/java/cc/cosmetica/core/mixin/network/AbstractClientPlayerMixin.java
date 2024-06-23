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

import cc.cosmetica.core.builtin.ApiCosmeticsHolder;
import cc.cosmetica.core.builtin.manager.ApiCosmeticManager;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Caches cosmetica data fetched from the Cosmetica API.
 * Used for implementing the main CosmeticManager.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin implements ApiCosmeticsHolder {
	@Unique
	private ApiCosmeticManager.ApiCosmetics cosmeticacore$apiCosmetics;

	@Override
	public ApiCosmeticManager.ApiCosmetics cosmeticacore$getCosmetics() {
		return this.cosmeticacore$apiCosmetics;
	}

	@Override
	public void cosmeticacore$setCosmetics(ApiCosmeticManager.ApiCosmetics cosmetics) {
		this.cosmeticacore$apiCosmetics = cosmetics;
	}
}
