package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.Cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Renderer for baked block/item models.
 */
public class BlockModelRenderer {
	// The index within cachedModelIds to garbage-collect for next.
	private static int gcIndex = 0;
	// Cache
	private static final List<String> CACHED_MODEL_IDS = new ArrayList<>();
	private static final Map<String, ModelCacheEntry> CACHE = new HashMap<>();

	/**
	 * Garbage collect the next item.
	 */
	public static void gc() {

	}

	/**
	 * Bake a model if not already baked, and link the cosmetics object to it. If no cosmetics objects exist in memory
	 * that refer to this baked model, it can get garbage collected.
	 * @param cosmetics the cosmetics object requesting the model be baked.
	 * @param id the id of the model.
	 * @implNote a weak reference to the Cosmetics object is stored.
	 */
	public static void bakeModel(Cosmetics cosmetics, String id) {
		BakedModel model = bakeModel();
		ModelCacheEntry entry = CACHE.computeIfAbsent(id, k -> {
			ModelCacheEntry entry_ = new ModelCacheEntry(model);
			CACHED_MODEL_IDS.add(id);
			return entry_;
		});

		entry.holders.add(new WeakReference<>(cosmetics));
	}

	public static Optional<BakedModel> getBakedModel(String id) {
		ModelCacheEntry entry = CACHE.get(id);
		if (entry == null) return Optional.empty();
		// return the cached baked model
		return Optional.of(entry.bakedModel);
	}

	private static class ModelCacheEntry {
		ModelCacheEntry(BakedModel model) {
			this.bakedModel = model;
			this.holders = new ArrayList<>();
		}

		final BakedModel bakedModel;
		final List<WeakReference<Cosmetics>> holders;
	}

	// bake
	private static BakedModel bakeModel() {
		// TODO
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	// render

	public static void renderModel(BakedModel model, PoseStack stack, MultiBufferSource multiBufferSource, ResourceLocation texture, int packedLight) {
		stack.pushPose();
		boolean isGUI3D = model.isGui3d();
		float transformStrength = 0.25F;
		float rotation = 0.0f;
		float transform = model.getTransforms().getTransform(ItemTransforms.TransformType.GROUND).scale.y();
		stack.translate(0.0D, rotation + transformStrength * transform, 0.0D);
		float xScale = model.getTransforms().ground.scale.x();
		float yScale = model.getTransforms().ground.scale.y();
		float zScale = model.getTransforms().ground.scale.z();

		stack.pushPose();

		final ItemTransforms.TransformType transformType = ItemTransforms.TransformType.FIXED;
		int overlayTyp = OverlayTexture.NO_OVERLAY;
		// ItemRenderer#render start
		stack.pushPose();

		model.getTransforms().getTransform(transformType).apply(false, stack);
		stack.translate(-0.5D, -0.5D, -0.5D);

		RenderType renderType = RenderType.entityTranslucent(texture); // hopefully this is the right one
		VertexConsumer vertexConsumer4 = multiBufferSource.getBuffer(renderType);
		renderModelLists(model, packedLight, overlayTyp, stack, vertexConsumer4);

		stack.popPose();
		// ItemRenderer#render end

		stack.popPose();
		if (!isGUI3D) {
			stack.translate(0.0F * xScale, 0.0F * yScale, 0.09375F * zScale);
		}

		stack.popPose();
	}

	// vanilla code that I don't want to rewrite:

	private static void renderModelLists(BakedModel bakedModel, int packedLight, int overlayType, PoseStack poseStack, VertexConsumer vertexConsumer) {
		Random random = new Random();
		final long seed = 42L;
		Direction[] var10 = Direction.values();
		int var11 = var10.length;

		for(int var12 = 0; var12 < var11; ++var12) {
			Direction direction = var10[var12];
			random.setSeed(seed);
			renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, direction, random), packedLight, overlayType);
		}

		random.setSeed(seed);
		renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, null, random), packedLight, overlayType);
	}

	private static void renderQuadList(PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> list, int i, int j) {
		PoseStack.Pose pose = poseStack.last();
		Iterator var9 = list.iterator();

		while(var9.hasNext()) {
			BakedQuad bakedQuad = (BakedQuad)var9.next();
			int k = -1;

			float f = (float)(k >> 16 & 255) / 255.0F;
			float g = (float)(k >> 8 & 255) / 255.0F;
			float h = (float)(k & 255) / 255.0F;
			vertexConsumer.putBulkData(pose, bakedQuad, f, g, h, i, j);
		}
	}
}
