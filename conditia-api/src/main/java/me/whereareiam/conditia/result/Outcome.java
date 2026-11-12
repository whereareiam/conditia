package me.whereareiam.conditia.result;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Whether a condition holds, as its type tells it: {@link #pass()}, {@link #fail()} or {@link #fail(Reason)}.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Outcome {
	private static final Outcome PASS = new Outcome(true, null);
	private static final Outcome FAIL = new Outcome(false, null);

	private final boolean passed;
	private final Reason reason;

	/**
	 * The condition holds.
	 *
	 * @return the outcome
	 */
	public static @NotNull Outcome pass() {
		return PASS;
	}

	/**
	 * The condition does not hold, without a reason.
	 *
	 * @return the outcome
	 */
	public static @NotNull Outcome fail() {
		return FAIL;
	}

	/**
	 * The condition does not hold, for a reason.
	 *
	 * @param reason the reason, one the type lists in its {@code reasons()}
	 * @return the outcome
	 */
	public static @NotNull Outcome fail(@NotNull Reason reason) {
		return new Outcome(false, reason);
	}

	/**
	 * Passes when the condition is true, fails without a reason otherwise.
	 *
	 * @param holds whether the condition holds
	 * @return the outcome
	 */
	public static @NotNull Outcome when(boolean holds) {
		return holds ? PASS : FAIL;
	}

	/**
	 * Whether the condition holds.
	 *
	 * @return whether it passed
	 */
	public boolean passed() {
		return passed;
	}

	/**
	 * Why the condition failed.
	 *
	 * @return the reason, empty when it passed or failed without one
	 */
	public @NotNull Optional<Reason> reason() {
		return Optional.ofNullable(reason);
	}

	/**
	 * This outcome as a completed stage, for a deferred type that can sometimes decide at once.
	 *
	 * @return the completed stage
	 */
	public @NotNull CompletionStage<Outcome> completed() {
		return CompletableFuture.completedStage(this);
	}
}
