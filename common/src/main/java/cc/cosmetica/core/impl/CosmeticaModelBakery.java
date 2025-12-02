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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

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
	public static BakedModel bakeModel(ResourceLocation location, BlockModel model) {
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Computing Baked Model: {}", location);
		AbstractTexture modelTexture = Minecraft.getInstance().getTextureManager().getTexture(location);

		if (modelTexture instanceof CosmeticaTexture) {
			CosmeticaTexture texture = (CosmeticaTexture) modelTexture;
			ModelSprite sprite = new ModelSprite(location, texture.getCurrentImage(),
					texture.getFrameHeight(), texture.getFrameCount(),
					() -> {});

			return model.bake(
					bakery,
					l -> sprite,
					BlockModelRotation.X0_Y0,
					location /*this resource location in bake is just used for debugging in the case of errors*/);
		}

		throw new IllegalArgumentException("Texture specified for Cosmetica model bake must be a CosmeticaTexture.");
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

	// ======================== //
	// Bounding Box calculation //
	// ======================== //

	public static AABB calculateBoundingBox(BlockModel model) {
		// Find all corners
		Collection<Vector3f> allCorners = new ArrayList<>();

		for (BlockElement element : model.getElements()) {
			Collection<Vector3f> corners = getUniqueCorners(element.from, element.to);

			// rotate corners if on a rotated element
			if (element.rotation != null) {
				Collection<Vector3f> rotated = new HashSet<>();

				for (Vector3f corner : corners) {
					Vector3f origin = element.rotation.origin.copy();
					origin.mul(16);
					rotated.add(
							rotateCorner(
									corner,
									origin,
									element.rotation.axis,
									element.rotation.angle
							));
				}

				corners = rotated;
			}

			allCorners.addAll(corners);
		}

		// just in case
		if (allCorners.isEmpty()) {
			return AABB.ofSize(0, 0, 0);
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
