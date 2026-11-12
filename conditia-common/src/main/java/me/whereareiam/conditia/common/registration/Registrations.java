package me.whereareiam.conditia.common.registration;

import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.model.Names;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The types and named lists registered now, with the owner of each. Names without an owner are the host's; an
 * owner's carry its prefix.
 */
public final class Registrations {
	private static final Set<String> RESERVED = Set.of("any", "not", "of");

	private final Map<String, Entry<ConditionType<?>>> types = new LinkedHashMap<>();
	private final Map<String, Entry<Requirements>> lists = new LinkedHashMap<>();

	/**
	 * Registers a type.
	 *
	 * @param owner the owner, empty for the host
	 * @param type  the type
	 * @return the name configs use
	 */
	public synchronized @NotNull String type(@NotNull Optional<String> owner, @NotNull ConditionType<?> type) {
		String name = name(owner, type.name());
		if (RESERVED.contains(name)) throw new IllegalArgumentException("`" + name + "` is a word of Conditia's own");
		if (types.containsKey(name)) throw new IllegalStateException("A type named " + name + " is registered already");

		types.put(name, new Entry<>(owner, type));

		return name;
	}

	/**
	 * Registers a named list, replacing the one of that name.
	 *
	 * @param owner    the owner, empty for the host
	 * @param name     the list's name
	 * @param requires its conditions
	 * @return the name configs use
	 */
	public synchronized @NotNull String list(@NotNull Optional<String> owner, @NotNull String name, @NotNull Requirements requires) {
		String full = name(owner, name);
		lists.put(full, new Entry<>(owner, requires));

		return full;
	}

	/**
	 * Removes a named list.
	 *
	 * @param owner the owner, empty for the host
	 * @param name  the list's name
	 */
	public synchronized void forget(@NotNull Optional<String> owner, @NotNull String name) {
		lists.remove(name(owner, name));
	}

	/**
	 * Removes everything an owner registered.
	 *
	 * @param owner the owner
	 */
	public synchronized void remove(@NotNull String owner) {
		Optional<String> of = Optional.of(owner);

		types.values().removeIf(entry -> entry.owner().equals(of));
		lists.values().removeIf(entry -> entry.owner().equals(of));
	}

	/**
	 * What is registered now.
	 *
	 * @return a snapshot that does not change
	 */
	public synchronized @NotNull Snapshot snapshot() {
		Map<String, ConditionType<?>> typeSnapshot = new LinkedHashMap<>();
		types.forEach((name, entry) -> typeSnapshot.put(name, entry.value()));

		Map<String, Requirements> listSnapshot = new LinkedHashMap<>();
		lists.forEach((name, entry) -> listSnapshot.put(name, entry.value()));

		return new Snapshot(Map.copyOf(typeSnapshot), Map.copyOf(listSnapshot));
	}

	private static String name(Optional<String> owner, String name) {
		if (!Names.valid(name)) throw new IllegalArgumentException("A name is lower case letters, digits and dashes: " + name);

		return owner.map(prefix -> prefix + ":" + name).orElse(name);
	}

	/**
	 * Types and named lists at one moment.
	 *
	 * @param types the types by the names configs use
	 * @param lists the named lists by the names configs use
	 */
	public record Snapshot(@NotNull Map<String, ConditionType<?>> types, @NotNull Map<String, Requirements> lists) {
	}

	private record Entry<V>(Optional<String> owner, V value) {
	}
}
