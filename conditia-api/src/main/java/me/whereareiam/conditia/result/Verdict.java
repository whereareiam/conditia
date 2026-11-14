package me.whereareiam.conditia.result;

import me.whereareiam.conditia.model.Part;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	 * Every condition held. When conditions of the {@link me.whereareiam.conditia.ConditionType.Recording Recording}
	 * shape decided it, the verdict carries what they record, and the host calls {@link #record()} once the action the
	 * check allowed happened; a host that never does leaves nothing recorded.
	 *
	 * <pre>{@code
	 * if (!(place.check(situation) instanceof Verdict.Passed passed)) return refuse();
	 * send(message);
	 * passed.record();
	 * }</pre>
	 *
	 * @param recordings what the deciding conditions record, in the config's order
	 */
	record Passed(@NotNull List<Recordable> recordings) implements Verdict {
		/**
		 * A verdict that passed and records nothing.
		 */
		public Passed() {
			this(List.of());
		}

		public Passed {
			recordings = List.copyOf(recordings);
		}

		/**
		 * Records every recording, each once, in order. One that throws does not keep the others from recording; the
		 * first failure is thrown afterwards, with the later ones suppressed.
		 *
		 * @throws RuntimeException the first failure of a recording
		 */
		public void record() {
			RuntimeException failure = null;
			for (Recordable recording : recordings) {
				try {
					recording.record();
				} catch (RuntimeException thrown) {
					if (failure == null) failure = thrown;
					else failure.addSuppressed(thrown);
				}
			}

			if (failure != null) throw failure;
		}
	}

	/**
	 * What a condition of the {@link me.whereareiam.conditia.ConditionType.Recording Recording} shape will record
	 * once the action its check allowed happened.
	 *
	 * @param path      the condition's path
	 * @param condition the type's name as configs use it
	 * @param action    records it
	 */
	record Recordable(@NotNull String path, @NotNull String condition, @NotNull Runnable action) {
		/**
		 * Records it.
		 */
		public void record() {
			action.run();
		}
	}

	/**
	 * A condition did not hold. A failed {@code not} reports the condition inside it that held, with the reason its
	 * type gave on passing, or itself without a reason when several conditions inside it held together; a failed
	 * {@code any} of several conditions reports itself, without a reason. A failed named list reports the condition
	 * in it that failed, at a path that runs through the reference.
	 *
	 * <p>The parts tell a host whose wording to show, such as the sender's or the recipient's: those of the place the
	 * condition was about, or, for an {@code any} or a {@code not}, those its conditions were about.</p>
	 *
	 * @param path      the condition's path, such as {@code commands.clear.requires[1].target[0].not}
	 * @param condition what the condition's line names: a type ({@code permission}, {@code shop:owns}), {@code any}
	 *                  or {@code not}
	 * @param parts     the parts of the place the condition was about, in the place's order; empty for a condition
	 *                  about no part
	 * @param reason    why it failed, or inside a {@code not} why it held, when its type said
	 * @param wording   the texts of the host's words for this failure, by word: each the text of the condition's
	 *                  line, or of the nearest line around it that has one
	 */
	record Failed(
			@NotNull String path,
			@NotNull String condition,
			@NotNull List<Part<?>> parts,
			@NotNull Optional<Reason> reason,
			@NotNull Map<String, String> wording
	) implements Verdict {
		/**
		 * Creates a verdict of a failed condition.
		 */
		public Failed {
			parts = List.copyOf(parts);
			wording = Map.copyOf(wording);
		}

		/**
		 * Creates a verdict of a failed condition whose lines carry no texts.
		 *
		 * @param path      the condition's path
		 * @param condition what the condition's line names
		 * @param parts     the parts of the place the condition was about
		 * @param reason    why it failed, when its type said
		 */
		public Failed(@NotNull String path, @NotNull String condition, @NotNull List<Part<?>> parts, @NotNull Optional<Reason> reason) {
			this(path, condition, parts, reason, Map.of());
		}

		/**
		 * The text of one of the host's words for this failure.
		 *
		 * @param word the word, such as {@code denied}
		 * @return the text, empty when no line around the condition has one
		 */
		public @NotNull Optional<String> wording(@NotNull String word) {
			return Optional.ofNullable(wording.get(word));
		}

		/**
		 * The same failure, with the texts of a line around it for the words it has no text for yet.
		 *
		 * @param texts the line's texts by word
		 * @return the failure with those texts
		 */
		public @NotNull Failed orWording(@NotNull Map<String, String> texts) {
			Map<String, String> merged = new HashMap<>(texts);
			merged.putAll(wording);

			return new Failed(path, condition, parts, reason, merged);
		}

		/**
		 * Whether the condition was about a part, so a host can choose that part's wording.
		 *
		 * @param part a part of the place
		 * @return whether the condition was about it
		 */
		public boolean about(@NotNull Part<?> part) {
			return parts.contains(part);
		}

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
