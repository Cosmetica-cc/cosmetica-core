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

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * An image cosmetic which contains additional information about icons. Not required for an icon.
 */
public class Icon extends ImageCosmetic {
    /**
     * Constructor for a (not external) icon cosmetic.
     * @param publicIcon whether this icon is public.
     * @param unlocked whether this icon is unlocked.
     * @param managed whether this icon is managed by
     */
    public Icon(CachedImage image, String name, String id,
                @Nullable GameProfile creator, @Nullable String thumbnail, int flags,
                boolean publicIcon, boolean unlocked, String modpackId) {
        super(image, name, id, creator, thumbnail, flags);
        this.publicIcon = publicIcon;
        this.unlocked = unlocked;
        this.modpackId = modpackId;
    }

    private final boolean publicIcon;
    private final boolean unlocked;
    private final @Nullable String modpackId;

    public boolean isPublicIcon() {
        return this.publicIcon;
    }

    public boolean isUnlocked() {
        return this.unlocked;
    }

    public Optional<String> getModpackId() {
        return Optional.ofNullable(modpackId);
    }
}
