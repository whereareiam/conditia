package me.whereareiam.conditia.adapter.configura;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.ClassUtil;
import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.SettingsReader;
import me.whereareiam.conditia.model.Node;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads conditions' settings with Configura's mapper, so they take every type Configura reads (durations, times,
 * enums). A bare value fills the only property of a settings class of the type's own, such as a record, or the
 * type's {@code main()}, and a single value fills a list ({@code permission: chat.global} for a list of nodes). A
 * record downgraded to an older Java, a plain class there, is read the same, through its constructor.
 */
@RequiredArgsConstructor
public final class ConfiguraSettings implements SettingsReader {
	private final ObjectMapper mapper;

	@Override
	public <S> @NotNull S read(@NotNull Class<S> settings, @NotNull Node value, @NotNull Optional<String> main) {
		JsonNode tree = Trees.text(bare(settings, value, main));

		try {
			return mapper.readerFor(settings).with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).readValue(tree);
		} catch (JsonMappingException exception) {
			throw new IllegalArgumentException(exception.getOriginalMessage(), exception);
		} catch (IOException exception) {
			throw new IllegalArgumentException(exception.getMessage(), exception);
		}
	}

	private Node bare(Class<?> settings, Node value, Optional<String> main) {
		if (value instanceof Node.Mapping || plain(settings)) return value;

		return Node.Mapping.of(main.orElseGet(() -> only(settings)), value);
	}

	// A value of the JDK's own, such as a Boolean, or an enum takes the bare value as it is.
	private static boolean plain(Class<?> settings) {
		return settings.isEnum() || settings.isPrimitive() || ClassUtil.isJDKClass(settings);
	}

	private String only(Class<?> settings) {
		BeanDescription description = mapper.getDeserializationConfig().introspect(mapper.constructType(settings));
		List<BeanPropertyDefinition> properties = description.findProperties();
		if (properties.size() != 1)
			throw new IllegalArgumentException(settings.getSimpleName() + " has several settings; write them as a mapping");

		return properties.getFirst().getName();
	}
}
