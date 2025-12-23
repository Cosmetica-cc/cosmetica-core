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

import com.mojang.authlib.GameProfile;
import gg.cloaks.javaclient.model.Accessory.AttachmentEnum;
import gg.cloaks.javaclient.model.OutfitAccessory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

/**
 * Represents an Accessory equipped on a user.
 */
public class Accessory implements Cosmetic {
	public Accessory(gg.cloaks.javaclient.model.Accessory accessory, @Nullable GameProfile creator,
					 boolean mirrored, CosmeticaModel model, Vec3 offset) {
		Objects.requireNonNull(accessory, "Accessory json object cannot be null.");
		this.jsonObject = accessory;
		this.creator = creator;
		this.mirrored = mirrored;
		this.model = model;
		this.offset = offset;
		this.flags = new HashSet<>();

		// initialise flags
		BigDecimal flags = accessory.getFlags();

		for (Flag flag : Flag.values()) {
			if (flag.isSet(flags.intValue())) {
				this.flags.add(flag);
			}
		}
	}

	private final gg.cloaks.javaclient.model.Accessory jsonObject;
	@Nullable
	private final GameProfile creator;
	private final CosmeticaModel model;
	protected Vec3 offset;
	protected boolean mirrored;
	private final Collection<Flag> flags;

	/**
	 * Get the underlying API Accessory object for this core accessory.
	 * @return the {@link gg.cloaks.javaclient.model.Accessory} for this accessory.
	 */
	public gg.cloaks.javaclient.model.Accessory getJsonObject() {
		return this.jsonObject;
	}

	@Override
	public String getName() {
		return this.jsonObject.getName();
	}

	@Override
	public String getId() {
		return this.jsonObject.getId();
	}

	@Override
	public Optional<GameProfile> getCreator() {
		return Optional.ofNullable(this.creator);
	}

	@Override
	public Optional<String> getThumbnail() {
		return Optional.ofNullable(this.jsonObject.getThumbnail());
	}

	public AttachmentEnum getAttachment() {
		return this.jsonObject.getAttachment();
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
		HIDE_WITH_HELMET(0x1),
		HIDE_WITH_CHESTPLATE(0x2),
		HIDE_WITH_LEGGINGS(0x4),
		HIDE_WITH_BOOTS(0x8),
		HIDE_WITH_CAPE(0x10),
		HIDE_WITH_ELYTRA(0x20),
		HIDE_WITH_PARROT(0x40);

		Flag(int mask) {
			this.mask = mask;
		}

		private final int mask;

		boolean isSet(int value) {
			return (value & mask) != 0;
		}
	}

	public static final class Adjustable extends Accessory {
		public Adjustable(gg.cloaks.javaclient.model.Accessory accessory, @Nullable GameProfile creator, CosmeticaModel model, AttachmentEnum attachmentEnum) {
			super(accessory, creator, false, model, attachmentTransform(attachmentEnum, 0, 0, 0));
			this.baseOffset = this.offset;
		}

		private final Vec3 baseOffset;

		public void setOffset(Vec3 offset) {
			this.offset = offset;
		}

		public Vec3 getBaseOffset() {
			return this.baseOffset;
		}

		public void setMirrored(boolean mirrored) {
			this.mirrored = mirrored;
		}
	}

	/**
	 * Create an {@link Accessory} from the given {@link OutfitAccessory}.
	 * @param accessory the OutfitAccessory received from the server.
	 * @return an Accessory from the given OutfitAccessory.
	 * @apiNote don't keep this longer than you need it so that the models and textures can be garbage collected.
	 */
	public static Accessory fromOutfitAccessory(OutfitAccessory accessory) {
		CosmeticaModel model = CosmeticaModel.getOrCreateCosmeticaModel(
				accessory.getAccessory().getId(),
				accessory.getAccessory().getModel(),
				accessory.getAccessory().getTexture(),
				accessory.getAccessory().getTicksPerFrame().intValue(),
				accessory.getAccessory().getFrames().intValue()
		);

		List<BigDecimal> offset = accessory.getOffset();

		return new Accessory(
				accessory.getAccessory(),
				Cosmetic.gameProfileOf(accessory.getAccessory().getCreator()),
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
	 * Create an {@link Adjustable} accessory from the given {@link gg.cloaks.javaclient.model.Accessory}.
	 * @param accessory the api Accessory received from the server.
	 * @return an adjustable accessory from the given api Accessory.
	 * @apiNote don't keep this longer than you need it so that the models and textures can be garbage collected.
	 */
	public static Adjustable fromAccessory(gg.cloaks.javaclient.model.Accessory accessory) {
		CosmeticaModel model = CosmeticaModel.getOrCreateCosmeticaModel(
				accessory.getId(),
				accessory.getModel(),
				accessory.getTexture(),
				accessory.getTicksPerFrame().intValue(),
				accessory.getFrames().intValue()
		);

		return new Adjustable(
				accessory,
				Cosmetic.gameProfileOf(accessory.getCreator()),
				model,
				accessory.getAttachment()
		);
	}

	/**
	 * Transform x, y, and z offsets from the server renderer space to world space.
	 * @return a Vec3 with the render offset to use in-game.
	 */
	public static Vec3 attachmentTransform(AttachmentEnum attachment, double x, double y, double z) {
		double dy;
		double dx;

		switch (attachment) {
		case HEAD:
			dy = 8.0 - 4;
			dx = 8.0 - 8;
			break;
		case RIGHT_ARM:
			dy = 0.0 - 6;
			dx = 8.0 - 8;
			break;
		case LEFT_ARM:
			dy = 0.0 - 6;
			dx = 7.0 - 8;
			break;
		default:
			dy = -2.0 - 6;
			dx = 8.0 - 8;
			break;
		}

		return new Vec3(
				(x + dx) / 16.0,
				(y + dy) / 16.0,
				(z) / 16.0
		);
	}
}
