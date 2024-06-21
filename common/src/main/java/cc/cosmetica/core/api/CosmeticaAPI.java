/*
 * Copyright 2024 Cosmetica
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

import cc.cosmetica.core.impl.CosmeticaAuthenticator;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import gg.cloaks.javaclient.ApiClient;
import gg.cloaks.javaclient.api.DefaultApi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides access to the authenticated instance of the Cosmetica API.
 */
public final class CosmeticaAPI {
	private CosmeticaAPI() {
		// NO-OP
	}

	/**
	 * Get the current instance of {@link DefaultApi}.
	 */
	public static DefaultApi getInstance() {
		return CosmeticaAuthenticator.getCurrentApi();
	}

	/**
	 * Perform a task async on the Cosmetica threadpool. Intended for API requests to cosmetica.
	 * @param request the request to perform.
	 * @return a {@link CompletableFuture} that promises the response of the request.
	 * @param <T> the type of the promise.
	 */
	public static <T> CompletableFuture<T> performAsync(Function<DefaultApi, T> request) {
		return CompletableFuture.supplyAsync(() -> request.apply(CosmeticaAuthenticator.getCurrentApi()), MasterCosmeticManager.HTTP_THREAD_POOL);
	}

	public static CompletableFuture<String> downloadAsync(String url) {

	}
}
