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

package cc.cosmetica.core.fabric;

import cc.cosmetica.core.CosmeticaCore;
import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.builtin.BuiltinManagers;
import net.fabricmc.api.ClientModInitializer;

public class CosmeticaCoreFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new BuiltinManagers().init();

        // Cache
        CosmeticaCore.onInitialiseClient();

        // Development Testing Auth
        String devAuth = System.getProperty("cosmetica.token");

        if (devAuth != null) {
            CosmeticaAPI.authenticate(devAuth);
//            new Thread(() -> {try {
//                CosmeticaAPI.login();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }});
        }
    }
}
