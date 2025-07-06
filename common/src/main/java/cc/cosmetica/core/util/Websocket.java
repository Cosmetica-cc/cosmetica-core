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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

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
	private EventLoopGroup group;
	private Channel channel;

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
	public void connect() throws InterruptedException, SSLException {
		if (this.group != null) {
			this.group.shutdownGracefully();
		}

		this.group = new NioEventLoopGroup();

		final String protocol = uri.getScheme();
		final String host = uri.getHost();
		final int port;
		final String path = uri.getPath();

		SslContext sslCtx;

//		Logging.getInstance().debug("Cosmetica websocket {}", uri);
		if ("wss".equalsIgnoreCase(protocol)) {
			sslCtx = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE)
					.build();
			port = uri.getPort() == -1 ? 443 : uri.getPort();
		} else {
			sslCtx = null;
			port = uri.getPort() == -1 ? 80 : uri.getPort();
		}

		final WebSocketClientHandler handler = new WebSocketClientHandler(
				WebSocketClientHandshakerFactory.newHandshaker(
						uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

		Bootstrap b = new Bootstrap();
		b.group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) {
						if (sslCtx != null) {
							ch.pipeline().addLast(sslCtx.newHandler(ch.alloc(), host, port));
						}

						ch.pipeline().addLast(
								new HttpClientCodec(),
								new HttpObjectAggregator(8192),
								WebSocketClientCompressionHandler.INSTANCE,
								handler);
					}
				})
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

		Channel ch = b.connect(host, port).sync().channel();
		handler.handshakeFuture().sync();

		this.channel = ch;
	}

	/**
	 * Send a json packet to the server.
	 * @param element the json packet to send.
	 * @throws IllegalStateException if the websocket connection is null or inactive.
	 */
	public void send(JsonElement element) throws IllegalStateException {
		if (this.channel == null) {
			throw new IllegalStateException("No websocket connection has been initiated");
		}

		// TODO !this.channel.isActive()

		WebSocketFrame frame = new TextWebSocketFrame(new Gson().toJson(element));
		this.channel.writeAndFlush(frame);
	}

	/**
	 * Ping the websocket to maintain connection. Does not error if the websocket connection is null or inactive but
	 * indicates in return value.
	 * @return whether the ping could be sent.
	 */
	public boolean ping() {
		if (this.channel == null || !this.channel.isActive()) {
			return false;
		}

		WebSocketFrame frame = new PingWebSocketFrame();
		this.channel.writeAndFlush(frame);
		return true;
	}

	/**
	 * Close the websocket.
	 * @return the future. Null if there was no channel.
	 */
	public ChannelFuture closeFuture() {
		if (this.channel == null) {
			return null; // this is ok probably
		}

		// Shut down websocket and its event loop group
		final EventLoopGroup currentGroup = this.group;
		return this.channel.closeFuture().addListener(gfl -> currentGroup.shutdownGracefully());
	}

	/**
	 * Netty Handler for websocket connection.
	 */
	public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
		public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
			this.handshaker = handshaker;
		}

		private final WebSocketClientHandshaker handshaker;
		private ChannelPromise handshakeFuture;

		public ChannelFuture handshakeFuture() {
			return this.handshakeFuture;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) {
			this.handshakeFuture = ctx.newPromise();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			this.handshaker.handshake(ctx.channel());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			// Call the callback. Handled by user.
			Logging.getInstance().debug("channel inactive");
			Websocket.this.connectionDropped();
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
			if (!this.handshaker.isHandshakeComplete()) {
				this.handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
				Logging.getInstance().debug("{} connected!", Websocket.this.name);
				Websocket.this.onConnected();
				this.handshakeFuture.setSuccess();
				return;
			}

			if (msg instanceof TextWebSocketFrame) {
				TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
//				Logging.getInstance().info("RECEIVED ON WEBSOCKET {}", textFrame.text());
				Websocket.this.receive(new JsonParser().parse(textFrame.text()));
			} else if (msg instanceof CloseWebSocketFrame) {
				Logging.getInstance().debug("{}: server closed connection", Websocket.this.name);
				ctx.close();
			}
			// PongWebSocketFrame also exists
//			else if (msg instanceof PongWebSocketFrame) {
//				System.out.println("pong!");
//			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			Logging.getInstance().error("Error in {}", cause, Websocket.this.name);
			if (!this.handshakeFuture.isDone()) {
				this.handshakeFuture.setFailure(cause);
			}
			ctx.close();
		}
	}
}
