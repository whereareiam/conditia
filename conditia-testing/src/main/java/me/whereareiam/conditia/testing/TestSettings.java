package me.whereareiam.conditia.testing;

import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.model.Node;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reads settings in tests without a config library: each settings class is read by a function given here.
 *
 * <pre>{@code
 * TestSettings settings = new TestSettings()
 *         .read(PermissionCondition.Settings.class, node -> new PermissionCondition.Settings(TestSettings.texts(node)));
 * }</pre>
 */
public final class TestSettings implements SettingsReader {
	private final Map<Class<?>, Function<Node, ?>> readers = new HashMap<>();

	/**
	 * Adds the function that reads one settings class.
	 *
	 * @param settings the settings class
	 * @param reader   reads the condition's value, bare or a mapping
	 * @param <S>      the settings type
	 * @return this reader
	 */
	public <S> @NotNull TestSettings read(@NotNull Class<S> settings, @NotNull Function<Node, S> reader) {
		readers.put(settings, reader);

		return this;
	}

	@Override
	public <S> @NotNull S read(@NotNull Class<S> settings, @NotNull Node value, @NotNull Optional<String> main) {
		Function<Node, ?> reader = readers.get(settings);
		if (reader == null) throw new IllegalArgumentException("No reader for " + settings.getSimpleName());

		return settings.cast(reader.apply(value));
	}

	/**
	 * The text of a bare scalar, or of the mapping's entry of that name.
	 *
	 * @param node the value
	 * @param key  the entry read from a mapping
	 * @return the text
	 * @throws IllegalArgumentException when there is no such text
	 */
	public static @NotNull String text(@NotNull Node node, @NotNull String key) {
		return switch (node) {
			case Node.Scalar scalar -> scalar.text();
			case Node.Mapping mapping when mapping.entries().get(key) instanceof Node.Scalar scalar -> scalar.text();
			default -> throw new IllegalArgumentException("Expected a text for " + key);
		};
	}

	/**
	 * The texts of a bare scalar or sequence, or of the mapping's entry of that name.
	 *
	 * @param node the value
	 * @param key  the entry read from a mapping
	 * @return the texts
	 * @throws IllegalArgumentException when there are no such texts
	 */
	public static @NotNull java.util.List<String> texts(@NotNull Node node, @NotNull String key) {
		return switch (node) {
			case Node.Scalar scalar -> java.util.List.of(scalar.text());
			case Node.Sequence sequence -> sequence.items().stream().map(item -> text(item, key)).toList();
			case Node.Mapping mapping when mapping.entries().containsKey(key) -> texts(mapping.entries().get(key), key);
			case Node.Mapping ignored -> throw new IllegalArgumentException("Expected texts for " + key);
		};
	}
}
