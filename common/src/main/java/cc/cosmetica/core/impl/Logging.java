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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles logging for the mod.
 */
public final class Logging {
	private Logging() {
	}

	private final Set<String> warnings = new HashSet<>();
	private final Logger logger = LogManager.getLogger("Cosmetica");
	private final boolean debug = Boolean.getBoolean("cosmetica.debug");

	public void debug(String message, Object... args) {
		if (debug) {
			info(message, args);
		} else {
			this.logger.debug(message, args);
		}
	}

	public void info(String message, Object... args) {
		this.logger.info(message, args);
	}

	public void warn(String message, Object... args) {
		this.logger.warn(message, args);
	}

	public void error(String message, Throwable t) {
		this.logger.error(message, t);
	}

	public void error(String message, Throwable t, Object... args) {
		message = message.replaceAll("\\{}", "%s");
		message = String.format(message, args);
		this.logger.error(message, t);
	}

	public void warnOnce(String warning, String message, Object... args) {
		if (warnings.add(warning)) {
			this.warn(message, args);
		}
	}

	public static Logging getInstance() {
		return INSTANCE;
	}

	private static final Logging INSTANCE = new Logging();
}
