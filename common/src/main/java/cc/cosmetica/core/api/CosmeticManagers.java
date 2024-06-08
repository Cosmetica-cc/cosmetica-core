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

package cc.cosmetica.core.api;

import cc.cosmetica.core.impl.MasterCosmeticManager;

/**
 * The place to register cosmetic managers.
 */
public final class CosmeticManagers {
	private CosmeticManagers() {
		// NO-OP
	}

	/**
	 * Register a Cosmetic manager to be used by Cosmetica Core.
	 * @param priority the priority of the CosmeticManager. Lower numbers are prioritised over higher ones.
	 * @param manager the manager to register.
	 * @apiNote the "API" cosmetic manager has a priority of 0.
	 */
	public static void registerCosmeticManager(int priority, CosmeticManager manager) {
		MasterCosmeticManager.registerCosmeticManager(priority, manager);
	}
}
