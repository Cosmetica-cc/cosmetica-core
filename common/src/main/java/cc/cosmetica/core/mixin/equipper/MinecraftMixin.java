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

package cc.cosmetica.core.mixin.equipper;

import cc.cosmetica.core.impl.CosmeticEquipper;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public abstract ClientPacketListener getConnection();

    @Shadow @Nullable public ClientLevel level;

    @Inject(
            method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At("HEAD")
    )
    private void onClearLevel(Screen screen, CallbackInfo ci) {
        ClientPacketListener clientPacketListener = this.getConnection();

        if (clientPacketListener == null) {
            if (this.level != null) {
                Logging.getInstance().warn("Tried to stop listening to websocket updates for remote player cosmetics, but client packet listener is none!");
            }
        } else {
            Logging.getInstance().debug(LoggingCategory.COSMETICS, "Detaching all player infos from their cosmetics manager");
            for (PlayerInfo info : clientPacketListener.getOnlinePlayers()) {
                ((CosmeticEquipper) info).cosmeticacore$onEntityRemoved();
            }
        }
    }
}
