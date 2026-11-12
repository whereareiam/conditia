package me.whereareiam.conditia.common;

import me.whereareiam.conditia.ConditionScope;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.Conditions;
import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.DeferredPlace;
import me.whereareiam.conditia.common.binding.Binder;
import me.whereareiam.conditia.common.place.Places;
import me.whereareiam.conditia.common.registration.Registrations;
import me.whereareiam.conditia.common.type.ChanceCondition;
import me.whereareiam.conditia.common.type.DateCondition;
import me.whereareiam.conditia.common.type.TimeCondition;
import me.whereareiam.conditia.model.Names;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Wording;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Conditia's standard {@link Conditions}. It knows {@code time}, {@code date} and {@code chance} from the start;
 * every registration, removal and closed scope binds all places again, one change at a time.
 *
 * <pre>{@code
 * Conditions conditions = new DefaultConditions(options);
 * conditions.register(new PermissionCondition());
 * }</pre>
 */
public final class DefaultConditions implements Conditions {
	private final ConditionsOptions options;
	private final Registrations registrations;
	private final Places places;
	private final OwnedRegistrar host;
	private final Set<String> owners = new HashSet<>();

	/**
	 * Creates the conditions of a host.
	 *
	 * @param options what the host tells Conditia
	 */
	public DefaultConditions(@NotNull ConditionsOptions options) {
		this.options = options;
		this.registrations = new Registrations(words(options));
		this.places = new Places(options.getLogger());
		this.host = new OwnedRegistrar(Optional.empty(), this);

		host.register(new TimeCondition(options.getClock()));
		host.register(new DateCondition(options.getClock()));
		host.register(new ChanceCondition(options.getRandom()));
	}

	@Override
	public @NotNull String register(@NotNull ConditionType<?> type) {
		return host.register(type);
	}

	@Override
	public @NotNull String name(@NotNull String name, @NotNull Requirements requires) {
		return host.name(name, requires);
	}

	@Override
	public void forget(@NotNull String name) {
		host.forget(name);
	}

	@Override
	public synchronized @NotNull ConditionScope scope(@NotNull String owner) {
		if (!Names.valid(owner)) throw new IllegalArgumentException("An owner's id is lower case letters, digits and dashes: " + owner);
		if (!owners.add(owner)) throw new IllegalStateException("The scope of " + owner + " is open already");

		return new OwnerScope(owner, this);
	}

	@Override
	public synchronized @NotNull DeferredPlace deferredPlace(@NotNull String path, @NotNull Requirements requires, @NotNull Part<?>... parts) {
		return places.openDeferred(binder(), path, requires, List.of(parts));
	}

	@Override
	public synchronized @NotNull Place place(@NotNull String path, @NotNull Requirements requires, @NotNull Part<?>... parts) {
		return places.open(binder(), path, requires, List.of(parts));
	}

	@Override
	public @NotNull Map<String, ConditionType<?>> types() {
		return registrations.snapshot().types();
	}

	Registrations registrations() {
		return registrations;
	}

	synchronized String change(Supplier<String> change) {
		String name = change.get();
		places.rebind(binder());

		return name;
	}

	synchronized void close(String owner) {
		registrations.remove(owner);
		owners.remove(owner);
		places.rebind(binder());
	}

	private Binder binder() {
		return new Binder(registrations.snapshot(), options.getNamedLists(), words(options), options.getReader());
	}

	private static Set<String> words(ConditionsOptions options) {
		return options.getWordings().stream().map(Wording::getWord).collect(Collectors.toUnmodifiableSet());
	}
}
