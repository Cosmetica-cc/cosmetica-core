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

package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.NametagRenderer;
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds lore to players.
 */
@Mixin(value = PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {
	public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel entityModel, float f) {
		super(context, entityModel, f);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(EntityRendererProvider.Context context, boolean bl, CallbackInfo ci) {
		this.cosmeticacore$equipmentAssets = context.getEquipmentAssets();
	}

	private @Unique EquipmentAssetManager cosmeticacore$equipmentAssets;

	@Inject(at = @At(value = "HEAD"),
			method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
	)
	private void shiftNametags(PlayerRenderState avatarRenderState, Component displayName, PoseStack stack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (avatarRenderState.nameTagAttachment != null) {
			boolean elytra = HumanoidAccessoriesLayer.hasLayer(avatarRenderState.chestEquipment, EquipmentClientInfo.LayerType.WINGS, this.cosmeticacore$equipmentAssets);
			avatarRenderState.nameTagAttachment = NametagRenderer.shiftNametags(avatarRenderState, this.getModel(), avatarRenderState.nameTagAttachment, new HumanoidAccessoriesLayer.HumanoidRenderEquipper(avatarRenderState), elytra);
		}
	}

	@Inject(at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			ordinal = 1
	), method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
	protected void onRenderNameTag(PlayerRenderState state, Component displayName, PoseStack stack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		// add lore
		NametagRenderer.renderLore(this.entityRenderDispatcher, state, this.getModel(), stack, buffer, this.getFont(), packedLight, false, null);

		// add nametag icons
		Cosmetics.getCosmetics(state).ifPresent(c -> {
			NametagConfig iconCosmetic = c.getNametag();
			CachedImage icon = iconCosmetic.getIcon().getImage();

			if (icon.isLoaded()) {
				NametagRenderer.prepareIcon(icon, state.isDiscrete ? 1 : 2, state.isDiscrete || iconCosmetic.isTransparentIcon(), true);
			}
		});
	}
}