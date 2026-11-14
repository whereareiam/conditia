package me.whereareiam.conditia.common.evaluation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Runs a bound check in order and stops at the first condition that does not hold, either at once on the caller's
 * thread or as a stage that waits for types answering later. Nothing here throws or completes exceptionally: a type
 * that throws makes the verdict {@link Verdict.Broken}.
 *
 * <p>A check that holds remembers the condition that decided it, with the reason its type gave on passing, so a
 * {@code not} around it fails with that condition and reason rather than with itself.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Evaluation {
	private static final Verdict PASSED = new Verdict.Passed();
	private static final Result HELD = new Result.Held(Optional.empty(), List.of());

	/**
	 * Runs a check whose conditions all answer at once.
	 *
	 * @param check     the bound check, bound for a place that needs its answer at once
	 * @param situation the values of the place's parts
	 * @return the verdict
	 */
	public static @NotNull Verdict now(@NotNull Check check, @NotNull Situation situation) {
		return immediately(check, situation).verdict();
	}

	/**
	 * Runs a check, waiting for the types that answer later.
	 *
	 * @param check     the bound check
	 * @param situation the values of the place's parts
	 * @return a stage completing with the verdict
	 */
	public static @NotNull CompletionStage<Verdict> later(@NotNull Check check, @NotNull Situation situation) {
		return eventually(check, situation).thenApply(Result::verdict);
	}

	private static Result immediately(Check check, Situation situation) {
		return switch (check) {
			case Check.All all -> {
				Result last = HELD;
				List<Verdict.Recordable> recordings = new ArrayList<>();
				for (Check each : all.checks()) {
					last = immediately(each, situation);
					if (!(last instanceof Result.Held held)) yield last;

					recordings.addAll(held.recordings());
				}

				yield all.checks().size() == 1 ? last : new Result.Held(Optional.empty(), List.copyOf(recordings));
			}
			case Check.Any any -> {
				Result last = HELD;
				for (Check each : any.checks()) {
					last = immediately(each, situation);
					if (!failed(last)) yield last;
				}

				yield any.checks().size() == 1 ? last : failed(any);
			}
			case Check.Not not -> negate(not, immediately(not.check(), situation));
			case Check.Test test -> switch (test.run()) {
				case Check.Immediate immediate -> immediate(test, immediate, situation);
				case Check.Recording recording -> recording(test, recording, situation);
				case Check.Deferred ignored -> new Result.Refused(new Verdict.Broken(test.path(), test.condition(),
						new IllegalStateException("The type answers later, and the place needs its answer at once")));
			};
			case Check.Worded worded -> worded(worded, immediately(worded.check(), situation));
		};
	}

	private static CompletionStage<Result> eventually(Check check, Situation situation) {
		return switch (check) {
			case Check.All all -> all(all, 0, HELD, List.of(), situation);
			case Check.Any any -> any(any, 0, HELD, situation);
			case Check.Not not -> eventually(not.check(), situation).thenApply(result -> negate(not, result));
			case Check.Test test -> switch (test.run()) {
				case Check.Immediate immediate -> CompletableFuture.completedStage(immediate(test, immediate, situation));
				case Check.Recording recording -> CompletableFuture.completedStage(recording(test, recording, situation));
				case Check.Deferred deferred -> deferred(test, deferred, situation);
			};
			case Check.Worded worded -> eventually(worded.check(), situation).thenApply(result -> worded(worded, result));
		};
	}

	private static CompletionStage<Result> all(Check.All all, int index, Result last, List<Verdict.Recordable> recordings, Situation situation) {
		List<Check> checks = all.checks();
		if (index == checks.size())
			return CompletableFuture.completedStage(checks.size() == 1 ? last : new Result.Held(Optional.empty(), recordings));

		return eventually(checks.get(index), situation).thenCompose(result -> result instanceof Result.Held held
				? all(all, index + 1, result, joined(recordings, held.recordings()), situation)
				: CompletableFuture.completedStage(result));
	}

	private static List<Verdict.Recordable> joined(List<Verdict.Recordable> first, List<Verdict.Recordable> second) {
		if (second.isEmpty()) return first;

		List<Verdict.Recordable> joined = new ArrayList<>(first);
		joined.addAll(second);

		return List.copyOf(joined);
	}

	private static CompletionStage<Result> any(Check.Any any, int index, Result last, Situation situation) {
		List<Check> checks = any.checks();
		if (index == checks.size()) return CompletableFuture.completedStage(checks.size() == 1 ? last : failed(any));

		return eventually(checks.get(index), situation).thenCompose(result -> failed(result)
				? any(any, index + 1, result, situation)
				: CompletableFuture.completedStage(result));
	}

	private static Result worded(Check.Worded worded, Result result) {
		return result instanceof Result.Refused(Verdict.Failed failed) ? new Result.Refused(failed.orWording(worded.wording())) : result;
	}

	private static Result negate(Check.Not not, Result result) {
		return switch (result) {
			case Result.Held held -> new Result.Refused(held.decided()
					.orElseGet(() -> new Verdict.Failed(not.path(), "not", not.parts(), Optional.empty())));
			// What failed inside a not held only by failing, so it records nothing.
			case Result.Refused(Verdict.Failed failed) -> new Result.Held(Optional.of(failed), List.of());
			case Result.Refused refused -> refused;
		};
	}

	private static Result immediate(Check.Test test, Check.Immediate run, Situation situation) {
		Outcome outcome;
		try {
			outcome = run.holds(situation);
		} catch (RuntimeException exception) {
			return new Result.Refused(new Verdict.Broken(test.path(), test.condition(), exception));
		}

		return result(test, outcome);
	}

	private static Result recording(Check.Test test, Check.Recording run, Situation situation) {
		Outcome outcome;
		try {
			outcome = run.holds(situation);
		} catch (RuntimeException exception) {
			return new Result.Refused(new Verdict.Broken(test.path(), test.condition(), exception));
		}

		Result result = result(test, outcome);
		if (!(result instanceof Result.Held held)) return result;

		Verdict.Recordable recordable = new Verdict.Recordable(test.path(), test.condition(), () -> run.record(situation));

		return new Result.Held(held.decided(), List.of(recordable));
	}

	private static CompletionStage<Result> deferred(Check.Test test, Check.Deferred run, Situation situation) {
		CompletionStage<Outcome> outcome;
		try {
			outcome = run.holds(situation);
		} catch (RuntimeException exception) {
			return CompletableFuture.completedStage(new Result.Refused(new Verdict.Broken(test.path(), test.condition(), exception)));
		}
		if (outcome == null) return CompletableFuture.completedStage(result(test, null));

		return outcome.handle((answer, error) -> error != null
				? new Result.Refused(new Verdict.Broken(test.path(), test.condition(), unwrap(error)))
				: result(test, answer));
	}

	private static Result result(Check.Test test, Outcome outcome) {
		if (outcome == null)
			return new Result.Refused(new Verdict.Broken(test.path(), test.condition(), new NullPointerException("The type answered without an outcome")));

		Verdict.Failed decided = new Verdict.Failed(test.path(), test.condition(), test.parts(), outcome.reason());

		return outcome.passed() ? new Result.Held(Optional.of(decided), List.of()) : new Result.Refused(decided);
	}

	private static boolean failed(Result result) {
		return result instanceof Result.Refused(Verdict.Failed ignored);
	}

	private static Result failed(Check.Any any) {
		return new Result.Refused(new Verdict.Failed(any.path(), "any", any.parts(), Optional.empty()));
	}

	private static Throwable unwrap(Throwable error) {
		return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
	}

	/**
	 * How a check came out, before it is a verdict.
	 */
	private sealed interface Result {
		Verdict verdict();

		/**
		 * The check held.
		 *
		 * @param decided    what a {@code not} around it reports: the condition that decided it, with the reason its
		 *                   type gave on passing; empty when no single condition decided it
		 * @param recordings what the recording conditions that decided it record, in the config's order
		 */
		record Held(Optional<Verdict.Failed> decided, List<Verdict.Recordable> recordings) implements Result {
			@Override
			public Verdict verdict() {
				return recordings.isEmpty() ? PASSED : new Verdict.Passed(recordings);
			}
		}

		/**
		 * The check failed or is broken.
		 *
		 * @param verdict the failed or broken verdict
		 */
		record Refused(Verdict verdict) implements Result {
		}
	}
}
