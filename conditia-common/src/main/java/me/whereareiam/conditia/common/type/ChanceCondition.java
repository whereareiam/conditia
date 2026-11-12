package me.whereareiam.conditia.common.type;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.util.random.RandomGenerator;

/**
 * {@code chance: 0.25}: holds for a share of checks, drawn anew each time.
 */
@RequiredArgsConstructor
public final class ChanceCondition implements ConditionType.Plain<ChanceCondition.Settings> {
	private final RandomGenerator random;

	@Override
	public @NotNull String name() {
		return "chance";
	}

	@Override
	public @NotNull Class<Settings> settings() {
		return Settings.class;
	}

	@Override
	public @NotNull Outcome holds(@NotNull Settings settings) {
		return Outcome.when(random.nextDouble() < settings.probability());
	}

	/**
	 * The settings of {@code chance}.
	 *
	 * @param probability the share of checks that pass, from 0 to 1
	 */
	public record Settings(double probability) {
		/**
		 * Checks the probability.
		 */
		public Settings {
			if (probability < 0 || probability > 1) throw new IllegalArgumentException("A chance is from 0 to 1, not " + probability);
		}
	}
}
