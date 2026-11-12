package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The values of a place's parts for one check. A situation never changes; {@link #and(Part, Object)} returns a new
 * one.
 *
 * <pre>{@code
 * Situation situation = Situation.of(Chat.SENDER, sender).and(Chat.RECIPIENT, recipient);
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Situation {
	private final @NotNull List<Value<?>> values;

	/**
	 * Starts a situation with one part's value.
	 *
	 * @param part  the part
	 * @param value its value
	 * @param <T>   the part's type
	 * @return the situation
	 */
	public static <T> @NotNull Situation of(@NotNull Part<T> part, @NotNull T value) {
		return new Situation(List.of(new Value<>(part, value)));
	}

	/**
	 * Adds a part's value.
	 *
	 * @param part  the part
	 * @param value its value
	 * @param <T>   the part's type
	 * @return a new situation with the value
	 * @throws IllegalArgumentException when the situation already has a value for the part
	 */
	public <T> @NotNull Situation and(@NotNull Part<T> part, @NotNull T value) {
		if (find(part).isPresent()) throw new IllegalArgumentException("The situation already has " + part);

		List<Value<?>> extended = new ArrayList<>(values);
		extended.add(new Value<>(part, value));

		return new Situation(List.copyOf(extended));
	}

	/**
	 * A part's value.
	 *
	 * @param part the part
	 * @param <T>  the part's type
	 * @return its value
	 * @throws IllegalArgumentException when the situation has no value for the part
	 */
	public <T> @NotNull T get(@NotNull Part<T> part) {
		return find(part).orElseThrow(() -> new IllegalArgumentException("The situation has no " + part));
	}

	/**
	 * Tells whether the situation has a value for a part.
	 *
	 * @param part the part
	 * @return whether it has one
	 */
	public boolean has(@NotNull Part<?> part) {
		return find(part).isPresent();
	}

	private <T> Optional<T> find(Part<T> part) {
		return values.stream()
				.filter(value -> value.part().equals(part))
				.findFirst()
				.map(value -> part.getType().cast(value.value()));
	}

	private record Value<T>(Part<T> part, T value) {
	}
}
