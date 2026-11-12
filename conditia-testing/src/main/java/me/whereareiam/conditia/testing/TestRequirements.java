package me.whereareiam.conditia.testing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.conditia.model.Node;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.StringReader;
import java.util.LinkedHashMap;

/**
 * Requirements and config values written as YAML in tests.
 *
 * <pre>{@code
 * Requirements requires = TestRequirements.yaml("""
 *         - permission: chat.global
 *         - recipient:
 *             - permission: chat.receive
 *         """);
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestRequirements {
	/**
	 * Reads requirements from YAML.
	 *
	 * @param yaml a list of conditions, or one condition
	 * @return the requirements
	 */
	public static @NotNull Requirements yaml(@NotNull String yaml) {
		return Requirements.of(node(yaml));
	}

	/**
	 * Reads a config value from YAML.
	 *
	 * @param yaml the value
	 * @return the value as Conditia reads it; an empty document is an empty mapping
	 */
	public static @NotNull Node node(@NotNull String yaml) {
		org.yaml.snakeyaml.nodes.Node root = new Yaml().compose(new StringReader(yaml));

		return root == null ? Node.Mapping.empty() : convert(root);
	}

	private static Node convert(org.yaml.snakeyaml.nodes.Node node) {
		return switch (node) {
			case ScalarNode scalar -> new Node.Scalar(scalar.getValue());
			case SequenceNode sequence -> new Node.Sequence(sequence.getValue().stream().map(TestRequirements::convert).toList());
			case MappingNode mapping -> mapping(mapping);
			default -> throw new IllegalArgumentException("Unsupported YAML node " + node.getNodeId());
		};
	}

	private static Node mapping(MappingNode mapping) {
		LinkedHashMap<String, Node> entries = new LinkedHashMap<>();
		for (NodeTuple tuple : mapping.getValue()) {
			if (!(tuple.getKeyNode() instanceof ScalarNode key)) throw new IllegalArgumentException("A key is a scalar");

			entries.put(key.getValue(), convert(tuple.getValueNode()));
		}

		return new Node.Mapping(entries);
	}
}
