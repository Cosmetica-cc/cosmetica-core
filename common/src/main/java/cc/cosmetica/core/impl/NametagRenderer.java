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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Render nametags and stuff in nametags.
 */
public final class NametagRenderer {
	private NametagRenderer() {
		// NO-OP
	}

	// default only in-game
	private static boolean showOwnNametag = true;
	private static boolean showOwnNametagInventory = false;
	private static boolean inventoryRendering;

	private static final float GLIDING_SWIMMING_CROUCHING = 0.49974638F;

	// ============ //
	// Nametag Icon //
	// ============ //

	// don't prevent the cached image being Garbage Collected
	@Nullable
	private static Icon preparedIcon;
	private static int iconDrawCount;
	private static boolean readjustNametagPosition;

	public static @Nullable Icon getPreparedIcon() {
		try {
			return preparedIcon == null ? null : preparedIcon;
		} finally {
			if (iconDrawCount > 0) {
				if (--iconDrawCount <= 0) {
					preparedIcon = null;
				}
			}
		}
	}

	public static boolean shouldReadjustNametagPosition() {
		return readjustNametagPosition;
	}

	/**
	 * Prepare an icon to render it at the front of the next font draw call.
	 * @param icon the icon to render.
	 * @param transparent whether to render the icon transparent.
	 * @param readjustTextPosition whether to shift the text left accordingly.
	 */
	public static void prepareIcon(CachedImage icon, boolean transparent, boolean readjustTextPosition) {
		prepareIcon(icon, 1, transparent, readjustTextPosition);
	}

	/**
	 * Prepare an icon to render it at the front of the next font draw call.
	 * @param icon the icon to render.
	 * @param count the number of font renders to add the icon to  Typically two for shadow.
	 * @param transparent whether to render the icon transparent.
	 * @param readjustTextPosition whether to shift the text left accordingly.
	 */
	public static void prepareIcon(CachedImage icon, int count, boolean transparent, boolean readjustTextPosition) {
		iconDrawCount = count;
		readjustNametagPosition = readjustTextPosition;
		preparedIcon = new Icon(new WeakReference<>(icon), transparent);
	}

	// ================ //
	// Show Own Nametag //
	// ================ //

	public static void configureThirdPersonNametag(boolean show, boolean showInInventory) {
		showOwnNametag = show;
		showOwnNametagInventory = showInInventory;
	}

	public static void setRenderingInventoryEntity(boolean inInventory) {
		inventoryRendering = inInventory;
	}

	/**
	 * Get whether the player's own nametag should currently be rendered.
	 * @return whether the player's own nametag should currently be rendered.
	 */
	public static boolean shouldShowOwnNametag() {
		return inventoryRendering && showOwnNametagInventory || !inventoryRendering && showOwnNametag;
	}

	// ========= //
	//   Lore    //
	// ========= //

	public static Vec3 shiftNametags(AvatarRenderState state, PlayerModel model, Vec3 position,
									 HumanoidAccessoriesLayer.ArmourEquipper equipper) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(state);
		boolean wearingHelmet = !state.headEquipment.isEmpty();
		boolean cloak = state.skin.cape() != null && state.showCape;
		final var chestLayers = equipper.getLayers(EquipmentSlot.CHEST);

		if (!state.isUpsideDown && cosmetics.isPresent()) {
			float hatTopY = 0;
			float torsoFixedHatTopY = 0;

			for (Accessory accessory : cosmetics.get().getAccessories()) {
				if (accessory.getAttachment() == AttachmentEnum.HEAD) {
					if (HumanoidAccessoriesLayer.canRenderAccessory(accessory, equipper, cloak, chestLayers)) {
						if (!accessory.getFlags().contains(Accessory.Flag.HIDE_WITH_HELMET) || !wearingHelmet) {
							hatTopY = Math.max(hatTopY, (float) (accessory.getModel().getBoundingBox().maxY + accessory.getOffset().y*16.0 - 12.0));
						}
					}
				}
			}

			if (hatTopY > 0 || torsoFixedHatTopY > 0) {
				float normalizedAngleMultiplier = (float) -(Math.abs(model.head.xRot) / 1.57 - 1);
				float lookAngleMultiplier;

				if (normalizedAngleMultiplier == GLIDING_SWIMMING_CROUCHING) { // Gliding with elytra, swimming, or crouching
					lookAngleMultiplier = 0;
				} else {
					lookAngleMultiplier = normalizedAngleMultiplier;
				}

				return position.add(new Vec3(0, Math.max(hatTopY * lookAngleMultiplier, torsoFixedHatTopY) / 16.0, 0));
			}
		}

		return position;
	}

	/**
	 * Submit lore to render on a player.
	 */
	public static void submitLore(AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState arg4) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(state);
		int i = state.showExtraEars ? -10 : 0;

		if (cosmetics.isPresent()) {
			NametagConfig lore = cosmetics.get().getLore().orElse(null);

			if (lore != null) {
				stack.pushPose();
				Vec3 zeroedAttachment = new Vec3(state.nameTagAttachment.x, 0, state.nameTagAttachment.z);
				stack.translate(0, state.nameTagAttachment.y, 0);
				stack.scale(0.75F, 0.75F, 0.75F);

				CachedImage loreIcon = lore.getIcon().getImage();
				boolean showLoreIcon = loreIcon.isLoaded();
				if (showLoreIcon) {
					((IconSubmitter) collector.order(0)).cosmeticacore$submitIcon(loreIcon, state.isDiscrete, true);
				}

				Component component = Component.literal(lore.getPrefix());
				collector.submitNameTag(
						stack,
						zeroedAttachment,
						i,
						component,
						!state.isDiscrete,
						state.lightCoords,
						arg4);
				stack.popPose();
				stack.translate(0.0F, 0.15F, 0.0F);
			}
		}

	}

	public static final class Icon {
		public Icon(WeakReference<CachedImage> image, boolean transparent) {
			this.image = image;
			this.transparent = transparent;
		}

		public final WeakReference<CachedImage> image;
		public final boolean transparent;
	}
}
