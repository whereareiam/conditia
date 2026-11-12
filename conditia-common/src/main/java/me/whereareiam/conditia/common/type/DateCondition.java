package me.whereareiam.conditia.common.type;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.LocalDate;

/**
 * {@code date: { from: 2026-12-20, to: 2027-01-06 }}: holds on the days from {@code from} to {@code to}, both
 * included, in the zone of Conditia's clock.
 */
@RequiredArgsConstructor
public final class DateCondition implements ConditionType.Plain<DateCondition.Settings> {
	private final Clock clock;

	@Override
	public @NotNull String name() {
		return "date";
	}

	@Override
	public @NotNull Class<Settings> settings() {
		return Settings.class;
	}

	@Override
	public @NotNull Outcome holds(@NotNull Settings settings) {
		LocalDate today = LocalDate.now(clock);

		return Outcome.when(!today.isBefore(settings.from()) && !today.isAfter(settings.to()));
	}

	/**
	 * The settings of {@code date}.
	 *
	 * @param from the first day that holds
	 * @param to   the last day that holds
	 */
	public record Settings(@NotNull LocalDate from, @NotNull LocalDate to) {
		/**
		 * Checks the order of the days.
		 */
		public Settings {
			if (to.isBefore(from)) throw new IllegalArgumentException("`to` is before `from`");
		}
	}
}
