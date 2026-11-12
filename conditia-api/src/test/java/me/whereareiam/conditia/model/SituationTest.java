package me.whereareiam.conditia.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SituationTest {
	private static final Part<String> SENDER = Part.of("sender", String.class);
	private static final Part<String> RECIPIENT = Part.of("recipient", String.class);

	@Test
	void and_KeepsTheEarlierSituation() {
		Situation sender = Situation.of(SENDER, "a");
		Situation both = sender.and(RECIPIENT, "b");

		assertEquals("b", both.get(RECIPIENT));
		assertFalse(sender.has(RECIPIENT));
	}

	@Test
	void and_OfAPartAlreadyGiven_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> Situation.of(SENDER, "a").and(SENDER, "b"));
	}

	@Test
	void part_WithAnInvalidName_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> Part.of("Sender", String.class));
	}
}
