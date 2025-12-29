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

package cc.cosmetica.core.api;

import cc.cosmetica.core.impl.Logging;
import com.mojang.authlib.GameProfile;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.model.AnimatedTextureCosmetic;
import gg.cloaks.javaclient.model.Outfit;
import gg.cloaks.javaclient.model.OutfitAccessory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Cosmetics that are loaded from an outfit.
 */
public class OutfitCosmetics implements Cosmetics {
	public OutfitCosmetics(Outfit outfit) {
		this.name = outfit.getName();
		this.id = outfit.getId();
		this.creator = Cosmetic.gameProfileOf(outfit.getCreator());
		this.accessories = new ArrayList<>();

		// read accessories
		for (OutfitAccessory accessory : outfit.getAccessories()) {
			this.accessories.add(Accessory.fromOutfitAccessory(accessory));
		}

		// read cloak and elytra
		AnimatedTextureCosmetic cloak = outfit.getCloak();
		AnimatedTextureCosmetic elytra = outfit.getElytra();

		this.cloak = cloak == null ? Optional.empty() : Optional.of(new ImageCosmetic(
                CosmeticaModel.getOrCreateCosmeticaImage(cloak),
                cloak.getName(),
                cloak.getId(),
                Cosmetic.gameProfileOf(cloak.getCreator()),
                cloak.getThumbnail(),
				cloak.getFlags()));
		this.elytra = elytra == null ? Optional.empty() : Optional.of(new ImageCosmetic(
				CosmeticaModel.getOrCreateCosmeticaImage(elytra),
				elytra.getName(),
				elytra.getId(),
				Cosmetic.gameProfileOf(elytra.getCreator()),
				elytra.getThumbnail(),
				elytra.getFlags()));
	}

	private final String name;
	private final String id;
	private final @Nullable GameProfile creator;
	private final List<Accessory> accessories;
	private final Optional<ImageCosmetic> cloak;
	private final Optional<ImageCosmetic> elytra;

	@Override
	public Optional<String> getOutfitName() {
		return Optional.of(this.name);
	}

	@Override
	public Optional<String> getOutfitId() {
		return Optional.of(this.id);
	}

	public Optional<GameProfile> getOutfitCreator() { return Optional.ofNullable(this.creator); }

	@Override
	public Optional<ImageCosmetic> getCloak() {
		return this.cloak;
	}

	@Override
	public Optional<ImageCosmetic> getElytra() {
		return this.elytra;
	}

	@Override
	public Collection<Accessory> getAccessories() {
		return this.accessories;
	}

	@Override
	public Optional<NametagConfig> getLore() {
		return Optional.empty();
	}

	@Override
	public NametagConfig getNametag() {
		return NametagConfig.EMPTY;
	}

	@Override
	public boolean isUpsideDown() {
		return false;
	}

	@Override
	public void enqueue(Runnable task, Runnable onFail) {
		// todo actually enqueue
		task.run();
	}

	@Override
	public String toString() {
		return "OutfitCosmetics{" + this.id + "}@" + Integer.toHexString(this.hashCode());
	}

	/**
	 * Get cosmetics by outift id.
	 * @param outfitId the outfit id. A v4 uuid.
	 * @return a completable future to contain the cosmetics on a successful call, otherwise contains null.
	 */
	public static CompletableFuture<? extends Cosmetics> getAsyncById(String outfitId) {
		return CosmeticaAPI.outfits().requestAsync(api -> api.get(outfitId))
				.thenApply(OutfitCosmetics::new)
				.exceptionally(e -> {
					/* probably no outfit exists */
					if (!(e instanceof ApiException && ((ApiException) e).getCode() == 404)) {
						Logging.getInstance().error("Error looking up outfit {}", e, outfitId);
					}

					return null;
				});
	}
}
