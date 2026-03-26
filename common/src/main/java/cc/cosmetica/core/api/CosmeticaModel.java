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

package cc.cosmetica.core.api;

import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.core.impl.BlockModelManager;
import cc.cosmetica.core.impl.CosmeticaModelBakery;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.LoggingCategory;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.cloaks.javaclient.model.AnimatedTextureCosmetic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Contains model data for a cosmetica model.
 */
public final class CosmeticaModel {
	public CosmeticaModel(Identifier texture) {
		this.texture = texture;
		this.boundingBox = ZERO_BOUNDS;
	}

	private final Identifier texture;
	private BlockStateModelPart model;
	private BlockModel unbakedModel; // cleared when the model is baked!
	private AABB boundingBox;
	private boolean textureLoaded;

	/**
	 * Mark the texture as loaded. If both texture and model are loaded, baking will start.
	 */
	public synchronized void setTextureLoaded() {
		this.textureLoaded = true;
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Model: texture finished loading for {}", this.texture);

		final BlockModel unbaked = this.unbakedModel;

		if (unbaked != null) {
			this.startBaking(unbaked);
		}
	}

	/**
	 * Set the model and bounding box for this {@link CosmeticaModel} to use.
	 * If both texture and model are loaded, baking will start.
	 */
	public synchronized void setModel(BlockModel model, AABB boundingBox) {
		Objects.requireNonNull(model, "Block model cannot be null (" + this.texture + ")");

		this.unbakedModel = model;
		this.boundingBox = boundingBox;

		if (this.textureLoaded) {
			this.startBaking(model);
		}
	}

	private void startBaking(BlockModel model) {
		// TODO should this be if(onRenderThread) bake else recordRenderCall(bake)? Is the speed gain negligible?
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Scheduling baking for {}", this.texture);

		Minecraft.getInstance().schedule(() -> {
			this.model = CosmeticaModelBakery.bakeModel(this.texture, model);
			this.unbakedModel = null; // free memory
			Logging.getInstance().debug(LoggingCategory.ASSETS, "Baked model {}", this.texture);
		});
	}

	/**
	 * Get the location for the texture for this model.
	 * @return the texture for this model.
	 */
	public Identifier getTexture() {
		return this.texture;
	}

	/**
	 * Get the baked model of this cosmetic. If it has not been baked yet, will return null.
	 * @return the baked model for this cosmetic model, or null if it has not been baked yet.
	 */
	@Nullable
	public BlockStateModelPart getBakedModel() {
		return this.model;
	}

	/**
	 * Get the bounding box of this model, or (0,0,0\0,0,0) if the model has not been downloaded yet.
	 * @return the bounding box of this model.
	 */
	public AABB getBoundingBox() {
		return this.boundingBox;
	}

	/**
	 * Render this model cosmetic on the given part, with the given transform.
	 * @param modelPart the model part on which to render.
	 * @param stack the Matrix Stack.
	 * @param multiBufferSource the buffer source.
	 * @param packedLight the packed light.
	 * @param x the x offset.
	 * @param y the y offset.
	 * @param z the z offset.
	 * @param mirror whether to mirror the model.
	 */
	public void renderOnPart(ModelPart modelPart, PoseStack stack, MultiBufferSource multiBufferSource, int packedLight, float x, float y, float z, boolean mirror) {
		BlockStateModelPart model = this.getBakedModel();
		if (model == null) return; // if it is not loaded, has errors with the baked model or cannot render it for another reason will return null
		stack.pushPose();
		float o = 1.0f;
		modelPart.translateAndRotate(stack);
		stack.scale(o, -o, -o);
		if (mirror) stack.scale(-1, 1, 1);
		stack.mulPose(new Quaternionf(new AxisAngle4f((float)Math.PI, YP))); // pi radians on y axis
		stack.translate(x, y, z); // vanilla: 0.0 second param
		stack.translate(-0.5, -0.25, -0.5);

		CosmeticaModelBakery.renderModel(
				model,
				stack,
				multiBufferSource,
				packedLight);
		// 26.1: removed texture parameter (buffers sourced from model)

		stack.popPose();
	}

