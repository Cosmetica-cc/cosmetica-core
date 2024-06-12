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

import cc.cosmetica.core.api.CosmeticaAPI;

/**
 * Caches cosmetica data fetched from the Cosmetica API.
 *
 * Used for implementing the main CosmeticManager.
 */
public class AbstractClientPlayerMixin {
	private void e$e() {
		CosmeticaAPI.getInstance().playersControllerSubmitTexturePacket();
		/**
		 * In order to take the load off the servers (and avoid rate limits), we forward the mojang api response used in
		 * game instead of using a network of workers. This is a more long-term sustainable approach to fetching username
		 * and texture data.
		 * This is perfectly secure on both ends. No sensitive data is exposed to the server, and the server can verify
		 * via the signature that the info hasn't been tampered.
		 * This should be called offthread.
		 * @param profile the game profile.
		 */
		// GameProfile
		//final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
	}
}
