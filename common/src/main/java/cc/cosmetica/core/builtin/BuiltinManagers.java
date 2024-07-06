/*
 * Copyright 2024 Cosmetica
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

package cc.cosmetica.core.builtin;

import cc.cosmetica.core.api.CosmeticManagers;
import cc.cosmetica.core.builtin.manager.ApiCosmeticManager;
import cc.cosmetica.core.builtin.manager.ArmourStandCosmeticManager;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.builtin.manager.TestCosmeticManager;
import cc.cosmetica.core.impl.UUIDs;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Registers the built-in cosmetic managers in the Cosmetica.
 */
public class BuiltinManagers {
	public void init() {
		CosmeticManagers.registerCosmeticManager(-100, new TestCosmeticManager());
		CosmeticManagers.registerCosmeticManager(0, new ApiCosmeticManager());
		CosmeticManagers.registerCosmeticManager(1, new ArmourStandCosmeticManager());
		CosmeticManagers.registerCosmeticManager(2, new SelfCosmeticManager());
	}
}
