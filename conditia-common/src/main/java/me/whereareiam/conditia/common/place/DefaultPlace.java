package me.whereareiam.conditia.common.place;

import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.common.binding.Binder;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.common.evaluation.Evaluation;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A place and its current binding. When registrations change it is bound again; while it cannot be bound, it denies
 * every check with a {@link Verdict.Broken}.
 */
public final class DefaultPlace implements Place {
	private final String path;
	private final Requirements requires;
	private final List<Part<?>> parts;
	private final Places places;
	private volatile Binding binding;

	DefaultPlace(String path, Requirements requires, List<Part<?>> parts, Places places, Check check) {
		this.path = path;
		this.requires = requires;
		this.parts = parts;
		this.places = places;
		this.binding = new Binding.Bound(check);
	}

	@Override
	public @NotNull String path() {
		return path;
	}

	@Override
	public @NotNull CompletionStage<Verdict> check(@NotNull Situation situation) {
		for (Part<?> part : parts)
			if (!situation.has(part)) throw new IllegalArgumentException("The situation of " + path + " lacks " + part);

		return switch (binding) {
			case Binding.Bound bound -> Evaluation.run(bound.check(), situation);
			case Binding.Unbound unbound -> CompletableFuture.completedStage(new Verdict.Broken(path, path, unbound.error()));
		};
	}

	@Override
	public void close() {
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
			binding = new Binding.Bound(binder.bind(path, requires, parts));

			return Optional.empty();
		} catch (ConditionException exception) {
			binding = new Binding.Unbound(exception);
			boolean known = previous instanceof Binding.Unbound unbound && unbound.error().getMessage().equals(exception.getMessage());

			return known ? Optional.empty() : Optional.of(exception);
		}
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
