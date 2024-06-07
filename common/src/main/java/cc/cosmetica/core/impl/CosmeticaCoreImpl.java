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
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.TreeSet;

/**
 * Implementation functionality.
 */
public final class CosmeticaCoreImpl {
	private CosmeticaCoreImpl() {
	}

	// sorted collection of cosmetic managers
	private static final Collection<PrioritisedManager> COSMETIC_MANAGERS = new TreeSet<>();

	public static void registerCosmeticManager(int priority, CosmeticManager manager) {
		COSMETIC_MANAGERS.add(new PrioritisedManager(priority, manager));
	}

	/**
	 * Polls to check if the manager controlling the cosmetics for the given LivingEntity should change. If so,
	 * changes the cosmetics on the living entity.
	 * @param entity the entity to poll cosmetics for.
	 * @param currentManager the reference for the cosmetic manager currently controlling cosmetics.
	 */
	public static void pollCosmetics(LivingEntity entity, IdentityCache<CosmeticManager> currentManager) {
		CosmeticManager selectedManager = null;

		// find which manager controls the cosmetics currently
		for (PrioritisedManager manager : COSMETIC_MANAGERS) {
			if (manager.manager.canManage(entity)) {
				selectedManager = manager.manager;
				break;
			}
		}

		CosmeticManager old = currentManager.getValue();
		CosmeticEquipper equipper = (CosmeticEquipper) entity;

		// if the manager was updated, perform the update procedure
		if (currentManager.checkAndSet(selectedManager)) {
			if (old != null) old.onRevoke(entity);

			if (selectedManager != null) {
				selectedManager.onAssign(entity);
				equipper.cosmeticacore$setCosmetics(selectedManager.getCosmetics(entity));
			} else {
				equipper.cosmeticacore$setCosmetics(null); // clear cosmetics
			}
		}
	}

	private static class PrioritisedManager implements Comparable<PrioritisedManager> {
		PrioritisedManager(int priority, CosmeticManager manager) {
			this.priority = priority;
			this.manager = manager;
		}

		private final int priority;
		private final CosmeticManager manager;

		@Override
		public int compareTo(@NotNull CosmeticaCoreImpl.PrioritisedManager pm) {
			return this.priority - pm.priority;
		}
	}
}
