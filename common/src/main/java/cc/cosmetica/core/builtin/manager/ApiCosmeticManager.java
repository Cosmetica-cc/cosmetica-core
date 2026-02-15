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
import cc.cosmetica.core.builtin.ApiCosmeticsHolder;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.model.PlayerResponse;
import gg.cloaks.javaclient.model.TexturePacketDto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Stores cosmetics from the API for other players.
 */
public class ApiCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(Either either) {
		return either.remotePlayerInfo != null && ((ApiCosmeticsHolder)either.remotePlayerInfo).cosmeticacore$getApiCosmetics() != null;
	}

	@Override
	public Cosmetics getCosmetics(Either entity) {
		return ((ApiCosmeticsHolder)entity.remotePlayerInfo).cosmeticacore$getApiCosmetics();
	}

	@Override
	public void onRevoke(Either entity) {
		Logging.getInstance().debug(LoggingCategory.LOOKUP, "Unsubscribing to API player updates for {}", entity.getId());
		CosmeticaAPI.unsubscribe(CosmeticaAPI.SubscriptionEvent.PLAYER, entity.getId(), API_MANAGER);
		// TODO clear built models to store minimal data when not owning a player (in case switch to another manager)
	}

	private static final char[] ALLOWED_USERNAME_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz".toCharArray();

	/**
	 * Look up and store Cosmetica data for the given game profile.
	 * @param profileIn the profile to look up and store data for.
	 */
	public static void lookUpGameProfile(GameProfile profileIn) {
		final Property textureProperty = Iterables.getFirst(profileIn.getProperties().get("textures"), null);

		final GameProfile profile;
		if (profileIn.getId() == null) {
			Logging.getInstance().warn("(Cosmetica) Profile has no uuid, {}", profileIn.getName());
			// use username to look up
			profile = new GameProfile(UUID.nameUUIDFromBytes(profileIn.getName().getBytes(StandardCharsets.UTF_8)), profileIn.getName());
		} else {
			profile = profileIn;
		}
		UUID uuid = profile.getId();

		if (textureProperty == null || !textureProperty.hasSignature()) {
			// use request via uuid or name if we cannot use the packet
			String lookupBy;

			// v3 uuid is for offline players
			if (uuid.version() == 3) {
				lookupBy = profile.getName();

				// skip names that aren't actual usernames
				// verify length
				if (lookupBy.isEmpty() || lookupBy.length() > 16) {
					return;
				}
				for (char c : lookupBy.toCharArray()) {
					// if the character isn't allowed, skip
					if (Arrays.binarySearch(ALLOWED_USERNAME_CHARACTERS, c) < 0) {
						return;
					}
				}
			} else {
				lookupBy = uuid.toString();
			}

			Logging.getInstance().debug(LoggingCategory.LOOKUP, "Looking up user by {}", lookupBy);

			CosmeticaAPI.players().requestAsync(api -> {
				try {
					return api.getPlayer(lookupBy);
				} catch (ApiException e) {
					if (e.getCode() != 404) {
						Logging.getInstance().error("Error fetching player data by name/id.", e);
					}
					return null;
				}
			}).exceptionally(e -> {
				Logging.getInstance().error("Error fetching player data by name/id.", e);
				return null;
			}).thenAcceptAsync(r -> {if (r != null) updatePlayer(profile, r, uuid.version() == 3);}, Minecraft.getInstance()); // TODO null check (if player leaves/worldchange, but warn. do we know skin load and player add order?)
		} else {
			// In order to take the load off the servers (and avoid rate limits), we forward the mojang api response used in
			// game instead of using a network of workers. This is a more long-term sustainable approach to fetching username
			// and texture data for the servers.

			// Make a request for player info using the texture packet endpoint.
			CosmeticaAPI.players().requestAsync(api -> {
				try {
					return api.submitTexturePacket(
							new TexturePacketDto()
									.value(textureProperty.value())
									.signature(textureProperty.signature())
					);
				} catch (ApiException e) {
					if (e.getCode() != 404) {
						Logging.getInstance().error("Error fetching player data ({}) for texture packet.", e, uuid);
					}
					return null;
				}
			}).thenAcceptAsync(r -> updatePlayer(profile, r, false), Minecraft.getInstance());
		}
	}

	/**
	 * Save cosmetics on the player given the given response. Please run this on the render thread.
	 * @param profile the profile for which to update the player.
	 * @param response the response received from the server.
	 * @param creaked whether the player was looked up with username.
	 */
	private static void updatePlayer(GameProfile profile, @Nullable PlayerResponse response, boolean creaked) {
		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("Cannot call updatePlayer() off the render thread!");
		}

		if (response == null) {
			Logging.getInstance().debug(LoggingCategory.LOOKUP, "Skipping update for {} (no data)", profile);
			return;
		}

		// check if game profile id matches user's id
		if (profile.equals(Minecraft.getInstance().getGameProfile())) {
			// configure own cosmetics
			Logging.getInstance().debug(LoggingCategory.LOOKUP, "Updating cosmetics for self, {}", profile);
			SelfCosmeticManager.update(response);
			return;
		}

		// Logic for players in world
		Level level = Minecraft.getInstance().level;
		if (level == null) {
			Logging.getInstance().warn("Skipping cosmetics update for {} (no level)", profile);
			return;
		}

		Logging.getInstance().debug(LoggingCategory.LOOKUP, "Updating cosmetics for {}", profile);
		Player player = level.getPlayerByUUID(profile.getId());

		// catch a case where the game profile is not quite the same, but it's still our player
		if (player == Minecraft.getInstance().player) {
			// configure own cosmetics
			SelfCosmeticManager.update(response);
		} else {
			assert Minecraft.getInstance().player != null;

			// Remote player
			PlayerInfo playerInfo = Minecraft.getInstance().player.connection.getPlayerInfo(profile.getId());
			if (playerInfo == null && profile.getName() != null) {
				playerInfo = Minecraft.getInstance().player.connection.getPlayerInfo(profile.getName());
			}

			if (playerInfo == null) {
				Logging.getInstance().warn("Tried to configure cosmetics of {}/{} no matching player found!", profile.getName(), profile.getId());
			} else {
				// create a new ApiCosmetics
				PlayerCosmetics cosmetics = PlayerCosmetics.fromResponse(response);

				// store on the player
				ApiCosmeticsHolder holder = ((ApiCosmeticsHolder) playerInfo);
				holder.cosmeticacore$setApiCosmetics(cosmetics);

				if (creaked) {
					CosmeticaAPI.subscribe(CosmeticaAPI.SubscriptionEvent.PLAYER_CREAKED, profile.getName(), API_MANAGER, () -> lookUpGameProfile(profile));
				} else {
					CosmeticaAPI.subscribe(CosmeticaAPI.SubscriptionEvent.PLAYER, profile.getId(), API_MANAGER, () -> lookUpGameProfile(profile));
				}
			}
		}
	}

	private static ResourceLocation API_MANAGER = ResourceLocation.fromNamespaceAndPath("cosmetica", "api");
}
