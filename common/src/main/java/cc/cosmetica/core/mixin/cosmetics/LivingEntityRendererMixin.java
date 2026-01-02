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

package cc.cosmetica.core.mixin.cosmetics;

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.render.HumanoidAccessoriesLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Add the custom layers for rendering cosmetics on humanoid models, and makes australians upside down.
 */
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("rawtypes")
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected abstract boolean addLayer(RenderLayer arg);


	@Inject(at=@At("TAIL"), method="<init>")
	private void init(EntityRendererProvider.Context context, EntityModel entityModel, float f, CallbackInfo ci) {
		if (entityModel instanceof HumanoidModel) {
			this.addLayer(new HumanoidAccessoriesLayer((LivingEntityRenderer)(Object)this));
		}
	}

	// ========== //
	// Aussie RSE //
	// ========== //

	@Redirect(method = "setupRotations", at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;stripFormatting(Ljava/lang/String;)Ljava/lang/String;"))
	private String redirectPlayersToOnlyOurCheck(String name, LivingEntity entity) {
		return entity instanceof AbstractClientPlayer ? "Dinnerbone" : ChatFormatting.stripFormatting(name);
	}

	@Redirect(method = "setupRotations", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isModelPartShown(Lnet/minecraft/world/entity/player/PlayerModelPart;)Z"))
	private boolean checkAustralians(Player player, PlayerModelPart part) {
		Optional<Cosmetics> cosmetics = Cosmetics.getCosmetics(player);

		String deformattedReal = ChatFormatting.stripFormatting(player.getName().getString());
		boolean real = (deformattedReal.equals("Dinnerbone") || deformattedReal.equals("Grumm")); // if they're dinnerbone or grumm use normal
		boolean realUpsideDown = player.isModelPartShown(part) && real;
		return realUpsideDown || (cosmetics.isPresent() && cosmetics.get().isUpsideDown());
	}
}
