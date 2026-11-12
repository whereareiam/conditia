package me.whereareiam.conditia;

import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

/**
 * Where condition types and named lists are registered. {@link Conditions} registers under plain names; a
 * {@link ConditionScope} registers under its owner's prefix and removes everything when it closes.
 *
 * <p>Every change binds all places again, so a place that refers to a type or list registered later starts to
 * work, and one that refers to something removed denies everyone until it comes back.</p>
 */
public interface ConditionRegistrar {
	/**
	 * Registers a condition type.
	 *
	 * @param type the type
	 * @return the name configs use for it: its name, or {@code <owner>:<name>} in a scope
	 * @throws IllegalArgumentException when the name is not lower case letters, digits and dashes
	 * @throws IllegalStateException    when a type already has that name
	 */
	@NotNull String register(@NotNull ConditionType<?> type);

	/**
	 * Registers a named list of conditions, or replaces the list of that name registered here before. A host decides
	 * the word that refers to named lists ({@code audience: moderator} in Yui); without one, they cannot be used.
	 *
	 * @param name     the list's name, lower case letters, digits and dashes
	 * @param requires the conditions, all of which must hold
	 * @return the name configs use for it: its name, or {@code <owner>:<name>} in a scope
	 */
	@NotNull String name(@NotNull String name, @NotNull Requirements requires);

	/**
	 * Removes a named list registered here.
	 *
	 * @param name the list's name as given to {@link #name(String, Requirements)}
	 */
	void forget(@NotNull String name);
}
