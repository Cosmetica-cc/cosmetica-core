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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.util.Response;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.api.DefaultApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
	 * Get whether cosmetica-core is currently authenticated.
	 *
	 * @return whether the cosmetica-core api instance is currently authenticated.
	 */
	public static boolean isAuthenticated() {
		return CosmeticaAuthenticator.isAuthenticated();
	}

	/**
	 * Perform a task async on the Cosmetica threadpool. Intended for API requests to cosmetica.
	 * If the request returns a 401, the API instance is deauthenticated.
	 *
	 * @param request the request to perform.
	 * @param <T>     the type of the promise.
	 * @return a {@link CompletableFuture} that promises the response of the request.
	 */
	public static <T> CompletableFuture<T> performAsync(Function<DefaultApi, T> request) {
		return CompletableFuture.supplyAsync(() -> request.apply(CosmeticaAuthenticator.getCurrentApi()), MasterCosmeticManager.HTTP_THREAD_POOL)
				.exceptionally(t -> {
					if (t instanceof ApiException && ((ApiException) t).getCode() == 401) {
						CosmeticaAuthenticator.deauthenticate();
					}

					throw (RuntimeException)t;
				});
	}

	/**
	 * Download data from a url asynchronously. The completable future will contain an exception if not a 2XX response.
	 * A successful response with no body will return empty string.
	 *
	 * @param url the url to download data.
	 * @return a completable future for the response.
	 */
	public static CompletableFuture<String> downloadAsync(String url) {
		Logging.getInstance().debug("Downloading " + url);

		return CompletableFuture.supplyAsync(() -> {
			try {
				try (Response response = Response.get(url)) {
					if (!response.isSuccessful()) {
						throw new ApiException(response.getStatusCode(),
								response.getEntity() == null ? "(no response body)" : response.readEntityString());
					}

					return response.getEntity() == null ? "" : response.readEntityString();
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Downloading from URL " + url, e);
			}
		}, MasterCosmeticManager.HTTP_THREAD_POOL);
	}

	/**
	 * Log in with the currently logged-in user. Does not spawn another thread.
	 * @return whether login was successful.
	 */
	public static boolean login() throws IOException {
		User user = Minecraft.getInstance().getUser();
		return CosmeticaAuthenticator.login(user.getGameProfile().getId(), user.getGameProfile().getName(), user.getAccessToken());
	}

	/**
	 * Log in with the given profile. The parameters must complement each other for this to work. Does not spawn another thread.
	 * @param uuid the uuid of the user to sign in as.
	 * @param username the username of the user to sign in as.
	 * @param accessToken the minecraft access token to use to sign in.
	 * @return whether login was successful.
	 */
	public static boolean authenticate(UUID uuid, String username, String accessToken) throws IOException {
		return CosmeticaAuthenticator.login(uuid, username, accessToken);
	}

	/**
	 * Immediately authenticate with the given JSON Web Token.
	 * @param jwt the json web token with which to authenticate.
	 */
	public static void authenticate(String jwt, UUID uuid) {
		CosmeticaAuthenticator.authenticate(jwt, uuid);
	}
}
