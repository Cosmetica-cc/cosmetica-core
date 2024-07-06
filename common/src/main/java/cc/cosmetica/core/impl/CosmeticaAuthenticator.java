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

import cc.cosmetica.core.api.PlayerCosmetics;
import cc.cosmetica.core.util.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.cloaks.javaclient.ApiClient;
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

/**
 * Handles authentication for Cosmetica.
 */
public final class CosmeticaAuthenticator {
	private CosmeticaAuthenticator() {
		// NO-OP
	}

	/* Constants */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final String apiUrl = System.getProperty("cosmetica.api", "https://api.cloaks.gg");

	/* Singleton */
	private static DefaultApi apiInstance;
	private static boolean authenticated;

	static {
		Logging.getInstance().debug("Using API url: {}", apiUrl);

		ApiClient defaultClient = Configuration.getDefaultApiClient().setBasePath(apiUrl);
		apiInstance = new DefaultApi(defaultClient);
	}

	public static DefaultApi getCurrentApi() {
		return apiInstance;
	}

	public static boolean isAuthenticated() {
		return authenticated;
	}

	public static void deauthenticate() {
		authenticated = false;
		apiInstance = new DefaultApi(Configuration.getDefaultApiClient());
	}

	public static void authenticate(String jwt) {
		ApiClient newClient = new ApiClient()
				.setBasePath(apiUrl)
				.addDefaultHeader("Authorization", "Bearer " + jwt);

		apiInstance = new DefaultApi(newClient);
		authenticated = true;
	}

	public static boolean login(UUID uuid, String username, String accessToken) throws IOException {
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
				authenticate(jo.get("jwt").getAsString());
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
