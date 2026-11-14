package me.whereareiam.conditia.common;

import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.model.Line;
import me.whereareiam.conditia.model.NoSettings;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.result.Reason;
import me.whereareiam.conditia.testing.TestSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A small chat host for tests: players with permissions and a position, and the types Socialismus would register.
 */
final class Chat {
	static final Part<Player> SENDER = Part.of("sender", Player.class);
	static final Part<Player> RECIPIENT = Part.of("recipient", Player.class);
	static final Part<Message> MESSAGE = Part.of("message", Message.class);

	static final TestSettings SETTINGS = new TestSettings()
			.read(Permission.Settings.class, node -> new Permission.Settings(TestSettings.texts(node, "nodes")))
			.read(Nearby.Settings.class, node -> new Nearby.Settings(Integer.parseInt(TestSettings.text(node, "blocks"))))
			.read(Owns.Settings.class, node -> new Owns.Settings(TestSettings.texts(node, "products")))
			.read(Broken.Settings.class, node -> new Broken.Settings(Boolean.parseBoolean(TestSettings.text(node, "late"))));

	private Chat() {
	}

	record Player(String name, Set<String> permissions, int position, Set<String> products) {
		Player(String name, Set<String> permissions, int position) {
			this(name, permissions, position, Set.of());
		}
	}

	record Message(String text) {
	}

	static final class Permission implements ConditionType.Single<Player, Permission.Settings> {
		record Settings(List<String> nodes) {
		}

		record Missing(List<String> nodes) implements Reason {
		}

		record Held(List<String> nodes) implements Reason {
			public @NotNull Map<String, String> values() {
				return Map.of("nodes", String.join(", ", nodes));
			}
		}

		public @NotNull String name() {
			return "permission";
		}

		public @NotNull Set<Class<? extends Reason>> reasons() {
			return Set.of(Missing.class, Held.class);
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<Settings> settings() {
			return Settings.class;
		}

		public @NotNull Outcome holds(@NotNull Player player, @NotNull Settings settings) {
			return player.permissions().containsAll(settings.nodes())
					? Outcome.pass(new Held(settings.nodes()))
					: Outcome.fail(new Missing(settings.nodes()));
		}
	}

	static final class Nearby implements ConditionType.Pair<Player, Player, Nearby.Settings> {
		record Settings(int blocks) {
		}

		public @NotNull String name() {
			return "nearby";
		}

		public @NotNull Class<Player> first() {
			return Player.class;
		}

		public @NotNull Class<Player> second() {
			return Player.class;
		}

		public @NotNull Class<Settings> settings() {
			return Settings.class;
		}

		public @NotNull Outcome holds(@NotNull Player from, @NotNull Player to, @NotNull Settings settings) {
			return Outcome.when(Math.abs(from.position() - to.position()) <= settings.blocks());
		}
	}

	// Holds when the first player stands after the second; tells which way round the parts came.
	static final class Ahead implements ConditionType.Pair<Player, Player, NoSettings> {
		public @NotNull String name() {
			return "ahead";
		}

		public @NotNull Class<Player> first() {
			return Player.class;
		}

		public @NotNull Class<Player> second() {
			return Player.class;
		}

		public @NotNull Class<NoSettings> settings() {
			return NoSettings.class;
		}

		public @NotNull Outcome holds(@NotNull Player first, @NotNull Player second, @NotNull NoSettings settings) {
			return Outcome.when(first.position() > second.position());
		}
	}

	// Answers later, as a type asking a shop's store would.
	static final class Owns implements ConditionType.Deferred.Single<Player, Owns.Settings> {
		record Settings(List<String> products) {
		}

		record MissingProducts(List<String> products) implements Reason {
			public @NotNull Map<String, String> values() {
				return Map.of("products", String.join(", ", products));
			}
		}

		record Owned(List<String> products) implements Reason {
		}

		public @NotNull String name() {
			return "owns";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<Settings> settings() {
			return Settings.class;
		}

		public @NotNull Set<Class<? extends Reason>> reasons() {
			return Set.of(MissingProducts.class, Owned.class);
		}

		public @NotNull CompletionStage<Outcome> holds(@NotNull Player player, @NotNull Settings settings) {
			List<String> missing = settings.products().stream().filter(product -> !player.products().contains(product)).toList();

			return (missing.isEmpty() ? Outcome.pass(new Owned(settings.products())) : Outcome.fail(new MissingProducts(missing))).completed();
		}
	}

	static final class Broken implements ConditionType.Deferred.Single<Player, Broken.Settings> {
		record Settings(boolean late) {
		}

		public @NotNull String name() {
			return "broken";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<Settings> settings() {
			return Settings.class;
		}

		public @NotNull CompletionStage<Outcome> holds(@NotNull Player player, @NotNull Settings settings) {
			if (!settings.late()) throw new IllegalStateException("thrown");

			return CompletableFuture.failedStage(new IllegalStateException("completed exceptionally"));
		}
	}

	static final class Faulty implements ConditionType.Single<Player, NoSettings> {
		public @NotNull String name() {
			return "faulty";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<NoSettings> settings() {
			return NoSettings.class;
		}

		public @NotNull Outcome holds(@NotNull Player player, @NotNull NoSettings settings) {
			throw new IllegalStateException("thrown at once");
		}
	}

	// About no part: the chat is closed for everyone.
	static final class Closed implements ConditionType.Plain<NoSettings> {
		public @NotNull String name() {
			return "closed";
		}

		public @NotNull Class<NoSettings> settings() {
			return NoSettings.class;
		}

		public @NotNull Outcome holds(@NotNull NoSettings settings) {
			return Outcome.fail();
		}
	}

	// Holds until the player used its line once: the use is recorded only when the host records the verdict.
	static final class Once implements ConditionType.Recording<Player, NoSettings> {
		final Set<String> used = new java.util.HashSet<>();

		public @NotNull String name() {
			return "once";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<NoSettings> settings() {
			return NoSettings.class;
		}

		public @NotNull Outcome holds(@NotNull Player player, @NotNull NoSettings settings, @NotNull Line line) {
			return Outcome.when(!used.contains(player.name() + "@" + line.getPath()));
		}

		public void record(@NotNull Player player, @NotNull NoSettings settings, @NotNull Line line) {
			used.add(player.name() + "@" + line.getPath());
		}
	}
}
