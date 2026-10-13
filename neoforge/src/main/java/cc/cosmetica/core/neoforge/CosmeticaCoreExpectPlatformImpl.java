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

package cc.cosmetica.core.neoforge;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * NeoForge implementation of CosmeticaCore expect platform.
 */
public class CosmeticaCoreExpectPlatformImpl {
	public static Path getGameDirectory() {
		return FMLPaths.GAMEDIR.get();
	}

	public static Path getConfigDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static String getModVersion() {
		List<ModInfo> mods = FMLLoader.getCurrent().getLoadingModList().getMods();

		ModInfo theMod = null;
		for (ModInfo mod : mods) {
			if ("cosmetica".equals(mod.getModId())) {
				theMod = mod;
				break;
			}
			if ("cosmetica-core".equals(mod.getModId())) {
				theMod = mod;
			}
		}

		// this shouldn't happen. cosmetica core will always be loaded.
		if (theMod == null) {
			return "cosmetica-core 0.0.0-0.0.0";
		}

		return theMod.getModId() + " " + theMod.getConfigElement("version").orElse("1");
	}

	public static boolean isDev() {
		return !FMLEnvironment.isProduction();
	}
}
