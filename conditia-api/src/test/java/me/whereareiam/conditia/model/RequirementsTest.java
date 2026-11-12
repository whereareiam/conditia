package me.whereareiam.conditia.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequirementsTest {
	@Test
	void of_OneConditionWithoutAList_IsAListOfOne() {
		Node.Mapping condition = Node.Mapping.of("permission", new Node.Scalar("chat.global"));

		assertEquals(List.of(condition), Requirements.of(condition).getConditions().items());
	}

	@Test
	void of_AnEmptyMapping_IsNone() {
		assertEquals(Requirements.none(), Requirements.of(Node.Mapping.empty()));
	}

	@Test
	void of_AScalar_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> Requirements.of(new Node.Scalar("everyone")));
	}
}
