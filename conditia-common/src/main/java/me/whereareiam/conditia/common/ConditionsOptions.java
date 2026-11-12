package me.whereareiam.conditia.common;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.model.Wording;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * What a host tells Conditia. Yui's, for example:
 *
 * <pre>{@code
 * ConditionsOptions options = ConditionsOptions.builder()
 *         .reader(feature.settings())
 *         .namedLists(Optional.of("audience"))
 *         .wording(Wording.onFailure("denied"))
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
	 * The words a condition's line may carry a text under, such as {@code denied}; none by default.
	 */
	@Singular("wording")
	private final @NotNull List<Wording> wordings;

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
