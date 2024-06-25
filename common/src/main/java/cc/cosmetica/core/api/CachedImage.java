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

import net.minecraft.client.renderer.texture.TextureManager;
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
		if (this == NO_TEXTURE) throw new IllegalArgumentException("Cannot set NO_TEXTURE as loaded.");
		this.loaded = true;
	}

	public static final CachedImage NO_TEXTURE = new CachedImage(TextureManager.INTENTIONAL_MISSING_TEXTURE);
}
