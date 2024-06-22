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

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.render.texture.CosmeticaHttpTexture;
import cc.cosmetica.core.render.texture.ModelSprite;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	private static final Path CACHE_DIRECTORY;
	private static final ResourceLocation LOADING_TEXTURE = new ResourceLocation("cosmetica-core", "icon.png");

	static {
		Path minecraftDir = findDefaultInstallDir("minecraft");

		if (Files.isDirectory(minecraftDir)) {
			CACHE_DIRECTORY = minecraftDir.resolve(".cosmetica");
		} else {
			CACHE_DIRECTORY = CosmeticaCoreExpectPlatform.getGameDirectory().resolve(".cosmetica");
		}
	}

	/*
	 * Adapted from code at https://github.com/FabricMC/fabric-installer
	 * Original license has been preserved for this method.
	 *
	 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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
	private static Path findDefaultInstallDir(String application) {
		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		Path dir;

		if (os.contains("win") && System.getenv("APPDATA") != null) {
			dir = Paths.get(System.getenv("APPDATA")).resolve("." + application);
		} else {
			String home = System.getProperty("user.home", ".");
			Path homeDir = Paths.get(home);

			if (os.contains("mac")) {
				dir = homeDir.resolve("Library").resolve("Application Support").resolve(application);
			} else {
				dir = homeDir.resolve("." + application);
			}
		}

		return dir.toAbsolutePath().normalize();
	}

	/**
	 * Garbage Collector. Checks the next item and removes it if it's pointed to nothing.
	 * Prevents memory leaks.
	 */
	public static void gc() {
		if (CACHED_MODEL_IDS.isEmpty()) return;

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

		// This is safe because CACHED_MODEL_IDS is only shrunk in this method.
		if (gcIndex > CACHED_MODEL_IDS.size()) {
			gcIndex = 0;
		}
	}

	/**
	 * Bake a model if not already baked, and return it.
	 * @param id the id of the model. Should be unique per-model, so I recommend adding a prefix related to the purpose.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link ResourceLocation} pathnames.
	 * @param jsonUrl the location of the Java Block/Item model json to download if the model hasn't been baked yet.
	 * @param textureUrl the location of the texture for this model.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the BakedModel is stored in cache.
	 * @return a {@link CosmeticaModel} with the model amnd texture location for this model.
	 */
	public static CosmeticaModel getOrBakeModel(String id, String jsonUrl,
												String textureUrl, int ticksPerFrame, int frames) {
		WeakReference<CosmeticaModel> modelRef = CACHE.get(id);
		CosmeticaModel model = modelRef == null ? null : modelRef.get(); // if the model doesn't exist or has expired, generate a new one

		if (model == null) {
			// model id. Primarily used for texture location.
			ResourceLocation textureLocation = getModelLocation(id);
			File cacheFile = getCacheFile(textureLocation).toFile();

			model = new CosmeticaModel(textureLocation);

			//try (InputStream is = new ByteArrayInputStream(jsonUrl.getBytes(StandardCharsets.UTF_8))) {
				// create texture
				AbstractTexture texture = new CosmeticaHttpTexture.Builder(textureUrl, LOADING_TEXTURE)
						.frames(frames, ticksPerFrame)
						.cached(cacheFile)
						.onLoad(model::setTextureLoaded)
						.build();

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
				//BlockModel blockModel = BlockModel.fromStream(new InputStreamReader(is, StandardCharsets.UTF_8));
				//blockModel.name = id;

				// store in cache
				CACHE.put(id, new WeakReference<>(model));
			//} catch (IOException e) {
			//	Logging.getInstance().error("Failed to parse model " + id, e);
			//}
		}

		return model;
	}

	private static Path getCacheFile(ResourceLocation textureLocation) {
		return CACHE_DIRECTORY.resolve(textureLocation.getNamespace()).resolve(textureLocation.getPath());
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


}
