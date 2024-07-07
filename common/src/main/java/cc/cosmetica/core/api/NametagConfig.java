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

import gg.cloaks.javaclient.model.Icon;

import javax.annotation.Nullable;

/**
 * How cosmetica should decorate a nametag.
 */
public class NametagConfig {
	/**
	 * Create a new nametag config.
	 * @param prefix the text to prepend to the nametag. For custom nametags, this is the primary text.
	 * @param suffix the text to append to the nametag.
	 * @param icon the icon to display before the nametag.
	 */
	public NametagConfig(String prefix, String suffix, @Nullable Icon icon) {
		// icon
		if (icon == null) {
			this.icon = CachedImage.NO_TEXTURE;
			this.transparentIcon = false;
		} else {
			this.icon = CosmeticaModel.getOrCreateImage("icon", icon.getId(), icon.getTexture(),
					icon.getFrames().intValue(), icon.getTicksPerFrame().intValue());
			this.transparentIcon = false; // TODO transparent icons
		}

		// affix
		this.prefix = prefix;
		this.suffix = suffix;
	}

	private final CachedImage icon;
	private final boolean transparentIcon;
	private final String prefix, suffix;

	/**
	 * Get the nametag icon to use. {@link CachedImage#NO_TEXTURE} if no texture.
	 * @return the nametag icon to use.
	 */
	public CachedImage getIcon() {
		return this.icon;
	}

	/**
	 * Get whether the icon retrieved from {@link CachedImage} should render with reduced opacity.
	 * @return whether the icon should be rendered with reduced opacity.
	 */
	public boolean isTransparentIcon() {
		return this.transparentIcon;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public static final NametagConfig EMPTY = new NametagConfig("", "", null);
}
