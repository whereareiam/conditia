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
 * Runs a bound check in order and stops at the first condition that does not hold. Nothing here completes
 * exceptionally: a type that throws makes the verdict {@link Verdict.Broken}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Evaluation {
	private static final Verdict PASSED = new Verdict.Passed();

	/**
	 * Runs a check.
	 *
	 * @param check     the bound check
	 * @param situation the values of the place's parts
	 * @return a stage completing with the verdict
	 */
	public static @NotNull CompletionStage<Verdict> run(@NotNull Check check, @NotNull Situation situation) {
		return switch (check) {
			case Check.All all -> all(all.checks(), 0, situation);
			case Check.Any any -> any(any, 0, situation);
			case Check.Not not -> run(not.check(), situation).thenApply(verdict -> switch (verdict) {
				case Verdict.Passed ignored -> new Verdict.Failed(not.path(), "not", Optional.empty());
				case Verdict.Failed ignored -> PASSED;
				case Verdict.Broken broken -> broken;
			});
			case Check.Named named -> run(named.check(), situation).thenApply(verdict -> switch (verdict) {
				case Verdict.Failed failed -> new Verdict.Failed(named.path(), named.condition(), failed.reason());
				default -> verdict;
			});
			case Check.Test test -> test(test, situation);
		};
	}

	private static CompletionStage<Verdict> all(List<Check> checks, int index, Situation situation) {
		if (index == checks.size()) return CompletableFuture.completedStage(PASSED);

		return run(checks.get(index), situation)
				.thenCompose(verdict -> verdict.passed() ? all(checks, index + 1, situation) : CompletableFuture.completedStage(verdict));
	}

	private static CompletionStage<Verdict> any(Check.Any any, int index, Situation situation) {
		if (index == any.checks().size())
			return CompletableFuture.completedStage(new Verdict.Failed(any.path(), "any", Optional.empty()));

		return run(any.checks().get(index), situation).thenCompose(verdict -> switch (verdict) {
			case Verdict.Failed ignored -> any(any, index + 1, situation);
			default -> CompletableFuture.completedStage(verdict);
		});
	}

	private static CompletionStage<Verdict> test(Check.Test test, Situation situation) {
		CompletionStage<Outcome> outcome;
		try {
			outcome = test.run().holds(situation);
		} catch (RuntimeException exception) {
			return CompletableFuture.completedStage(new Verdict.Broken(test.path(), test.condition(), exception));
		}

		return outcome.handle((result, error) -> {
			if (error != null) return new Verdict.Broken(test.path(), test.condition(), unwrap(error));
			if (result == null) return new Verdict.Broken(test.path(), test.condition(), new NullPointerException("The type completed without an outcome"));

			return result.passed() ? PASSED : new Verdict.Failed(test.path(), test.condition(), result.reason());
		});
	}

	private static Throwable unwrap(Throwable error) {
		return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
	}
}
