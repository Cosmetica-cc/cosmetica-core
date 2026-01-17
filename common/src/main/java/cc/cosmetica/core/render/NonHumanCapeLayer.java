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

import cc.cosmetica.core.api.Cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Cape layer for non-humans.
 */
public class NonHumanCapeLayer<T extends LivingEntityRenderState, M extends EntityModel<T>>
		extends RenderLayer<T, M> {

	public NonHumanCapeLayer(RenderLayerParent<T, M> renderLayerParent, ModelPart cloak, EquipmentAssetManager equipmentAssetManager) {
		super(renderLayerParent);
		this.cloak = cloak.getChild("cape");
		this.equipmentAssets = equipmentAssetManager;
	}

	private final ModelPart cloak;
	private final EquipmentAssetManager equipmentAssets;

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

	@Override
	public void render(PoseStack stack, MultiBufferSource multiBufferSource, int i, LivingEntityRenderState renderState, float a, float b) {
		//cosmetica
		if (renderState.isInvisible) {
			return;
		}

		Optional<Cosmetics> optionalCosmetics = Cosmetics.getCosmetics(renderState);

		if (!optionalCosmetics.isPresent()) {
			return;
		}
		Cosmetics cosmetics = optionalCosmetics.get();
		if (!cosmetics.getCloak().isPresent() || !cosmetics.getCloak().get().getImage().isLoaded()) {
			return;
		}

		if (renderState instanceof HumanoidRenderState humanoidState) {
			if (this.hasLayer(humanoidState.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
				return;
			}
		}

		stack.pushPose();
		if (renderState instanceof HumanoidRenderState humanoidState) {
			if (this.hasLayer(humanoidState.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
				stack.translate(0.0F, -0.053125F, 0.06875F);
			}
		}

		// PlayerCapeModel

		stack.translate(0.0, 0.0, 0.3);
//			double d = Mth.lerp((double)h, renderState.xCloakO, renderState.xCloak) - Mth.lerp((double)h, renderState.xo, renderState.getX());
//			double e = Mth.lerp((double)h, renderState.yCloakO, renderState.yCloak) - Mth.lerp((double)h, renderState.yo, renderState.getY());
//			double m = Mth.lerp((double)h, renderState.zCloakO, renderState.zCloak) - Mth.lerp((double)h, renderState.zo, renderState.getZ());

		//cosmetica start
		double d = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, renderState.xo, renderState.getX());
		double e = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, renderState.yo, renderState.getY());
		double m = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, renderState.zo, renderState.getZ());
		// cosmetica end
		float n = renderState.bodyRot;
		double o = Mth.sin(n * ((float) Math.PI / 180));
		double p = -Mth.cos(n * ((float) Math.PI / 180));
		float q = (float) e * 10.0f;
		q = Mth.clamp(q, -6.0f, 32.0f);
		float r = (float) (d * o + m * p) * 100.0f;
		r = Mth.clamp(r, 0.0f, 150.0f);
		float s = (float) (d * p - m * o) * 100.0f;
		s = Mth.clamp(s, -20.0f, 20.0f);
		if (r < 0.0f) {
			r = 0.0f;
		}
		if (renderState instanceof HumanoidRenderState humanoid && humanoid.isCrouching) {
			q += 25.0f;
		}
		stack.mulPose(createRotation(XP, (6.0f + r / 2.0f + q)));
		stack.mulPose(createRotation(ZP, (s / 2.0f)));
		stack.mulPose(createRotation(YP, (180.0f - s / 2.0f)));
		// cosmetica start

		ResourceLocation cloakLocation = cosmetics.getCloak().get().getImage().location;
		RenderType type = RenderType.entityTranslucent(cloakLocation);

		VertexConsumer vertexConsumer = multiBufferSource.getBuffer(type);
		// cosmetica end
		this.cloak.render(stack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
		stack.popPose();
	}

	private static final Vector3f XP = new Vector3f(1, 0, 0);
	private static final Vector3f YP = new Vector3f(0, 1, 0);
	private static final Vector3f ZP = new Vector3f(0, 0, 1);

	private static Quaternionf createRotation(Vector3f axis, float degrees) {
		return new Quaternionf(new AxisAngle4f((float)Math.toRadians(degrees), axis));
	}
}
