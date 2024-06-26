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

package cc.cosmetica.core.mixin.cape;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Enables partial transparency on Elytras and add cosmetica elyt.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {
	@Shadow @Final private static ResourceLocation WINGS_LOCATION;

	@Redirect(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
	)
	private RenderType addCosmeticaTransparentElytras(ResourceLocation resourceLocation,
													  PoseStack poseStack, MultiBufferSource multiBufferSource, int i, LivingEntity livingEntity) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(livingEntity);

		if (cosmetics.isPresent()) {
			// set the return value to our elytra
			CachedImage image = cosmetics.get().getElytra();
			resourceLocation = image.isLoaded() ? image.location : WINGS_LOCATION;

			// use translucent for cosmetica wings
			return RenderType.entityTranslucent(resourceLocation);
		}

		// default
		return RenderType.armorCutoutNoCull(resourceLocation);
	}

	@Redirect(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getArmorFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
	)
	private VertexConsumer readdGlint(MultiBufferSource buffers, RenderType layer, boolean armour, boolean glint) {
		return glint ? VertexMultiConsumer.create(buffers.getBuffer(RenderType.entityGlint()), buffers.getBuffer(layer)) : buffers.getBuffer(layer);
	}
}
