package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.common.registration.Registrations.Snapshot;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Node;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Binds requirements to a place's parts with what is registered at one moment. Every problem is a
 * {@link ConditionException} naming the config path.
 */
public final class Binder {
	private final Snapshot snapshot;
	private final Optional<String> word;
	private final TestBinding tests;

	/**
	 * Creates a binder.
	 *
	 * @param snapshot what is registered
	 * @param word     the host's word for named lists, if it uses them
	 * @param reader   reads conditions' settings
	 */
	public Binder(@NotNull Snapshot snapshot, @NotNull Optional<String> word, @NotNull SettingsReader reader) {
		this.snapshot = snapshot;
		this.word = word;
		this.tests = new TestBinding(reader);
	}

	/**
	 * Binds requirements.
	 *
	 * @param path     the requirements' path
	 * @param requires the requirements
	 * @param parts    the place's parts, the main participant first
	 * @param timing   when the place needs its answer
	 * @return the bound check
	 * @throws ConditionException when the requirements cannot be bound
	 */
	public @NotNull Check bind(@NotNull String path, @NotNull Requirements requires, @NotNull List<Part<?>> parts, @NotNull Timing timing) {
		checkParts(path, parts);
		Scope scope = new Scope(parts, new PartChoice(parts), timing);

		return list(path, requires.getConditions(), Focus.start(), scope);
	}

	private void checkParts(String path, List<Part<?>> parts) {
		if (parts.isEmpty()) return;

		Set<String> seen = new HashSet<>();
		for (Part<?> part : parts) {
			String name = part.getName();
			if (!seen.add(name)) throw new ConditionException(path, "two parts are named `" + name + "`");
			if (snapshot.types().containsKey(name) || word.filter(name::equals).isPresent() || Set.of("any", "not", "of").contains(name))
				throw new ConditionException(path, "the part `" + name + "` has the name of a condition");
		}
	}

	private Check list(String path, Node node, Focus focus, Scope scope) {
		return switch (node) {
			case Node.Sequence sequence -> {
				List<Check> checks = new ArrayList<>();
				for (int index = 0; index < sequence.items().size(); index++)
					checks.add(condition(path + "[" + index + "]", sequence.items().get(index), focus, scope));

				yield new Check.All(List.copyOf(checks));
			}
			case Node.Mapping mapping -> condition(path, mapping, focus, scope);
			case Node.Scalar scalar -> throw new ConditionException(path, "expected conditions, found \"" + scalar.text() + "\"");
		};
	}

	private Check condition(String path, Node node, Focus focus, Scope scope) {
		if (!(node instanceof Node.Mapping mapping) || mapping.entries().size() != 1)
			throw new ConditionException(path, "a condition is one `name: value` entry");

		Map.Entry<String, Node> entry = mapping.entries().firstEntry();
		String key = entry.getKey();
		String at = path + "." + key;

		return switch (key) {
			case "any" -> new Check.Any(at, items(list(at, entry.getValue(), focus, scope)));
			case "not" -> new Check.Not(at, list(at, entry.getValue(), focus, scope));
			default -> named(at, key, entry.getValue(), focus, scope);
		};
	}

	private static List<Check> items(Check check) {
		return check instanceof Check.All all ? all.checks() : List.of(check);
	}

	private Check named(String path, String key, Node value, Focus focus, Scope scope) {
		if (word.filter(key::equals).isPresent()) return reference(path, key, value, focus, scope);

		Optional<Part<?>> part = scope.parts().stream().filter(candidate -> candidate.getName().equals(key)).findFirst();
		if (part.isPresent()) return list(path, value, focus.about(part.get()), scope);

		return Optional.ofNullable(snapshot.types().get(key))
				.map(type -> tests.bind(path, key, type, value, focus, scope.choice(), scope.timing()))
				.orElseThrow(() -> new ConditionException(path, "no condition type or part is named `" + key + "`"));
	}

	private Check reference(String path, String key, Node value, Focus focus, Scope scope) {
		if (!(value instanceof Node.Scalar scalar)) throw new ConditionException(path, "`" + key + "` names one list");

		String name = scalar.text();
		Requirements list = snapshot.lists().get(name);
		if (list == null) throw new ConditionException(path, "no " + key + " is named `" + name + "`");
		if (focus.lists().contains(name)) throw new ConditionException(path, "the " + key + " `" + name + "` refers to itself");

		return new Check.Named(path, key + " " + name, list(path, list.getConditions(), focus.enter(name), scope));
	}

	private record Scope(List<Part<?>> parts, PartChoice choice, Timing timing) {
	}
}
