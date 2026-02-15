package cc.cosmetica.core.neoforge.mixin;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "render",
            at = @At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"),
            locals= LocalCapture.CAPTURE_FAILHARD)
    private void beforeRenderName(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci, List list, List list2, int j, int k, int l, int o, int p, int q, boolean bl, int r, int n, int s, int t, int u,
                                  List list3, int w, int x, int v, int y, int z, int aa, PlayerInfo playerInfo2, PlayerTabOverlay.ScoreDisplayEntry scoreDisplayEntry, GameProfile gameProfile) {
        Level level = Minecraft.getInstance().level;

        if (level != null && gameProfile.getId() != null) {
            Player player = level.getPlayerByUUID(gameProfile.getId());

            (player == null ? Cosmetics.getCosmetics(playerInfo2) : Cosmetics.getCosmetics(player)).ifPresent(cosmetics -> {
                    NametagConfig nametagConfig = cosmetics.getNametag();
                    CachedImage icon = nametagConfig.getIcon().getImage();

                    if (icon.isLoaded()) {
                        NametagRenderer.prepareIcon(icon, 2, nametagConfig.isTransparentIcon(), false);
                    }
                });
        }
    }
}
