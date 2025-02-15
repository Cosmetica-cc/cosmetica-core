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

import com.google.gson.*;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Assists making post and get requests and parsing the responses.
 */
public class Response implements Closeable {
	private Response(CloseableHttpClient client, CloseableHttpResponse response) {
		this.client = client;
		this.response = response;
		this.status = this.response.getStatusLine();
	}

	private final CloseableHttpClient client;
	private final CloseableHttpResponse response;
	private final StatusLine status;

	public StatusLine getStatus() {
		return this.status;
	}

	public int getStatusCode() {
		return this.status.getStatusCode();
	}

	/**
	 * Get whether this response is successful.
	 * @return whether this response is successful. It is considered successful if the status code is in the 2XX range.
	 */
	public boolean isSuccessful() {
		int code = this.getStatusCode();
		return code >= 200 && code < 300;
	}

	@Nullable
	public HttpEntity getEntity() {
		return this.response.getEntity();
	}

	/**
	 * Parse this response as a string. This will consume the entity content.
	 * @return the response as a string.
	 * @throws IOException if an IO exception occurs during the operation.
	 */
	public String readEntityString() throws IOException {
		HttpEntity entity = this.getEntity();
		return entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
	}

	/**
	 * Parse this response as a byte array. This will consume the entity content.
	 * @return the response as a byte array.
	 * @throws IOException if an IO exception occurs during the operation.
	 */
	public byte[] readEntityBytes() throws IOException {
		HttpEntity entity = this.getEntity();
		return entity == null ? new byte[0] : EntityUtils.toByteArray(this.getEntity());
	}

	/**
	 * Parse this response as JSON. This will consume the entity content.
	 * @return the response body as a JSON element.
	 * @throws NullPointerException if the response is empty.
	 * @throws IOException if an IO exception occurs during the operation.
	 * @throws JsonParseException if the JSON is malformed.
	 */
	public JsonElement readEntityJson() throws NullPointerException, IOException, JsonParseException {
		String s = EntityUtils.toString(Objects.requireNonNull(this.getEntity(), "Response body is missing"), StandardCharsets.UTF_8).trim();
		return new JsonParser().parse(s);
	}

	@Override
	public void close() throws IOException {
		this.response.close();
		this.client.close();
	}

	private static final int DEFAULT_TIMEOUT = 20 * 1000;

	/**
	 * Make a get request using the default timeout.
	 * @param request the url to request to.
	 */
	public static Response get(String request) throws ParseException, IOException {
		return get(request, DEFAULT_TIMEOUT);
	}

	/**
	 * Make a get request.
	 * @param request the url to request to.
	 * @param timeout the request timeout, in milliseconds.
	 */
	public static Response get(String request, int timeout) throws ParseException, IOException {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(timeout)
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();

		CloseableHttpClient client = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.build();

		final HttpGet get = new HttpGet(request);

		CloseableHttpResponse response = client.execute(get);
		return new Response(client, response);
	}

	/**
	 * Make a post request using the default itmeout.
	 * @param url the url to submit a post request to.
	 * @param body the Application/Json body.
	 * @return the response from the server.
	 * @throws IOException if there is an IO issue performing the request.
	 */
	public static Response post(String url, JsonElement body) throws IOException {
		return post(url, body, DEFAULT_TIMEOUT);
	}

	public static Response post(String url, JsonElement body, int timeout) throws IOException {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(timeout)
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();

		CloseableHttpClient client = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.build();

		HttpEntity entity = new StringEntity(GSON.toJson(body), ContentType.APPLICATION_JSON);

		final HttpPost post = new HttpPost(url);
		post.setEntity(entity);

		CloseableHttpResponse response = client.execute(post);

		return new Response(client, response);
	}

	private static final Gson GSON = new Gson();
}
