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

package cc.cosmetica.core.forge;

import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Forge implementation of CosmeticaCore expect platform.
 */
public class CosmeticaCoreExpectPlatformImpl {
	public static Path getGameDirectory() {
		return FMLPaths.GAMEDIR.get();
	}

	public static Path getConfigDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static boolean isDev() {
		return !FMLEnvironment.production;
	}

	public static Optional<String> getClientName() {
		return FMLLoader.getLoadingModList().getMods()
				.stream()
				.map(info -> info.<String>getConfigElement("cosmetica-client"))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}
}
