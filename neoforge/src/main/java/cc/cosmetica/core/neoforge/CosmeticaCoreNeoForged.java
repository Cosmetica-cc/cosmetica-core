package cc.cosmetica.core.neoforge;

import cc.cosmetica.core.CosmeticaCore;
import cc.cosmetica.core.builtin.BuiltinManagers;
import cc.cosmetica.core.impl.CosmeticaSession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("cosmetica_core")
public class CosmeticaCoreNeoForged {
    public CosmeticaCoreNeoForged(IEventBus bus) {
        bus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        new BuiltinManagers().init();

        // GC
        CosmeticaCore.onInitialiseClient();

        // Development Testing Auth
        String devAuth = System.getProperty("cosmetica.token");

        if (devAuth != null && !FMLEnvironment.isProduction()) {
            CosmeticaSession.authenticate(devAuth, "development", null);
        }
    }
}
