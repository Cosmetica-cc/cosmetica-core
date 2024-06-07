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

/**
 * Simple cache that compares equality via identity, with ==.
 * @param <T> the type of value to store.
 */
public class IdentityCache<T> {
	private T value;

	/**
	 * Checks if the identity of the current value and new value are the same. If they are not, it sets the new value
	 * and returns true. Otherwise, it returns false.
	 * @param newValue the new value to set.
	 * @return whether the cache was updated (it did not contain the given value).
	 */
	public boolean checkAndSet(T newValue) {
		if (newValue == value) {
			return false; // Value is the same, no update needed
		} else {
			value = newValue;
			return true; // Value is different, updated
		}
	}

	public T getValue() {
		return value;
	}
}
