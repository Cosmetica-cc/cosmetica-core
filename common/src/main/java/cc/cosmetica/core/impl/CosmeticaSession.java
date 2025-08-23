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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.LoginResult;
import cc.cosmetica.core.builtin.manager.SelfCosmeticManager;
import cc.cosmetica.core.util.Response;
import cc.cosmetica.core.util.Websocket;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import gg.cloaks.javaclient.ApiClient;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.Configuration;
import gg.cloaks.javaclient.api.DefaultApi;
import gg.cloaks.javaclient.model.AfricaSession;
import gg.cloaks.javaclient.model.CosmeticaUser;
import gg.cloaks.javaclient.model.PlayerResponse;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cc.cosmetica.core.api.LoginResult.Code.*;

/**
 * Handles authentication and websocket for Cosmetica.
 */
public final class CosmeticaSession {
	/**
	 * Create a new Cosmetica Session.
	 * @param client the API controller.
	 * @param token the user's token. Leave blank if not signed in.
	 * @param user the user's uuid. Set to null if not signed in.
	 */
	private CosmeticaSession(ApiClient client, String token, @Nullable UUID user) {
		this.api = new DefaultApi(client);
		this.sessionToken = token;
		this.user = user;
	}

	public final DefaultApi api;
	public final String sessionToken;
	private final @Nullable UUID user;
	private Websocket websocket;

	public boolean isAuthenticated() {
		return user != null;
	}

