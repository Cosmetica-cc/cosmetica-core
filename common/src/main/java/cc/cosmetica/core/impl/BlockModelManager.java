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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.CosmeticaModel;
import cc.cosmetica.core.api.texture.CosmeticaTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Renderer and manager for baked block/item models.
 */
public class BlockModelManager {
	// Cache
	private static final WeakCache<CosmeticaModel> MODEL_CACHE = new WeakCache<>();
	private static final WeakCache<CachedImage> IMAGE_CACHE = new WeakCache<>();

	private static final Path CACHE_DIRECTORY;
	public static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("cosmetica-core", "icon.png");
	public static final ImageCacheManager IMAGE_CACHE_MANAGER;

	static {
		// find cache directory location
		// preferred location: .minecraft/.cosmetica
		Path minecraftDir = findDefaultInstallDir("minecraft");

		if (Files.isDirectory(minecraftDir)) {
			CACHE_DIRECTORY = minecraftDir.resolve(".cosmetica");
		} else {
			CACHE_DIRECTORY = CosmeticaCoreExpectPlatform.getGameDirectory().resolve(".cosmetica");
		}

		Logging.getInstance().debug(LoggingCategory.ASSETS, "Cosmetica cache directory: {}", CACHE_DIRECTORY);

		// create cache directory if it doesn't exist
		if (!Files.exists(CACHE_DIRECTORY)) {
			try {
				Files.createDirectory(CACHE_DIRECTORY);

				// stupid windows
				if (Util.getPlatform() == Util.OS.WINDOWS) {
					try {
						Files.setAttribute(CACHE_DIRECTORY, "dos:hidden", true);
					} catch (Exception e) {
						Logging.getInstance().warn("Failed to set dos:hidden for cache file on windows", e);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Error creating Cosmetica cache directory", e);
			}
		}

		IMAGE_CACHE_MANAGER = new ImageCacheManager(CACHE_DIRECTORY);
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
		MODEL_CACHE.gc();
		IMAGE_CACHE.gc();
	}

	/**
	 * Create and start baking a model if not already loaded, and return the global instance for that model.
	 * Designed to avoid duplicating models for the same cosmetic.
	 * @param modelId the id of the model. Should be unique per-model, so I recommend adding a prefix related to the purpose.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link ResourceLocation} pathnames.
	 * @param textureId the id of the model's texture. Should be unique per-texture, so I recommend adding a prefix related to the purpose.
	 *           Allowed characters are the union of characters allowed in base64 strings, and characters allowed in
	 *           {@link ResourceLocation} pathnames.
	 * @param jsonSource the location of the Java Block/Item model json to download if the model hasn't been created yet.
	 * @param textureUrl the location of the texture for this model.
	 * @param ticksPerFrame the number of ticks each frame should be shown for. Ignored if the texture is static.
	 * @param frames the number of frames in the image. Set to 0 for a static texture.
	 *               Image frames are to be stored as a tilesheet, top to bottom.
	 * @implNote a weak reference to the {@link CosmeticaModel} is stored in cache.
	 * @return a {@link CosmeticaModel} with the model amnd texture location for this model.
	 */
	public static CosmeticaModel getOrCreateModel(String modelId, String textureId, Supplier<CompletableFuture<String>> jsonSource,
												  String textureUrl, int ticksPerFrame, int frames) {
		CosmeticaModel model = MODEL_CACHE.get(modelId);

		// if the model doesn't exist or has expired, generate a new one
		if (model == null) {
			// model id. Primarily used for texture location.
			ResourceLocation textureLocation = getLocation(textureId);
			File cacheFile = getCacheFile(textureLocation, IMAGE_CACHE_MANAGER).toFile();

			model = new CosmeticaModel(textureLocation);

			// create texture
			AbstractTexture texture = new CosmeticaTexture.Builder(textureUrl, FALLBACK_TEXTURE)
					.frames(frames, ticksPerFrame)
					.cached(cacheFile)
					.onLoad(image -> {
						// don't store a reference to the CosmeticaModel or it will prevent GC
						CosmeticaModel _model = MODEL_CACHE.get(modelId);

						if (_model != null) {
							Logging.getInstance().debug(LoggingCategory.ASSETS, "Texture loaded for {} ({})", modelId, textureId);
							_model.setTextureLoaded();
						} else {
							Logging.getInstance().warn("Texture failed to load for {} ({}) (Model is missing)", modelId, textureId);
						}
					})
					.build();

			// upload texture
			// don't use isOnRenderThreadOrInit
			if (RenderSystem.isOnRenderThread()) {
				Logging.getInstance().debug(LoggingCategory.ASSETS, "Registering texture {} for cosmetic {}", textureId, modelId);
				Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
			}
			else {
				RenderSystem.recordRenderCall(() -> {
					Logging.getInstance().debug(LoggingCategory.ASSETS, "Registering texture {} for cosmetic {}", textureId, modelId);
					Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
				});
			}

			// load model
			final CosmeticaModel lambdaHack = model;
			jsonSource.get()
					.exceptionally(ex -> { // handle non-success responses
						Logging.getInstance().error("Failed to download block model for {}", ex, modelId);
						return null;
					})
					.thenAccept(json -> {
						if (json == null) return;

						try (InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
							BlockModel blockModel = BlockModel.fromStream(new InputStreamReader(is, StandardCharsets.UTF_8));
							blockModel.name = modelId;

							// calculate bounds
							AABB aabb = CosmeticaModelBakery.calculateBoundingBox(blockModel);
							Logging.getInstance().debug(LoggingCategory.ASSETS, "Bounding Box calculation for {}: {}", blockModel.name, aabb);

							lambdaHack.setModel(blockModel, aabb);
						} catch (IOException | RuntimeException e) {
							Logging.getInstance().error("Failed to parse model " + modelId, e);
						}
					});

			// store in cache
			MODEL_CACHE.cacheWeakly(modelId, model);
		}

		return model;
	}

	/**
	 * Get or download an image for the given id. This ensures a given image is only in memory once and is removed when
	 * all references are gone.
	 * @param id the id of the image.
	 * @param textureBuilder the builder to set up the texture. Note! OnLoad and Cache will be overridden.
	 * @implNote a weak reference to the CachedImage is stored in cache.
	 * @return a {@link CachedImage}.
	 */
	public static CachedImage getOrCreateImage(String id, CosmeticaTexture.Builder textureBuilder) {
		CachedImage image = IMAGE_CACHE.get(id);

		// if the image doesn't exist or has expired, generate a new one
		if (image == null) {
			// image id. Primarily used for texture location.
			ResourceLocation textureLocation = getLocation(id);
			File cacheFile = getCacheFile(textureLocation, IMAGE_CACHE_MANAGER).toFile();

			image = new CachedImage(textureLocation, textureBuilder.getTicksPerFrame());

			// create texture
			final int frameCount = textureBuilder.getFrames();

			AbstractTexture texture = textureBuilder
					.cached(cacheFile)
					.onLoad(nativeImage -> {
						// don't store a reference to the CachedImage or it will prevent GC
						CachedImage _image = IMAGE_CACHE.get(id);

						if (_image != null) {
							Logging.getInstance().debug(LoggingCategory.ASSETS, "Texture loaded for {}", id);
							_image.setLoaded(nativeImage.getWidth(), nativeImage.getHeight() / frameCount);
						} else {
							Logging.getInstance().warn("Texture failed to load for {} (CachedImage is missing)", id);
						}
					})
					.build();

			// store in cache (do now!)
			IMAGE_CACHE.cacheWeakly(id, image);

			// upload texture
			// don't use isOnRenderThreadOrInit because we spawn other threads on init.
			if (RenderSystem.isOnRenderThread()) {
				Logging.getInstance().debug(LoggingCategory.ASSETS, "Registering texture for cosmetic {} at {}", id, textureLocation);
				Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
			}
			else {
				RenderSystem.recordRenderCall(() -> {
					Logging.getInstance().debug(LoggingCategory.ASSETS, "Registering texture for cosmetic {} at {}", id, textureLocation);
					Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
				});
			}
		}

		return image;
	}

	// public: Internally exposed for Cosmetica 2
	public static Path getCacheFile(ResourceLocation textureLocation, @Nullable ImageCacheManager manager) {
		Path path = getUngroupedPath(textureLocation);
		String fileName = path.getFileName().toString();
		String subdirectory = getSubdirectory(fileName);

		// mark accessed
		if (manager != null) {
			manager.mark(path.getParent(), subdirectory);
		}
		return path.getParent().resolve(subdirectory).resolve(fileName);
	}

	private static Path getUngroupedPath(ResourceLocation textureLocation) {
		Path basePath = CACHE_DIRECTORY;

		// default namespace
		if (!"cosmetica-core".equals(textureLocation.getNamespace())) {
			basePath = CACHE_DIRECTORY.resolve(textureLocation.getNamespace());
		}

		// add the path location
		return basePath.resolve(textureLocation.getPath());
	}

	/**
	 * For caching large numbers of files it is easier to have less files in a directory
	 */
	private static String getSubdirectory(String fileName) {
		String subdirectory = fileName.length() < 2 ? "xx" : fileName.substring(0, 2);
		// _ character is used in the replacement for capitals so it will appear more often. so split it into more categories
		if (fileName.length() > 2 && subdirectory.charAt(0) == '_' || subdirectory.charAt(1) == '_') {
			subdirectory = fileName.substring(0, 3);
		}
		return subdirectory;
	}

	/**
	 * Set the images to preserve. This replaces existing images.
	 * @param images the images in the image cache to preserve. These won't be deleted even after expiry.
	 */
	public static void preserveImages(List<ResourceLocation> images) {
		IMAGE_CACHE_MANAGER.setKeep(images.stream().map(rl -> {
			Path path = getUngroupedPath(rl);
			String fileName = path.getFileName().toString();
			String subdirectory = getSubdirectory(fileName);

			// same method as keep contains check (yes, forced forward slash and string usage)
			return path.getParent().toString() + "/" + subdirectory;
		}).collect(Collectors.toList()));
	}

	/**
	 * Get the location the model's texture is registered at.
	 * @param id the model id, including any prefix used.
	 * @return the location of the model's texture.
	 */
	public static ResourceLocation getLocation(String id) {
		return new ResourceLocation("cosmetica-core", pathify(id));
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

	// caches
	private static class WeakCache<T> {
		private int gcIndex = 0;
		private final List<String> cachedIds = new ArrayList<>();
		private final Map<String, WeakReference<T>> cache = new HashMap<>();

		T get(String id) {
			WeakReference<T> ref = cache.get(id);
			return ref == null ? null : ref.get();
		}

		synchronized void cacheWeakly(String id, T t) {
			if (id == null)
				throw new IllegalStateException("Cannot store ID null");
			if (t == null)
				throw new IllegalStateException("Cannot store a value of null");

			// in case overriding
			if (!cache.containsKey(id))
				cachedIds.add(id);

			cache.put(id, new WeakReference<>(t));
		}

		/**
		 * Garbage Collector. Checks the next item and removes it if it's pointed to nothing.
		 * Prevents memory leaks.
		 */
		synchronized void gc() {
			if (cachedIds.isEmpty()) return;

			String id = cachedIds.get(gcIndex);

			if (id == null)
				throw new IllegalStateException("Fetched cached ID but it was null");

			// if object is no longer held in memory
			if (cache.get(id).get() == null) {
				// remove from cache
				cachedIds.remove(gcIndex);
				cache.remove(id);
				Logging.getInstance().debug(LoggingCategory.GARBAGE_COLLECTOR, "Cosmetica GC: removing {}", id);

				// free the texture
				ResourceLocation textureLocation = getLocation(id);
				AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(getLocation(id));
				if (texture != null) Minecraft.getInstance().getTextureManager().safeClose(textureLocation, texture);
			} else {
				gcIndex++; // check the next one.
				// not necessary if removed as the next item shifts back
			}

			// This is safe because CACHED_MODEL_IDS is only shrunk in this method.
			if (gcIndex >= cachedIds.size()) {
				gcIndex = 0;
			}
		}
	}
}
