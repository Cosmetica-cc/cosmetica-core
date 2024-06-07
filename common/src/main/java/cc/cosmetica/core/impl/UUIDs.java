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

import java.util.UUID;

/**
 * Util class to handle UUIDs.
 */
public class UUIDs {
	/**
	 * Convert string to uuid, whether dashed or dashless.
	 */
	public static UUID fromString(String uuid) {
		if (uuid.length() == 32) {
			// dashless to dashed
			uuid = uuid.substring(0, 8) + "-" + uuid.substring(8, 8+4) + "-" + uuid.substring(12, 12+4)
					+ "-" + uuid.substring(16, 16+4) + "-" + uuid.substring(20);
		}

		return UUID.fromString(uuid);
	}
}
