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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.util.Response;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.api.DefaultApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Provides access to the authenticated instance of the Cosmetica web API.
 */
public final class CosmeticaAPI {
	private CosmeticaAPI() {
		// NO-OP
	}

	/**
	 * Get the current instance of {@link DefaultApi}.
	 */
	public static DefaultApi getInstance() {
		return CosmeticaSession.getCurrentSession().api;
	}

	/**
	 * Get whether cosmetica-core is currently authenticated.
	 *
	 * @return whether the cosmetica-core api instance is currently authenticated.
	 */
	public static boolean isAuthenticated() {
		return CosmeticaSession.getCurrentSession().isAuthenticated();
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
		return CompletableFuture.supplyAsync(() -> request.apply(CosmeticaSession.getCurrentSession().api), MasterCosmeticManager.HTTP_THREAD_POOL)
				.exceptionally(t -> {
					if (t instanceof ApiException && ((ApiException) t).getCode() == 401) {
						CosmeticaSession.deauthenticate();
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
		return CosmeticaSession.login(user.getGameProfile().getId(), user.getGameProfile().getName(), user.getAccessToken());
	}

	/**
	 * Log in with the given profile. The parameters must complement each other for this to work. Does not spawn another thread.
	 * @param uuid the uuid of the user to sign in as.
	 * @param username the username of the user to sign in as.
	 * @param accessToken the minecraft access token to use to sign in.
	 * @return whether login was successful.
	 */
	public static boolean login(UUID uuid, String username, String accessToken) throws IOException {
		return CosmeticaSession.login(uuid, username, accessToken);
	}

	/**
	 * Immediately authenticate with the given JSON Web Token.
	 * @param jwt the json web token with which to authenticate.
	 */
	public static void authenticate(String jwt) {
		CosmeticaSession.authenticate(jwt);
	}

	/**
	 * Get the current session token as a string. This can be used for caching.
	 * @return the session token. If not authenticated, returns an empty string.
	 */
	public static String getSessionToken() {
		return CosmeticaSession.getCurrentSession().sessionToken;
	}

	/**
	 * Immediately deauthenticate the API.
	 */
	public static void deauthenticate() {
		CosmeticaSession.deauthenticate();
	}

	/**
	 * Subscribe to receive updates for this event when connected to the websocket. This is a synchronous call.
	 * Each owner can only have one callback for a specific event active at a time. Therefore, design your owner keys
	 * carefully based on requirements for your use.
	 * @param eventType the event type.
	 * @param event the specific event to subscribe to.
	 * @param owner identifier for the owner of the subscription.
	 * @param callback the callback to run when the subscription is received.
	 */
	public static <T> void subscribe(SubscriptionEvent<T> eventType, T event, ResourceLocation owner, Runnable callback) {
		CosmeticaSession.subscribe(eventType.name + " " + event.toString(), owner, callback);
	}

	/**
	 * Unsubscribe all event callbacks for this event belonging to the owner. This is a synchronous call.
	 * @param eventType the event type.
	 * @param event the specific event to subscribe to.
	 * @param owner the identifier for the owner of the subscription.
	 */
	public static <T> void unsubscribe(SubscriptionEvent<T> eventType, T event, ResourceLocation owner) {
		CosmeticaSession.unsubscribe(eventType.name + " " + event.toString(), owner);
	}

	/**
	 * Event types for subscriptions.
	 */
	public static class SubscriptionEvent<T> {
		private SubscriptionEvent(String name) {
			this.name = name;
		}

		public final String name;

		/**
		 * Subscribe to players updates by their uuid.
		 */
		public static final SubscriptionEvent<UUID> PLAYER = new SubscriptionEvent<>("player");

		/**
		 * Subscribe to player updates by their usernames. Intended for when the player is craked.
		 */
		public static final SubscriptionEvent<String> PLAYER_CREAKED = new SubscriptionEvent<>("player");

		/**
		 * Subscribe to outfit updates.
		 */
		public static final SubscriptionEvent<UUID> OUTFIT = new SubscriptionEvent<>("outfit");
	}
}
