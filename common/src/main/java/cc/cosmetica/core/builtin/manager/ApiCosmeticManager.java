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
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.builtin.CosmeticaPlayerHolder;
import cc.cosmetica.core.impl.Logging;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.model.TexturePacketDto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class ApiCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer && ((CosmeticaPlayerHolder)entity).cosmeticacore$getResponse() != null;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return null;
	}

	/**
	 * Look up and store Cosmetica data for the given game profile.
	 * @param profile the profile to look up and store data for.
	 */
	public static void lookUpGameProfile(GameProfile profile) {
		final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
		UUID uuid = profile.getId();

		if (uuid == null) {
			Logging.getInstance().warn("(Cosmetica) Profile has no uuid, {}", profile.getName());
			return;
		}

		if (textureProperty == null || !textureProperty.hasSignature()) {
			// use request via uuid or name if we cannot use the packet

			String lookupBy;

			if (uuid.version() == 4) {
				lookupBy = uuid.toString();
			} else {
				lookupBy = profile.getName();
			}

			CosmeticaAPI.performAsync(api -> {
				try {
					return api.playersControllerGetPlayer(lookupBy);
				} catch (ApiException e) {
					Logging.getInstance().error("Error fetching player data for texture packet.", e);
					return null;
				}
			}).thenAccept(r -> ((CosmeticaPlayerHolder)Minecraft.getInstance().level.getPlayerByUUID(profile.getId())).cosmeticacore$setResponse(r)); // TODO null check (if player leaves/worldchange, but warn. do we know skin load and player add order?)
		} else {
			// In order to take the load off the servers (and avoid rate limits), we forward the mojang api response used in
			// game instead of using a network of workers. This is a more long-term sustainable approach to fetching username
			// and texture data for the servers.

			// Make a request for player info using the texture packet endpoint.
			CosmeticaAPI.performAsync(api -> {
				try {
					return api.playersControllerSubmitTexturePacket(
							new TexturePacketDto()
									.value(textureProperty.getValue())
									.signature(textureProperty.getSignature())
					);
				} catch (ApiException e) {
					Logging.getInstance().error("Error fetching player data for texture packet.", e);
					return null;
				}
			}).thenAccept(r -> ((CosmeticaPlayerHolder)Minecraft.getInstance().level.getPlayerByUUID(profile.getId())).cosmeticacore$setResponse(r));
		}
	}
}
