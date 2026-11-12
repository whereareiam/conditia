package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@code requires:} list as read from a config, before it is bound to a place. A host's config model holds it;
 * {@code conditia-adapter-configura} reads it from a file.
 *
 * <pre>{@code
 * requires:
 *   - permission: chat.global
 *   - recipient:
 *       - permission: chat.receive
 *   - nearby: 50
 * }</pre>
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Requirements {
	private static final Requirements NONE = new Requirements(new Node.Sequence(List.of()));

	/**
	 * The list, a sequence of conditions all of which must hold.
	 */
	private final @NotNull Node.Sequence conditions;

	/**
	 * Requirements from a config value: a sequence, or one condition written without a list.
	 *
	 * @param node the value
	 * @return the requirements
	 * @throws IllegalArgumentException when the value is a scalar
	 */
	public static @NotNull Requirements of(@NotNull Node node) {
		return switch (node) {
			case Node.Sequence sequence -> new Requirements(sequence);
			case Node.Mapping mapping when mapping.entries().isEmpty() -> NONE;
			case Node.Mapping mapping -> new Requirements(new Node.Sequence(List.of(mapping)));
			case Node.Scalar scalar -> throw new IllegalArgumentException("Requirements are a list of conditions, not \"" + scalar.text() + "\"");
		};
	}

	/**
	 * Requirements that always hold.
	 *
	 * @return the empty list
	 */
	public static @NotNull Requirements none() {
		return NONE;
	}
}
