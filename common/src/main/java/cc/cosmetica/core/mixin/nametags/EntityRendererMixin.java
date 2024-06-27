package cc.cosmetica.core.mixin.nametags;

import cc.cosmetica.core.impl.NametagRenderer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Set icon texture before rendering nametag.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

}
