package me.whereareiam.conditia.common.place;

import me.whereareiam.conditia.common.binding.Binder;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.common.binding.Timing;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.Function;

/**
 * A place and its current binding. When registrations change it is bound again; while it cannot be bound, it denies
 * every check with a {@link Verdict.Broken}.
 */
abstract sealed class BoundPlace permits DefaultDeferredPlace, DefaultPlace {
	private final String path;
	private final Requirements requires;
	private final List<Part<?>> parts;
	private final Timing timing;
	private final Places places;
	private volatile Binding binding;

	BoundPlace(String path, Requirements requires, List<Part<?>> parts, Timing timing, Places places, Check check) {
		this.path = path;
		this.requires = requires;
		this.parts = parts;
		this.timing = timing;
		this.places = places;
		this.binding = new Binding.Bound(check);
	}

	/**
	 * Checks the situation with the current binding.
	 *
	 * @param situation the values of the place's parts
	 * @param bound     runs the bound check
	 * @param unbound   wraps the verdict of a place that cannot be bound
	 * @return the verdict, as the place gives it
	 */
	<R> R check(Situation situation, Function<Check, R> bound, Function<Verdict, R> unbound) {
		for (Part<?> part : parts)
			if (!situation.has(part)) throw new IllegalArgumentException("The situation of " + path + " lacks " + part);

		return switch (binding) {
			case Binding.Bound current -> bound.apply(current.check());
			case Binding.Unbound current -> unbound.apply(new Verdict.Broken(path, path, current.error()));
		};
	}

	String path() {
		return path;
	}

	void forget() {
		places.remove(this);
	}

	/**
	 * Binds the place again.
	 *
	 * @param binder binds with what is registered now
	 * @return the problem when it cannot be bound and could before, or a different one than before
	 */
	Optional<ConditionException> rebind(Binder binder) {
		Binding previous = binding;
		try {
			binding = new Binding.Bound(binder.bind(path, requires, parts, timing));

			return Optional.empty();
		} catch (ConditionException exception) {
			binding = new Binding.Unbound(exception);
			boolean known = previous instanceof Binding.Unbound unbound && unbound.error().getMessage().equals(exception.getMessage());

			return known ? Optional.empty() : Optional.of(exception);
		}
	}

	SequencedMap<String, String> wording() {
		return switch (binding) {
			case Binding.Bound current -> Collections.unmodifiableSequencedMap(Check.wording(current.check()));
			case Binding.Unbound ignored -> Collections.unmodifiableSequencedMap(new LinkedHashMap<>());
		};
	}

	boolean bound() {
		return binding instanceof Binding.Bound;
	}

	private sealed interface Binding {
		record Bound(Check check) implements Binding {
		}

		record Unbound(ConditionException error) implements Binding {
		}
	}
}
