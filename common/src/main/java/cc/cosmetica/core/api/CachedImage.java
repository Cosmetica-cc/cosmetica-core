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

import cc.cosmetica.core.api.texture.CosmeticaTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

/**
 * A reference to an image that is cached. Once this reference is garbage collected, the image will be removed.
 */
public final class CachedImage {
	/**
	 * Create a new CachedImage, given the resource location and frame period.
	 * You should be using {@link CosmeticaModel#getOrCreateImage(String, String, CosmeticaTexture.Builder)} or similar
	 * methods to download, cache, and create an instance rather than this direct constructor.
	 * @param location the location of the cached image.
	 * @param framePeriod the frame period.
	 */
	public CachedImage(Identifier location, int framePeriod) {
		this.location = location;
		this.framePeriod = framePeriod;
	}

	public final Identifier location;
	private boolean loaded;
	private int width, height, framePeriod;

	public boolean isLoaded() {
		return this.loaded;
	}

	/**
	 * Get the width, if the image is loaded. Otherwise 0.
	 * @return the width of the cached image.
	 */
	public int getWidth() {
		return this.width;
	}

	/**
	 * Get the height, if the image is loaded. Otherwise 0.
	 * @return the height of the cached image.
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Get the frame period, in ticks per frame, of the cosmetic. 0 if static.
	 * Will give the API frame period even when it has failed to load.
	 * @return the frame period in ticks.
	 */
	public int getFramePeriod() {
		return this.framePeriod;
	}

	public void setLoaded(int width, int height) {
		if (this == NO_TEXTURE) throw new IllegalArgumentException("Cannot set NO_TEXTURE as loaded.");
		this.width = width;
		this.height = height;
		this.loaded = true;
	}

	public static final CachedImage NO_TEXTURE = new CachedImage(TextureManager.INTENTIONAL_MISSING_TEXTURE, 0);
}
