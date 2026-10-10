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

package cc.cosmetica.core.util;

import net.minecraft.resources.ResourceLocation;

public final class Ids {
    private Ids() {}

    public static ResourceLocation minecraft(String value) {
//        return ResourceLocation.withDefaultNamespace(value);
        return new ResourceLocation("minecraft", value);
    }

    public static ResourceLocation fromString(String value) {
//        return ResourceLocation.tryBySeparator(value, ':');
        return new ResourceLocation(value);
    }
}
