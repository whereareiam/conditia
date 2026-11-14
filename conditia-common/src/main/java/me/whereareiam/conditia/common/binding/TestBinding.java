package me.whereareiam.conditia.common.binding;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Line;
import me.whereareiam.conditia.model.Node;
import me.whereareiam.conditia.model.NoSettings;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Outcome;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Binds one condition of a registered type: separates {@code of} from the settings, reads the settings and chooses
 * the parts.
 */
@RequiredArgsConstructor
final class TestBinding {
	private final SettingsReader reader;

	<S> Check bind(String path, String name, ConditionType<S> type, Node value, Focus focus, PartChoice choice, Timing timing) {
		if (timing == Timing.IMMEDIATE && type instanceof ConditionType.Deferred<S>)
			throw new ConditionException(path, "`" + name + "` answers later, and this place needs its answer at once");

		Written written = type.settings() == NoSettings.class ? partsOnly(path, value) : written(path, value);
		S settings = settings(path, type, written.settings());

		return switch (type) {
			case ConditionType.Plain<S> plain -> {
				noParts(path, name, written);

				yield new Check.Test(path, name, List.of(), (Check.Immediate) situation -> plain.holds(settings));
			}
			case ConditionType.Single<?, S> single -> {
				Part<?> part = choice.single(path, single.subject(), written.of(), focus);

				yield new Check.Test(path, name, List.of(part), single(single, settings, part));
			}
			case ConditionType.Pair<?, ?, S> pair -> {
				List<Part<?>> parts = choice.pair(path, pair.first(), pair.second(), written.of(), focus);

				yield new Check.Test(path, name, parts, pair(pair, settings, parts));
			}
			case ConditionType.Recording<?, S> recording -> {
				Part<?> part = choice.single(path, recording.subject(), written.of(), focus);

				yield new Check.Test(path, name, List.of(part), recording(recording, settings, part, Line.of(path)));
			}
			case ConditionType.Deferred.Plain<S> plain -> {
				noParts(path, name, written);

				yield new Check.Test(path, name, List.of(), (Check.Deferred) situation -> plain.holds(settings));
			}
			case ConditionType.Deferred.Single<?, S> single -> {
				Part<?> part = choice.single(path, single.subject(), written.of(), focus);

				yield new Check.Test(path, name, List.of(part), single(single, settings, part));
			}
			case ConditionType.Deferred.Pair<?, ?, S> pair -> {
				List<Part<?>> parts = choice.pair(path, pair.first(), pair.second(), written.of(), focus);

				yield new Check.Test(path, name, parts, pair(pair, settings, parts));
			}
		};
	}

	private static void noParts(String path, String name, Written written) {
		if (!written.of().isEmpty()) throw new ConditionException(path, "`" + name + "` is about no part, so it takes no `of`");
	}

	private <S> S settings(String path, ConditionType<S> type, Node value) {
		if (type.settings() == NoSettings.class) return type.settings().cast(NoSettings.INSTANCE);

		try {
			return reader.read(type.settings(), value, type.main());
		} catch (IllegalArgumentException exception) {
			throw new ConditionException(path, "the settings cannot be read: " + exception.getMessage(), exception);
		}
	}

	private static <T, S> Check.Run single(ConditionType.Single<T, S> type, S settings, Part<?> part) {
		return (Check.Immediate) situation -> type.holds(type.subject().cast(situation.get(part)), settings);
	}

	private static <A, B, S> Check.Run pair(ConditionType.Pair<A, B, S> type, S settings, List<Part<?>> parts) {
		return (Check.Immediate) situation -> type.holds(
				type.first().cast(situation.get(parts.get(0))),
				type.second().cast(situation.get(parts.get(1))),
				settings
		);
	}

	private static <T, S> Check.Run recording(ConditionType.Recording<T, S> type, S settings, Part<?> part, Line line) {
		return new Check.Recording() {
			@Override
			public @NotNull Outcome holds(@NotNull Situation situation) {
				return type.holds(type.subject().cast(situation.get(part)), settings, line);
			}

			@Override
			public void record(@NotNull Situation situation) {
				type.record(type.subject().cast(situation.get(part)), settings, line);
			}
		};
	}

	private static <T, S> Check.Run single(ConditionType.Deferred.Single<T, S> type, S settings, Part<?> part) {
		return (Check.Deferred) situation -> type.holds(type.subject().cast(situation.get(part)), settings);
	}

	private static <A, B, S> Check.Run pair(ConditionType.Deferred.Pair<A, B, S> type, S settings, List<Part<?>> parts) {
		return (Check.Deferred) situation -> type.holds(
				type.first().cast(situation.get(parts.get(0))),
				type.second().cast(situation.get(parts.get(1))),
				settings
		);
	}

	// A type without settings: its value names the parts, or is empty for the default parts.
	private static Written partsOnly(String path, Node value) {
		return switch (value) {
			case Node.Scalar scalar when scalar.text().isEmpty() -> new Written(List.of(), value);
			case Node.Mapping mapping when mapping.entries().isEmpty() -> new Written(List.of(), value);
			case Node.Mapping mapping when mapping.entries().size() == 1 && mapping.entries().containsKey("of") ->
					new Written(names(path, mapping.entries().get("of")), Node.Mapping.empty());
			case Node.Mapping ignored -> throw new ConditionException(path, "this condition takes no settings, only `of`");
			default -> new Written(names(path, value), Node.Mapping.empty());
		};
	}

	private static Written written(String path, Node value) {
		if (!(value instanceof Node.Mapping mapping) || !mapping.entries().containsKey("of")) return new Written(List.of(), value);

		LinkedHashMap<String, Node> settings = new LinkedHashMap<>(mapping.entries());
		Node of = settings.remove("of");

		return new Written(names(path, of), new Node.Mapping(settings));
	}

	private static List<String> names(String path, Node node) {
		return switch (node) {
			case Node.Scalar scalar -> List.of(scalar.text());
			case Node.Sequence sequence -> sequence.items().stream()
					.map(item -> item instanceof Node.Scalar scalar ? scalar.text() : invalid(path))
					.toList();
			case Node.Mapping ignored -> List.of(invalid(path));
		};
	}

	private static String invalid(String path) {
		throw new ConditionException(path, "`of` names parts");
	}

	private record Written(List<String> of, Node settings) {
	}
}
