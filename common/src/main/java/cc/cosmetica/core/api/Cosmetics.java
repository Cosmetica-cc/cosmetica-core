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

import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.impl.CosmeticEquipper;
import cc.cosmetica.core.impl.HasCosmeticsRenderState;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.impl.NametagRenderer;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Interface for the cosmetics equipped on an entity.
 * Do not keep non-weak references to this (without proper management), as it will prevent this data being garbage
 * collected when a player is removed.
 */
public interface Cosmetics {
	/**
	 * Get the name of the outfit being worn.
	 * @return the display name of the outfit being worn. Empty if no outfit is being worn.
	 */
	Optional<String> getOutfitName();

	/**
	 * Get the id of the outfit being worn.
	 * @return the id of the outfit being worn. Empty if no outfit is being worn.
	 */
	Optional<String> getOutfitId();

	/**
	 * Get the cloak texture.
	 * @return the cloak texture.
	 */
	Optional<ImageCosmetic> getCloak();

	/**
	 * Get the elytra texture.
	 * @return the elytra texture.
	 */
	Optional<ImageCosmetic> getElytra();

	/**
	 * Get the accessories equipped on this entity.
	 * @return the accessories this entity is equipping.
	 */
	Collection<Accessory> getAccessories();

	/**
	 * Get Cosmetica's modifications to the entity's nametag.
	 * @return Cosmetica's modifications to the entity's nametag.
	 */
	NametagConfig getNametag();

	/**
	 * Get the entity's lore. This is a second nametag below their name.
	 * @return an optional containing the lore, if present.
	 */
	Optional<NametagConfig> getLore();

	/**
	 * Get whether the entity should be flipped upside-down.
	 * @return whether the entity should be flipped upside-down.
	 */
	boolean isUpsideDown();

	/**
	 * Enqueue a task to run when Cosmetics load (or fail), or run immediately.
	 * @param task the task to run when cosmetics load.
	 */
	void enqueue(Runnable task, Runnable onFail);

	/**
	 * Get the container for cosmetics being worn by the given entity.
	 * @param entity the entity for which to get the container.
	 * @return the container.
	 */
	static Optional<Cosmetics> getCosmetics(LivingEntity entity) {
		CosmeticEquipper equipper = (CosmeticEquipper) entity;
		return equipper.cosmeticacore$getCosmetics();
	}

	/**
	 * Get the container for cosmetics being worn by the given remote player (not ones' self).
	 * @param remotePlayerInfo the player for which to get the container.
	 * @return the container.
	 */
	static Optional<Cosmetics> getCosmetics(PlayerInfo remotePlayerInfo) {
		CosmeticEquipper equipper = (CosmeticEquipper) remotePlayerInfo;
		return equipper.cosmeticacore$getCosmetics();
	}

	/**
	 * Get the cosmetics to render on a given entity during a render pass.
	 * @param state the render state for which to get the container.
	 * @return the cosmetics to render.
	 */
	static Optional<Cosmetics> getCosmetics(LivingEntityRenderState state) {
		HasCosmeticsRenderState cosmeticsRenderState = (HasCosmeticsRenderState) state;
		return cosmeticsRenderState.cosmeticacore$getCosmetics();
	}

	/**
	 * Call this to update the entity's cosmetics, only if the given manager is still current.
	 * @param entity the entity to update cosmetics for.
	 * @param manager the manager for which to update.
	 */
	static void updateCosmetics(LivingEntity entity, CosmeticManager manager) {
		CosmeticEquipper equipper = (CosmeticEquipper) entity;
		equipper.cosmeticacore$updateCosmetics(manager);
	}

	/**
	 * Register the cosmetics change callback. This will only run in the world!
	 * @param onChange a consumer that takes the entity, and new cosmetics whenever the cosmetics on an entity changes.
	 *                 <ul><li>The {@linkplain CosmeticManager.Either equipper} will never be null.</li>
	 *                 <li>The cosmetics parameter may be null.</li></ul>
	 */
	static void registerCosmeticsChangeCallback(BiConsumer<CosmeticManager.@NotNull Either, @Nullable Cosmetics> onChange) {
		MasterCosmeticManager.addCallback(onChange);
	}

	/**
	 * Register a callback for fetching new data for self.
	 * This does not catch de-authentications. For de-authentication, see {@link CosmeticaAPI#addAuthenticationChangeCallback(Consumer)} which runs earlier.
	 * However, it does catch mods clearing cosmetics via {@link SelfCosmeticManager#clear()}.
	 * <b>Note:</b> Both clearing cosmetics and updating cosmetics with an {@link gg.cloaks.javaclient.model.Outfit Outfit} may provide null player response!
	 */
	static void registerSelfDataFetchCallback(BiConsumer<@Nullable PlayerResponse, Cosmetics> onFetch) {
		MasterCosmeticManager.addSelfCallback(onFetch);
	}

	/**
	 * Configure whether the local player's nametag should show. By default it shows in game.
	 * @param show whether the nametag should display in third person.
	 * @param showInInventory whether the nametag should display during {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#renderEntityInInventory(int, int, int, float, float, LivingEntity) InventoryScreen#renderEntityInInventory}.
	 */
	static void configureOwnNametag(boolean show, boolean showInInventory) {
		NametagRenderer.configureThirdPersonNametag(show, showInInventory);
	}

	/**
	 * Configure whether armour stand arms should show for arm cosmetics.
	 * @param showForCosmetics whether the armour stand arms should be forced to display if an armour stand has arm
	 *                         cosmetics.
	 */
	static void configureArmourStandArms(boolean showForCosmetics) {
		MasterCosmeticManager.armourStandArms = showForCosmetics;
	}

	/**
	 * Configure whether capes supplied by vanilla and some other mods should be hidden if a player has cosmetics loaded.
	 * This affects how cosmetica overrides cape textures and does not affect cosmetica's external capes system.
	 * @param hideVanillaCapes whether capes provided by the game should be hidden.
	 */
	static void configureHideOfficialCapes(boolean hideVanillaCapes) {
		MasterCosmeticManager.hideVanillaCapes = hideVanillaCapes;
	}
}
