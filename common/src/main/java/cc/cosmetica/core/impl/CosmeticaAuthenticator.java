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

import gg.cloaks.javaclient.ApiClient;
import gg.cloaks.javaclient.Configuration;
import gg.cloaks.javaclient.api.DefaultApi;

/**
 * Handles authentication for Cosmetica.
 */
public final class CosmeticaAuthenticator {
	private CosmeticaAuthenticator() {
		// NO-OP
	}

	private static DefaultApi apiInstance;

	static {
		ApiClient defaultClient = Configuration.getDefaultApiClient();
		Logging.getInstance().debug("Using API url: {}", System.getProperty("cosmetica.api", "https://api.cloaks.gg"));
		defaultClient.setBasePath(System.getProperty("cosmetica.api", "https://api.cloaks.gg"));

		apiInstance = new DefaultApi(defaultClient);
	}

	public static DefaultApi getCurrentApi() {
		return apiInstance;
	}
}
