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
import cc.cosmetica.core.impl.LoggingCategory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Websocket {
	public Websocket(String name, String uri) {
		this.name = name;

		try {
			this.uri = new URI(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Not a valid uri: " + uri);
		}
	}

	private final String name;
	private final URI uri;
	private WebSocket socket = null;
	private WebSocketClientHandler listener = null;

	/**
	 * Called upon the websocket being connected successfully.
	 */
	protected abstract void onConnected();

	/**
	 * Called upon connection dropped.
	 */
	protected abstract void connectionDropped();

	/**
	 * Called when data is received from the websocket.
	 * @param data the data packet received.
	 */
	protected abstract void receive(JsonElement data);

	/**
	 * Try connect to the websocket. Blocking method.
	 */
	public void connect() {
		if (this.socket != null) {
			this.listener.clientClosed.set(true);
			this.socket.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting");
		}

		this.socket = HTTP_CLIENT.newWebSocketBuilder()
				.buildAsync(this.uri, this.listener = new WebSocketClientHandler())
				.join();
	}

	/**
	 * Send a json packet to the server.
	 * @param element the json packet to send.
	 * @throws IllegalStateException if the websocket connection is null or inactive.
	 */
	public void send(JsonElement element) throws IllegalStateException {
		if (this.socket == null) {
			throw new IllegalStateException("No websocket connection has been initiated");
		}

		// TODO !this.channel.isActive()

		this.socket.sendText(new Gson().toJson(element), true)
				.exceptionally(err -> {
					Logging.getInstance().error("Websocket message send failed", err);
					return null;
				});
	}

	/**
	 * Ping the websocket to maintain connection. Does not error if the websocket connection is null or inactive but
	 * indicates in return value.
	 * @return whether the ping could be sent.
	 */
	public boolean ping() {
		if (this.socket == null) {
			return false;
		}

		ByteBuffer pingPayload = ByteBuffer.wrap("keep-alive".getBytes());
		CompletableFuture<WebSocket> pingFuture = this.socket.sendPing(pingPayload);

		pingFuture.whenComplete((ws, err) -> {
			if (err != null) {
				Logging.getInstance().error("Websocket ping failed", err);
			}
		});

		return true;
	}

	/**
	 * Close the websocket.
	 */
	public void closeFuture() {
		if (this.socket == null) {
			return; // this is ok probably
		}

		this.listener.clientClosed.set(true);
		this.socket.sendClose(WebSocket.NORMAL_CLOSURE, "Websocket Client Closed");
	}

	/**
	 * Netty Handler for websocket connection.
	 */
	public class WebSocketClientHandler implements WebSocket.Listener {
		private final AtomicBoolean clientClosed = new AtomicBoolean(false);

		@Override
		public void onOpen(WebSocket webSocket) {
			Logging.getInstance().debug(
					LoggingCategory.WEBSOCKET,
					"{} connected!",
					Websocket.this.name
			);

			Websocket.this.onConnected();
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			try {
				Websocket.this.receive(JsonParser.parseString(data.toString()));
			} catch (Exception e) {
				Logging.getInstance().error(
						"{}: Error parsing websocket message",
						e,
						Websocket.this.name
				);
			}

			webSocket.request(1);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			Logging.getInstance().error("{}: Caught websocket error", error, Websocket.this.name);
			Websocket.this.connectionDropped();
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			boolean intentional = clientClosed.get();
			Logging.getInstance().debug(
					LoggingCategory.WEBSOCKET,
					"{}: server closed connection ({} - {}). Intentional: {}",
					Websocket.this.name,
					statusCode,
					reason,
					intentional
			);

			if (!intentional) {
				Websocket.this.connectionDropped();
			}

			return CompletableFuture.completedFuture(null);
		}
	}

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
}
