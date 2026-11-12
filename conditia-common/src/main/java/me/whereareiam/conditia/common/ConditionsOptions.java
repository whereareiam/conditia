package me.whereareiam.conditia.common;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import me.whereareiam.conditia.SettingsReader;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * What a host tells Conditia. Yui's, for example:
 *
 * <pre>{@code
 * ConditionsOptions options = ConditionsOptions.builder()
 *         .reader(feature.settings())
 *         .namedLists(Optional.of("audience"))
 *         .build();
 * }</pre>
 */
@Getter
@Builder
public final class ConditionsOptions {
	/**
	 * Reads conditions' settings from config values.
	 */
	private final @NonNull SettingsReader reader;

	/**
	 * The host's word for a reference to a named list, such as {@code audience}; empty when the host has no named
	 * lists.
	 */
	@Builder.Default
	private final @NotNull Optional<String> namedLists = Optional.empty();

	/**
	 * Where places that can no longer be bound are logged.
	 */
	@Builder.Default
	private final @NotNull System.Logger logger = System.getLogger("Conditia");

	/**
	 * The clock of {@code time} and {@code date}, and its zone.
	 */
	@Builder.Default
	private final @NotNull Clock clock = Clock.systemDefaultZone();

	/**
	 * The source of {@code chance}.
	 */
	@Builder.Default
	private final @NotNull RandomGenerator random = RandomGenerator.getDefault();
}
