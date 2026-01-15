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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import net.minecraft.client.gui.Font;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collection;
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

	/**
	 * Render lore on a player.
	 * @param entityRenderDispatcher the entity render dispatcher.
	 * @param player the player to render the lore for and on.
	 * @param playerModel the model of said player
	 * @param stack the pose stack for rendering.
	 * @param multiBufferSource the buffer source for rendering.
	 * @param font the font to draw text with.
	 * @param packedLight the environment light.
	 */
	public static void renderLore(EntityRenderDispatcher entityRenderDispatcher, Player player, PlayerModel<AbstractClientPlayer> playerModel, PoseStack stack, MultiBufferSource multiBufferSource, Font font, int packedLight) {
		double squaredDistance = entityRenderDispatcher.distanceToSqr(player);

		if (squaredDistance <= 4096.0D) {
			Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(player);

			if (cosmetics.isPresent()) {
				renderLore(
						stack,
						entityRenderDispatcher.cameraOrientation(),
						font,
						multiBufferSource,
						cosmetics.get().getLore().orElse(null),
						cosmetics.get().getAccessories(),
						player.hasItemInSlot(EquipmentSlot.HEAD),
						!player.isSleeping(), // doNametagShift
						player.isDiscrete(), // sneaking
						false, // upside down
						player.getBbHeight(),
						playerModel.head.xRot,
						packedLight);
			}
		}
	}

	/**
	 * Render lore, but not necessarily bound to a player.
	 */
	public static void renderLore(PoseStack stack, Quaternion cameraOrientation, Font font,
								  MultiBufferSource multiBufferSource, @Nullable NametagConfig lore, Collection<Accessory> hats,
								  boolean wearingHelmet, boolean doNametagShift, boolean discrete, boolean upsideDown,
								  float playerHeight, float xRotHead, int packedLight) {
		// how much do we need to shift up nametags?

		// upside down players don't need nametags shifted up
		if (!upsideDown) {
			float hatTopY = 0;
			float torsoFixedHatTopY = 0;

			if (doNametagShift) {
				for (Accessory accessory : hats) {
					if (accessory.getAttachment() == AttachmentEnum.HEAD) {
						if (!accessory.getFlags().contains(Accessory.Flag.HIDE_WITH_HELMET) || !wearingHelmet) {
							hatTopY = Math.max(hatTopY, (float) accessory.getModel().getBoundingBox().maxY);
//							else {
//								torsoFixedHatTopY = Math.max(torsoFixedHatTopY, (float) accessory.bounds().y1());
//							}
						}
					}
				}
			}

			if (hatTopY > 0 || torsoFixedHatTopY > 0) {
				float normalizedAngleMultiplier = (float) -(Math.abs(xRotHead) / 1.57 - 1);
				float lookAngleMultiplier;

				if (normalizedAngleMultiplier == GLIDING_SWIMMING_CROUCHING) { // Gliding with elytra, swimming, or crouching
					lookAngleMultiplier = 0;
				} else {
					lookAngleMultiplier = normalizedAngleMultiplier;
				}

				stack.translate(0, Math.max(hatTopY * lookAngleMultiplier, torsoFixedHatTopY)/ 16, 0);
			}
		}

		// render lore
		if (lore != null) {
			Component component = new TextComponent(lore.getPrefix() /* Prefix doubles as main text */);
			CachedImage loreIcon = lore.getIcon().getImage();
			boolean showLoreIcon = loreIcon.isLoaded();

			boolean fullyRender = !discrete;

			float height = playerHeight + 0.25F;

			stack.translate(0, 0.1, 0);

			stack.pushPose();
			stack.translate(0.0D, height, 0.0D);
			stack.mulPose(cameraOrientation);
			stack.scale(-0.025F, -0.025F, 0.025F);
			stack.scale(0.75F, 0.75F, 0.75F);
			Matrix4f textModel = stack.last().pose();

			float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
			int alphaARGB = (int) (backgroundOpacity * 255.0F) << 24;

			float xOffset = (float) (-font.width(component) / 2);

			if (showLoreIcon) prepareIcon(loreIcon, discrete, true);
			font.drawInBatch(component, xOffset, 0, 0x20FFFFFF, false, textModel, multiBufferSource, fullyRender, alphaARGB, packedLight);

			if (fullyRender) {
				if (showLoreIcon) prepareIcon(loreIcon, false, true);
				font.drawInBatch(component, xOffset, 0, -1, false, textModel, multiBufferSource, false, 0, packedLight);
			}

			stack.popPose();
		}
	}

	public static int debug(Font instance, Component component, float offsetX, float offsetY, int color, boolean bl, Matrix4f transform, MultiBufferSource mbs, boolean bl2, int k, int i) {
		k = 0;
		instance.drawInBatch(component, offsetX, offsetY, color, bl, transform, mbs, bl2, k, i);
		return 0;
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
