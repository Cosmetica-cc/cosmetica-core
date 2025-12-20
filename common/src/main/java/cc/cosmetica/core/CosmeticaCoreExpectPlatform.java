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

package cc.cosmetica.core;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * Adapter for forge/fabric specific methods.
 */
public class CosmeticaCoreExpectPlatform {
	/**
	 * Get the game directory.
	 */
	@ExpectPlatform
	public static Path getGameDirectory() {
		throw new AssertionError();
	}

	/**
	 * Get the config directory.
	 */
	@ExpectPlatform
	public static Path getConfigDirectory() {
		throw new AssertionError();
	}

	/**
	 * Get if in development.
	 */
	@ExpectPlatform
	public static boolean isDev() {
		throw new AssertionError();
	}
}
