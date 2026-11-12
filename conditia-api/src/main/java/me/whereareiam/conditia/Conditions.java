package me.whereareiam.conditia;

import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A host's conditions: the types and named lists it knows, and the places where requirements are checked.
 * Registrations made here have plain names; owners register through {@link #scope(String)}.
 *
 * <pre>{@code
 * Place place = conditions.place("chat.global.requires", chat.getRequires(), Chat.SENDER, Chat.RECIPIENT);
 * place.check(Situation.of(Chat.SENDER, sender).and(Chat.RECIPIENT, recipient))
 *         .thenAccept(verdict -> ...);
 * }</pre>
 */
public interface Conditions extends ConditionRegistrar {
	/**
	 * Opens the scope of an owner.
	 *
	 * @param owner the owner's id, lower case letters, digits and dashes
	 * @return the owner's scope
	 * @throws IllegalStateException when the owner's scope is already open
	 */
	@NotNull ConditionScope scope(@NotNull String owner);

	/**
	 * Binds requirements to a place: reads every condition's settings and decides which parts each is about. A host
	 * creates its places after its owners have registered, so an unknown type stops startup.
	 *
	 * @param path     where the requirements stand in the host's config, such as {@code commands.clear.requires}; it
	 *                 begins every path in errors and verdicts
	 * @param requires the requirements
	 * @param parts    the parts every check of this place supplies; the first is the main participant
	 * @return the place, bound again after every change of registrations until it is closed
	 * @throws ConditionException when a condition names an unknown type, list or part, its settings cannot be read,
	 *                            no part fits it, or a part's name is also a type's name
	 */
	@NotNull Place place(@NotNull String path, @NotNull Requirements requires, @NotNull Part<?>... parts);

	/**
	 * The types registered now, by the names configs use.
	 *
	 * @return a snapshot of the types
	 */
	@NotNull Map<String, ConditionType<?>> types();
}
