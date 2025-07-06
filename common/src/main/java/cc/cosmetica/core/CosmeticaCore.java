package cc.cosmetica.core;

import cc.cosmetica.core.impl.BlockModelManager;
import net.fabricmc.api.ClientModInitializer;

public class CosmeticaCore implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockModelManager.IMAGE_CACHE_MANAGER.runCacheGC();
    }
}
