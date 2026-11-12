package me.whereareiam.conditia.common.place;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.DeferredPlace;
import me.whereareiam.conditia.common.binding.Binder;
import me.whereareiam.conditia.common.binding.Timing;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The open places. They are bound when created and again after every change of registrations; a place that can no
 * longer be bound is logged once with its problem, and once more when it works again.
 */
@RequiredArgsConstructor
public final class Places {
	private final Set<BoundPlace> places = ConcurrentHashMap.newKeySet();
	private final System.Logger logger;

	/**
	 * Binds requirements to a new place that waits for types answering later.
	 *
	 * @param binder   binds with what is registered now
	 * @param path     the requirements' path
	 * @param requires the requirements
	 * @param parts    the place's parts
	 * @return the place
	 * @throws ConditionException when the requirements cannot be bound
	 */
	public @NotNull DeferredPlace openDeferred(@NotNull Binder binder, @NotNull String path, @NotNull Requirements requires, @NotNull List<Part<?>> parts) {
		DefaultDeferredPlace place = new DefaultDeferredPlace(path, requires, parts, this, binder.bind(path, requires, parts, Timing.DEFERRED));
		places.add(place);

		return place;
	}

	/**
	 * Binds requirements to a new place that needs its answer at once.
	 *
	 * @param binder   binds with what is registered now
	 * @param path     the requirements' path
	 * @param requires the requirements
	 * @param parts    the place's parts
	 * @return the place
	 * @throws ConditionException when the requirements cannot be bound, or name a type that answers later
	 */
	public @NotNull Place open(@NotNull Binder binder, @NotNull String path, @NotNull Requirements requires, @NotNull List<Part<?>> parts) {
		DefaultPlace place = new DefaultPlace(path, requires, parts, this, binder.bind(path, requires, parts, Timing.IMMEDIATE));
		places.add(place);

		return place;
	}

	/**
	 * Binds every place again.
	 *
	 * @param binder binds with what is registered now
	 */
	public void rebind(@NotNull Binder binder) {
		for (BoundPlace place : places) {
			boolean wasBound = place.bound();
			Optional<ConditionException> problem = place.rebind(binder);

			problem.ifPresent(exception -> logger.log(System.Logger.Level.WARNING, "Denying everyone at " + exception.getMessage()));
			if (!wasBound && place.bound()) logger.log(System.Logger.Level.INFO, place.path() + " works again");
		}
	}

	void remove(BoundPlace place) {
		places.remove(place);
	}
}
