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
