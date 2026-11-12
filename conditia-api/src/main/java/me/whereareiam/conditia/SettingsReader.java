package me.whereareiam.conditia;

import me.whereareiam.conditia.model.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Reads a condition's settings from its config value. A host gives Conditia the reader of its config library;
 * {@code conditia-adapter-configura} has Configura's.
 */
public interface SettingsReader {
	/**
	 * Reads settings.
	 *
	 * @param settings the class to read
	 * @param value    the condition's value without {@code of}: a mapping of settings, or a bare scalar or sequence
	 * @param main     the component a bare value fills when the settings have more than one
	 * @param <S>      the settings type
	 * @return the settings
	 * @throws IllegalArgumentException when the value does not fit the settings; the message is shown with the path
	 */
	<S> @NotNull S read(@NotNull Class<S> settings, @NotNull Node value, @NotNull Optional<String> main);
}