	private void logInToAfrica() {
		// Log in to africa websocket
		CosmeticaAPI.performAsync(DefaultApi::africaControllerRequestSession)
				.thenApply(africaSession -> {
					Logging.getInstance().info("Connecting to {}", africaSession.getName());
					Logging.getInstance().info(africaSession.getMessage());

					Websocket websocket1 = createWebsocket(africaSession);

					try {
						websocket1.connect();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					// connected. set websocket
					this.websocket = websocket1;

					JsonObject authData = new JsonObject();
					authData.add("uuid", new JsonPrimitive(this.user.toString()));
					authData.add("token", new JsonPrimitive(africaSession.getToken()));
					sendEvent(websocket1, "auth", authData);

					// Resubscribe to events
					synchronized (WEBSOCKET_SUBSCRIPTIONS) {
						// don't bother re-subscribing if nothing to resubscribe to
						if (!WEBSOCKET_SUBSCRIPTIONS.isEmpty()) {
							JsonArray eventIds = new JsonArray();

							WEBSOCKET_SUBSCRIPTIONS.forEach((eventId, listeners) -> eventIds.add(eventId));

							JsonObject data = new JsonObject();
							data.add("subscriptions", eventIds);

							sendEvent(websocket1, "subscribe", data);
						}
					}

					return websocket1;
				})
				.exceptionally(ex -> {
					if (ex instanceof ApiException) {
						if (((ApiException) ex).getCode() == 424) {
							Logging.getInstance().error("Africa servers are full or offline!");
						} else {
							Logging.getInstance().error("Error connecting to Africa", ex);
						}
					} else {
						Logging.getInstance().error("Could not connect to Africa", ex);
					}

					// try reconnect again if it fails and we are still current auth
					if (CosmeticaSession.this == getCurrentSession()) {
						reconnectSocket();
					}

					return null;
				});
	}

	/**
	 * Create a new websocket for an africa session. Does not start the connection.
	 * @param africaSession the session to create the websocket for.
	 * @return the new websocket.
	 */
	private @Nonnull Websocket createWebsocket(AfricaSession africaSession) {
		return new Websocket("Cosmetica Websocket", africaSession.getUrl()) {
			@Override
			protected void onConnected() {
				reconnectTimeout = 0;
			}

			@Override
			protected void connectionDropped() {
				// upon drop only reconnect if still authenticated the same.
				if (CosmeticaSession.this == getCurrentSession()) {
					reconnectSocket();
				}
			}

			@Override
			protected void receive(JsonElement data) {
				if (DEBUG_WEBSOCKET) {
					Logging.getInstance().debug("Websocket Received {}", data);
				}

				JsonObject obj = data.getAsJsonObject();

				if ("event".equals(obj.get("event").getAsString())) {
					String eventId = obj.get("data").getAsString();

					// run callbacks
					synchronized (WEBSOCKET_SUBSCRIPTIONS) {
						WEBSOCKET_SUBSCRIPTIONS.getOrDefault(eventId, ImmutableMap.of()).forEach((rl, run) -> {
							run.run();
						});
					}
				}
			}
		};
	}

	private synchronized void sendEvent(String event, JsonElement data) {
		if (websocket != null) {
			sendEvent(websocket, event, data);
		}
	}

	// synchronised, because it would be pretty bad if it became null before it closed the socket
	private synchronized void closeSocket() {
		if (websocket != null) {
			Logging.getInstance().debug("Closing africa websocket");
			websocket.closeFuture();
			websocket = null;
		}
	}

	/* Constants */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(t -> new Thread(t, "Cosmetica Reconnector"));
	private static final String BASE_PATH = System.getProperty("cosmetica.api", "https://api.cloaks.gg");

	/* Keep track of subscriptions so we can re-subscribe on reconnect / making a new session */
	private static final Map<String, Map<ResourceLocation, Runnable>> WEBSOCKET_SUBSCRIPTIONS = new HashMap<>();

	/* Singleton */
	private static CosmeticaSession authenticationInstance;
	private static int reconnectTimeout = 0;
	private static boolean isScheduled;

	static {
		Logging.getInstance().debug("Using API url: {}", BASE_PATH);

		ApiClient defaultClient = Configuration.getDefaultApiClient().setBasePath(BASE_PATH);
		authenticationInstance = new CosmeticaSession(defaultClient, "",null);

		// close socket before shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> getCurrentSession().closeSocket()));

		// start ping for websockets
		pingAfrica();
	}

	/* Called every 30 seconds to maintain connection */
	private static void pingAfrica() {
		// ping current socket
		Websocket socket = getCurrentSession().websocket;
		if (socket != null) socket.ping();

		// schedule new ping
		SCHEDULER.schedule(CosmeticaSession::pingAfrica, 30, TimeUnit.SECONDS);
	}

	/* Session */
	public static CosmeticaSession getCurrentSession() {
		return authenticationInstance;
	}

	/* Events */
	public static void subscribe(String eventId, ResourceLocation key, Runnable callback) {
		JsonArray eventIds = new JsonArray();
		eventIds.add(eventId);

		JsonObject data = new JsonObject();
		data.add("subscriptions", eventIds);

		synchronized (WEBSOCKET_SUBSCRIPTIONS) { // TODO synchronise on this on reconnect too.
			boolean hasExistingEvent = WEBSOCKET_SUBSCRIPTIONS.containsKey(eventId);
			WEBSOCKET_SUBSCRIPTIONS.computeIfAbsent(eventId, k -> new HashMap<>()).put(key, callback);

			if (!hasExistingEvent) {
				getCurrentSession().sendEvent("subscribe", data);
			}
		}
	}

	public static void unsubscribe(String eventId, ResourceLocation key) {
		JsonArray eventIds = new JsonArray();
		eventIds.add(eventId);

		JsonObject data = new JsonObject();
		data.add("subscriptions", eventIds);

		synchronized (WEBSOCKET_SUBSCRIPTIONS) {
			Map<ResourceLocation, Runnable> rr = WEBSOCKET_SUBSCRIPTIONS.get(eventId);

			// do we actually have subscriptions
			if (rr != null) {
				rr.remove(key);

				// clear memory and unsubscribe on the websocket if nothing more needed.
				if (rr.isEmpty()) {
					WEBSOCKET_SUBSCRIPTIONS.remove(eventId);
					getCurrentSession().sendEvent("unsubscribe", data);
				}
			}
		}
	}

	/* Authenticating and Deauthenticating */

	public static void deauthenticate() {
		authenticationInstance.closeSocket(); // close existing auth websocket
		/* Default client already has base path set */
		authenticationInstance = new CosmeticaSession(Configuration.getDefaultApiClient(), "", null);
	}

	public static void authenticate(String jwt) {
		ApiClient newClient = new ApiClient()
				.setBasePath(BASE_PATH)
				.addDefaultHeader("Authorization", "Bearer " + jwt);

		// ensure old websocket is disconnected
		authenticationInstance.closeSocket();

		// find uuid for jwt
		UUID uuid;

		try {
			byte[] info = Base64.getDecoder().decode(jwt.split("\\.")[1]);
			JsonObject object = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(info))).getAsJsonObject();
			uuid = UUID.fromString(object.get("sub").getAsString());
		} catch (JsonParseException | IndexOutOfBoundsException e) {
			throw new RuntimeException("Malformed JWT", e);
		}

