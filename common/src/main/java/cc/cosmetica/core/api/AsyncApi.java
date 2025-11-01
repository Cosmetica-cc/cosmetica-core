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

import cc.cosmetica.core.impl.CosmeticaSession;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import gg.cloaks.javaclient.ApiException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Interface for Asynchronous interaction with an api.
 */
public final class AsyncApi<API> {
    public AsyncApi(API api) {
        this.api = api;
    }

    private API api;

    /**
     * Perform a task async on the Cosmetica threadpool. Intended for API requests to cosmetica.
     * If the request returns a 401, the API instance will be deauthenticated.
     *
     * @param request the request to perform.
     * @param <T>     the type of the promise.
     * @return a {@link CompletableFuture} that promises the response of the request.
     */
    public <T> CompletableFuture<T> requestAsync(Function<API, T> request) {
        return CompletableFuture.supplyAsync(() -> request.apply(this.api), MasterCosmeticManager.HTTP_THREAD_POOL)
                .exceptionally(t -> {
                    if (t instanceof ApiException && ((ApiException) t).getCode() == 401) {
                        CosmeticaSession.deauthenticate();
                    }

                    throw (RuntimeException)t;
                });
    }

    /**
     * Get the underlying API object.
     * @return the underlying API object.
     */
    public API get() {
        return this.api;
    }
}
