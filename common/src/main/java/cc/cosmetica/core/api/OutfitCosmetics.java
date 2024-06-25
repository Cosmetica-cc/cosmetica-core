package cc.cosmetica.core.api;

import gg.cloaks.javaclient.model.AnimatedTextureCosmetic;
import gg.cloaks.javaclient.model.Outfit;
import gg.cloaks.javaclient.model.OutfitAccessory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cosmetics that are loaded from an outfit.
 */
public class OutfitCosmetics implements Cosmetics {
	public OutfitCosmetics(Outfit outfit) {
		this.name = outfit.getName();
		this.id = outfit.getId();
		this.accessories = new ArrayList<>();

		// read accessories
		for (OutfitAccessory accessory : outfit.getAccessories()) {
			this.accessories.add(Accessory.fromOutfitAccessory(accessory));
		}

		// read cloak and elytra
		AnimatedTextureCosmetic cloak = outfit.getCloak();
		AnimatedTextureCosmetic elytra = outfit.getElytra();

		this.cloak = cloak == null ? CachedImage.NO_TEXTURE : CosmeticaModel.getOrCreateImage("cape", cloak);
		this.elytra = elytra == null ? CachedImage.NO_TEXTURE : CosmeticaModel.getOrCreateImage("cape", elytra);
	}

	private final String name;
	private final String id;
	private final List<Accessory> accessories;
	private final CachedImage cloak;
	private final CachedImage elytra;

	@Override
	public Optional<String> getOutfitName() {
		return Optional.of(this.name);
	}

	@Override
	public Optional<String> getOutfitId() {
		return Optional.of(this.id);
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
		return Optional.empty();
	}

	@Override
	public boolean isUpsideDown() {
		return false;
	}
}
