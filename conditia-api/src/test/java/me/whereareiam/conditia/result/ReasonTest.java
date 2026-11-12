package me.whereareiam.conditia.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReasonTest {
	record MissingProducts() implements Reason {
	}

	@Test
	void key_IsTheConditionAndTheRecordInLowerCaseWithDashes() {
		assertEquals("products:owns.missing-products", Reason.key("products:owns", MissingProducts.class));
	}
}
