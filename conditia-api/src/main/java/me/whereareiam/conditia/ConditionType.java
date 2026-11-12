package me.whereareiam.conditia;

import me.whereareiam.conditia.result.Outcome;
import me.whereareiam.conditia.result.Reason;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * A kind of condition that configs can name, such as {@code permission} or {@code nearby}. A type is a class
 * implementing one of three shapes, chosen by how many parts of a place it is about:
 *
 * <ul>
 *   <li>{@link Plain}: about no part, such as {@code time} or {@code chance};</li>
 *   <li>{@link Single}: about one part, such as {@code permission} of a player or {@code channel-type} of an
 *       interaction;</li>
 *   <li>{@link Pair}: about two parts, such as {@code nearby} for a sender and a recipient.</li>
 * </ul>
 *
 * <p>Conditia decides at binding which parts a condition is about, so a type's code gets them as plain typed
 * parameters:</p>
 *
 * <pre>{@code
 * public final class PermissionCondition implements ConditionType.Single<Player, PermissionCondition.Settings> {
 *     public record Settings(List<String> nodes) {}
 *
 *     public String name() { return "permission"; }
 *     public Class<Player> subject() { return Player.class; }
 *     public Class<Settings> settings() { return Settings.class; }
 *
 *     public CompletionStage<Outcome> holds(Player player, Settings settings) {
 *         return Outcome.when(settings.nodes().stream().allMatch(player::hasPermission)).completed();
 *     }
 * }
 * }</pre>
 *
 * @param <S> the settings a condition of this type is written with, usually a record
 */
public sealed interface ConditionType<S> permits ConditionType.Plain, ConditionType.Single, ConditionType.Pair {
	/**
	 * The name configs use for this type, in lower case with dashes, such as {@code channel-type}. A type registered
	 * through an owner's scope is known as {@code <owner>:<name>}.
	 *
	 * @return the type's name
	 */
	@NotNull String name();

	/**
	 * The class a condition's settings are read into. A bare value in a config ({@code permission: chat.global})
	 * fills the settings' only component, or the one {@link #main()} names. A type that takes no settings returns
	 * {@code NoSettings.class}; its value then names the parts it is about.
	 *
	 * @return the settings class
	 */
	@NotNull Class<S> settings();

	/**
	 * The component of the settings that a bare value fills, for settings with more than one component.
	 *
	 * @return the component's name, or empty when the settings have a single component or take no bare value
	 */
	default @NotNull Optional<String> main() {
		return Optional.empty();
	}

	/**
	 * The reasons this type may fail with, so a host can check at startup that each has a text in every language.
	 *
	 * @return the reason classes, empty when the type fails without a reason
	 * @see Reason#key(String, Class)
	 */
	default @NotNull Set<Class<? extends Reason>> reasons() {
		return Set.of();
	}

	/**
	 * A type about no part of a place.
	 *
	 * @param <S> the settings
	 */
	non-sealed interface Plain<S> extends ConditionType<S> {
		/**
		 * Tells whether the condition holds now.
		 *
		 * @param settings the condition's settings
		 * @return a stage that completes with the outcome; completing exceptionally makes the check broken
		 */
		@NotNull CompletionStage<Outcome> holds(@NotNull S settings);
	}

	/**
	 * A type about one part of a place, such as a player or an interaction.
	 *
	 * @param <T> the type of the part
	 * @param <S> the settings
	 */
	non-sealed interface Single<T, S> extends ConditionType<S> {
		/**
		 * The class of the part this type is about; a part fits when its type is this class or a subtype.
		 *
		 * @return the subject class
		 */
		@NotNull Class<T> subject();

		/**
		 * Tells whether the condition holds for the subject.
		 *
		 * @param subject  the part's value in the situation being checked
		 * @param settings the condition's settings
		 * @return a stage that completes with the outcome; completing exceptionally makes the check broken
		 */
		@NotNull CompletionStage<Outcome> holds(@NotNull T subject, @NotNull S settings);
	}

	/**
	 * A type about two parts of a place, such as a sender and a recipient.
	 *
	 * @param <A> the type of the first part
	 * @param <B> the type of the second part
	 * @param <S> the settings
	 */
	non-sealed interface Pair<A, B, S> extends ConditionType<S> {
		/**
		 * The class of the first part.
		 *
		 * @return the first part's class
		 */
		@NotNull Class<A> first();

		/**
		 * The class of the second part.
		 *
		 * @return the second part's class
		 */
		@NotNull Class<B> second();

		/**
		 * Tells whether the condition holds for the two parts.
		 *
		 * @param first    the first part's value
		 * @param second   the second part's value
		 * @param settings the condition's settings
		 * @return a stage that completes with the outcome; completing exceptionally makes the check broken
		 */
		@NotNull CompletionStage<Outcome> holds(@NotNull A first, @NotNull B second, @NotNull S settings);
	}
}
