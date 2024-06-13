package cc.cosmetica.core.mixin;

import cc.cosmetica.core.impl.BlockModelManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tick the {@link cc.cosmetica.core.impl.BlockModelManager} gc.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(at = @At("HEAD"), method="tick")
	private void onTick(CallbackInfo ci) {
		BlockModelManager.gc();
	}
}
