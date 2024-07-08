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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.PlayerCosmetics;
import cc.cosmetica.core.util.Response;
import cc.cosmetica.core.util.Websocket;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import gg.cloaks.javaclient.ApiClient;
import gg.cloaks.javaclient.ApiException;
import gg.cloaks.javaclient.Configuration;
import gg.cloaks.javaclient.api.DefaultApi;
import gg.cloaks.javaclient.model.CosmeticaUser;

import javax.crypto.Cipher;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles authentication for Cosmetica.
 */
public final class CosmeticaAuthenticator {
	private CosmeticaAuthenticator() {
		// NO-OP
	}

	/* Constants */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(t -> new Thread(t, "Cosmetica Reconnector"));
	private static final String BASE_PATH = System.getProperty("cosmetica.api", "https://api.cloaks.gg");

	/* Singleton */
	private static DefaultApi apiInstance;
	private static boolean authenticated;

	private static Websocket websocket;
	private static int reconnectTimeout = 0;

	static {
		Logging.getInstance().debug("Using API url: {}", BASE_PATH);

		ApiClient defaultClient = Configuration.getDefaultApiClient().setBasePath(BASE_PATH);
		apiInstance = new DefaultApi(defaultClient);
	}

	public static DefaultApi getCurrentApi() {
		return apiInstance;
	}

	public static boolean isAuthenticated() {
		return authenticated;
	}

	// synchronised, because it would be pretty bad if it became null before it closed the socket
	private static synchronized void resetWebsocket() {
		if (websocket != null) {
			websocket.closeFuture();
			websocket = null;
		}
	}

	public static void deauthenticate() {
		authenticated = false;
		apiInstance = new DefaultApi(Configuration.getDefaultApiClient());
		resetWebsocket();
	}

	public static void authenticate(String jwt, UUID uuid) {
		ApiClient newClient = new ApiClient()
				.setBasePath(BASE_PATH)
				.addDefaultHeader("Authorization", "Bearer " + jwt);

		apiInstance = new DefaultApi(newClient);
		authenticated = true;
		// ensure websocket is disconnected
		resetWebsocket();

		// log in to africa
		logInToAfrica(uuid);
	}

	private static void reconnect(DefaultApi session, UUID user) {
		// Compute new timeout (get longer each attempt)
		int timeout = reconnectTimeout;
		reconnectTimeout = reconnectTimeout == 0 ? 2 : Math.min(reconnectTimeout * 2, 60);

		// Schedule reconnect
		Logging.getInstance().warn("Cosmetica Africa disconnected unexpectedly. Attempting reconnect in {} seconds.", timeout);

		SCHEDULER.schedule(() -> {
			if (apiInstance != session) {
				Logging.getInstance().info("Session changed. Aborting reconnect.");
			} else {
				try {
					Logging.getInstance().debug("Attempting to reconnect to Cosmetica Africa...");
					logInToAfrica(user);
				} catch (Exception e) {
					System.out.println("Reconnect attempt failed: " + e.getMessage());
				}
			}
		}, timeout, TimeUnit.SECONDS);
	}

	private static void logInToAfrica(UUID userUUID) {
		// a reference to the api instance being used for this session.
		final DefaultApi api = apiInstance;

		// Log in to africa websocket
		CosmeticaAPI.performAsync(DefaultApi::africaControllerRequestSession)
				.thenApply(africaSession -> {
					Logging.getInstance().info("Connecting to {}", africaSession.getName());
					Logging.getInstance().info(africaSession.getMessage());

					Websocket websocket1 = new Websocket("Cosmetica Websocket", africaSession.getUrl()) {
						@Override
						protected void onConnected() {
							reconnectTimeout = 0;
						}

						@Override
						protected void connectionDropped() {
							// upon drop only reconnect if still authenticated the same.
							if (api == apiInstance) {
								reconnect(api, userUUID);
							}
						}

						@Override
						protected void receive(JsonElement data) {
							Logging.getInstance().info("{}", data);
						}
					};

					try {
						websocket1.connect();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					JsonObject authData = new JsonObject();
					authData.add("uuid", new JsonPrimitive(userUUID.toString()));
					authData.add("token", new JsonPrimitive(africaSession.getToken()));
					sendEvent(websocket1, "auth", authData);

					return websocket1;
				})
				.exceptionally(ex -> {
					if (ex instanceof ApiException) {
						if (((ApiException) ex).getCode() == 412) {
							Logging.getInstance().error("Africa servers are full or offline!");
						} else {
							Logging.getInstance().error("Error connecting to Africa", ex);
						}
					} else {
						Logging.getInstance().error("Could not connect to Africa", ex);
					}

					// try reconnect again if it fails
					reconnect(api, userUUID);

					return null;
				});
	}

	private static void sendEvent(Websocket websocket, String event, JsonElement data) {
		JsonObject packet = new JsonObject();
		packet.add("event", new JsonPrimitive(event));
		packet.add("data", data);
		System.out.println(new Gson().toJson(packet));
		websocket.send(packet);
	}

	public static boolean login(UUID uuid, String username, String accessToken) throws IOException {
		// ensure we are deauthenticated.
		deauthenticate();

		// Get the authentication server to authenticate with
		String authURL = apiInstance.authControllerGetAuthServer().getUrl();

		// Initiate a session
		JsonObject keyRequest = new JsonObject();
		keyRequest.addProperty("username", username);

		String sessionId;
		String verifyToken;
		byte[] publicKey;

		try (Response response = Response.post(authURL + "/java/key", keyRequest)) {
			if (response.isSuccessful()) {
				JsonObject jo = response.readEntityJson().getAsJsonObject();

				sessionId = jo.get("sessionId").getAsString();
				verifyToken = jo.get("verifyToken").getAsString();
				publicKey = Base64.getDecoder().decode(jo.get("publicKey").getAsString());
			} else {
				logBadResponse("Request to key was not successful", response);
				return false;
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
				return false;
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
			return false;
		}

		// Verify with auth server
		JsonObject verifyRequest = new JsonObject();
		verifyRequest.addProperty("secret", sharedSecretEncrypted);
		verifyRequest.addProperty("verifyToken", verifyTokenEncrypted);
		verifyRequest.addProperty("sessionId", sessionId);

		try (Response response = Response.post(authURL + "/java/verify", verifyRequest)) {
			if (response.isSuccessful()) {
				JsonObject jo = response.readEntityJson().getAsJsonObject();
				authenticate(jo.get("jwt").getAsString(), uuid);
				Logging.getInstance().debug("Cosmetica: Logged in as {}", username);

				// set user
				CosmeticaUser user = apiInstance.getApiClient().getObjectMapper().readValue(
						new Gson().toJson(jo.get("user")),
						CosmeticaUser.class
				);
				PlayerCosmetics.setOwnCosmetics(PlayerCosmetics.fromUser(user));
				return true;
			} else {
				logBadResponse("Cosmetica authentication failed", response);
				return false;
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
			throw new RuntimeException("For some reason your computer's java install thinks SHA-1 is not a hashing algorithm.", e);
		}
	}
}
