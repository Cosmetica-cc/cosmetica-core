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

import cc.cosmetica.core.api.texture.CosmeticaTexture;
import cc.cosmetica.core.render.texture.ModelSprite;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

/**
 * Bakes cosmetica models. A lot of code is reused from the vanilla game.
 */
public final class CosmeticaModelBakery {
	private CosmeticaModelBakery() {
		// NO-OP
	}

	/**
	 * The model bakery.
	 * Set by ModelManagerMixin.
	 */
	public static ModelBakery bakery;

	/**
	 * Bake the given block model with the texture at the given location.
	 * @param location the location to get the texture for. Also used in debug messages.
	 *                 Must refer to an {@link CosmeticaTexture}.
	 * @param model the model to bake.
	 */
	public static BlockModelPart bakeModel(ResourceLocation location, BlockModel model) {
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Computing Baked Model: {}", location);
		AbstractTexture modelTexture = Minecraft.getInstance().getTextureManager().getTexture(location);

		if (modelTexture instanceof CosmeticaTexture) {
			CosmeticaTexture texture = (CosmeticaTexture) modelTexture;
			ModelSprite sprite = new ModelSprite(location, texture.getCurrentImage(),
					texture.getFrameHeight(), texture.getFrameCount(),
					() -> {});
			final String debugName = location.toString();

			return new Variant(location).bake(new ModelBaker() {
				@Override
				public ResolvedModel getModel(ResourceLocation resourceLocation) {
					return new ResolvedModel() {
						@Override
						public UnbakedModel wrapped() {
							return model;
						}

						@Override
						public @Nullable ResolvedModel parent() {
							return null;
						}

						@Override
						public String debugName() {
							return debugName;
						}
					};
				}

				@Override
				public SpriteGetter sprites() {
					return new SpriteGetter() {
						@Override
						public TextureAtlasSprite get(Material material, ModelDebugName modelDebugName) {
							return sprite;
						}

						@Override
						public TextureAtlasSprite reportMissingReference(String string, ModelDebugName modelDebugName) {
							return sprite;
						}
					};
				}

				@Override
				public <T> T compute(SharedOperationKey<T> sharedOperationKey) {
					return sharedOperationKey.compute(this);
				}
			});
		}

		throw new IllegalArgumentException("Texture specified for Cosmetica model bake must be a CosmeticaTexture.");
	}

	// render
	public static void renderModel(BlockModelPart model, PoseStack stack, MultiBufferSource multiBufferSource, ResourceLocation texture, int packedLight) {
		VertexConsumer consumer = multiBufferSource.getBuffer(RenderType.armorTranslucent(texture));

		int[] tints = new int[0];
		for (Direction direction : Direction.values()) {
			List<BakedQuad> quads = model.getQuads(direction);
			renderQuadList(
					stack,
					consumer,
					quads,
					tints,
					packedLight,
					OverlayTexture.NO_OVERLAY
					);
		}

		List<BakedQuad> quads = model.getQuads(null);
		renderQuadList(
				stack,
				consumer,
				quads,
				tints,
				packedLight,
				OverlayTexture.NO_OVERLAY
		);
	}

	// Adapted from ItemRenderer
	private static void renderQuadList(PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tintLayers, int lighting, int overlay) {
		PoseStack.Pose pose = poseStack.last();

		for (BakedQuad bakedQuad : quads) {
			float f;
			float g;
			float h;
			float l;
			if (bakedQuad.isTinted()) {
				int k = getLayerColorSafe(tintLayers, bakedQuad.tintIndex());
				f = ARGB.alpha(k) / 255.0F;
				g = ARGB.red(k) / 255.0F;
				h = ARGB.green(k) / 255.0F;
				l = ARGB.blue(k) / 255.0F;
			} else {
				f = 1.0F;
				g = 1.0F;
				h = 1.0F;
				l = 1.0F;
			}

			vertexConsumer.putBulkData(pose, bakedQuad, g, h, l, f, lighting, overlay);
		}
	}

	private static int getLayerColorSafe(int[] tintLayers, int index) {
		return index >= 0 && index < tintLayers.length ? tintLayers[index] : -1;
	}

	// ======================== //
	// Bounding Box calculation //
	// ======================== //

