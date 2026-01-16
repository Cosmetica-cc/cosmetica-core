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
import cc.cosmetica.core.impl.LoggingCategory;
import cc.cosmetica.core.impl.MasterCosmeticManager;
import cc.cosmetica.core.util.Response;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides access to the authenticated instance of the Cosmetica web API.
 */
public final class CosmeticaAPI {
	private CosmeticaAPI() {
		// NO-OP
	}

//	/**
//	 * Get asynchronous access to an API controller.
//	 * @param cstr a constructor for an API found in {@link gg.cloaks.javaclient.api}
//	 */
//	public static <T> AsyncApi<T> asyncController(Function<ApiClient, T> cstr) {
//		return new AsyncApi<>(cstr.apply(CosmeticaSession.getCurrentSession().client));
//	}

	/**
	 * Get the accessories controller for the API.
	 * @return an asynchronous accessor to the accessories api.
	 */
	public static AsyncApi<AccessoriesApi> accessories() {
		return CosmeticaSession.getCurrentSession().accessoriesApi;
	}

	/**
	 * Get the controller for the africa API.
	 * @return an asynchronous accessor to the africa api.
	 */
	public static AsyncApi<AfricaApi> africa() {
		return CosmeticaSession.getCurrentSession().africaApi;
	}

	/**
	 * Get the auth controller for the API.
	 * @return an asynchronous accessor to the auth api.
	 */
	public static AsyncApi<AuthApi> auth() {
		return CosmeticaSession.getCurrentSession().authApi;
	}

	/**
	 * Get the capes controller for the API.
	 * @return an asynchronous accessor to the capes api.
	 */
	public static AsyncApi<CapesApi> capes() {
		return CosmeticaSession.getCurrentSession().capesApi;
	}

	/**
	 * Get the downloads controller for the API.
	 * @return an asynchronous accessor to the downloads api.
	 */
	public static AsyncApi<DownloadsApi> downloads() {
		return CosmeticaSession.getCurrentSession().downloadsApi;
	}

	/**
	 * Get the controller for the external capes API.
	 * @return an asynchronous accessor to the external capes api.
	 */
	public static AsyncApi<ExternalCapesApi> externalCapes() {
		return CosmeticaSession.getCurrentSession().externalCapesApi;
	}

	/**
	 * Get the controller for the icons API.
	 * @return an asynchronous accessor to the icons api.
	 */
	public static AsyncApi<IconsApi> icons() {
		return CosmeticaSession.getCurrentSession().iconsApi;
	}

	/**
	 * Get the controller for the leaderboard API.
	 * @return an asynchronous accessor to the leaderboard api.
	 */
	public static AsyncApi<LeaderboardApi> leaderboard() {
		return CosmeticaSession.getCurrentSession().leaderboardApi;
	}

	/**
	 * Get the controller for the lore API.
	 * @return an asynchronous accessor to the lore api.
	 */
	public static AsyncApi<LoreApi> lore() {
		return CosmeticaSession.getCurrentSession().loreApi;
	}

	/**
	 * Get the controller for the outfits API.
	 * @return an asynchronous accessor to the outfits api.
	 */
	public static AsyncApi<OutfitsApi> outfits() {
		return CosmeticaSession.getCurrentSession().outfitsApi;
	}

	/**
	 * Get the controller for the player API.
	 * @return an asynchronous accessor to the player api.
	 */
	public static AsyncApi<PlayersApi> players() {
		return CosmeticaSession.getCurrentSession().playerApi;
	}

	/**
	 * Get the controller for the premium API.
	 * @return an asynchronous accessor to the premium api.
	 */
	public static AsyncApi<PremiumApi> premiumApi() {
		return CosmeticaSession.getCurrentSession().premiumApi;
	}

	/**
	 * Get the controller for the renderer API.
	 * @return an asynchronous accessor to the renderer api.
	 */
	public static AsyncApi<RendererApi> renderer() {
		return CosmeticaSession.getCurrentSession().rendererApi;
	}

	/**
	 * Get the controller for the roles API.
	 * @return an asynchronous accessor to the roles api.
	 */
	public static AsyncApi<RolesApi> roles() {
		return CosmeticaSession.getCurrentSession().rolesApi;
	}

	/**
	 * Get the controller for the search API.
	 * @return an asynchronous accessor to the search api.
	 */
	public static AsyncApi<SearchApi> search() {
		return CosmeticaSession.getCurrentSession().searchApi;
	}

	/**
	 * Get the controller for the settings API.
	 * @return an asynchronous accessor to the settings api.
	 */
	public static AsyncApi<SettingsApi> settings() {
		return CosmeticaSession.getCurrentSession().settingsApi;
	}

	/**
	 * Get the controller for the statistics API.
	 * @return an asynchronous accessor to the statistics api.
	 */
	public static AsyncApi<StatsApi> stats() {
		return CosmeticaSession.getCurrentSession().statsApi;
	}

