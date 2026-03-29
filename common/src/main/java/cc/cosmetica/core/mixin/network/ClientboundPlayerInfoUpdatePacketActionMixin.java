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

package cc.cosmetica.core.mixin.network;

import cc.cosmetica.core.builtin.manager.ApiCosmeticManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundPlayerInfoUpdatePacket.Action.class)
public class ClientboundPlayerInfoUpdatePacketActionMixin {
	// This captures only other players
	// See comment in Cosmetica.forwardPublicUserInfoToNametag (v1)
	@Inject(at = @At("RETURN"), method = "lambda$static$0")
	private static void afterAddPlayer(ClientboundPlayerInfoUpdatePacket.EntryBuilder entryBuilder, RegistryFriendlyByteBuf registryFriendlyByteBuf, CallbackInfo ci) {
		final GameProfile profile = entryBuilder.profile;

		if (profile != null) {
			ApiCosmeticManager.lookUpGameProfile(profile);
		}
	}
}