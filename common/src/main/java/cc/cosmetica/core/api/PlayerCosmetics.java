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

package cc.cosmetica.core.api;

import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import gg.cloaks.javaclient.model.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cosmetics stored on the player from the API.
 */
public final class PlayerCosmetics implements Cosmetics {
	/**
	 * Don't use this directly. Instead, invoke the factory method.
	 * @param outfit the outfit of the player.
	 * @param lore the lore of the player.
	 * @param icon the icon on the player.
	 */
	private PlayerCosmetics(@Nullable Outfit outfit, @Nullable Lore lore, @Nullable Icon icon) {
		// nametag
		this.nametag = new NametagConfig("", "", icon);

		// lore
		if (lore == null) {
			this.lore = null;
		} else {
			this.lore = new NametagConfig(lore.getFormatted().replaceAll("&", "§"), "", null);
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
				accessories.add(Accessory.fromOutfitAccessory(accessory));
			}
		} else {
			// no outfit
			this.outfitName = null;
			this.outfitId = null;
			this.cloak = CachedImage.NO_TEXTURE;
			this.elytra = CachedImage.NO_TEXTURE;
		}
	}

	private final CachedImage cloak;
	private final CachedImage elytra;
	private final List<Accessory> accessories;
	private final @Nullable String outfitName, outfitId;
	private final NametagConfig nametag;
	private final @Nullable NametagConfig lore;

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
	public NametagConfig getNametag() {
		return this.nametag;
	}

	@Override
	public Optional<NametagConfig> getLore() {
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

	/**
	 * Create a player cosmetics from the given {@link PlayerResponse}.
	 * @param response the response.
	 * @return the cosmetics object created from the response.
	 */
	public static PlayerCosmetics fromResponse(PlayerResponse response) {
		// read accessories
		if (response.isIsUser()) {
			CosmeticaUser user = response.getUser();

			// read data from the response
			assert user != null; // response.isIsUser()

			return new PlayerCosmetics(user.getOutfit(), user.getLore(), user.getIcon());
		} else {
			CosmeticaPlayer player = response.getPlayer();

			assert player != null; // !response.isIsUser()

			return new PlayerCosmetics(null, null, null);
		}
	}

	/**
	 * Create a player cosmetics from the given {@link CosmeticaUser}.
	 * @param user the user to create cosmetics for.
	 * @return the cosmetics object from the data in the user object.
	 */
	public static PlayerCosmetics fromUser(CosmeticaUser user) {
		return new PlayerCosmetics(user.getOutfit(), user.getLore(), user.getIcon());
	}

	/**
	 * Set the cosmetics to be used by the local player.
	 * @param cosmetics the cosmetics to be used by the local player.
	 * @implNote via the built-in manager SelfCosmeticManager.
	 */
	public static void setOwnCosmetics(PlayerCosmetics cosmetics) {
		SelfCosmeticManager.cosmetics = cosmetics;
	}
}