	/**
	 * Render this model cosmetic on the given part, with the given transform.
	 * @param modelPart the model part on which to render.
	 * @param stack the Matrix Stack.
	 * @param collector the node collector.
	 * @param packedLight the packed light.
	 * @param x the x offset.
	 * @param y the y offset.
	 * @param z the z offset.
	 * @param mirror whether to mirror the model.
	 */
	public void submitOnPart(ModelPart modelPart, PoseStack stack, SubmitNodeCollector collector, int packedLight, float x, float y, float z, boolean mirror) {
		BlockStateModelPart model = this.getBakedModel();
		if (model == null) return; // if it is not loaded, has errors with the baked model or cannot render it for another reason will return null
		stack.pushPose();
		float o = 1.0f;
		modelPart.translateAndRotate(stack);
		stack.scale(o, -o, -o);
		if (mirror) stack.scale(-1, 1, 1);
		stack.mulPose(new Quaternionf(new AxisAngle4f((float)Math.PI, YP))); // pi radians on y axis
		stack.translate(x, y, z); // vanilla: 0.0 second param
		stack.translate(-0.5, -0.25, -0.5);

		collector.submitBlockModel(
				stack,
				RenderTypes.armorTranslucent(this.getTexture()),
				ImmutableList.of(model),
				BlockModelRenderState.EMPTY_TINTS,
				// light, overlay, outline
				packedLight, OverlayTexture.NO_OVERLAY, 0
		);

//		CosmeticaModelBakery.renderModel(
//				model,
//				stack,
//				multiBufferSource,
//				this.getTexture(),
//				packedLight);

		stack.popPose();
	}



	private static final Vector3f YP = new Vector3f(0, 1, 0);
	private static final AABB ZERO_BOUNDS = AABB.ofSize(Vec3.ZERO, 0, 0, 0);

	// ==== Direct Model/Image Overloads ==== //

	/**
	 * Get or bake a model for the given id.
	 * @param id the id of the model. Should be unique per-model.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param textureCategory the category to cache the texture in. Rules are the same as textureId.
	 * @param textureId the id of the model's texture. Should be unique per-texture, per category.
	 *           It is recommended to use {@link CosmeticaModel#textureId(String)} for cosmetica models.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param modelURL the url to the Java Block/Item model json to use if the model hasn't been created yet.
	 * @param textureURL the url for the texture to download, if the model has not been created yet.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Set to a negative number to have multiple frames, but not auto-animate.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the BakedModel is stored in cache.
	 * @return a {@link CosmeticaModel} with the model and texture location for this model.
	 */
	public static CosmeticaModel getOrCreateModel(String id, String textureCategory, String textureId, String modelURL,
												  String textureURL, int ticksPerFrame, int frames) {
		return BlockModelManager.getOrCreateModel(id, textureCategory + "/" + textureId, () -> CosmeticaAPI.downloadAsync(modelURL), textureURL, ticksPerFrame, frames);
	}

