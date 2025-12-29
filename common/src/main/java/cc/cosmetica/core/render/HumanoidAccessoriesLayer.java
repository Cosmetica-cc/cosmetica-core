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

package cc.cosmetica.core.render;

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.mixin.PlayerModelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Renderer for Cosmetic models on humanoid entities.
 */
public class HumanoidAccessoriesLayer<E extends LivingEntity, M extends HumanoidModel<E>> extends RenderLayer<E, M> {
	public HumanoidAccessoriesLayer(RenderLayerParent<E, M> renderLayerParent) {
		super(renderLayerParent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, E entity,
					   float f, float g, float pitch, float j, float k, float l) {
		if (entity.isInvisible())return;//don't show cosmetics when invisible

		Minecraft.getInstance().getProfiler().push("accessories");

		Cosmetics.getCosmetics(entity).ifPresent(cosmetics -> {
			for (Accessory accessory : cosmetics.getAccessories()) {
				this.renderAccessory(accessory, poseStack, multiBufferSource, light, entity);
			}
		});

		Minecraft.getInstance().getProfiler().pop();
	}

	private void renderAccessory(Accessory accessory, PoseStack stack, MultiBufferSource multiBufferSource, int light, E entity) {
		//System.out.println("rendering accessory " + accessory.getName() + " on " + accessory.getAttachment().getValue()	);
		// Check if accessory can be rendered
		Collection<Accessory.Flag> flags = accessory.getFlags();

		if (flags.contains(Accessory.Flag.HIDE_WITH_HELMET)) {
			if (entity.hasItemInSlot(EquipmentSlot.HEAD)) {
				return;
			}
		}

		if (entity.hasItemInSlot(EquipmentSlot.CHEST)) {
			if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem) {
				if (flags.contains(Accessory.Flag.HIDE_WITH_ELYTRA)) {
					return;
				}
			} else {
				if (flags.contains(Accessory.Flag.HIDE_WITH_CHESTPLATE)) {
					return;
				}
			}
		}
		if (entity instanceof AbstractClientPlayer &&
				((AbstractClientPlayer)entity).getCloakTextureLocation() != null &&
				(!entity.hasItemInSlot(EquipmentSlot.CHEST) || !(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem))
		) {
			if (flags.contains(Accessory.Flag.HIDE_WITH_CLOAK)) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_LEGGINGS)) {
			if (entity.hasItemInSlot(EquipmentSlot.LEGS)) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_BOOTS)) {
			if (entity.hasItemInSlot(EquipmentSlot.FEET)) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_PARROT)) {
			if (entity instanceof AbstractClientPlayer) {
				HumanoidArm side = null;

				switch (accessory.getAttachment()) {
					case HEAD:
					case BODY:
						// decide based on which side it is skewed to
						// If not skewed hide with either parrot
						Vec3 centre = accessory.getModel().getBoundingBox().getCenter();
						if (Math.abs(centre.x - 8) > 0.5) {
							side = (centre.x > 8 ^ accessory.isMirrored()) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
						}
						break;
					case LEFT_ARM:
					case LEFT_LEG:
						side = accessory.isMirrored() ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
						break;
					case RIGHT_ARM:
					case RIGHT_LEG:
						side = accessory.isMirrored() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
					case UNKNOWN_DEFAULT_OPEN_API:
						break;
				}

				if (side == null || side == HumanoidArm.LEFT) {
					if (!((AbstractClientPlayer) entity).getShoulderEntityLeft().isEmpty()) {
						return;
					}
				}
				if (side == null || side == HumanoidArm.RIGHT) {
					if (!((AbstractClientPlayer) entity).getShoulderEntityRight().isEmpty()) {
						return;
					}
				}
			}
		}

		ModelPart part = null;

		// additional shifting for slim/thick arms
		float additionalXOffset = 0;

		switch (accessory.getAttachment()) {
			case HEAD:
				part = this.getParentModel().head;
				break;
			case BODY:
				part = this.getParentModel().body;
				break;
			case LEFT_ARM:
				part = accessory.isMirrored() ?
						this.getParentModel().rightArm :
						this.getParentModel().leftArm;

				// thin skin: shift
				if (this.getParentModel() instanceof PlayerModel) {
					if (((PlayerModelAccessor) this.getParentModel()).isSlim()) {
						additionalXOffset += 0.5f / 16.0f;
					}
				}
				break;
			case RIGHT_ARM:
				part = accessory.isMirrored() ?
						this.getParentModel().leftArm :
						this.getParentModel().rightArm;

				// thin skin: shift
				if (this.getParentModel() instanceof PlayerModel) {
					if (((PlayerModelAccessor) this.getParentModel()).isSlim()) {
						additionalXOffset += 0.5f / 16.0f;
					}
				}
				break;
			case LEFT_LEG:
				part = accessory.isMirrored() ?
						this.getParentModel().rightLeg :
						this.getParentModel().leftLeg;
				break;
			case RIGHT_LEG:
				part = accessory.isMirrored() ?
						this.getParentModel().leftLeg :
						this.getParentModel().rightLeg;
				break;
			case UNKNOWN_DEFAULT_OPEN_API:
				Logging.getInstance().warnOnce(
						"attachment_unknown_accessory",
						"Unknown attachment for accessory: {}",
						accessory.getName());
				return;
		}

		Vec3 offset = accessory.getOffset();

		if (part.visible) {
			accessory.getModel().renderOnPart(
					part, stack, multiBufferSource, light,
					(float) offset.x + additionalXOffset, (float) offset.y, (float) offset.z,
					accessory.isMirrored()
			);
		}
	}
}
