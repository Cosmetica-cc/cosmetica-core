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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.*;
import cc.cosmetica.core.builtin.ApiCosmeticsHolder;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class ApiCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer && false;//fixme debug memory  && ((ApiCosmeticsHolder)entity).cosmeticacore$getApiCosmetics() != null;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return null;//fixme debug memory((ApiCosmeticsHolder)entity).cosmeticacore$getApiCosmetics();
	}

	@Override
	public void onAssign(LivingEntity entity) {
		System.out.println("i am now assigned this entity " + entity.getName());
	}

	@Override
	public void onRevoke(LivingEntity entity) {
		System.out.println("i am no longer assigned this entity " + entity.getName());
		// TODO clear built models to store minimal data when not owning a player (in case switch to another manager)
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
				// TODO check valid name
			}
			Logging.getInstance().debug("Looking up user by {}", lookupBy);

			CosmeticaAPI.performAsync(api -> {
				try {
					return api.playersControllerGetPlayer(lookupBy);
				} catch (ApiException e) {
					if (e.getCode() != 404) {
						Logging.getInstance().error("Error fetching player data by name/id.", e);
					}
					return null;
				}
			}).exceptionally(e -> {
				Logging.getInstance().error("Error fetching player data by name/id.", e);
				return null;
			}).thenAccept(r -> {if (r != null)Minecraft.getInstance().tell(() -> updatePlayer(profile, r));}); // TODO null check (if player leaves/worldchange, but warn. do we know skin load and player add order?)
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
					if (e.getCode() != 404) {
						Logging.getInstance().error("Error fetching player data for texture packet.", e);
					}
					return null;
				}
			}).thenAccept(r -> Minecraft.getInstance().tell(() -> updatePlayer(profile, r)));
		}
	}

	/**
	 * Save cosmetics on the player given the given response. Please run this on the render thread.
	 * @param profile the profile for which to update the player.
	 * @param response the response received from the server.
	 */
	private static void updatePlayer(GameProfile profile, @Nullable PlayerResponse response) {
		if (response == null) {
			Logging.getInstance().debug("Skipping update for {} (no data)", profile);
			return;
		}

		Level level = Minecraft.getInstance().level;
		if (level == null) {
			Logging.getInstance().debug("Skipping update for {} (no level)", profile);
			return;
		}

		Logging.getInstance().debug("Updating cosmetics for {}", profile);
		Player player = level.getPlayerByUUID(profile.getId());

		if (player == null) {
			Logging.getInstance().warn("Tried to configure cosmetics of {}/{} no matching player found!", profile.getName(), profile.getId());
		} else {
			ApiCosmeticsHolder holder = ((ApiCosmeticsHolder) player);

			// create a new ApiCosmetics
			ApiCosmetics cosmetics = ApiCosmetics.fromResponse(response);
			// store on the player
			holder.cosmeticacore$setApiCosmetics(cosmetics);
		}
	}

	/**
	 * The Cosmetics stored on the player from the API.
	 * NOTE: Do not keep non-weak references to this outside the player mixin itself for garbage collection reasons.
	 */
	public static final class ApiCosmetics implements Cosmetics {
		private ApiCosmetics(@Nullable Outfit outfit, @Nullable Lore lore) {
			// lore
			if (lore == null) {
				this.lore = null;
			} else {
				this.lore = lore.getFormatted().replaceAll("&", "§");
			}

			// outfit
			this.accessories = new ArrayList<>();

			if (outfit != null) {
				// save outfit
				this.outfitId = outfit.getId();
				this.outfitName = outfit.getName();

				// set cape
				@Nullable AnimatedTextureCosmetic cloak = outfit.getCloak();
				@Nullable AnimatedTextureCosmetic elytra = outfit.getElytra();

				if (cloak != null) this.cloak = CosmeticaModel.getOrCreateImage("cape", cloak); else this.cloak = CachedImage.NO_TEXTURE;
				if (elytra != null) this.elytra = CosmeticaModel.getOrCreateImage("cape", elytra); else this.elytra = CachedImage.NO_TEXTURE;

				// equip acessories
				for (OutfitAccessory accessory : outfit.getAccessories()) {
					Accessory k =Accessory.fromOutfitAccessory(accessory);
//					MasterCosmeticManager.HTTP_THREAD_POOL.submit(() -> { // debug memory
//						try {
//							Thread.sleep(10000);
//							System.out.println("ok dont need " + k.getName() + " anymore");
//						} catch (InterruptedException e) {
//							throw new RuntimeException(e);
//						}
//					});
					accessories.add(k);
				}
			} else {
				// no outfit
				this.outfitName = null;
				this.outfitId = null;
				this.cloak = CachedImage.NO_TEXTURE;
				this.elytra = CachedImage.NO_TEXTURE;
			}
		}

		// this will be changed if outfit change is received from server.
		// 1. replace playerresponse data on player (probably not necessary with code structure but good practise)
		// 2. tell apicosmeticamanager to replace cosmetics (if it's an ApiCosmetics)
		private final CachedImage cloak;
		private final CachedImage elytra;
		private final List<Accessory> accessories;
		private final @Nullable String outfitName, outfitId;
		private final @Nullable String lore;

		// TODO I might not use Optional to prevent this constant object creation
		@Override
		public Optional<String> getOutfitId() {
			return Optional.ofNullable(this.outfitId);
		}

		@Override
		public Optional<String> getOutfitName() {
			return Optional.ofNullable(this.outfitName);
		}

		@Override
		public CachedImage getCloak() {
			return this.cloak;
		}

		@Override
		public CachedImage getElytra() {
			return this.elytra;
		}

		@Override
		public Collection<Accessory> getAccessories() {
			return this.accessories;
		}

		@Override
		public Optional<String> getLore() {
			return Optional.ofNullable(this.lore);
		}

		@Override
		public boolean isUpsideDown() {
			return false;
		}

		@Override
		public void enqueue(Runnable task, Runnable onFail) {
			//todo actual enqueue
			task.run();
		}

		public static ApiCosmetics fromResponse(PlayerResponse response) {
			// read accessories
			if (response.isIsUser()) {
				CosmeticaUser user = response.getUser();

				// read data from the response
				assert user != null; // response.isIsUser()

				return new ApiCosmetics(user.getOutfit(), user.getLore());
			} else {
				CosmeticaPlayer player = response.getPlayer();

				assert player != null; // !response.isUser()

				return new ApiCosmetics(null, null);
			}
		}
	}
}
