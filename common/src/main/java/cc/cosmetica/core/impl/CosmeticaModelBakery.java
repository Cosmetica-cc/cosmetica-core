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
import cc.cosmetica.core.render.model.BlockModel;
import cc.cosmetica.core.render.model.FaceInfo;
import cc.cosmetica.core.render.texture.ModelSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.GeometryUtils;
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
	 * @param model the model to model.
	 */
	public static List<BakedQuad> bakeModel(Identifier location, BlockModel model) {
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Computing Baked Model: {}", location);
		AbstractTexture modelTexture = Minecraft.getInstance().getTextureManager().getTexture(location);

		if (modelTexture instanceof CosmeticaTexture) {
			CosmeticaTexture texture = (CosmeticaTexture) modelTexture;
			ModelSprite sprite = new ModelSprite(location, texture.getCurrentImage(),
					texture.getFrameHeight(), texture.getFrameCount(),
					() -> {});
			RenderType renderType = RenderTypes.entityTranslucent(location);

			List<BakedQuad> bakedQuads = new ArrayList<>(model.getElementCount() * 6);

			for (BlockModel.Element element : model.getElements()) {
				Vector3f from = element.from();
				Vector3f to = element.to();

				final class QuadAdderHelper {
					void addQuad(FaceInfo faceInfo, Direction direction) {
						CosmeticaModelBakery.addQuad(
								bakedQuads,
								sprite,
								renderType,
								faceInfo,
								from,
								to,
								element.getFace(direction),
								element.rotation()
						);
					}
				}

				QuadAdderHelper helper = new QuadAdderHelper();

				helper.addQuad(FaceInfo.NORTH, Direction.NORTH);
				helper.addQuad(FaceInfo.EAST, Direction.EAST);
				helper.addQuad(FaceInfo.SOUTH, Direction.SOUTH);
				helper.addQuad(FaceInfo.WEST, Direction.WEST);
				helper.addQuad(FaceInfo.UP, Direction.UP);
				helper.addQuad(FaceInfo.DOWN, Direction.DOWN);
			}

			return bakedQuads;
		}

		throw new IllegalArgumentException("Texture specified for Cosmetica model model must be a CosmeticaTexture.");
	}

	private static void addQuad(List<BakedQuad> output,
								TextureAtlasSprite sprite, RenderType renderType,
								FaceInfo faceInfo,
								Vector3f from, Vector3f to,
								BlockModel.Face face,
								BlockModel.Rotation rotation) {
		Vector3f corner0 = rotateCorner(faceInfo.vertexOrder[0].select(from, to), rotation)
				.div(16.0f);
		Vector3f corner1 = rotateCorner(faceInfo.vertexOrder[1].select(from, to), rotation)
				.div(16.0f);
		Vector3f corner2 = rotateCorner(faceInfo.vertexOrder[2].select(from, to), rotation)
				.div(16.0f);
		Vector3f corner3 = rotateCorner(faceInfo.vertexOrder[3].select(from, to), rotation)
				.div(16.0f);

		final int quadrant = face.rotation/90 & 3;

		final float[] uv0 = { face.getU(0 + quadrant), face.getV(0 + quadrant) };
		final float[] uv1 = { face.getU(1 + quadrant), face.getV(1 + quadrant) };
		final float[] uv2 = { face.getU(2 + quadrant), face.getV(2 + quadrant) };
		final float[] uv3 = { face.getU(3 + quadrant), face.getV(3 + quadrant) };

		final long[] uvs = new long[] {
				UVPair.pack(
						uv0[0] / 16.0f,
						uv0[1] / 16.0f
				),
				UVPair.pack(
						uv1[0] / 16.0f,
						uv1[1] / 16.0f
				),
				UVPair.pack(
						uv2[0] / 16.0f,
						uv2[1] / 16.0f
				),
				UVPair.pack(
						uv3[0] / 16.0f,
						uv3[1] / 16.0f
				)
		};

		output.add(new BakedQuad(
				corner0, corner1, corner2, corner3,
				uvs[0], uvs[1], uvs[2], uvs[3],
				0,
				calculateFacing(corner0, corner1, corner2, corner3),
				sprite,
				true,
				0
		));
	}

	// Vanilla Vertex Direction Calculations
	@NotNull
	private static Direction calculateFacing(final Vector3f ...positions) {
		Vector3f normal = new Vector3f();
		GeometryUtils.normal(positions[0], positions[1], positions[2], normal);
		return findClosestDirection(normal);
	}

	@NotNull
	private static Direction findClosestDirection(final Vector3f direction) {
		if (!Float.isFinite(direction.x()) || !Float.isFinite(direction.z()) || !Float.isFinite(direction.y())) {
			return Direction.UP;
		} else {
			Direction result = null;
			float closestProduct = 0.0F;

			for (Direction dir : Direction.values()) {
				float dotProduct = direction.dot(dir.step());
				if (dotProduct >= 0.0F && dotProduct > closestProduct) {
					closestProduct = dotProduct;
					result = dir;
				}
			}

			return result == null ? Direction.UP : result;
		}
	}

	// render
	public static void renderModel(List<BakedQuad> model, PoseStack stack, MultiBufferSource multiBufferSource, Identifier texture, int packedLight, float alpha) {
		VertexConsumer consumer = multiBufferSource.getBuffer(RenderTypes.armorTranslucent(texture));

		int[] tints = new int[0];
//		for (Direction direction : Direction.values()) {
//			List<BakedQuad> quads = model.getQuads(direction);
//			renderQuadList(
//					stack,
//					consumer,
//					quads,
//					tints,
//					packedLight,
//					OverlayTexture.NO_OVERLAY
//			);
//		}

		renderQuadList(
				stack,
				consumer,
				model,
				tints,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				alpha
		);
	}

	// Adapted from ItemRenderer
	private static void renderQuadList(PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tintLayers, int lighting, int overlay, float alpha) {
		PoseStack.Pose pose = poseStack.last();

		for (BakedQuad bakedQuad : quads) {
			float a;
			float r;
			float g;
			float b;
			if (bakedQuad.isTinted()) {
				int k = getLayerColorSafe(tintLayers, bakedQuad.tintIndex());
				a = alpha * (ARGB.alpha(k) / 255.0F);
				r = ARGB.red(k) / 255.0F;
				g = ARGB.green(k) / 255.0F;
				b = ARGB.blue(k) / 255.0F;
			} else {
				a = alpha;
				r = 1.0F;
				g = 1.0F;
				b = 1.0F;
			}

			vertexConsumer.putBulkData(pose, bakedQuad, r, g, b, a, lighting, overlay);
		}
	}

	private static int getLayerColorSafe(int[] tintLayers, int index) {
		return index >= 0 && index < tintLayers.length ? tintLayers[index] : -1;
	}

	// ======================== //
	// Bounding Box calculation //
	// ======================== //

	public static AABB calculateBoundingBox(BlockModel model) {
		// Find all corners
		Collection<Vector3f> allCorners = new ArrayList<>();

		for (BlockModel.Element element : model.getElements()) {
			Vector3f from = element.from();
			Vector3f to = element.to();

			Collection<Vector3f> corners = getUniqueCorners(from, to);

			// rotate corners if on a rotated element
			if (element.rotation().x != 0 || element.rotation().y != 0 || element.rotation().z != 0) {
				Collection<Vector3f> rotated = new HashSet<>();

				BlockModel.Rotation rotation = element.rotation();

				for (Vector3f corner : corners) {
					rotated.add(
							rotateCorner(
									corner,
									rotation
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

	private static Vector3f rotateCorner(Vector3f corner, BlockModel.Rotation rotation) {
		if (rotation.x != 0) {
			corner = rotateCorner(corner, rotation.origin, Direction.Axis.X, rotation.x);
		}
		if (rotation.y != 0) {
			corner = rotateCorner(corner, rotation.origin, Direction.Axis.Y, -rotation.y);
		}
		if (rotation.z != 0) {
			corner = rotateCorner(corner, rotation.origin, Direction.Axis.Z, rotation.z);
		}
		return corner;
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
