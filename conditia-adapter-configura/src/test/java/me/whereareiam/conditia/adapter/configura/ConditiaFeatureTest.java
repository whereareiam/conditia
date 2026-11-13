package me.whereareiam.conditia.adapter.configura;

import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.Conditions;
import me.whereareiam.conditia.common.ConditionsOptions;
import me.whereareiam.conditia.common.DefaultConditions;
import me.whereareiam.conditia.common.type.TimeCondition;
import me.whereareiam.conditia.model.Node;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static me.whereareiam.conditia.testing.TestRequirements.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditiaFeatureTest {
	private static final Part<Player> SENDER = Part.of("sender", Player.class);
	private static final Part<Player> RECIPIENT = Part.of("recipient", Player.class);

	private final Configura configura = Configura.builder()
			.format(Format.YAML)
			.feature(new ConditiaFeature())
			.build();
	private final ConfiguraSettings settings = new ConfiguraSettings(configura.mapper());

	@Test
	void requirements_AreReadFromAConfigAndChecked() {
		ChannelConfig config = read("""
				requires:
				  - permission: chat.global
				  - recipient:
				      - permission: [chat.receive]
				""");
		Conditions conditions = new DefaultConditions(ConditionsOptions.builder().reader(settings).build());
		conditions.register(new Permission());

		boolean passed = conditions.deferredPlace("chat.requires", config.requires, SENDER, RECIPIENT)
				.check(Situation.of(SENDER, new Player(Set.of("chat.global"))).and(RECIPIENT, new Player(Set.of("chat.receive"))))
				.toCompletableFuture()
				.join()
				.passed();

		assertTrue(passed);
	}

	@Test
	void requirements_WhenAbsent_AreNone() {
		assertEquals(Requirements.none(), read("name: global").requires);
	}

	@Test
	void requirements_AreWrittenWithTheirNumbers() {
		ChannelConfig config = read("requires: [nearby: 50]");

		String written = new String(configura.writeBytes(config), StandardCharsets.UTF_8);

		assertTrue(written.contains("nearby: 50"), written);
	}

	@Test
	void bareValue_FillsTheOnlyComponent() {
		assertEquals(new Permission.Settings(List.of("chat.global")), settings.read(Permission.Settings.class, node("chat.global"), Optional.empty()));
	}

	@Test
	void bareValue_FillsTheMainComponent() {
		Cooldown cooldown = settings.read(Cooldown.class, node("10s"), Optional.of("duration"));

		assertEquals(Duration.ofSeconds(10), cooldown.duration());
	}

	@Test
	void settings_ReadConfiguraTypes() {
		TimeCondition.Settings time = settings.read(TimeCondition.Settings.class, node("{ from: '08:00', to: '22:00' }"), Optional.empty());

		assertEquals(LocalTime.of(8, 0), time.from());
	}

	@Test
	void bareValue_ForSeveralComponentsWithoutMain_IsRefused() {
		assertThrows(IllegalArgumentException.class, () -> settings.read(Cooldown.class, node("10s"), Optional.empty()));
	}

	@Test
	void settings_ThatDoNotFit_AreRefused() {
		assertThrows(IllegalArgumentException.class, () -> settings.read(Cooldown.class, Node.Mapping.of("duration", new Node.Scalar("soon")), Optional.empty()));
	}

	@Test
	void bareValue_FillsTheOnlyPropertyOfAPlainClass_AsADowngradedRecordIs() {
		assertEquals(List.of("chat.global"), settings.read(Nodes.class, node("chat.global"), Optional.empty()).nodes);
	}

	@Test
	void plainSettings_TakeTheValueAsItIs() {
		assertFalse(settings.read(Boolean.class, node("false"), Optional.empty()));
	}

	private ChannelConfig read(String yaml) {
		return configura.read(yaml.getBytes(StandardCharsets.UTF_8), ChannelConfig.class);
	}

	public static final class ChannelConfig {
		public String name = "global";
		public Requirements requires = Requirements.none();
	}

	record Player(Set<String> permissions) {
	}

	record Cooldown(Duration duration, String group) {
	}

	// A record as a downgrade to an older Java leaves it: a final class whose one constructor takes its property.
	static final class Nodes {
		private final List<String> nodes;

		Nodes(List<String> nodes) {
			this.nodes = nodes;
		}
	}

	static final class Permission implements ConditionType.Single<Player, Permission.Settings> {
		record Settings(List<String> nodes) {
		}

		public @NotNull String name() {
			return "permission";
		}

		public @NotNull Class<Player> subject() {
			return Player.class;
		}

		public @NotNull Class<Settings> settings() {
			return Settings.class;
		}

		public @NotNull Outcome holds(@NotNull Player player, @NotNull Settings settings) {
			return Outcome.when(player.permissions().containsAll(settings.nodes()));
		}
	}
}
