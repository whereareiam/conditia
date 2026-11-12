package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * The rule for names of parts, types, named lists and owners: lower case letters, digits and dashes, starting with
 * a letter.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Names {
	private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]*");

	/**
	 * Tells whether a name follows the rule.
	 *
	 * @param name the name
	 * @return whether it is lower case letters, digits and dashes, starting with a letter
	 */
	public static boolean valid(@NotNull String name) {
		return NAME.matcher(name).matches();
	}
}
