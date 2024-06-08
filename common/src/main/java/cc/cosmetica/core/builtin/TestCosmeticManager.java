package cc.cosmetica.core.builtin;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cosmetic manager for test cosmetics.
 */
public class TestCosmeticManager implements CosmeticManager {
	@Override
	public boolean canManage(LivingEntity entity) {
		return false;
	}

	@Override
	public Cosmetics getCosmetics(LivingEntity entity) {
		return null;
	}
}
