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

import java.util.HashSet;
import java.util.Set;

/**
 * Handles logging for the mod.
 */
public abstract class Logging {
	protected Logging() {
		instance = this;
	}

	private final Set<String> warnings = new HashSet<>();

	public abstract void debug(String message, Object... args);
	public abstract void info(String message, Object... args);
	public abstract void warn(String message, Object... args);
	public abstract void error(String message, Exception exception);

	public void warnOnce(String warning, String message, Object... args) {
		if (warnings.add(warning)) {
			this.warn(message, args);
		}
	}

	public static Logging getInstance() {
		return instance;
	}

	private static Logging instance;
}
