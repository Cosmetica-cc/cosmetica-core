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

package cc.cosmetica.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Fabric implementation of CosmeticaCore expect platform.
 */
public class CosmeticaCoreExpectPlatformImpl {
	public static Path getGameDirectory() {
		return FabricLoader.getInstance().getGameDir();
	}

	public static Path getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static boolean isDev() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	public static Optional<String> getClientName() {
		return FabricLoader.getInstance().getAllMods().stream()
				.map(ModContainer::getMetadata)
				.map( mm -> mm.getCustomValue("cosmetica-client"))
				.filter(Objects::nonNull)
				.filter(cv -> cv.getType() == CustomValue.CvType.STRING)
				.map(CustomValue::getAsString)
				.findFirst();
	}
}
