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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Implementation functionality for managing cosmetics.
 */
public final class MasterCosmeticManager {
	private MasterCosmeticManager() {
	}

	// updated by MinecraftMixin
	public static int tickCount;

	// on by default
	public static boolean armourStandArms = true;
	public static boolean hideVanillaCapes = true;

	public static final ExecutorService HTTP_THREAD_POOL;
	static {
		AtomicInteger integer = new AtomicInteger(1);
		HTTP_THREAD_POOL = Executors.newFixedThreadPool(30, r -> new Thread(r, "Cosmetica Worker #" + integer.getAndIncrement()));
	}

	// sorted collection of cosmetic managers
	private static final TreeSet<PrioritisedManager> COSMETIC_MANAGERS = new TreeSet<>();
	// callbacks
	private static final Collection<BiConsumer<CosmeticManager.Either, Cosmetics>> CALLBACKS = new ArrayList<>();
	private static final Collection<BiConsumer<PlayerResponse, Cosmetics>> SELF_CALLBACKS = new ArrayList<>();

	public static void registerCosmeticManager(int priority, CosmeticManager manager) {
		if (!COSMETIC_MANAGERS.add(new PrioritisedManager(priority, manager))) {
			throw new IllegalArgumentException("Duplicate manager at priority " + priority);
		}
	}

	public static void addCallback(BiConsumer<CosmeticManager.Either, Cosmetics> callback) {
		CALLBACKS.add(callback);
	}
	public static void addSelfCallback(BiConsumer<PlayerResponse, Cosmetics> callback) {
		SELF_CALLBACKS.add(callback);
	}

	/**
	 * Polls to check if the manager controlling the cosmetics for the given LivingEntity should change. If so,
	 * changes the cosmetics on the living entity.
	 * @param either the entity to poll cosmetics for.
	 * @param currentManager the reference for the cosmetic manager currently controlling cosmetics.
	 */
	public static void pollCosmetics(CosmeticManager.Either either, IdentityCache<CosmeticManager> currentManager) {
		// check it's distributed to this tick.
		if ((either.getId().getLeastSignificantBits() & 3) != (tickCount & 3)) {
			return;
		}

		// recompute the correct manager
		CosmeticManager selectedManager = null;

		// find which manager controls the cosmetics currently
		for (PrioritisedManager manager : COSMETIC_MANAGERS) {
			if (manager.manager.canManage(either)) {
				selectedManager = manager.manager;
				break;
			}
		}

		// we need to get the old cosmetic manager BEFORE we set
		CosmeticManager old = currentManager.getValue();

		CosmeticEquipper equipper = either.entity == null ? (CosmeticEquipper) either.remotePlayerInfo : (CosmeticEquipper) either.entity;

		// if the manager was updated, perform the update procedure
		if (currentManager.replace(selectedManager)) {
			// assign -> revoke, so we don't unsubscribe from event updates on the webhook if they use the same one
			if (selectedManager != null) {
				selectedManager.onAssign(either);
			}

			// inform old cosmetic manager the player has been revoked
			if (old != null) old.onRevoke(either);

			// equip or clear
			equipper.cosmeticacore$updateCosmetics(selectedManager);
		} else if (selectedManager != null) {
			equipper.cosmeticacore$refreshCosmetics(selectedManager);
		}
	}

	/**
	 * Update all listeners to an entity's cosmetics change. called by mixin/LivingEntityMixin.
	 */
	public static void post(LivingEntity entity, @Nullable Cosmetics newCosmetics) {
		CosmeticManager.Either either = new CosmeticManager.Either(entity);

		for (BiConsumer<CosmeticManager.Either, Cosmetics> consumer : CALLBACKS) {
			consumer.accept(either, newCosmetics);
		}
	}

	/**
	 * Update all listeners to a remote player's cosmetics change. called by mixin/PlayerInfoMixin.
	 */
	public static void post(PlayerInfo remotePlayerInfo, @Nullable Cosmetics newCosmetics) {
		CosmeticManager.Either either = new CosmeticManager.Either(remotePlayerInfo);

		for (BiConsumer<CosmeticManager.Either, Cosmetics> consumer : CALLBACKS) {
			consumer.accept(either, newCosmetics);
		}
	}

	/**
	 * Update all listeners to new data received for the self. The cosmetics may not be loaded yet.
	 */
	public static void post(@Nullable PlayerResponse user, @Nullable Cosmetics newCosmetics) {
		for (BiConsumer<PlayerResponse, Cosmetics> consumer : SELF_CALLBACKS) {
			consumer.accept(user, newCosmetics);
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
		public int compareTo(@NotNull MasterCosmeticManager.PrioritisedManager pm) {
			return this.priority - pm.priority;
		}
	}
}
