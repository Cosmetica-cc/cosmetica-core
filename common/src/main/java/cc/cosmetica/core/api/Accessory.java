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

package cc.cosmetica.core.api;

import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import gg.cloaks.javaclient.model.OutfitAccessory;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Represents an Accessory equipped on a user.
 */
public final class Accessory {
	public Accessory(String name, AttachmentEnum attachment, boolean mirrored, CosmeticaModel model, Vec3 offset) {
		this.name = name;
		this.attachment = attachment;
		this.mirrored = mirrored;
		this.model = model;
		this.offset = offset;
		this.flags = new HashSet<>();
	}

	private final String name;
	private final AttachmentEnum attachment;
	private final CosmeticaModel model;
	private final Vec3 offset;
	private final boolean mirrored;
	private final Collection<Flag> flags;

	public String getName() {
		return this.name;
	}

	public AttachmentEnum getAttachment() {
		return this.attachment;
	}

	public CosmeticaModel getModel() {
		return this.model;
	}

	public boolean isMirrored() {
		return this.mirrored;
	}

	public Vec3 getOffset() {
		return this.offset;
	}

	public Collection<Flag> getFlags() {
		return this.flags;
	}

	public enum Flag {
		HIDE_WITH_HELMET
	}

	/**
	 * Create an {@link Accessory} from the given {@link OutfitAccessory}.
	 * @param accessory the OutfitAccessory received from the server.
	 * @return an Accessory from the given OutfitAccessory.
	 * @apiNote don't keep this longer than you need it so that the models and textures can be garbage collected.
	 */
	public static Accessory fromOutfitAccessory(OutfitAccessory accessory) {
		CosmeticaModel model = CosmeticaModel.getOrCreateModel(
				"accessory",
				accessory.getAccessory().getId(),
				accessory.getAccessory().getModel(),
				accessory.getAccessory().getTexture(),
				accessory.getAccessory().getTicksPerFrame().intValue(),
				accessory.getAccessory().getFrames().intValue()
		);

		List<BigDecimal> offset = accessory.getOffset();

		return new Accessory(
				accessory.getAccessory().getName(),
				accessory.getAccessory().getAttachment(),
				accessory.isMirrored(),
				model,
				attachmentTransform(
						accessory.getAccessory().getAttachment(),
						offset.get(0).doubleValue(),
						offset.get(1).doubleValue(),
						offset.get(2).doubleValue()
				)
		);
	}

	/**
	 * Transform x, y, and z offsets from the server renderer space to world space.
	 * @return a Vec3 with the render offset.
	 */
	private static Vec3 attachmentTransform(AttachmentEnum attachment, double x, double y, double z) {
		double dy;
		double dx;

		switch (attachment) {
		case HEAD:
			dy = 8.0;
			dx = 8.0;
			break;
		case RIGHT_ARM:
			dy = 0.0;
			dx = 8.0;
			break;
		case LEFT_ARM:
			dy = 0.0;
			dx = 7.0;
			break;
		default:
			dy = -2.0;
			dx = 8.0;
			break;
		}

		return new Vec3(
				(x + dx) / 16.0,
				(y + dy) / 16.0,
				(z + 8.0) / 16.0
		);
	}
}
