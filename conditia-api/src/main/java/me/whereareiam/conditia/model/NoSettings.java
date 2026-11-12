package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The settings of a type that takes none. Conditia reads nothing for it, so its value in a config names the parts
 * the condition is about: {@code above: [actor, target]}, or nothing for the default parts.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoSettings {
	/**
	 * The only instance, passed to such a type's {@code holds}.
	 */
	public static final NoSettings INSTANCE = new NoSettings();
}