		authenticationInstance = new CosmeticaSession(newClient, jwt, uuid);

		// Fetch own cosmetics
		Logging.getInstance().debug("Logged in to {}, fetching own cosmetics.", uuid);
		// TODO make texture packet request and submit that instead.
		CosmeticaAPI.performAsync(DefaultApi::usersControllerGetSelf)
				.thenAccept(user -> {
					Logging.getInstance().debug("Received Login Cosmetics");
					// set self cosmetics
					SelfCosmeticManager.update(new PlayerResponse().isUser(true).user(user));
					// Don't set ApiCosmeticsHolder cosmetics. That is only for other players.
				})
				.exceptionally(t -> {
					Logging.getInstance().error("Error loading own cosmetics", t);
					return null;
				});

		// log in to africa
		authenticationInstance.logInToAfrica();
	}

	private static void reconnectSocket() {
		synchronized (SCHEDULER) {
			if (isScheduled) {
				return;
			}

			isScheduled = true;
		}

		// Compute new timeout (get longer each attempt)
		int timeout = reconnectTimeout;
		reconnectTimeout = reconnectTimeout == 0 ? 2 : Math.min(reconnectTimeout * 2, 60);

		// Schedule reconnect
		Logging.getInstance().warn("Cosmetica Africa disconnected unexpectedly. Attempting reconnect in {} seconds.", timeout);

		SCHEDULER.schedule(() -> {
			CosmeticaSession session = getCurrentSession();

			// we are running the scheduled task.
			synchronized (SCHEDULER) {
				isScheduled = false;
			}

			if (!session.isAuthenticated()) {
				Logging.getInstance().info("Session changed. Aborting reconnect.");
			} else {
				try {
					Logging.getInstance().debug("Attempting to reconnect to Cosmetica Africa...");
					session.logInToAfrica();
				} catch (Exception e) {
					System.out.println("Reconnect attempt failed: " + e.getMessage());
				}
			}
		}, timeout, TimeUnit.SECONDS);
	}

	private static final boolean DEBUG_WEBSOCKET = Boolean.getBoolean("cosmetica.websocketdebug");

	private static void sendEvent(Websocket websocket, String event, JsonElement data) {
		JsonObject packet = new JsonObject();
		packet.add("event", new JsonPrimitive(event));
		packet.add("data", data);
		if (DEBUG_WEBSOCKET) {
			Logging.getInstance().debug("Websocket.Send {}", new Gson().toJson(packet));
		}
		websocket.send(packet);
	}

	// secret 'api'
	public static boolean silence400 = false;

	public static LoginResult login(UUID uuid, String username, String accessToken) throws IOException {
		// ensure we are deauthenticated.
		deauthenticate();

		// Get the authentication server to authenticate with
		String authURL = getCurrentSession().api.authControllerGetAuthServer().getUrl();

		// Initiate a session
		JsonObject keyRequest = new JsonObject();
		keyRequest.addProperty("username", username);

		String sessionId;
		String verifyToken;
		byte[] publicKey;

		try (Response response = Response.post(authURL + "/java/key", keyRequest)) {
			if (response.isSuccessful()) {
				JsonObject job = response.readEntityJson().getAsJsonObject();

				sessionId = job.get("sessionId").getAsString();
				verifyToken = job.get("verifyToken").getAsString();
				publicKey = Base64.getDecoder().decode(job.get("publicKey").getAsString());
			} else if (response.getStatusCode() == 400) {
				JsonObject job = response.readEntityJson().getAsJsonObject();

				String code = job.get("code").getAsString();
				String message = job.get("message").getAsString();

				if (!silence400) {
					logBadResponse("Request to key was not successful", response);
				}
				return new LoginResult(false, LoginResult.Code.forKeyApi(code), message);
			} else {
				logBadResponse("Request to key was not successful", response);
				return new LoginResult(false, GENERIC_KEY_ERROR, "Error fetching Key (error code " + response.getStatusCode() + ")");
			}
		}

		// Generate Shared Secret
		byte[] sharedSecret = new byte[16];
		SECURE_RANDOM.nextBytes(sharedSecret);

		// Join server via mojang endpoint
		String serverId = combinedHash("".getBytes(StandardCharsets.US_ASCII), sharedSecret, publicKey);

		JsonObject loginRequest = new JsonObject();
		loginRequest.addProperty("accessToken", accessToken);
		loginRequest.addProperty("selectedProfile", uuid.toString().replaceAll("-", ""));
		loginRequest.addProperty("serverId", serverId);

		try (Response response = Response.post("https://sessionserver.mojang.com/session/minecraft/join", loginRequest)) {
			// Ensure successful
			if (!response.isSuccessful()) {
				logBadResponse("Could not log in to Cosmetica", response);
				return new LoginResult(false, MOJANG_LOGIN_ERROR, "Failed to join session server (error code " + response.getStatusCode() + ")");
			}
		}

		// Encrypt verify token and shared secret
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
		String sharedSecretEncrypted;
		String verifyTokenEncrypted;

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKeyO = keyFactory.generatePublic(keySpec);

			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKeyO);

			sharedSecretEncrypted = Base64.getEncoder().encodeToString(cipher.doFinal(sharedSecret));
			verifyTokenEncrypted = Base64.getEncoder().encodeToString(
					cipher.doFinal(verifyToken.getBytes(StandardCharsets.US_ASCII))
			);
		} catch (GeneralSecurityException e) {
			Logging.getInstance().error("Error encrypting data", e);
			return new LoginResult(false, ENCRYPTION_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		// Verify with auth server
		JsonObject verifyRequest = new JsonObject();
		verifyRequest.addProperty("secret", sharedSecretEncrypted);
		verifyRequest.addProperty("verifyToken", verifyTokenEncrypted);
		verifyRequest.addProperty("sessionId", sessionId);

		try (Response response = Response.post(authURL + "/java/verify", verifyRequest)) {
			if (response.isSuccessful()) {
				JsonObject jo = response.readEntityJson().getAsJsonObject();
				authenticate(jo.get("jwt").getAsString());
				Logging.getInstance().debug("Cosmetica: Logged in as {}", username);

				// set user
				CosmeticaUser user = getCurrentSession().api.getApiClient().getObjectMapper().readValue(
						new Gson().toJson(jo.get("user")),
						CosmeticaUser.class
				);
				SelfCosmeticManager.update(
						new PlayerResponse().isUser(true).user(user)
				);
				return new LoginResult(true, SUCCESS, "");
			} else {
				logBadResponse("Cosmetica authentication failed", response);
				return new LoginResult(false, GENERIC_VERIFY_ERROR, "Failed to verify login (error code " + response.getStatusCode() + ")");
			}
		}
	}

	private static void logBadResponse(String message, Response response) throws IOException {
		Logging.getInstance().warn("{}: Error {}, {}", message, response.getStatusCode(), response.getEntity() == null ? "null" : response.readEntityString());
	}

	/**
	 * Combined hash for minecraft login.
	 * @param bytes the byte arrays to combine.
	 * @return the SHA-1 hash used by minecraft for login purposes.
	 */
	private static String combinedHash(byte[]... bytes) {
		// concatenate the arrays
		int space = 0;
		for (byte[] arr : bytes) space += arr.length;
		byte[] theBigMan = new byte[space];

		space = 0;

		for (byte[] arr : bytes) {
			System.arraycopy(arr, 0, theBigMan, space, arr.length);
			space += arr.length;
		}

		try {
			// Minecraft Hash
			// https://gist.github.com/unascribed/70e830d471d6a3272e3f
			return new BigInteger(MessageDigest.getInstance("SHA-1").digest(theBigMan)).toString(16);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Your computer's java install is missing an SHA-1 implementation.", e);
		}
	}
}