	public static AABB calculateBoundingBox(JsonElement model) {
		// Find all corners
		Collection<Vector3f> allCorners = new ArrayList<>();

		for (JsonElement e : model.getAsJsonObject().get("elements").getAsJsonArray()) {
			JsonObject element = e.getAsJsonObject();

			JsonArray from = element.getAsJsonArray("from");
			JsonArray to = element.getAsJsonArray("to");

			Collection<Vector3f> corners = getUniqueCorners(
					new Vector3f(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat()),
					new Vector3f(to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat())
			);

			// rotate corners if on a rotated element
			if (element.has("rotation")) {
				Collection<Vector3f> rotated = new HashSet<>();

				JsonObject rotation = element.getAsJsonObject("rotation");

				for (Vector3f corner : corners) {
					JsonArray originJson = rotation.get("origin").getAsJsonArray();
					Vector3f origin = new Vector3f(
							originJson.get(0).getAsFloat(),
							originJson.get(1).getAsFloat(),
							originJson.get(2).getAsFloat()
					);

					String axisJson = rotation.get("axis").getAsString();
					Direction.Axis axis = switch (axisJson.toLowerCase(Locale.ROOT)) {
						case "x" -> Direction.Axis.X;
						case "y" -> Direction.Axis.Y;
						case "z" -> Direction.Axis.Z;
						default -> {
							Logging.getInstance().warn("Bad axis for model element. Got " + axisJson);
							yield Direction.Axis.Y;
						}
					};

//					origin.mul(16);
					rotated.add(
							rotateCorner(
									corner,
									origin,
									axis,
									rotation.get("angle").getAsFloat()
							));
				}

				corners = rotated;
			}

			allCorners.addAll(corners);
		}

		// just in case
		if (allCorners.isEmpty()) {
			return AABB.ofSize(Vec3.ZERO, 0, 0, 0);
		}

		// Calculate the bounding box from the corners
		float[] smallest = new float[3];
		float[] largest = new float[3];
		Iterator<Vector3f> cornerIterator = allCorners.iterator();

		// first corner
		Vector3f corner = cornerIterator.next();
		largest[0] = smallest[0] = corner.x();
		largest[1] = smallest[1] = corner.y();
		largest[2] = smallest[2] = corner.z();

		while (cornerIterator.hasNext()) {
			corner = cornerIterator.next();

			// Update smallest coordinates
			if (corner.x() < smallest[0]) {
				smallest[0] = corner.x();
			}
			if (corner.y() < smallest[1]) {
				smallest[1] = corner.y();
			}
			if (corner.z() < smallest[2]) {
				smallest[2] = corner.z();
			}

			// Update largest coordinates
			if (corner.x() > largest[0]) {
				largest[0] = corner.x();
			}
			if (corner.y() > largest[1]) {
				largest[1] = corner.y();
			}
			if (corner.z() > largest[2]) {
				largest[2] = corner.z();
			}
		}

		return new AABB(smallest[0], smallest[1], smallest[2], largest[0], largest[1], largest[2]);
	}

	private static Collection<Vector3f> getUniqueCorners(Vector3f from, Vector3f to) {
		Set<Vector3f> corners = new HashSet<>();

		corners.add(from);

		corners.add(new Vector3f(to.x(), from.y(), from.z()));
		corners.add(new Vector3f(from.x(), to.y(), from.z()));
		corners.add(new Vector3f(from.x(), from.y(), to.z()));

		corners.add(new Vector3f(from.x(), to.y(), to.z()));
		corners.add(new Vector3f(to.x(), from.y(), to.z()));
		corners.add(new Vector3f(to.x(), to.y(), from.z()));

		corners.add(to);

		return corners;
	}

	private static Vector3f rotateCorner(Vector3f corner, Vector3f origin, Direction.Axis axis, float angle) {
		float[] others;

		switch (axis) {
		case X:
			others = rotatePoint(
					corner.y(), corner.z(),
					origin.y(), origin.z(),
					(float)Math.toRadians(angle)
			);

			return new Vector3f(corner.x(), others[0], others[1]);
		case Y:
		default:
			others = rotatePoint(
					corner.x(), corner.z(),
					origin.x(), origin.z(),
					(float)Math.toRadians(angle)
			);

			return new Vector3f(others[0], corner.y(), others[1]);
		case Z:
			others = rotatePoint(
					corner.x(), corner.y(),
					origin.x(), origin.y(),
					(float)Math.toRadians(angle)
			);

			return new Vector3f(others[0], others[1], corner.z());
		}
	}

	private static float[] rotatePoint(float pt0, float pt1, float o0, float o1, float angle) {
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		pt0 -= o0;
		pt1 -= o1;
		float nx = pt0 * cos - pt1 * sin;
		float ny = pt0 * sin + pt1 * cos;
		return new float[] {nx + o0, ny + o1};
	}
}
