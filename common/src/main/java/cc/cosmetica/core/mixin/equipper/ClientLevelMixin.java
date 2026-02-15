package cc.cosmetica.core.mixin.equipper;

import cc.cosmetica.core.impl.CosmeticEquipper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        assert Minecraft.getInstance().player != null; // We are in game
        ClientPacketListener connection = Minecraft.getInstance().player.connection;

        for (PlayerInfo info : connection.getOnlinePlayers()) {
            ((CosmeticEquipper) info).cosmeticacore$pollCosmetics();
        }
    }
}
