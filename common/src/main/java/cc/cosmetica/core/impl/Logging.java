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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.CosmeticaCoreExpectPlatform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Handles logging for the mod.
 */
public final class Logging {
	private Logging() {
		if (this.debug) {
			Path config = CosmeticaCoreExpectPlatform.getConfigDirectory().resolve("cosmetica").resolve("debug.properties");

			try (BufferedReader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
				Properties debugConfig = new Properties();
				debugConfig.load(reader);

				for (String s : debugConfig.stringPropertyNames()) {
					if ("true".equals(debugConfig.getProperty(s))) {
						debugCategories.add(s);
					}
				}

			} catch (NoSuchFileException e) {
				// File does not exist, ignore
				debug(null, "No debug config file, enabling all logging...");
			} catch (IOException e) {
				// Other unexpected error (e.g. permissions)
				error("Error reading {}: ", e);
			}
		}
	}

	private final Set<String> warnings = new HashSet<>();
	private final Logger logger = LogManager.getLogger("Cosmetica");
	private final boolean debug = Boolean.getBoolean("cosmetica.debug");
	private final Set<String> debugCategories = new HashSet<>();

	public void debug(@Nullable LoggingCategory category, String message, Object... args) {
		if (debug && (this.debugCategories.isEmpty() || category == null || this.debugCategories.contains(category.name))) {
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

	public void error(String message, Object... args) {
		this.logger.error(message, args);
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
