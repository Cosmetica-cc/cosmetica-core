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

package cc.cosmetica.core.render;

import cc.cosmetica.core.api.Accessory;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.mixin.PlayerModelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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
		Cosmetics.getCosmetics(entity).ifPresent(cosmetics -> {
			for (Accessory accessory : cosmetics.getAccessories()) {
				//System.out.println("rendering accessory " + accessory.getName() + " on " + accessory.getAttachment().getValue()	);
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
							additionalXOffset += 0.5f;
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
							additionalXOffset += 0.5f;
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
					continue;
				}

				Vec3 offset = accessory.getOffset();

				if (part.visible) {
					accessory.getModel().renderOnPart(
							part, poseStack, multiBufferSource, light,
							(float) offset.x + additionalXOffset, (float) offset.y, (float) offset.z,
							accessory.isMirrored()
					);
				}
			}
		});
	}
}
