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

import cc.cosmetica.core.api.*;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import gg.cloaks.javaclient.model.CosmeticaUser;
import gg.cloaks.javaclient.model.Outfit;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

import static cc.cosmetica.core.api.NametagConfig.NO_ICON;

/**
 * Cosmetics received from the API for yourself.
 */
public class SelfCosmeticManager implements CosmeticManager {
	public SelfCosmeticManager() {
		CosmeticaAPI.subscribe(
				CosmeticaAPI.SubscriptionEvent.PLAYER,
				Minecraft.getInstance().getUser().getProfileId(),
				SELF_MANAGER, () -> ApiCosmeticManager.lookUpGameProfile(Minecraft.getInstance().getGameProfile()));
	}

	private static Cosmetics cosmetics = NoneCosmetics.NONE;

	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof LocalPlayer && cosmetics != NoneCosmetics.NONE;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return cosmetics;
	}

	/**
	 * Set the cosmetics to be used by the local player.
	 * @param user the user data containing cosmetics to be used by the local player.
	 * @implNote via the built-in manager SelfCosmeticManager.
	 */
	public static void update(PlayerResponse user) {
		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("Cosmetics must be updated from main thread");
		}

		cosmetics = PlayerCosmetics.fromResponse(user);
		MasterCosmeticManager.post(user, cosmetics);
	}

	/**
	 * Update only lore and icon for a cosmetica user.
	 * @param user the user to update lore and icon for.
	 */
	public static void updateLoreAndIcon(CosmeticaUser user) {
		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("Cosmetics must be updated from main thread");
		}

		ImageCosmetic iconImage = user.getIcon() == null ? NO_ICON : ImageCosmetic.fromIcon(user.getIcon());
		NametagConfig nametag = new NametagConfig(user.getPrefix(), user.getSuffix(), iconImage, !user.isOnline());

		cosmetics = new PlayerCosmetics(
				cosmetics.getCloak().orElse(null),
				cosmetics.getElytra().orElse(null),
				cosmetics.getAccessories(),
				cosmetics.getOutfitName().orElse(null),
				cosmetics.getOutfitId().orElse(null),
				nametag,
				user.getLore() == null ? null : NametagConfig.fromLore(user.getLore()),
				user.isUpsideDown()
		);

		MasterCosmeticManager.post(new PlayerResponse().isUser(true).user(user), cosmetics);
	}

	/**
	 * Update the outfit to be used by the local player.
	 * @param outfit the outfit data containing outfit cosmetics to be used by the local player.
	 * @return whether self needs to be fetched again to refresh external capes.
	 */
	public static boolean update(Outfit outfit) {
		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("Cosmetics must be updated from main thread");
		}

		OutfitCosmetics outfitCosmetics = new OutfitCosmetics(outfit);
		boolean shouldFetchSelf;

		if (cosmetics == NoneCosmetics.NONE) {
			cosmetics = outfitCosmetics;
			MasterCosmeticManager.post((PlayerResponse) null, cosmetics);
			shouldFetchSelf = true;
		} else {
			cosmetics = new PlayerCosmetics(
					outfitCosmetics.getCloak() .orElse(cosmetics.getCloak() .isPresent() && cosmetics.getCloak() .get().isExternal() ? cosmetics.getCloak() .get() : null),
					outfitCosmetics.getElytra().orElse(cosmetics.getElytra().isPresent() && cosmetics.getElytra().get().isExternal() ? cosmetics.getElytra().get() : null),
					outfitCosmetics.getAccessories(),
					outfitCosmetics.getOutfitName().orElseThrow(() -> new IllegalStateException("Outfit cosmetics with no outfit name")),
					outfitCosmetics.getOutfitId().orElseThrow(() -> new IllegalStateException("Outfit cosmetics with no outfit name")),
					cosmetics.getNametag(),
					cosmetics.getLore().orElse(null),
					cosmetics.isUpsideDown()
			);
			MasterCosmeticManager.post((PlayerResponse) null, cosmetics);
			shouldFetchSelf = !cosmetics.getCloak().isPresent() || !cosmetics.getElytra().isPresent();
		}

		return shouldFetchSelf;
	}

	public static void clear() {
		cosmetics = NoneCosmetics.NONE;
		MasterCosmeticManager.post((PlayerResponse) null, cosmetics);
	}
	// todo detect account switching to change cosmetics

	/**
	 * Get the current self cosmetics.
	 * @return the current self cosmetics.
	 */
	public static Optional<Cosmetics> getCosmetics() {
		return cosmetics == NoneCosmetics.NONE ? Optional.empty() : Optional.of(cosmetics);
	}

	private static final Identifier SELF_MANAGER = Identifier.fromNamespaceAndPath("cosmetica", "self");
}
