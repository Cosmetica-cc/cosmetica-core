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

package cc.cosmetica.core.api;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles what cosmetics should exist on a given set of entities.
 * Register these with {@link CosmeticManagers#registerCosmeticManager(int, CosmeticManager)}.
 */
public interface CosmeticManager {
	/**
	 * Get whether this cosmetic manager can and should manage this entity.
	 * @param entity the entity to query whether it should be managed.
	 * @return whether this entity should be managed by this cosmetic manager.
	 * @apiNote This can be called nearly every tick for every LivingEntity on the client, so make it fast and concise!
	 * 		    If this returns true, {@link CosmeticManager#getCosmetics(Either)} will be called shortly after.
	 */
	boolean canManage(Either entity);

	/**
	 * Get the cosmetics to apply to the given entity being managed.
	 * @param entity the entity being managed by this.
	 * @return the cosmetics that should be rendered on this living entity. Cannot be null once {@link CosmeticManager#canManage(Either)} has returned true.
	 */
	Cosmetics getCosmetics(Either entity);

	/**
	 * Called when a living entity is assigned to this cosmetic manager. This is called before {@link CosmeticManager#getCosmetics(Either)}.
	 * @param entity the entity that has been assigned to this cosmetic manager.
	 */
	default void onAssign(Either entity) {
		// Default behaviour : do nothing
	}

	/**
	 * Called when an entity is assigned to a different cosmetic manager, or is removed from the world.
	 * @param entity the entity that was being managed by this cosmetic manager.
	 */
	default void onRevoke(Either entity) {
		// Default behaviour : do nothing
	}

	/**
	 * Represents an option for a manager to manage.
	 * Do not guarantee on Either objects being identical. Instead use the contents of this wrapper.
	 */
	final class Either {
		public Either(LivingEntity entity) {
			if (entity instanceof RemotePlayer) {
				throw new IllegalArgumentException("RemotePlayer should be called with Either(PlayerInfo)");
			}

			this.entity = entity;
			this.remotePlayerInfo = null;
		}

		public Either(PlayerInfo player) {
			this.entity = null;
			this.remotePlayerInfo = player;
		}

		public final LivingEntity entity;
		public final PlayerInfo remotePlayerInfo;

		public UUID getId() {
			if (this.remotePlayerInfo != null) {
				return this.remotePlayerInfo.getProfile().getId();
			} else {
                assert this.entity != null;
                return this.entity.getUUID();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			if (obj instanceof Either) return ((Either) obj).remotePlayerInfo == this.remotePlayerInfo && ((Either) obj).entity == this.entity;
			return false;
		}

		@Override
		public int hashCode() {
			if (this.remotePlayerInfo == null) {
				return Objects.hash(this.entity);
			} else {
				return Objects.hash(this.remotePlayerInfo);
			}
		}
	}
}
