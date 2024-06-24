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

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetics;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import net.minecraft.client.gui.Font;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Render nametags and stuff in nametags.
 */
public final class NametagRenderer {
	private NametagRenderer() {
		// NO-OP
	}

	private static final float GLIDING_SWIMMING_CROUCHING = 0.49974638F;

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

	public static void renderLore(PoseStack stack, Quaternion cameraOrientation, Font font,
								  MultiBufferSource multiBufferSource, @Nullable String lore, Collection<Accessory> hats,
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
							//hatTopY = Math.max(hatTopY, (float) accessory.bounds().y1());
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
			Component component = new TextComponent(lore);

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

			font.drawInBatch(component, xOffset, 0, 553648127, false, textModel, multiBufferSource, fullyRender, alphaARGB, packedLight);

			if (fullyRender) {
				font.drawInBatch(component, xOffset, 0, -1, false, textModel, multiBufferSource, false, 0, packedLight);
			}

			stack.popPose();
		}
	}
}
