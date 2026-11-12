package me.whereareiam.conditia.common.type;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInTypesTest {
	private static final Clock LATE_EVENING = Clock.fixed(Instant.parse("2026-12-24T23:30:00Z"), ZoneOffset.UTC);

	@Test
	void time_WithinTheSameDay() {
		TimeCondition time = new TimeCondition(LATE_EVENING);

		assertTrue(holds(time.holds(new TimeCondition.Settings(LocalTime.of(20, 0), LocalTime.of(23, 59)))));
		assertFalse(holds(time.holds(new TimeCondition.Settings(LocalTime.of(8, 0), LocalTime.of(22, 0)))));
	}

	@Test
	void time_PastMidnight() {
		assertTrue(holds(new TimeCondition(LATE_EVENING).holds(new TimeCondition.Settings(LocalTime.of(22, 0), LocalTime.of(6, 0)))));
	}

	@Test
	void date_IncludesBothDays() {
		DateCondition date = new DateCondition(LATE_EVENING);

		assertTrue(holds(date.holds(new DateCondition.Settings(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 24)))));
		assertFalse(holds(date.holds(new DateCondition.Settings(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 6)))));
	}

	@Test
	void date_EndingBeforeItStarts_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> new DateCondition.Settings(LocalDate.of(2027, 1, 6), LocalDate.of(2026, 12, 20)));
	}

	@Test
	void chance_ComparesTheDrawWithTheProbability() {
		ChanceCondition chance = new ChanceCondition(fixed(0.3));

		assertTrue(holds(chance.holds(new ChanceCondition.Settings(0.5))));
		assertFalse(holds(chance.holds(new ChanceCondition.Settings(0.2))));
	}

	@Test
	void chance_OutsideZeroToOne_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> new ChanceCondition.Settings(1.5));
	}

	private static boolean holds(java.util.concurrent.CompletionStage<me.whereareiam.conditia.result.Outcome> outcome) {
		return outcome.toCompletableFuture().join().passed();
	}

	private static RandomGenerator fixed(double draw) {
		return new RandomGenerator() {
			public long nextLong() {
				return 0;
			}

			public double nextDouble() {
				return draw;
			}
		};
	}
}
