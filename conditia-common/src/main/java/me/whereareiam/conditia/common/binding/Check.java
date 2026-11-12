package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Requirements bound to a place: which types run, with which settings and parts, combined as the config says.
 */
public sealed interface Check permits Check.All, Check.Any, Check.Not, Check.Named, Check.Test {
	/**
	 * Holds when every check holds; a list in a config, or a participant block.
	 *
	 * @param checks the checks in order
	 */
	record All(@NotNull List<Check> checks) implements Check {
	}

	/**
	 * Holds when one of the checks holds.
	 *
	 * @param path   the {@code any}'s path
	 * @param checks the checks in order
	 */
	record Any(@NotNull String path, @NotNull List<Check> checks) implements Check {
	}

	/**
	 * Holds when the check does not.
	 *
	 * @param path  the {@code not}'s path
	 * @param check the check
	 */
	record Not(@NotNull String path, @NotNull Check check) implements Check {
	}

	/**
	 * A named list, bound for this place.
	 *
	 * @param path      the reference's path
	 * @param condition the host's word and the list's name
	 * @param check     the list's conditions
	 */
	record Named(@NotNull String path, @NotNull String condition, @NotNull Check check) implements Check {
	}

	/**
	 * A condition of a registered type, with its settings and parts.
	 *
	 * @param path      the condition's path
	 * @param condition the type's name as configs use it
	 * @param run       runs the type on a situation
	 */
	record Test(@NotNull String path, @NotNull String condition, @NotNull Run run) implements Check {
	}

	/**
	 * A type bound to its settings and parts: one that answers at once or one that answers later.
	 */
	sealed interface Run permits Immediate, Deferred {
	}

	/**
	 * Runs the type now, on the caller's thread.
	 */
	@FunctionalInterface
	non-sealed interface Immediate extends Run {
		/**
		 * Runs the type.
		 *
		 * @param situation the values of the place's parts
		 * @return the type's outcome
		 */
		@NotNull Outcome holds(@NotNull Situation situation);
	}

	/**
	 * Runs a type that answers later.
	 */
	@FunctionalInterface
	non-sealed interface Deferred extends Run {
		/**
		 * Runs the type.
		 *
		 * @param situation the values of the place's parts
		 * @return the type's outcome when it has answered
		 */
		@NotNull CompletionStage<Outcome> holds(@NotNull Situation situation);
	}
}
