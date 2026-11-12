package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.CompletionStage;

/**
 * Requirements bound to a place: which types run, with which settings and parts, combined as the config says.
 */
public sealed interface Check permits Check.All, Check.Any, Check.Not, Check.Test, Check.Worded {
	/**
	 * The texts the check's lines carry, by the path of each, such as {@code chat.requires[0].denied}, in the
	 * config's order.
	 *
	 * @param check the check
	 * @return the texts
	 */
	static @NotNull SequencedMap<String, String> wording(@NotNull Check check) {
		SequencedMap<String, String> texts = new LinkedHashMap<>();
		collect(check, texts);

		return texts;
	}

	private static void collect(Check check, SequencedMap<String, String> texts) {
		switch (check) {
			case All all -> all.checks().forEach(each -> collect(each, texts));
			case Any any -> any.checks().forEach(each -> collect(each, texts));
			case Not not -> collect(not.check(), texts);
			case Test ignored -> {
			}
			case Worded worded -> {
				worded.wording().forEach((word, text) -> texts.put(worded.path() + "." + word, text));
				collect(worded.check(), texts);
			}
		}
	}

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
	 * @param parts  the parts its conditions are about, in the place's order
	 * @param checks the checks in order
	 */
	record Any(@NotNull String path, @NotNull List<Part<?>> parts, @NotNull List<Check> checks) implements Check {
	}

	/**
	 * Holds when the check does not.
	 *
	 * @param path  the {@code not}'s path
	 * @param parts the parts its conditions are about, in the place's order
	 * @param check the check
	 */
	record Not(@NotNull String path, @NotNull List<Part<?>> parts, @NotNull Check check) implements Check {
	}

	/**
	 * A condition of a registered type, with its settings and parts.
	 *
	 * @param path      the condition's path
	 * @param condition the type's name as configs use it
	 * @param parts     the parts the condition is about, in the order the type takes them
	 * @param run       runs the type on a situation
	 */
	record Test(@NotNull String path, @NotNull String condition, @NotNull List<Part<?>> parts, @NotNull Run run) implements Check {
	}

	/**
	 * A check whose line carries texts under the host's words.
	 *
	 * @param path    the line's path
	 * @param wording the texts by word, in the config's order
	 * @param check   the check
	 */
	record Worded(@NotNull String path, @NotNull SequencedMap<String, String> wording, @NotNull Check check) implements Check {
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
