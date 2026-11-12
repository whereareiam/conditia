package me.whereareiam.conditia.common.type;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.LocalTime;
import java.util.concurrent.CompletionStage;

/**
 * {@code time: { from: "08:00", to: "22:00" }}: holds from {@code from} until before {@code to} each day, in the
 * zone of Conditia's clock. A range past midnight ({@code from: "22:00", to: "06:00"}) holds overnight.
 */
@RequiredArgsConstructor
public final class TimeCondition implements ConditionType.Plain<TimeCondition.Settings> {
	private final Clock clock;

	@Override
	public @NotNull String name() {
		return "time";
	}

	@Override
	public @NotNull Class<Settings> settings() {
		return Settings.class;
	}

	@Override
	public @NotNull CompletionStage<Outcome> holds(@NotNull Settings settings) {
		LocalTime now = LocalTime.now(clock);
		boolean sameDay = !settings.from().isAfter(settings.to());
		boolean within = sameDay
				? !now.isBefore(settings.from()) && now.isBefore(settings.to())
				: !now.isBefore(settings.from()) || now.isBefore(settings.to());

		return Outcome.when(within).completed();
	}

	/**
	 * The settings of {@code time}.
	 *
	 * @param from the first moment that holds
	 * @param to   the first moment that no longer holds
	 */
	public record Settings(@NotNull LocalTime from, @NotNull LocalTime to) {
	}
}
