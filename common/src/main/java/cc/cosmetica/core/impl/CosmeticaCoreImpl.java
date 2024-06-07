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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Implementation functionality.
 */
public final class CosmeticaCoreImpl {
	private CosmeticaCoreImpl() {
	}

	private static final PriorityQueue<PriorityManager> COSMETIC_MANAGERS = new PriorityQueue<>();

	public static void registerCosmeticManager(int priority, CosmeticManager manager) {
		COSMETIC_MANAGERS.add(new PriorityManager(priority, manager));
	}

	private static class PriorityManager implements Comparable<PriorityManager> {
		PriorityManager(int priority, CosmeticManager manager) {
			this.priority = priority;
			this.manager = manager;
		}

		private final int priority;
		private final CosmeticManager manager;

		@Override
		public int compareTo(@NotNull CosmeticaCoreImpl.PriorityManager pm) {
			return this.priority - pm.priority;
		}
	}
}
