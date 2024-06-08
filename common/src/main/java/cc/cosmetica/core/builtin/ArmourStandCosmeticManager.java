package cc.cosmetica.core.builtin;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Manages cosmetics for armour stands that represent outfits.
 */
public class ArmourStandCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(LivingEntity entity) {
		return entity instanceof ArmorStand; // && has outfit
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return null;
	}
}
