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

import cc.cosmetica.core.api.Cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Cape layer for non-humans.
 */
public class NonHumanCapeLayer<T extends LivingEntity, M extends EntityModel<T>>
		extends RenderLayer<T, M> {

	public NonHumanCapeLayer(RenderLayerParent<T, M> renderLayerParent, M parentModel) {
		super(renderLayerParent);

		this.cloak = new ModelPart(parentModel, 0, 0);
		this.cloak.setTexSize(64, 32);
		this.cloak.addBox(-5.0f, 0.0f, -1.0f, 10.0f, 16.0f, 1.0f, 0.0f);// f);
	}

	private final ModelPart cloak;

	@Override
	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, LivingEntity livingEntity, float f, float g, float h, float j, float k, float l) {
		//cosmetica
		if (livingEntity.isInvisible()) {
			return;
		}

		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(livingEntity);

		if (!cosmetics.isPresent()) {
			return;
		}
		if (!cosmetics.get().getCloak().isLoaded()) {
			return;
		}

		// vanilla continue
		ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
		if (itemStack.getItem() == Items.ELYTRA) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0, 0.0, 0.125);
//			double d = Mth.lerp((double)h, livingEntity.xCloakO, livingEntity.xCloak) - Mth.lerp((double)h, livingEntity.xo, livingEntity.getX());
//			double e = Mth.lerp((double)h, livingEntity.yCloakO, livingEntity.yCloak) - Mth.lerp((double)h, livingEntity.yo, livingEntity.getY());
//			double m = Mth.lerp((double)h, livingEntity.zCloakO, livingEntity.zCloak) - Mth.lerp((double)h, livingEntity.zo, livingEntity.getZ());

		//cosmetica start
		double d = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, livingEntity.xo, livingEntity.getX());
		double e = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, livingEntity.yo, livingEntity.getY());
		double m = 0;//Mth.lerp((double) h, 0, 0) - Mth.lerp((double) h, livingEntity.zo, livingEntity.getZ());
		// cosmetica end
		float n = livingEntity.yBodyRotO + (livingEntity.yBodyRot - livingEntity.yBodyRotO);
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
		float t = 0.0f;//Mth.lerp(h, livingEntity.oBob, livingEntity.bob);
		q += Mth.sin(Mth.lerp(h, livingEntity.walkDistO, livingEntity.walkDist) * 6.0f) * 32.0f * t;
		if (livingEntity.isCrouching()) {
			q += 25.0f;
		}
		poseStack.mulPose(Vector3f.XP.rotationDegrees(6.0f + r / 2.0f + q));
		poseStack.mulPose(Vector3f.ZP.rotationDegrees(s / 2.0f));
		poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0f - s / 2.0f));
		// cosmetica start
		ResourceLocation cloakLocation = cosmetics.get().getCloak().location;
		// cosmetica end
		VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entitySolid(cloakLocation));
		this.cloak.render(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}
}
