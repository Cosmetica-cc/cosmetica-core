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
import gg.cloaks.javaclient.model.ExternalCape;
import gg.cloaks.javaclient.model.Icon;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents data about a simple image cosmetic.
 */
public final class ImageCosmetic implements Cosmetic {
    /**
     * Constructor for a cosmetica image cosmetic.
     */
    public ImageCosmetic(CachedImage image, String name, String id, @Nullable GameProfile creator, @Nullable String thumbnail,
                         int flags) {
        this.image = image;
        this.name = name;
        this.id = id;
        this.creator = creator;
        this.thumbnail = thumbnail;
        this.flags = flags;
        this.external = false;
    }

    /**
     * Constructor for an external image cosmetic.
     */
    public ImageCosmetic(CachedImage image, String name, String id, @Nullable GameProfile creator, int flags) {
        this.image = image;
        this.name = name;
        this.id = id;
        this.creator = creator;
        this.thumbnail = null;
        this.flags = flags;
        this.external = true;
    }

    private final CachedImage image;

    private final String name;
    private final String id;
    private final boolean external;
    @Nullable
    private final GameProfile creator;
    @Nullable
    private final String thumbnail;
    private final int flags;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public boolean isExternal() {
        return this.external;
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
    public Optional<String> getThumbnail() {
        return Optional.ofNullable(this.thumbnail);
    }

    /**
     * Get the flags for this {@link ImageCosmetic}. The interpretation of these depends on the cosmetic.
     * @apiNote refer to <a href="https://api.cloaks.gg/docs">the documentation</a> for interpretation.
     */
    public int getFlags() {
        return this.flags;
    }

    /**
     * Create an ImageCosmetic from the API.
     * @param cosmetic the cosmetic.
     * @return a new {@link ImageCosmetic}.
     */
    public static ImageCosmetic fromAPI(AnimatedTextureCosmetic cosmetic) {
        return new ImageCosmetic(
                CosmeticaModel.getOrCreateCosmeticaImage(cosmetic),
                cosmetic.getName(),
                cosmetic.getId(),
                Cosmetic.gameProfileOf(cosmetic.getCreator()),
                cosmetic.getThumbnail(),
                cosmetic.getFlags().intValue());
    }

    /**
     * Create an ImageCosmetic from an external cape.
     * @param cosmetic the cosmetic.
     * @return a new {@link ImageCosmetic}.
     */
    public static ImageCosmetic fromExternalCape(ExternalCape cosmetic) {
        return new ImageCosmetic(
                CosmeticaModel.getOrCreateImage(
                        "externalcapes",
                        CosmeticaModel.textureId(cosmetic.getTexture()),
                        new CosmeticaTexture.Builder(cosmetic.getTexture(), BlockModelManager.FALLBACK_TEXTURE)
                                .frames(cosmetic.getFrames().intValue(), cosmetic.getTicksPerFrame().intValue())),
                cosmetic.getName() == null ? (cosmetic.getServiceName() + " Cape") : cosmetic.getName(),
                cosmetic.getId(),
                // Dummy game profile for service
                new GameProfile(
                        UUID.nameUUIDFromBytes(cosmetic.getService().name().getBytes(StandardCharsets.UTF_8)),
                        cosmetic.getServiceName()
                ),
                0);
    }

    /**
     * Create an ImageCosmetic from an API Icon.
     * @param icon the icon.
     * @return a new {@link ImageCosmetic}.
     */
    public static ImageCosmetic fromIcon(Icon icon) {
        if (icon.getTexture() == null) {
            throw new IllegalArgumentException("Tried to create image cosmetic from Icon with no texture?");
        }

        return new ImageCosmetic(
                CosmeticaModel.getOrCreateCosmeticaImage(
                        new CosmeticaTexture.Builder(icon.getTexture(), BlockModelManager.FALLBACK_TEXTURE)
                                .frames(icon.getFrames().intValue(), icon.getTicksPerFrame().intValue())
                ),
                icon.getName(),
                icon.getId(),
                Cosmetic.gameProfileOf(icon.getCreator()),
                icon.getThumbnail(),
                icon.getFlags().intValue());
    }
}
