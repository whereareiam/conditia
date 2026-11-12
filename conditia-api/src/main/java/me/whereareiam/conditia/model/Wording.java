package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A word a host lets a condition's line carry a text under, such as what a player is told when the line denies. The
 * host declares its words and when each applies; Conditia reserves them, reads their texts and reports the texts of
 * the nearest line that has them. What a text means, such as a reference to a phrase, is the host's.
 *
 * <pre>{@code
 * ConditionsOptions.builder()
 *         .wording(Wording.onFailure("denied"))
 *         .wording(Wording.onFailure("hint"));
 * }</pre>
 *
 * <pre>{@code
 * requires:
 *   - permission: chat.staff
 *     denied: "Only staff can write here."
 *     hint: "Ask a moderator."
 * }</pre>
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Wording {
	/**
	 * The word configs use, in lower case with dashes.
	 */
	private final @NotNull String word;

	/**
	 * When the word's text applies.
	 */
	private final @NotNull When when;

	/**
	 * Declares a word whose text applies when its line fails: a {@link me.whereareiam.conditia.result.Verdict.Failed}
	 * reports it.
	 *
	 * @param word the word, lower case letters, digits and dashes
	 * @return the wording
	 * @throws IllegalArgumentException when the word is not lower case letters, digits and dashes
	 */
	public static @NotNull Wording onFailure(@NotNull String word) {
		if (!Names.valid(word)) throw new IllegalArgumentException("A word is lower case letters, digits and dashes: " + word);

		return new Wording(word, When.FAILURE);
	}

	@Override
	public String toString() {
		return word + " (" + when + ")";
	}

	/**
	 * When a word's text applies.
	 */
	public enum When {
		/**
		 * When its line fails.
		 */
		FAILURE
	}
}
