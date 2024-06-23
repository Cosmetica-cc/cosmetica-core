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
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import gg.cloaks.javaclient.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.*;

public class ApiCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer && ((ApiCosmeticsHolder)entity).cosmeticacore$getCosmetics() != null;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return ((ApiCosmeticsHolder)entity).cosmeticacore$getCosmetics();
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
					Logging.getInstance().error("Error fetching player data by name/id.", e);
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
					Logging.getInstance().error("Error fetching player data for texture packet.", e);
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
			ApiCosmetics cosmetics = new ApiCosmetics().updateCosmetics(response);
			// store on the player
			holder.cosmeticacore$setCosmetics(cosmetics);
		}
	}

	/**
	 * The Cosmetics stored on the player from the API.
	 * NOTE: Do not keep non-weak references to this outside the player mixin itself for garbage collection reasons.
	 */
	public static final class ApiCosmetics implements Cosmetics {
		private ApiCosmetics() {
			// default values
			this.accessories = new ArrayDeque<>();
		}

		public ApiCosmetics updateCosmetics(PlayerResponse response) {
			// read accessories
			if (response.isIsUser()) {
				CosmeticaUser user = response.getUser();

				// convert data
				assert user != null; // response.isIsUser()
				Outfit outfit = user.getOutfit();

				if (outfit != null) {
					List<Accessory> accessories = new ArrayList<>();

					for (OutfitAccessory accessory : outfit.getAccessories()) {
						CosmeticaModel model = CosmeticaModel.getOrCreateModel(
								accessory.getAccessory().getId(),
								accessory.getAccessory().getModel(),
								accessory.getAccessory().getTexture(),
								accessory.getAccessory().getTicksPerFrame().intValue(),
								accessory.getAccessory().getFrames().intValue()
						);

						List<BigDecimal> offset = accessory.getOffset();

						accessories.add(new Accessory(
								accessory.getAccessory().getName(),
								accessory.getAccessory().getAttachment(),
								accessory.isMirrored(),
								model,
								attachmentTransform(
										accessory.getAccessory().getAttachment(),
										offset.get(0).doubleValue(),
										offset.get(1).doubleValue(),
										offset.get(2).doubleValue()
								))
						);
					}
					// TODO once all accessories load, pop accessories
					// This does mean if a newer accessory loads, the one in the middle which hasn't finished downloadig will show
					// instead have a way of removing all items en

					this.accessories.add(accessories);
				}
			}

			return this;
		}

		// this will be changed if outfit change is received from server.
		// 1. replace playerresponse data on player (probably not necessary with code structure but good practise)
		// 2. tell apicosmeticamanager to replace cosmetics (if it's an ApiCosmetics)
		private final Queue<List<Accessory>> accessories;

		@Override
		public Collection<Accessory> getAccessories() {
			return this.accessories.peek();
		}

		/**
		 * Transform x, y, and z offsets from the server renderer space to world space.
		 * @return a Vec3 with the render offset.
		 */
		private static Vec3 attachmentTransform(AttachmentEnum attachment, double x, double y, double z) {
			double dy;
			double dx;
			
			switch (attachment) {
			case HEAD:
				dy = 8.0;
				dx = 8.0;
				break;
			case RIGHT_ARM:
				dy = 0.0;
				dx = 8.5;
				break;
			case LEFT_ARM:
				dy = 0.0;
				dx = 7.5;
				break;
			default:
				dy = -2.0;
				dx = 8.0;
				break;
			}

			return new Vec3(
					(x + dx) / 16.0,
					(y + dy) / 16.0,
					(z + 8.0) / 16.0
			);
		}
	}
}
