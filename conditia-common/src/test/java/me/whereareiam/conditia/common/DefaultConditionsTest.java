package me.whereareiam.conditia.common;

import me.whereareiam.conditia.ConditionScope;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.Conditions;
import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.DeferredPlace;
import me.whereareiam.conditia.common.Chat.Player;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

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
		conditions.register(new Chat.Faulty());
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
	void place_ChecksAtOnce() {
		Place place = place("""
				- permission: chat.global
				- recipient: [permission: chat.receive]
				- nearby: 50
				""");

		assertTrue(check(place).passed());
		assertFailed("chat.requires[0].permission", "permission", check(place("- permission: chat.receive")));
	}

	@Test
	void place_WithATypeThatAnswersLater_CannotBeBound() {
		ConditionException exception = assertThrows(ConditionException.class, () -> place("- recipient: [owns: vip]"));

		assertEquals("chat.requires[0].recipient[0].owns", exception.getPath());
		assertTrue(exception.getMessage().contains("answers later"));
	}

	@Test
	void place_WithATypeThatAnswersLaterInsideANamedList_CannotBeBound() {
		conditions.name("customer", yaml("- not: { owns: vip }"));

		ConditionException exception = assertThrows(ConditionException.class, () -> place("- audience: customer"));

		assertEquals("chat.requires[0].audience[0].not.owns", exception.getPath());
	}

	@Test
	void place_WithATypeThatThrows_IsBroken() {
		Verdict.Broken broken = assertInstanceOf(Verdict.Broken.class, check(place("- faulty:")));

		assertEquals("chat.requires[0].faulty", broken.path());
		assertEquals("thrown at once", broken.error().getMessage());
	}

	@Test
	void place_WhoseTypeIsRegisteredAgainAnsweringLater_DeniesEveryone() {
		ConditionScope scope = conditions.scope("shop");
		scope.register(new Chat.Permission());
		Place place = place("- shop:permission: chat.global");

		assertTrue(check(place).passed());

		scope.close();
		ConditionScope again = conditions.scope("shop");
		again.register(new Later());

		assertInstanceOf(Verdict.Broken.class, check(place));

		again.close();
		conditions.scope("shop").register(new Chat.Permission());
		assertTrue(check(place).passed());
	}

	@Test
	void deferredPlace_TakesTypesThatAnswerAtOnceAndLater() {
		Player customer = new Player("customer", Set.of("chat.global"), 0, Set.of("vip"));
		DeferredPlace place = deferred("""
				- permission: chat.global
				- owns: vip
				""");

		assertTrue(place.check(Situation.of(Chat.SENDER, customer).and(Chat.RECIPIENT, RECIPIENT)).toCompletableFuture().join().passed());
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

		ConditionException exception = assertThrows(ConditionException.class, () -> deferred("- audience: loop"));

		assertEquals("chat.requires[0].audience[0].audience", exception.getPath());
	}

	@Test
	void place_WithAnUnknownType_CannotBeBound() {
		ConditionException exception = assertThrows(ConditionException.class, () -> deferred("- recipient: [verified: true]"));

		assertEquals("chat.requires[0].recipient[0].verified", exception.getPath());
	}

	@Test
	void place_WithUnreadableSettings_CannotBeBound() {
		ConditionException exception = assertThrows(ConditionException.class, () -> deferred("- nearby: far"));

		assertEquals("chat.requires[0].nearby", exception.getPath());
	}

	@Test
	void place_WhereSeveralPartsFit_AsksForOf() {
		ConditionException exception = assertThrows(ConditionException.class, () ->
				conditions.deferredPlace("chat.requires", yaml("- permission: chat.global"), Chat.MESSAGE, Chat.SENDER, Chat.RECIPIENT));

		assertTrue(exception.getMessage().contains("`of`"));
	}

	@Test
	void place_WithAPartNamedLikeAType_CannotBeBound() {
		assertThrows(ConditionException.class, () ->
				conditions.deferredPlace("chat.requires", yaml("[]"), me.whereareiam.conditia.model.Part.of("owns", Player.class)));
	}

	@Test
	void scope_RegistersWithItsPrefix_AndItsPlacesDenyAfterItCloses() {
		ConditionScope scope = conditions.scope("shop");
		scope.register(new Chat.Owns());
		DeferredPlace place = deferred("- shop:owns: []");

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
		DeferredPlace place = deferred("[]");

		assertThrows(IllegalArgumentException.class, () -> place.check(Situation.of(Chat.SENDER, SENDER)));
	}

	@Test
	void closedPlace_IsNoLongerBoundAgain() {
		ConditionScope scope = conditions.scope("shop");
		scope.register(new Chat.Owns());
		DeferredPlace place = deferred("- shop:owns: []");

		place.close();
		scope.close();

		assertTrue(check(place).passed());
	}

	private Verdict check(String requires) {
		return check(deferred(requires));
	}

	private DeferredPlace deferred(String requires) {
		return conditions.deferredPlace("chat.requires", yaml(requires), Chat.SENDER, Chat.RECIPIENT);
	}

	private Place place(String requires) {
		return conditions.place("chat.requires", yaml(requires), Chat.SENDER, Chat.RECIPIENT);
	}

	private static Verdict check(Place place) {
		return place.check(Situation.of(Chat.SENDER, SENDER).and(Chat.RECIPIENT, RECIPIENT));
	}

	private static Verdict check(DeferredPlace place) {
		return place.check(Situation.of(Chat.SENDER, SENDER).and(Chat.RECIPIENT, RECIPIENT)).toCompletableFuture().join();
	}

	private static Verdict.Failed assertFailed(String path, String condition, Verdict verdict) {
		Verdict.Failed failed = assertInstanceOf(Verdict.Failed.class, verdict);
		assertEquals(path, failed.path());
		assertEquals(condition, failed.condition());

		return failed;
	}

	// A permission type that answers later, registered under the name an immediate one had.
	private static final class Later implements ConditionType.Deferred.Single<Player, Chat.Permission.Settings> {
		public @NotNull String name() {
			return "permission";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<Chat.Permission.Settings> settings() {
			return Chat.Permission.Settings.class;
		}

		public @NotNull CompletionStage<Outcome> holds(@NotNull Player player, Chat.Permission.@NotNull Settings settings) {
			return Outcome.pass().completed();
		}
	}
}
