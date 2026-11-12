package me.whereareiam.conditia.common.evaluation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Runs a bound check in order and stops at the first condition that does not hold, either at once on the caller's
 * thread or as a stage that waits for types answering later. Nothing here throws or completes exceptionally: a type
 * that throws makes the verdict {@link Verdict.Broken}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Evaluation {
	private static final Verdict PASSED = new Verdict.Passed();

	/**
	 * Runs a check whose conditions all answer at once.
	 *
	 * @param check     the bound check, bound for a place that needs its answer at once
	 * @param situation the values of the place's parts
	 * @return the verdict
	 */
	public static @NotNull Verdict now(@NotNull Check check, @NotNull Situation situation) {
		return switch (check) {
			case Check.All all -> {
				for (Check each : all.checks()) {
					Verdict verdict = now(each, situation);
					if (!verdict.passed()) yield verdict;
				}

				yield PASSED;
			}
			case Check.Any any -> {
				for (Check each : any.checks()) {
					Verdict verdict = now(each, situation);
					if (!(verdict instanceof Verdict.Failed)) yield verdict;
				}

				yield new Verdict.Failed(any.path(), "any", any.parts(), Optional.empty());
			}
			case Check.Not not -> negate(not, now(not.check(), situation));
			case Check.Test test -> switch (test.run()) {
				case Check.Immediate immediate -> immediate(test, immediate, situation);
				case Check.Deferred ignored -> new Verdict.Broken(test.path(), test.condition(),
						new IllegalStateException("The type answers later, and the place needs its answer at once"));
			};
		};
	}

	/**
	 * Runs a check, waiting for the types that answer later.
	 *
	 * @param check     the bound check
	 * @param situation the values of the place's parts
	 * @return a stage completing with the verdict
	 */
	public static @NotNull CompletionStage<Verdict> later(@NotNull Check check, @NotNull Situation situation) {
		return switch (check) {
			case Check.All all -> all(all.checks(), 0, situation);
			case Check.Any any -> any(any, 0, situation);
			case Check.Not not -> later(not.check(), situation).thenApply(verdict -> negate(not, verdict));
			case Check.Test test -> switch (test.run()) {
				case Check.Immediate immediate -> CompletableFuture.completedStage(immediate(test, immediate, situation));
				case Check.Deferred deferred -> deferred(test, deferred, situation);
			};
		};
	}

	private static CompletionStage<Verdict> all(List<Check> checks, int index, Situation situation) {
		if (index == checks.size()) return CompletableFuture.completedStage(PASSED);

		return later(checks.get(index), situation)
				.thenCompose(verdict -> verdict.passed() ? all(checks, index + 1, situation) : CompletableFuture.completedStage(verdict));
	}

	private static CompletionStage<Verdict> any(Check.Any any, int index, Situation situation) {
		if (index == any.checks().size())
			return CompletableFuture.completedStage(new Verdict.Failed(any.path(), "any", any.parts(), Optional.empty()));

		return later(any.checks().get(index), situation).thenCompose(verdict -> switch (verdict) {
			case Verdict.Failed ignored -> any(any, index + 1, situation);
			default -> CompletableFuture.completedStage(verdict);
		});
	}

	private static Verdict negate(Check.Not not, Verdict verdict) {
		return switch (verdict) {
			case Verdict.Passed ignored -> new Verdict.Failed(not.path(), "not", not.parts(), Optional.empty());
			case Verdict.Failed ignored -> PASSED;
			case Verdict.Broken broken -> broken;
		};
	}

	private static Verdict immediate(Check.Test test, Check.Immediate run, Situation situation) {
		Outcome outcome;
		try {
			outcome = run.holds(situation);
		} catch (RuntimeException exception) {
			return new Verdict.Broken(test.path(), test.condition(), exception);
		}

		return verdict(test, outcome);
	}

	private static CompletionStage<Verdict> deferred(Check.Test test, Check.Deferred run, Situation situation) {
		CompletionStage<Outcome> outcome;
		try {
			outcome = run.holds(situation);
		} catch (RuntimeException exception) {
			return CompletableFuture.completedStage(new Verdict.Broken(test.path(), test.condition(), exception));
		}
		if (outcome == null) return CompletableFuture.completedStage(verdict(test, null));

		return outcome.handle((result, error) -> error != null
				? new Verdict.Broken(test.path(), test.condition(), unwrap(error))
				: verdict(test, result));
	}

	private static Verdict verdict(Check.Test test, Outcome outcome) {
		if (outcome == null) return new Verdict.Broken(test.path(), test.condition(), new NullPointerException("The type answered without an outcome"));

		return outcome.passed() ? PASSED : new Verdict.Failed(test.path(), test.condition(), test.parts(), outcome.reason());
	}

	private static Throwable unwrap(Throwable error) {
		return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
	}
}
