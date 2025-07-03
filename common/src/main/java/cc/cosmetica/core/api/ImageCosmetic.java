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
import cc.cosmetica.core.impl.BlockModelManager;
import com.mojang.authlib.GameProfile;
import gg.cloaks.javaclient.model.AnimatedTextureCosmetic;
import gg.cloaks.javaclient.model.Icon;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Represents data about a simple image cosmetic.
 */
public final class ImageCosmetic implements Cosmetic {
    public ImageCosmetic(CachedImage image, String name, String id, @Nullable GameProfile creator, String thumbnail) {
        this.image = image;
        this.name = name;
        this.id = id;
        this.creator = creator;
        this.thumbnail = thumbnail;
    }

    private final CachedImage image;

    private final String name;
    private final String id;
    @Nullable
    private final GameProfile creator;
    private final String thumbnail;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Get the image for rendering this cosmetic.
     * @return the {@linkplain CachedImage image} of this cosmetic. {@link CachedImage#NO_TEXTURE} if no image.
     */
    public CachedImage getImage() {
        return this.image;
    }

    @Override
    public Optional<GameProfile> getCreator() {
        return Optional.ofNullable(this.creator);
    }

    @Override
    public String getThumbnail() {
        return this.thumbnail;
    }

    /**
     * Create an ImageCosmetic from the API.
     * @param cosmetic the cosmetic.
     * @param category the image category for image caching.
     * @return a new {@link ImageCosmetic}.
     */
    public static ImageCosmetic fromAPI(AnimatedTextureCosmetic cosmetic, String category) {
        return new ImageCosmetic(
                CosmeticaModel.getOrCreateImage(category, cosmetic),
                cosmetic.getName(),
                cosmetic.getId(),
                Cosmetic.gameProfileOf(cosmetic.getCreator()),
                cosmetic.getThumbnail());
    }

    /**
     * Create an ImageCosmetic from an API Icon.
     * @param icon the icon.
     * @return a new {@link ImageCosmetic}.
     */
    public static ImageCosmetic fromIcon(Icon icon) {
        return new ImageCosmetic(
                CosmeticaModel.getOrCreateImage("icon", icon.getId(),
                        new CosmeticaTexture.Builder(icon.getTexture(), BlockModelManager.FALLBACK_TEXTURE)
                                .frames(icon.getFrames().intValue(), icon.getTicksPerFrame().intValue())),
                icon.getName(),
                icon.getId(),
                Cosmetic.gameProfileOf(icon.getCreator()),
                icon.getThumbnail());
    }
}
