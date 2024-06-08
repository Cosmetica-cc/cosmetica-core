package cc.cosmetica.core.builtin;

import cc.cosmetica.core.api.CosmeticManagers;

/**
 * Registers the built-in cosmetic managers in the Cosmetica.
 */
public class BuiltinManagers {
	public void init() {
		CosmeticManagers.registerCosmeticManager(-100, new TestCosmeticManager());
		CosmeticManagers.registerCosmeticManager(0, new ApiCosmeticManager());
		CosmeticManagers.registerCosmeticManager(50, new CachedCosmeticManager());
	}
}
