package me.whereareiam.conditia.adapter.chassis;

import me.whereareiam.chassis.ExtensionContext;
import me.whereareiam.chassis.model.ExtensionDescriptor;
import me.whereareiam.chassis.model.Version;
import me.whereareiam.chassis.service.Services;
import me.whereareiam.conditia.ConditionScope;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.Conditions;
import me.whereareiam.conditia.common.ConditionsOptions;
import me.whereareiam.conditia.common.DefaultConditions;
import me.whereareiam.conditia.model.NoSettings;
import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.testing.TestSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionConditionsTest {
	private final Conditions conditions = new DefaultConditions(ConditionsOptions.builder()
			.reader(new TestSettings())
			.build());

	@Test
	void open_NamesTheScopeAfterTheExtension_AndClosesItWithTheExtension() throws Exception {
		Context context = new Context();

		ConditionScope scope = new ExtensionConditions(conditions).open(context);
		String name = scope.register(new Verified());

		assertEquals("verification:verified", name);
		assertTrue(conditions.types().containsKey(name));

		context.stop();
		assertFalse(conditions.types().containsKey(name));
	}

	@Test
	void open_AfterTheExtensionStopped_OpensItAgain() throws Exception {
		Context first = new Context();
		new ExtensionConditions(conditions).open(first);
		first.stop();

		ConditionScope scope = new ExtensionConditions(conditions).open(new Context());

		assertEquals("verification", scope.owner());
	}

	private static final class Verified implements ConditionType.Plain<NoSettings> {
		public @NotNull String name() {
			return "verified";
		}

		public @NotNull Class<NoSettings> settings() {
			return NoSettings.class;
		}

		public @NotNull Outcome holds(@NotNull NoSettings settings) {
			return Outcome.pass();
		}
	}

	private static final class Context implements ExtensionContext {
		private final List<AutoCloseable> resources = new ArrayList<>();

		public @NotNull ExtensionDescriptor descriptor() {
			return ExtensionDescriptor.builder()
					.id("verification")
					.name("Verification")
					.version(Version.parse("1.0.0"))
					.main("example.Verification")
					.build();
		}

		public @NotNull Path dataFolder() {
			return Path.of("verification");
		}

		public @NotNull System.Logger logger() {
			return System.getLogger("verification");
		}

		public @NotNull Services services() {
			throw new UnsupportedOperationException();
		}

		public void onClose(@NotNull AutoCloseable resource) {
			resources.add(resource);
		}

		void stop() throws Exception {
			for (AutoCloseable resource : resources)
				resource.close();
		}
	}
}
