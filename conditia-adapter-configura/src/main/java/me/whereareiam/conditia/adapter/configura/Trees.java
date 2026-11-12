package me.whereareiam.conditia.adapter.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.conditia.model.Node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Turns Jackson's trees into Conditia's nodes and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Trees {
	private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
	private static final Pattern INTEGER = Pattern.compile("-?\\d{1,18}");
	private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+");

	static Node node(JsonNode tree) {
		if (tree.isArray()) {
			List<Node> items = new ArrayList<>();
			tree.forEach(item -> items.add(node(item)));

			return new Node.Sequence(items);
		}

		if (tree.isObject()) {
			LinkedHashMap<String, Node> entries = new LinkedHashMap<>();
			tree.properties().forEach(entry -> entries.put(entry.getKey(), node(entry.getValue())));

			return new Node.Mapping(entries);
		}

		return new Node.Scalar(tree.isNull() || tree.isMissingNode() ? "" : tree.asText());
	}

	// Scalars stay text, so Jackson coerces them into whatever a settings component is.
	static JsonNode text(Node node) {
		return switch (node) {
			case Node.Scalar scalar -> NODES.textNode(scalar.text());
			case Node.Sequence sequence -> array(sequence, Trees::text);
			case Node.Mapping mapping -> object(mapping, Trees::text);
		};
	}

	// Scalars that read as numbers or booleans are written as such, so a written file keeps `nearby: 50`.
	static JsonNode typed(Node node) {
		return switch (node) {
			case Node.Scalar scalar -> scalar(scalar.text());
			case Node.Sequence sequence -> array(sequence, Trees::typed);
			case Node.Mapping mapping -> object(mapping, Trees::typed);
		};
	}

	private static JsonNode scalar(String text) {
		if (INTEGER.matcher(text).matches()) return NODES.numberNode(Long.parseLong(text));
		if (DECIMAL.matcher(text).matches()) return NODES.numberNode(new BigDecimal(text));
		if (text.equals("true") || text.equals("false")) return NODES.booleanNode(Boolean.parseBoolean(text));

		return NODES.textNode(text);
	}

	private static ArrayNode array(Node.Sequence sequence, Function<Node, JsonNode> convert) {
		ArrayNode array = NODES.arrayNode();
		sequence.items().forEach(item -> array.add(convert.apply(item)));

		return array;
	}

	private static ObjectNode object(Node.Mapping mapping, Function<Node, JsonNode> convert) {
		ObjectNode object = NODES.objectNode();
		for (Map.Entry<String, Node> entry : mapping.entries().entrySet())
			object.set(entry.getKey(), convert.apply(entry.getValue()));

		return object;
	}
}
