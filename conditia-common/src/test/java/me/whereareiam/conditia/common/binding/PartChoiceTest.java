package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartChoiceTest {
	private static final Part<Integer> ACTOR = Part.of("actor", Integer.class);
	private static final Part<Integer> TARGET = Part.of("target", Integer.class);
	private static final Part<String> CHANNEL = Part.of("channel", String.class);

	private final PartChoice choice = new PartChoice(List.of(ACTOR, TARGET, CHANNEL));

	@Test
	void single_TakesTheMainPartWhenItFits() {
		assertEquals(ACTOR, choice.single("p", Integer.class, List.of(), Focus.start()));
	}

	@Test
	void single_TakesTheOnlyFittingPartOtherwise() {
		assertEquals(CHANNEL, choice.single("p", String.class, List.of(), Focus.start()));
	}

	@Test
	void single_AcceptsASubtypeOfItsSubject() {
		assertEquals(CHANNEL, choice.single("p", CharSequence.class, List.of(), Focus.start()));
	}

	@Test
	void single_InABlock_TakesTheBlocksPart() {
		assertEquals(TARGET, choice.single("p", Integer.class, List.of(), Focus.start().about(TARGET)));
	}

	@Test
	void single_OfAPartOfAnotherType_IsRefused() {
		assertThrows(ConditionException.class, () -> choice.single("p", Integer.class, List.of("channel"), Focus.start()));
	}

	@Test
	void pair_TakesTheMainPartAndTheNextFittingOne() {
		assertEquals(List.of(ACTOR, TARGET), choice.pair("p", Integer.class, Integer.class, List.of(), Focus.start()));
	}

	@Test
	void pair_InABlock_StartsWithTheBlocksPart() {
		assertEquals(List.of(TARGET, ACTOR), choice.pair("p", Integer.class, Integer.class, List.of(), Focus.start().about(TARGET)));
	}

	@Test
	void pair_OfOnePart_IsRefused() {
		assertThrows(ConditionException.class, () -> choice.pair("p", Integer.class, Integer.class, List.of("actor"), Focus.start()));
	}
}