	/**
	 * Get or bake a model for the given id, with a model override.
	 * @param id the id of the model. Should be unique per-model.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param textureCategory the category to cache the texture in. Rules are the same as textureId.
	 * @param textureId the id of the model's texture. Should be unique per-texture, per category.
	 *           It is recommended to use {@link CosmeticaModel#textureId(String)} for cosmetica models.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param model an input stream to the Java Block/Item model json to use if the model hasn't been created yet.
	 * @param textureURL the url for the texture to download, if the model has not been created yet.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Set to a negative number to have multiple frames, but not auto-animate.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the BakedModel is stored in cache.
	 * @return a {@link CosmeticaModel} with the model and texture location for this model.
	 */
	public static CosmeticaModel getOrCreateModel(String id, String textureCategory, String textureId, InputStreamSupplier model,
												String textureURL, int ticksPerFrame, int frames) {
		return BlockModelManager.getOrCreateModel(id, textureCategory + "/" + textureId, () -> CompletableFuture.supplyAsync(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(model.open()))) {
				return reader.lines().collect(Collectors.joining("\n"));
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to read model " + id + " from input stream", e);
			}
		}), textureURL, ticksPerFrame, frames);
	}

	@FunctionalInterface
	public interface InputStreamSupplier {
		InputStream open() throws IOException;
	}

	/**
	 * Get or download an image for the given id. This ensures a given image is only in memory once and is removed when
	 * all references are gone.
	 * @param category the category of the image. Allowed characters are the same as id.
	 * @param textureId the id of the image. Should be unique per-image, per category.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param texture The builder from which to set up the texture. Note! {@code cached} and {@code onLoad} will be overridden.
	 * @implNote a weak reference to the CachedImage is stored in cache.
	 * @return a {@link CachedImage}.
	 */
	public static CachedImage getOrCreateImage(String category, String textureId, CosmeticaTexture.Builder texture) {
		return BlockModelManager.getOrCreateImage(category + "/" + textureId, texture);
	}

	// ==== Cosmetica Core Overloads ==== //

	/**
	 * Get or bake a cosmetica model for the given id.
	 * @param id the id of the model. Should be unique per-model, per category.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link Identifier} pathnames.
	 * @param modelURL the url to the Java Block/Item model json to use if the model hasn't been created yet.
	 * @param textureURL the url for the texture to download, if the model has not been created yet.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Set to a negative number to have multiple frames, but not auto-animate.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the BakedModel is stored in cache.
	 * @return a {@link CosmeticaModel} with the model and texture location for this model.
	 */
	public static CosmeticaModel getOrCreateCosmeticaModel(String id, String modelURL,
														   String textureURL, int ticksPerFrame, int frames) {
		return BlockModelManager.getOrCreateModel(id, "textures/" + textureId(textureURL), () -> CosmeticaAPI.downloadAsync(modelURL), textureURL, ticksPerFrame, frames);
	}

	/**
	 * Get or download an image for the given cosmetic. This ensures a given image is only in memory once and is removed when
	 * all references are gone.
	 * @param cosmetic the animated texture cosmetic. The id will be retrieved from cosmetic#getId()
	 * @implNote a weak reference to the CachedImage is stored in cache.
	 * @return a {@link CachedImage}.
	 */
	public static CachedImage getOrCreateCosmeticaImage(AnimatedTextureCosmetic cosmetic) {
		Objects.requireNonNull(cosmetic.getTexture(), "Cannot create image for cosmetic with null texture.");

		String textureId = textureId(cosmetic.getTexture());

		return BlockModelManager.getOrCreateImage("textures/" + textureId,
				new CosmeticaTexture.Builder(cosmetic.getTexture(), BlockModelManager.FALLBACK_TEXTURE)
						.frames(cosmetic.getFrames().intValue(), cosmetic.getTicksPerFrame().intValue()));
	}

	/**
	 * Get or download a cosmetica image for the given id. This ensures a given image is only in memory once and is removed when
	 * all references are gone.
	 * @param texture The builder from which to set up the texture. Note! {@code cached} and {@code onLoad} will be overridden.
	 * @implNote a weak reference to the CachedImage is stored in cache.
	 * @return a {@link CachedImage}.
	 */
	public static CachedImage getOrCreateCosmeticaImage(CosmeticaTexture.Builder texture) {
		return BlockModelManager.getOrCreateImage("textures/" + textureId(texture.getURL()), texture);
	}

	/**
	 * Extract the texture ID from a texture URL.
	 * @param textureURL the texture url from cosmetica or nametag. Undefined behaviour with other services.
	 * @return the texture id.
	 */
	public static String textureId(String textureURL) {
		String[] parts = textureURL.split("/");
		String textureIdPart = parts[parts.length - 1];

		parts = textureIdPart.split("\\.");
		return "textures/" + parts[0];
	}
}
