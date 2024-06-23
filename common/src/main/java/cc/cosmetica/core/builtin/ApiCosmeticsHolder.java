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

package cc.cosmetica.core.builtin;

import cc.cosmetica.core.builtin.manager.ApiCosmeticManager;

/**
 * Duck interface to access and modify the api cosmetics on a player.
 * This is ok as we will ensure only minimal data is stored if the Api Cosmetic Manager is not selected,
 * and this will be correctly garbage collected when the player is removed from the world.
 */
public interface ApiCosmeticsHolder {
	ApiCosmeticManager.ApiCosmetics cosmeticacore$getCosmetics();
	void cosmeticacore$setCosmetics(ApiCosmeticManager.ApiCosmetics cosmetics);
}
