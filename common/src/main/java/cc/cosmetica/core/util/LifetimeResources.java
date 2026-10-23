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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LifetimeResources {
    private static final List<ExecutorService> EXECUTORS = new ArrayList<>();
    private static final List<Runnable> SHUTDOWN_HOOKS = new ArrayList<>();

    public static <T extends ExecutorService> T registerExecutor(T service) {
        EXECUTORS.add(service);
        return service;
    }

    public static ExecutorService newSingleThreadExecutor(String threadName) {
        return registerExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName(threadName);
            return t;
        }));
    }

    public static ExecutorService newFixedThreadPool(int size, String commonName) {
        final AtomicInteger number = new AtomicInteger(1);
        return registerExecutor(Executors.newFixedThreadPool(size, r -> {
            Thread t = new Thread(r);
            t.setName(commonName + number.getAndIncrement());
            return t;
        }));
    }

    public static ScheduledExecutorService newScheduler(String name) {
        ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1, t -> new Thread(t, name));
        service.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        service.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return registerExecutor(service);
    }

    public static ScheduledExecutorService newScheduler(int threads, ThreadFactory threadFactory) {
        ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(threads, threadFactory);
        service.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        service.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return registerExecutor(service);
    }

    public static void addShutdownHook(@NotNull Runnable runnable) {
        Objects.requireNonNull(runnable);
        SHUTDOWN_HOOKS.add(runnable);
    }

    public static void shutdown() {
        Logging.getInstance().info("Shutting down Cosmetica executors and schedulers");

        for (Runnable runnable : SHUTDOWN_HOOKS) {
            try {
                runnable.run();
            } catch (Exception e) {
                Logging.getInstance().error("A shutdown hook failed", e);
            }
        }

        for (ExecutorService executor : EXECUTORS) {
            executor.shutdown();
        }
    }
}
