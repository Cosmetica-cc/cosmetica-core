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

package cc.cosmetica.core.mixin.texture;

import cc.cosmetica.core.render.texture.AnimatedHttpTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Correct the upload of Http textures to support animated textures.
 */
@Mixin(HttpTexture.class)
public class HttpTextureMixin {
	@Inject(at = @At("HEAD"), method = "upload", cancellable = true)
	private void onUpload(NativeImage nativeImage, CallbackInfo ci) {
		if ((Object) this instanceof AnimatedHttpTexture) {
			((AnimatedHttpTexture) (Object) this).firstUpload(nativeImage, false);
			ci.cancel();
		}
	}
}