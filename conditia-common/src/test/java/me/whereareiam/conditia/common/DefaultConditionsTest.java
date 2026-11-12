package me.whereareiam.conditia.common;

import me.whereareiam.conditia.ConditionScope;
import me.whereareiam.conditia.Conditions;
import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.common.Chat.Player;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static me.whereareiam.conditia.testing.TestRequirements.yaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConditionsTest {
	private static final Player SENDER = new Player("sender", Set.of("chat.global"), 0);
	private static final Player RECIPIENT = new Player("recipient", Set.of("chat.receive"), 40);

	private Conditions conditions;

	@BeforeEach
	void setUp() {
		conditions = new DefaultConditions(ConditionsOptions.builder()
				.reader(Chat.SETTINGS)
				.namedLists(Optional.of("audience"))
				.build());

		conditions.register(new Chat.Permission());
		conditions.register(new Chat.Nearby());
		conditions.register(new Chat.Ahead());
		conditions.register(new Chat.Owns());
		conditions.register(new Chat.Broken());
	}

	@Test
	void single_WithoutOf_IsAboutTheMainParticipant() {
		assertTrue(check("- permission: chat.global").passed());
		assertFailed("chat.requires[0].permission", "permission", check("- permission: chat.receive"));
	}

	@Test
	void participantBlock_IsAboutItsPart() {
		Verdict verdict = check("""
				- recipient:
				    - permission: chat.receive
				    - not: { permission: chat.global }
				""");

		assertTrue(verdict.passed());
	}

	@Test
	void of_NamesThePart() {
		assertTrue(check("- permission: { nodes: chat.receive, of: recipient }").passed());
	}

	@Test
	void pair_WithoutOf_TakesTheMainParticipantAndTheNextFittingPart() {
		assertTrue(check("- nearby: 50").passed());
		assertFailed("chat.requires[0].nearby", "nearby", check("- nearby: 10"));
	}

	@Test
	void typeWithoutSettings_ItsValueNamesTheParts() {
		assertTrue(check("- ahead: [recipient, sender]").passed());
		assertFailed("chat.requires[0].ahead", "ahead", check("- ahead:"));
	}

	@Test
	void any_HoldsWhenOneHolds_AndReportsItselfWhenNone() {
		assertTrue(check("- any: [permission: chat.admin, permission: chat.global]").passed());
		assertFailed("chat.requires[0].any", "any", check("- any: [permission: chat.admin, permission: chat.mod]"));
	}

	@Test
	void not_ReportsItself() {
		assertFailed("chat.requires[0].not", "not", check("- not: { permission: chat.global }"));
	}

	@Test
	void check_StopsAtTheFirstFailure() {
		Verdict verdict = check("""
				- permission: chat.global
				- recipient: [owns: [vip, hat]]
				- broken: false
				""");

		Verdict.Failed failed = assertFailed("chat.requires[1].recipient[0].owns", "owns", verdict);
		assertEquals(Optional.of("owns.missing-products"), failed.key());
		assertEquals(Map.of("products", "vip, hat"), failed.reason().orElseThrow().values());
	}

	@Test
	void aTypeThatThrowsOrFails_IsBroken() {
		Verdict thrown = check("- broken: false");
		Verdict late = check("- broken: true");

		assertEquals("thrown", assertInstanceOf(Verdict.Broken.class, thrown).error().getMessage());
		assertEquals("completed exceptionally", assertInstanceOf(Verdict.Broken.class, late).error().getMessage());
	}

	@Test
	void namedList_ReportsTheReferenceWithTheReasonInside_AndFollowsTheBlock() {
		conditions.name("customer", yaml("- owns: vip"));

		Verdict verdict = check("- recipient: [audience: customer]");

		Verdict.Failed failed = assertFailed("chat.requires[0].recipient[0].audience", "audience customer", verdict);
		assertTrue(failed.reason().isPresent());
	}

	@Test
	void namedList_ThatRefersToItself_CannotBeBound() {
		conditions.name("loop", yaml("- audience: loop"));

		ConditionException exception = assertThrows(ConditionException.class, () -> place("- audience: loop"));

		assertEquals("chat.requires[0].audience[0].audience", exception.getPath());
	}

	@Test
	void place_WithAnUnknownType_CannotBeBound() {
		ConditionException exception = assertThrows(ConditionException.class, () -> place("- recipient: [verified: true]"));

		assertEquals("chat.requires[0].recipient[0].verified", exception.getPath());
	}

	@Test
	void place_WithUnreadableSettings_CannotBeBound() {
		ConditionException exception = assertThrows(ConditionException.class, () -> place("- nearby: far"));

		assertEquals("chat.requires[0].nearby", exception.getPath());
	}

	@Test
	void place_WhereSeveralPartsFit_AsksForOf() {
		ConditionException exception = assertThrows(ConditionException.class, () ->
				conditions.place("chat.requires", yaml("- permission: chat.global"), Chat.MESSAGE, Chat.SENDER, Chat.RECIPIENT));

		assertTrue(exception.getMessage().contains("`of`"));
	}

	@Test
	void place_WithAPartNamedLikeAType_CannotBeBound() {
		assertThrows(ConditionException.class, () ->
				conditions.place("chat.requires", yaml("[]"), me.whereareiam.conditia.model.Part.of("owns", Player.class)));
	}

	@Test
	void scope_RegistersWithItsPrefix_AndItsPlacesDenyAfterItCloses() {
		ConditionScope scope = conditions.scope("shop");
		scope.register(new Chat.Owns());
		Place place = place("- shop:owns: []");

		assertTrue(check(place).passed());

		scope.close();
		assertInstanceOf(Verdict.Broken.class, check(place));

		conditions.scope("shop").register(new Chat.Owns());
		assertTrue(check(place).passed());
	}

	@Test
	void scope_OfAnOwnerOpenAlready_IsRefused() {
		conditions.scope("shop");

		assertThrows(IllegalStateException.class, () -> conditions.scope("shop"));
	}

	@Test
	void check_OfASituationWithoutAPart_IsRefused() {
		Place place = place("[]");

		assertThrows(IllegalArgumentException.class, () -> place.check(Situation.of(Chat.SENDER, SENDER)));
	}

	@Test
	void closedPlace_IsNoLongerBoundAgain() {
		ConditionScope scope = conditions.scope("shop");
		scope.register(new Chat.Owns());
		Place place = place("- shop:owns: []");

		place.close();
		scope.close();

		assertTrue(check(place).passed());
	}

	private Verdict check(String requires) {
		return check(place(requires));
	}

	private Place place(String requires) {
		return conditions.place("chat.requires", yaml(requires), Chat.SENDER, Chat.RECIPIENT);
	}

	private static Verdict check(Place place) {
		return place.check(Situation.of(Chat.SENDER, SENDER).and(Chat.RECIPIENT, RECIPIENT)).toCompletableFuture().join();
	}

	private static Verdict.Failed assertFailed(String path, String condition, Verdict verdict) {
		Verdict.Failed failed = assertInstanceOf(Verdict.Failed.class, verdict);
		assertEquals(path, failed.path());
		assertEquals(condition, failed.condition());

		return failed;
	}
}
