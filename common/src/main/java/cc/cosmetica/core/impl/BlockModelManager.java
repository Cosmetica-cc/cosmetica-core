/*
 * Copyright 2024 Cosmetica
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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.render.texture.AnimatedTexture;
import cc.cosmetica.core.render.texture.Base64Texture;
import cc.cosmetica.core.render.texture.ModelSprite;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jdk.internal.loader.Resource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Renderer and manager for baked block/item models.
 */
public class BlockModelManager {
	// The index within cachedModelIds to garbage-collect for next.
	private static int gcIndex = 0;
	// Cache
	private static final List<String> CACHED_MODEL_IDS = new ArrayList<>();
	private static final Map<String, WeakReference<CosmeticaModel>> CACHE = new HashMap<>();

	/**
	 * The model bakery.
	 * Set by ModelManagerMixin.
	 */
	public static ModelBakery bakery;

	/**
	 * Garbage Collector. Checks the next item and removes it if it's unnecessary.
	 */
	public static void gc() {
		String gcModelId = CACHED_MODEL_IDS.get(gcIndex);

		// if object is no longer held in memory
		if (CACHE.get(gcModelId).get() == null) {
			// remove from cache
			CACHED_MODEL_IDS.remove(gcIndex);
			CACHE.remove(gcModelId);
			// free the texture
			ResourceLocation textureLocation = getModelLocation(gcModelId);
			AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(getModelLocation(gcModelId));
			if (texture != null) Minecraft.getInstance().getTextureManager().safeClose(textureLocation, texture);
		} else {
			gcIndex++; // check the next one.
			// not necessary if removed as the next item shifts back
		}

		if (gcIndex > CACHED_MODEL_IDS.size()) {
			gcIndex = 0;
		}
	}

	/**
	 * Bake a model if not already baked, and return it.
	 * @param id the id of the model. Should be unique per-model, so I recommend adding a prefix related to the purpose.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link ResourceLocation} pathnames.
	 * @param modelJson the Java Block/Item model json to use if the model hasn't been baked yet.
	 * @param textureBase64 the base64 texture to use, if the model has not been baked yet.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the BakedModel is stored in cache.
	 * @return a {@link CosmeticaModel} with the model amnd texture location for this model.
	 */
	public static CosmeticaModel getOrBakeModel(String id, String modelJson,
												String textureBase64, int ticksPerFrame, int frames) {
		WeakReference<CosmeticaModel> modelRef = CACHE.get(id);
		CosmeticaModel model = modelRef == null ? null : modelRef.get(); // if the model doesn't exist or has expired, generate a new one

		if (model == null) {
			// model id. Primarily used for texture location.
			ResourceLocation textureLocation = getModelLocation(id);

			try (InputStream is = new ByteArrayInputStream(modelJson.getBytes(StandardCharsets.UTF_8))) {
				// create texture
				AnimatedTexture texture = Base64Texture.create(textureLocation, textureBase64.substring(22), ticksPerFrame, frames);

				// upload texture
				if (RenderSystem.isOnRenderThreadOrInit()) {
					Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
				}
				else {
					RenderSystem.recordRenderCall(() -> {
						Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
					});
				}

				// create model
				BlockModel blockModel = BlockModel.fromStream(new InputStreamReader(is, StandardCharsets.UTF_8));
				blockModel.name = id;
				model = new CosmeticaModel(textureLocation, bakeModel(textureLocation, blockModel));

				// store in cache
				CACHE.put(id, new WeakReference<>(model));
			} catch (IOException e) {
				Logging.getInstance().error("Failed to parse model " + id, e);
			}
		}

		return model;
	}

	/**
	 * Get the location the model's texture is registered at.
	 * @param id the model id, including any prefix used.
	 * @return the location of the model's texture.
	 */
	public static ResourceLocation getModelLocation(String id) {
		return new ResourceLocation("cosmetica-core", "models/" + pathify(id));
	}

	/**
	 * Take an id that can contain base64 characters and spit out text that is allowed in ResourceLocation pathnames.
	 * @param id the id to pathify.
	 * @return the resulting string.
	 */
	private static String pathify(String id) {
		StringBuilder result = new StringBuilder();

		for (char c : id.toCharArray()) {
			if (c == '+') {
				result.append(".");
			}
			else if (c == '=') {
				result.append("__");
			}
			else if (Character.isUpperCase(c)) {
				result.append("_").append(Character.toLowerCase(c));
			}
			else {
				result.append(c);
			}
		}

		return result.toString();
	}

	// bake

	/**
	 * Bake the given block model with the texture at the given location.
	 * @param location the location to get the texture for. Also used in debug messages.
	 *                 Must refer to an {@link AnimatedTexture}.
	 * @param model the model to bake.
	 * @return the newly created baked model.
	 */
	private static BakedModel bakeModel(ResourceLocation location, BlockModel model) {
		Logging.getInstance().debug("Computing Baked Model: {}", location);
		AbstractTexture modelTexture = Minecraft.getInstance().getTextureManager().getTexture(location);

		if (modelTexture instanceof AnimatedTexture) {
			ModelSprite sprite = new ModelSprite(location, (AnimatedTexture) modelTexture);

			return model.bake(
					bakery,
					l -> sprite,
					BlockModelRotation.X0_Y0,
					location /*this resource location in bake is just used for debugging in the case of errors*/);
		}

		throw new IllegalArgumentException("Texture specified for Cosmetica model bake must be an AnimatedTexture.");
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
