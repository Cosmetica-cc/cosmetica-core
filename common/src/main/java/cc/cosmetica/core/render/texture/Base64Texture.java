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

package cc.cosmetica.core.render.texture;

import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.mixin.texture.NativeImageAccessorMixin;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * An animated texture loaded from a base64 image string.
 */
public class Base64Texture extends AnimatedTexture {
	private Base64Texture(ResourceLocation path, CompletableFuture<String> base64, int frames) {
		super(frames);
		this.base64 = base64;
		this.path = path;

		base64.thenAccept(downloadedBase64 -> RenderSystem.recordRenderCall(() -> this.loadImage(downloadedBase64, false)));
	}

	private final ResourceLocation path;
	private final CompletableFuture<String> base64;

	@Override
	public void load(ResourceManager resourceManager) {
		if (((NativeImageAccessorMixin) (Object) this.image).getPixels() == 0) {
			if (RenderSystem.isOnRenderThreadOrInit()) {
				this.reload();
			} else {
				RenderSystem.recordRenderCall(this::reload);
			}

			return;
		} else {
			this.upload();
		}
	}

	private void reload() {
		Logging.getInstance().debug("Re-uploading texture {}", this.path);

		if (this.base64.isDone() && !this.base64.isCompletedExceptionally()) {
			if (this.loadImage(this.base64.join(), true)) { // load the image
				this.upload();
			}
		}
	}

	private boolean loadImage(String base64, boolean reload) {
		try {
			this.image = loadBase64(base64);
			this.setupAnimations();
			return true;
		} catch (IOException e) {
			Logging.getInstance().error(reload ? "Error reloading Base64 Texture" : "Error loading Base64 Texture", e);
			return false;
		}
	}

	private static NativeImage loadBase64(String base64) throws IOException {
		if(base64.length() < 1000) { //TODO: Tweak this number
			return NativeImage.fromBase64(base64);
		} else {
			//For large images, NativeImage.fromBase64 does not work because it tries to allocate it on the stack and fails
			byte[] bs = Base64.getDecoder().decode(base64.replace("\n", "").getBytes(StandardCharsets.UTF_8));
			ByteBuffer buffer = MemoryUtil.memAlloc(bs.length);
			buffer.put(bs);
			buffer.rewind();
			NativeImage image = NativeImage.read(buffer);
			MemoryUtil.memFree(buffer);
			return image;
		}
	}

	public static Base64Texture create(ResourceLocation path, String base64, int ticksPerFrame, int frames) throws IOException {
		if (frames > 0) {
			return new TickingTexture(path, CompletableFuture.completedFuture(base64), ticksPerFrame, frames);
		}
		else {
			return new Base64Texture(path, CompletableFuture.completedFuture(base64), 0);
		}
	}

	public static Base64Texture download(ResourceLocation path, String url, int ticksPerFrame, int frames) throws IOException {
		CompletableFuture<String> base64 = CompletableFuture.supplyAsync()
		if (frames > 0) {
			return new TickingTexture(path, CompletableFuture.completedFuture(base64), ticksPerFrame, frames);
		}
		else {
			return new Base64Texture(path, CompletableFuture.completedFuture(base64), 0);
		}
	}

	private static class TickingTexture extends Base64Texture implements Tickable {
		private TickingTexture(ResourceLocation path, CompletableFuture<String> base64,
							   int ticksPerFrame, int frames) {
			super(path, base64, frames);
			this.ticksPerFrame = ticksPerFrame;
		}

		@Override
		public void tick() {
			this.doTick();
		}
	}
}
