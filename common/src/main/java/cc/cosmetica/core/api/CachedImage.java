package cc.cosmetica.core.api;

import net.minecraft.resources.ResourceLocation;

/**
 * A reference to an image that is cached. Once this reference is garbage collected, the image will be removed.
 */
public final class CachedImage {
	public CachedImage(ResourceLocation location) {
		this.location = location;
	}

	public final ResourceLocation location;
	private boolean loaded;

	public boolean isLoaded() {
		return this.loaded;
	}

	public void setLoaded() {
		this.loaded = true;
	}
}