	/**
	 * Get the controller for the user API.
	 * @return an asynchronous accessor to the user api.
	 */
	public static AsyncApi<UsersApi> users() {
		return CosmeticaSession.getCurrentSession().userApi;
	}
	/**
	 * Get the controller for the verify API.
	 * @return an asynchronous accessor to the verify api.
	 */
	public static AsyncApi<VerifyApi> verify() {
		return CosmeticaSession.getCurrentSession().verifyApi;
	}

	/**
	 * Get whether cosmetica-core is currently authenticated.
	 * @return whether the cosmetica-core api instance is currently authenticated.
	 */
	public static boolean isAuthenticated() {
		return CosmeticaSession.getCurrentSession().isAuthenticated();
	}

	/**
	 * Get whether cosmetica-core is currently connected to the websocket (africa).
	 * @return whether the cosmetica-core is currently connected to the websocket.
	 */
	public static boolean isWebsocketConnected() {
		return CosmeticaSession.getCurrentSession().isWebsocketConnected();
	}

	/**
	 * Add the given authentication change callback.
	 * @param callback the callback to add.
	 * @apiNote be careful when using this to retry logins, as {@link CosmeticaAPI#login(String, boolean, String)} and similar deauthenticate internally.
	 */
	public static void addAuthenticationChangeCallback(Consumer<AuthChangeReason> callback) {
		CosmeticaSession.addAuthChangeCallback(callback);
	}

	/**
	 * Remove an authentication change callback.
	 * @param callback the callback object to remove.
	 * @return whether the callback was present.
	 */
	public static boolean removeAuthenticationChangeaCallback(Consumer<AuthChangeReason> callback) {
		return CosmeticaSession.removeAuthChangeCallback(callback);
	}

	/**
	 * Download data from a url asynchronously. The completable future will contain an exception if not a 2XX response.
	 * A successful response with no body will return empty string.
	 *
	 * @param url the url to download data.
	 * @return a completable future for the response.
	 */
	public static CompletableFuture<String> downloadAsync(String url) {
		Logging.getInstance().debug(LoggingCategory.ASSETS, "Downloading " + url);

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
	 * @param client the client name string (identifies your client).
	 * @param useCloudSettings whether to switch to cloud settings on startup.
	 * @param modpackId pack id, if one should be used. Especially when associated with a custom icon brand.
	 * @return whether login was successful.
	 */
	public static LoginResult login(String client, boolean useCloudSettings, @Nullable String modpackId) throws IOException {
		User user = Minecraft.getInstance().getUser();
		return CosmeticaSession.login(
				user.getProfileId(),
				user.getName(),
				user.getAccessToken(),
				client, useCloudSettings, modpackId);
	}

	/**
	 * Log in with the given profile. The parameters must complement each other for this to work. Does not spawn another thread.
	 * @param uuid the uuid of the user to sign in as.
	 * @param username the username of the user to sign in as.
	 * @param accessToken the minecraft access token to use to sign in.
	 * @param client the client name string (identifies your client).
	 * @param useCloudSettings whether to switch to cloud settings on startup.
	 * @param modpackId pack id, if one should be used. Especially when associated with a custom icon brand.
	 * @return whether login was successful.
	 */
	public static LoginResult login(UUID uuid, String username, String accessToken, String client,
									boolean useCloudSettings, @Nullable String modpackId) throws IOException {
		return CosmeticaSession.login(uuid, username, accessToken, client, useCloudSettings, modpackId);
	}

	/**
	 * Immediately authenticate with the given JSON Web Token.
	 * @param jwt the json web token with which to authenticate.
	 * @param client the client name string (identifies your client).
	 * @param useCloudSettings whether to switch to cloud settings on startup.
	 * @param modpackId pack id, if one should be used. Especially when associated with a custom icon brand.
	 */
	public static void authenticate(String jwt, String client, boolean useCloudSettings, @Nullable String modpackId) {
		CosmeticaSession.authenticate(jwt, client, new CosmeticaSession.AuthenticationData(useCloudSettings, modpackId));
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
		CosmeticaSession.deauthenticate(AuthChangeReason.MANUAL_DEAUTHENTICATE);
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

	/**
	 * Enum for authentication change reasons.
	 */
	public enum AuthChangeReason {
		/**
		 * The user has authenticated.
		 */
		AUTHENTICATED,
		/**
		 * An API user called {@link CosmeticaAPI#deauthenticate()}.
		 */
		MANUAL_DEAUTHENTICATE,
		/**
		 * A 401 was received from a Cosmetica API request, indicating an invalid login.
		 */
		ERROR_401,
		/**
		 * As the implementation may change, it is not guaranteed by the api
		 * this will always be called at the same places by the implementation.
		 */
		REFRESH_LOGIN
	}
}
