package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A typed part of a place, such as the sender of a chat message or the target of a command. A host declares its
 * parts once and names them in configs: a part's name opens a block of conditions about it
 * ({@code recipient: [...]}), and {@code of} picks it for one condition.
 *
 * <pre>{@code
 * public static final Part<Player> SENDER = Part.of("sender", Player.class);
 * public static final Part<Player> RECIPIENT = Part.of("recipient", Player.class);
 * }</pre>
 *
 * @param <T> the type of the part's value
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Part<T> {
	/**
	 * The name configs use, in lower case with dashes.
	 */
	private final @NotNull String name;

	/**
	 * The class of the part's value.
	 */
	private final @NotNull Class<T> type;

	/**
	 * Declares a part.
	 *
	 * @param name the name configs use, lower case letters, digits and dashes
	 * @param type the class of its value
	 * @param <T>  the type of its value
	 * @return the part
	 * @throws IllegalArgumentException when the name is not lower case letters, digits and dashes
	 */
	public static <T> @NotNull Part<T> of(@NotNull String name, @NotNull Class<T> type) {
		if (!Names.valid(name)) throw new IllegalArgumentException("A part's name is lower case letters, digits and dashes: " + name);

		return new Part<>(name, type);
	}

	@Override
	public String toString() {
		return name + " (" + type.getSimpleName() + ")";
	}
}
