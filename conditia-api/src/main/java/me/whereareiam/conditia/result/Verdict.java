package me.whereareiam.conditia.result;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The result of checking a place: it {@link Passed passed}, a condition {@link Failed failed}, or a condition is
 * {@link Broken broken}. Failed and broken name the first condition that did not hold, by its config path.
 */
public sealed interface Verdict permits Verdict.Passed, Verdict.Failed, Verdict.Broken {
	/**
	 * Whether every condition held.
	 *
	 * @return whether the verdict is {@link Passed}
	 */
	default boolean passed() {
		return this instanceof Passed;
	}

	/**
	 * Every condition held.
	 */
	record Passed() implements Verdict {
	}

	/**
	 * A condition did not hold. A failed {@code any} and a failed {@code not} report themselves, without a reason; a
	 * failed named list reports its reference with the reason of the condition in it that failed.
	 *
	 * @param path      the condition's path, such as {@code commands.clear.requires[1].target[0].not}
	 * @param condition what the condition's line names: a type ({@code permission}), {@code any}, {@code not}, or a
	 *                  named list's word and name ({@code audience moderator})
	 * @param reason    why it failed, when its type said
	 */
	record Failed(@NotNull String path, @NotNull String condition, @NotNull Optional<Reason> reason) implements Verdict {
		/**
		 * The key of the reason's texts.
		 *
		 * @return the key, empty without a reason
		 * @see Reason#key(String, Class)
		 */
		public @NotNull Optional<String> key() {
			return reason.map(value -> Reason.key(condition, value.getClass()));
		}
	}

	/**
	 * A condition could not be checked: its type threw, or the place cannot be bound since a type or named list it
	 * refers to was removed. A broken check denies.
	 *
	 * @param path      the condition's path, or the place's when it cannot be bound
	 * @param condition what the condition's line names, or the place's path when it cannot be bound
	 * @param error     what went wrong
	 */
	record Broken(@NotNull String path, @NotNull String condition, @NotNull Throwable error) implements Verdict {
	}
}
