package cc.cosmetica.core.api;

import cc.cosmetica.core.impl.BlockModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Contains model data for a cosmetica model.
 */
public interface CosmeticaModel {
	/**
	 * Get the location for the texture for this model.
	 * @return the texture for this model.
	 */
	ResourceLocation getTexture();

	/**
	 * Get the baked model of this cosmetic. If it has not been baked yet, bake the model.
	 * @return the baked model for this cosmetic model.
	 */
	BakedModel getBakedModel();

	/**
	 * Render this model cosmetic on the given part, with the given transform.
	 * @param modelPart the model part to render.
	 * @param stack the Matrix Stack.
	 * @param multiBufferSource the buffer source.
	 * @param packedLight the packed light.
	 * @param x the x offset.
	 * @param y the y offset.
	 * @param z the z offset.
	 * @param mirror whether to mirror the model.
	 */
	default void renderOnPart(ModelPart modelPart, PoseStack stack, MultiBufferSource multiBufferSource, int packedLight, float x, float y, float z, boolean mirror) {
		BakedModel model = this.getBakedModel();
		if (model == null) return; // if it has errors with the baked model or cannot render it for another reason will return null
		stack.pushPose();
		float o = 1.001f; // prevent z fighting
		modelPart.translateAndRotate(stack);
		stack.scale(o, -o, -o);
		stack.mulPose(new Quaternion(Vector3f.YP, (float)Math.PI, false)); // pi radians on y axis
		stack.translate(x, y, z); // vanilla: 0.0 second param
		if (mirror) stack.scale(-1, 1, 1);
		BlockModelRenderer.renderModel(
				model,
				stack,
				multiBufferSource,
				this.getTexture(),
				packedLight);

		stack.popPose();
	}
}
