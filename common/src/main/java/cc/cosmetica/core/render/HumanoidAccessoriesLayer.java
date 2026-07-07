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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Renderer for Cosmetic models on humanoid entities.
 */
public class HumanoidAccessoriesLayer<S extends HumanoidRenderState, M extends HumanoidModel<? super S>> extends RenderLayer<S, M> {
	public HumanoidAccessoriesLayer(RenderLayerParent<S, M> renderLayerParent, EquipmentAssetManager equipmentAssets) {
		super(renderLayerParent);
		this.equipmentAssets = equipmentAssets;
	}

	private final EquipmentAssetManager equipmentAssets;

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float a, float b) {
		if (state.isInvisibleToPlayer) return;//don't show cosmetics when invisible

		ProfilerFiller profilerFiller = Profiler.get();
		profilerFiller.push("accessories");

		Cosmetics.getCosmetics(state).ifPresent(cosmetics -> {
			boolean cloak = (cosmetics.getCloak().isPresent() || (state instanceof AvatarRenderState prs && prs.skin.cape() != null)) &&
					(!(state instanceof AvatarRenderState prs) || prs.showCape);

			for (Accessory accessory : cosmetics.getAccessories()) {
				this.renderAccessory(accessory, poseStack, collector, light, cloak, state);
			}
		});

		profilerFiller.pop();
	}

	private void renderAccessory(Accessory accessory, PoseStack stack, SubmitNodeCollector collector, int light, boolean cloak, HumanoidRenderState state) {
		// Check if accessory can be rendered
		boolean elytra = hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS, equipmentAssets);
		if (!canRenderAccessory(accessory, new HumanoidRenderEquipper(state), cloak, elytra)) {
			return;
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
						additionalXOffset -= 0.5f / 16.0f;
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
			accessory.getModel().submitOnPart(
					part, stack, collector, light,
					(float) offset.x + additionalXOffset, (float) offset.y, (float) offset.z,
					accessory.isMirrored(), state.isInvisible, state.outlineColor
			);
		}
	}

	// Vanilla method for checking whether elytra renders or for humanoid models
	public static boolean hasLayer(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, EquipmentAssetManager equipmentAssets) {
		Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && !equippable.assetId().isEmpty()) {
			EquipmentClientInfo equipmentClientInfo = equipmentAssets.get(equippable.assetId().get());
			return !equipmentClientInfo.getLayers(layerType).isEmpty();
		} else {
			return false;
		}
	}

	public static boolean canRenderAccessory(Accessory accessory, ArmourEquipper equipper, boolean cloak, boolean hasElytra) {
		Collection<Accessory.Flag> flags = accessory.getFlags();

		if (flags.contains(Accessory.Flag.HIDE_WITH_HELMET)) {
			if (equipper.hasItemInSlot(EquipmentSlot.HEAD)) {
				return false;
			}
		}

		if (equipper.hasItemInSlot(EquipmentSlot.CHEST)) {
			if (hasElytra) {
				if (flags.contains(Accessory.Flag.HIDE_WITH_ELYTRA)) {
					return false;
				}
			} else {
				if (flags.contains(Accessory.Flag.HIDE_WITH_CHESTPLATE)) {
					return false;
				}
			}
		}
		if (cloak && !hasElytra) {
			if (flags.contains(Accessory.Flag.HIDE_WITH_CLOAK)) {
				return false;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_LEGGINGS)) {
			if (equipper.hasItemInSlot(EquipmentSlot.LEGS)) {
				return false;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_BOOTS)) {
			if (equipper.hasItemInSlot(EquipmentSlot.FEET)) {
				return false;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_PARROT)) {
			if (equipper.hasLeftShoulderEntity() || equipper.hasRightShoulderEntity()) {
				HumanoidArm side = null;

				switch (accessory.getAttachment()) {
					case HEAD:
					case BODY:
						// decide based on which side it is skewed to
						// If not skewed hide with either parrot
						Vec3 centre = accessory.getModel().getBoundingBox().getCenter();
						Vec3 offset = accessory.getOffset();

						double horizontalPosition = centre.x - 8 + offset.x * 16;

						if (Math.abs(horizontalPosition) > 0.5) {
							side = (horizontalPosition < 8 ^ accessory.isMirrored()) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
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
					if (equipper.hasLeftShoulderEntity()) {
						return false;
					}
				}
				if (side == null || side == HumanoidArm.RIGHT) {
					if (equipper.hasRightShoulderEntity()) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public interface ArmourEquipper {
		ItemStack getItemBySlot(EquipmentSlot equipmentSlot);
		boolean hasLeftShoulderEntity();
		boolean hasRightShoulderEntity();

		default boolean hasItemInSlot(EquipmentSlot equipmentSlot) {
			return !getItemBySlot(equipmentSlot).isEmpty();
		}
	}

	public static final class HumanoidRenderEquipper implements ArmourEquipper {
		public HumanoidRenderEquipper(HumanoidRenderState state) {
			this.state = state;
		}

		private final HumanoidRenderState state;

		@Override
		public boolean hasItemInSlot(EquipmentSlot equipmentSlot) {
			switch (equipmentSlot) {
			case MAINHAND:
				return !state.getMainHandItemStack().isEmpty();
			case OFFHAND:
				return state.mainArm == HumanoidArm.LEFT ? !state.rightHandItemStack.isEmpty() : !state.leftHandItemStack.isEmpty();
			default:
				return ArmourEquipper.super.hasItemInSlot(equipmentSlot);
			}
		}

		@Override
		public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
			switch (equipmentSlot) {
			case MAINHAND:
			default:
				return ItemStack.EMPTY; // unsupported
			case OFFHAND:
				return ItemStack.EMPTY; // unsupported
			case FEET:
				return state.feetEquipment;
			case LEGS:
				return state.legsEquipment;
			case CHEST:
			case BODY:
				return state.chestEquipment;
			case HEAD:
				return state.headEquipment;
            }
		}

		@Override
		public boolean hasLeftShoulderEntity() {
			// FIXME compatibility with modded entities?
			return state instanceof AvatarRenderState playerRenderState && playerRenderState.parrotOnLeftShoulder != null;
		}

		@Override
		public boolean hasRightShoulderEntity() {
			return state instanceof AvatarRenderState playerRenderState && playerRenderState.parrotOnRightShoulder != null;
		}
	}
}
