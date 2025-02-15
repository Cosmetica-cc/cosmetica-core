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

import cc.cosmetica.core.impl.UUIDs;
import com.mojang.authlib.GameProfile;
import gg.cloaks.javaclient.model.CosmeticaUser;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Interface for a general cosmetic.
 */
public interface Cosmetic {
    /**
     * Get the name of this Cosmetic.
     */
    String getName();

    /**
     * Get the id of this Cosmetic.
     * @return the string ID for this cosmetic.
     */
    String getId();

    /**
     * Get the creator if present.
     * @return a creator, if the property is present. Otherwise, empty.
     */
    Optional<GameProfile> getCreator();

    /**
     * Get the URL of the Accessory's official thumbnail. If no thumbnail, may be an empty string.
     * @return the URL of the accessory's official thumbnail. At the time of writing, the image format is {@code webp}.
     */
    String getThumbnail();

    /**
     * Convert a {@link CosmeticaUser} to a {@link GameProfile}.
     * @param user the user to convert into a {@link GameProfile}.
     * @return if the user is null, then null. Otherwise, a valid full GameProfile.
     */
    @Nullable
    static GameProfile gameProfileOf(@Nullable CosmeticaUser user) {
        return user == null ? null : new GameProfile(
                UUIDs.fromString(user.getUuid()),
                user.getUsername());
    }
}
