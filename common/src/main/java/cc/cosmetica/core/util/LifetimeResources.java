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

package cc.cosmetica.core.util;

import cc.cosmetica.core.impl.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class LifetimeResources {
    private static final List<ExecutorService> EXECUTORS = new ArrayList<>();

    public static <T extends ExecutorService> T registerExecutor(T service) {
        EXECUTORS.add(service);
        return service;
    }

    public static ExecutorService newFixedThreadPool(int size, ThreadFactory threadFactory) {
        return registerExecutor(Executors.newFixedThreadPool(size, threadFactory));
    }

    public static ScheduledExecutorService newScheduler(String name) {
        return registerExecutor(Executors.newSingleThreadScheduledExecutor(t -> new Thread(t, name)));
    }

    public static void shutdown() {
        Logging.getInstance().info("Shutting down Cosmetica executors and schedulers");

        for (ExecutorService executor : EXECUTORS) {
            executor.shutdown();
        }
    }
}
