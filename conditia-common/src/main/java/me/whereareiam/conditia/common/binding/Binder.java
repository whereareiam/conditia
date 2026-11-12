package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.common.registration.Registrations;
import me.whereareiam.conditia.common.registration.Registrations.Snapshot;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Node;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Binds requirements to a place's parts with what is registered at one moment. Every problem is a
 * {@link ConditionException} naming the config path.
 */
public final class Binder {
	private final Snapshot snapshot;
	private final Optional<String> word;
	private final Set<String> words;
	private final TestBinding tests;

	/**
	 * Creates a binder.
	 *
	 * @param snapshot what is registered
	 * @param word     the host's word for named lists, if it uses them
	 * @param words    the host's words a line may carry a text under
	 * @param reader   reads conditions' settings
	 */
	public Binder(@NotNull Snapshot snapshot, @NotNull Optional<String> word, @NotNull Set<String> words, @NotNull SettingsReader reader) {
		this.snapshot = snapshot;
		this.word = word;
		this.words = words;
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
			if (snapshot.types().containsKey(name) || word.filter(name::equals).isPresent() || Registrations.RESERVED.contains(name) || words.contains(name))
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
		if (!(node instanceof Node.Mapping mapping)) throw new ConditionException(path, "a condition is one `name: value` entry");

		SequencedMap<String, String> wording = new LinkedHashMap<>();
		List<Map.Entry<String, Node>> conditions = new ArrayList<>();
		mapping.entries().forEach((key, value) -> {
			if (words.contains(key)) wording.put(key, text(path + "." + key, value));
			else conditions.add(Map.entry(key, value));
		});
		if (conditions.size() != 1) throw new ConditionException(path, "a condition is one `name: value` entry" + wordsHint());

		Map.Entry<String, Node> entry = conditions.getFirst();
		Check check = condition(path + "." + entry.getKey(), entry, focus, scope);

		return wording.isEmpty() ? check : new Check.Worded(path, wording, check);
	}

	private static String text(String path, Node value) {
		if (value instanceof Node.Scalar(String text)) return text;

		throw new ConditionException(path, "expected a text");
	}

	private String wordsHint() {
		if (words.isEmpty()) return "";

		return ", with texts under " + words.stream().sorted().map(each -> "`" + each + "`").collect(Collectors.joining(", "));
	}

	private Check condition(String at, Map.Entry<String, Node> entry, Focus focus, Scope scope) {
		String key = entry.getKey();

		return switch (key) {
			case "any" -> {
				List<Check> checks = items(list(at, entry.getValue(), focus, scope));

				yield new Check.Any(at, about(checks, scope), checks);
			}
			case "not" -> {
				Check check = list(at, entry.getValue(), focus, scope);

				yield new Check.Not(at, about(List.of(check), scope), check);
			}
			default -> named(at, key, entry.getValue(), focus, scope);
		};
	}

	// The parts the conditions under an `any` or a `not` are about, in the place's order.
	private static List<Part<?>> about(List<Check> checks, Scope scope) {
		Set<Part<?>> parts = new HashSet<>();
		checks.forEach(check -> collect(check, parts));

		return scope.parts().stream().filter(parts::contains).toList();
	}

	private static void collect(Check check, Set<Part<?>> parts) {
		switch (check) {
			case Check.All all -> all.checks().forEach(each -> collect(each, parts));
			case Check.Any any -> parts.addAll(any.parts());
			case Check.Worded worded -> collect(worded.check(), parts);
			case Check.Not not -> parts.addAll(not.parts());
			case Check.Test test -> parts.addAll(test.parts());
		}
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

		return list(path, list.getConditions(), focus.enter(name), scope);
	}

	private record Scope(List<Part<?>> parts, PartChoice choice, Timing timing) {
	}
}
