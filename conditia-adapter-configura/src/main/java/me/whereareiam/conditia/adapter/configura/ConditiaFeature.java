package me.whereareiam.conditia.adapter.configura;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.configura.ConfiguraFeature;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Lets Configura read and write {@link Requirements}, so a host's config model holds its {@code requires:} lists as
 * fields:
 *
 * <pre>{@code
 * Configura configura = Configura.builder()
 *         .feature(new ConditiaFeature())
 *         .build();
 * ConditionsOptions options = ConditionsOptions.builder()
 *         .reader(new ConfiguraSettings(configura.mapper()))
 *         .build();
 * }</pre>
 *
 * <p>An absent or empty list reads as {@link Requirements#none()}.</p>
 */
public final class ConditiaFeature implements ConfiguraFeature {
	@Override
	public @NotNull List<Module> modules(@NotNull ObjectMapper mapper) {
		SimpleModule module = new SimpleModule("conditia");
		module.addDeserializer(Requirements.class, new Reading());
		module.addSerializer(Requirements.class, new Writing());

		return List.of(module);
	}

	private static final class Reading extends JsonDeserializer<Requirements> {
		@Override
		public Requirements deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonNode tree = parser.readValueAsTree();
			if (tree == null || tree.isNull()) return Requirements.none();

			try {
				return Requirements.of(Trees.node(tree));
			} catch (IllegalArgumentException exception) {
				return context.reportInputMismatch(Requirements.class, exception.getMessage());
			}
		}

		@Override
		public Requirements getNullValue(DeserializationContext context) {
			return Requirements.none();
		}
	}

	private static final class Writing extends JsonSerializer<Requirements> {
		@Override
		public void serialize(Requirements requirements, JsonGenerator generator, SerializerProvider provider) throws IOException {
			generator.writeTree(Trees.typed(requirements.getConditions()));
		}
	}
}
