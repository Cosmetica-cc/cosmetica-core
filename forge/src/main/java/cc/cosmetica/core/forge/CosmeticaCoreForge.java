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

import cc.cosmetica.core.CosmeticaCore;
import cc.cosmetica.core.builtin.BuiltinManagers;
import cc.cosmetica.core.impl.CosmeticaSession;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("cosmetica_core")
public class CosmeticaCoreForge {
	public CosmeticaCoreForge(FMLJavaModLoadingContext context) {
		context.getModEventBus().addListener(this::onClientSetup);
	}

	private void onClientSetup(FMLClientSetupEvent event) {
		new BuiltinManagers().init();

		// GC
		CosmeticaCore.onInitialiseClient();

		// Development Testing Auth
		String devAuth = System.getProperty("cosmetica.token");

		if (devAuth != null && !FMLEnvironment.production) {
			CosmeticaSession.authenticate(devAuth, "development", null);
		}
	}
}
