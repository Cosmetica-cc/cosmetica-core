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
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
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
	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, HumanoidRenderState state, float a, float b) {
		if (state.isInvisibleToPlayer) return;//don't show cosmetics when invisible

		ProfilerFiller profilerFiller = Profiler.get();
		profilerFiller.push("accessories");

		Cosmetics.getCosmetics(state).ifPresent(cosmetics -> {
			boolean cloak = (cosmetics.getCloak().isPresent() || (state instanceof PlayerRenderState prs && prs.skin.capeTexture() != null)) &&
				(!(state instanceof PlayerRenderState prs) || prs.showCape);

			for (Accessory accessory : cosmetics.getAccessories()) {
				this.renderAccessory(accessory, poseStack, multiBufferSource, light, cloak, state);
			}
		});

		profilerFiller.pop();
	}

	// Vanilla method for checking whether elytra renders or for humanoid models
	private boolean hasLayer(ItemStack itemStack, EquipmentClientInfo.LayerType layerType) {
		Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && !equippable.assetId().isEmpty()) {
			EquipmentClientInfo equipmentClientInfo = this.equipmentAssets.get(equippable.assetId().get());
			return !equipmentClientInfo.getLayers(layerType).isEmpty();
		} else {
			return false;
		}
	}

	private void renderAccessory(Accessory accessory, PoseStack stack, MultiBufferSource multiBufferSource, int light, boolean cloak, HumanoidRenderState state) {
		//System.out.println("rendering accessory " + accessory.getName() + " on " + accessory.getAttachment().getValue()	);
		// Check if accessory can be rendered
		Collection<Accessory.Flag> flags = accessory.getFlags();

		if (flags.contains(Accessory.Flag.HIDE_WITH_HELMET)) {
			if (!state.headEquipment.isEmpty()) {
				return;
			}
		}

		boolean hasElytra = hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS);

		if (!state.chestEquipment.isEmpty()) {
			if (hasElytra) {
				if (flags.contains(Accessory.Flag.HIDE_WITH_ELYTRA)) {
					return;
				}
			} else {
				if (flags.contains(Accessory.Flag.HIDE_WITH_CHESTPLATE)) {
					return;
				}
			}
		}
		if (cloak && !hasElytra) {
			if (flags.contains(Accessory.Flag.HIDE_WITH_CLOAK)) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_LEGGINGS)) {
			if (!state.legsEquipment.isEmpty()) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_BOOTS)) {
			if (!state.feetEquipment.isEmpty()) {
				return;
			}
		}

		if (flags.contains(Accessory.Flag.HIDE_WITH_PARROT)) {
			if (state instanceof PlayerRenderState playerRenderState) {
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
					// FIXME compatibility with modded entities?
					if (playerRenderState.parrotOnLeftShoulder != null) {
						return;
					}
				}
				if (side == null || side == HumanoidArm.RIGHT) {
					if (playerRenderState.parrotOnRightShoulder != null) {
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
