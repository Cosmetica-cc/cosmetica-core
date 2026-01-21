package cc.cosmetica.core.fabric.mixin;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.NametagRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    /// RENDERING ICON ///

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I")
    )
    private void beforeRenderName(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci,
                                  @Local GameProfile gameProfile) {
        Level level = Minecraft.getInstance().level;

        if (level != null && gameProfile.getId() != null) {
            Player player = level.getPlayerByUUID(gameProfile.getId());

            if (player != null) {
                Cosmetics.getCosmetics(player).ifPresent(cosmetics -> {
                    NametagConfig nametagConfig = cosmetics.getNametag();
                    CachedImage icon = nametagConfig.getIcon().getImage();

                    if (icon.isLoaded()) {
                        NametagRenderer.prepareIcon(icon, 2, nametagConfig.isTransparentIcon(), false);
                    }
                });
            }
        }
    }
}
