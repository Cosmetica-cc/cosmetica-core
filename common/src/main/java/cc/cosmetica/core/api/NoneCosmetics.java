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

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Optional;

/**
 * A blank {@link Cosmetics} object. Useful to prevent null pointers when they are unwanted.
 */
public final class NoneCosmetics implements Cosmetics {
    private NoneCosmetics() {
    }

    @Override
    public Optional<String> getOutfitName() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getOutfitId() {
        return Optional.empty();
    }

    @Override
    public Optional<ImageCosmetic> getCloak() {
        return Optional.empty();
    }

    @Override
    public Optional<ImageCosmetic> getElytra() {
        return Optional.empty();
    }

    @Override
    public Collection<Accessory> getAccessories() {
        return ImmutableList.of();
    }

    @Override
    public NametagConfig getNametag() {
        return NametagConfig.EMPTY;
    }

    @Override
    public Optional<NametagConfig> getLore() {
        return Optional.empty();
    }

    @Override
    public boolean isUpsideDown() {
        return false;
    }

    @Override
    public void enqueue(Runnable task, Runnable onFail) {
        task.run();
    }

    public static final NoneCosmetics NONE = new NoneCosmetics();
}
