package me.whereareiam.conditia.model;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * A config value as Conditia reads it, independent of the config library: a scalar, a sequence or a mapping whose
 * entries keep their order.
 */
public sealed interface Node permits Node.Scalar, Node.Sequence, Node.Mapping {
	/**
	 * A scalar, kept as the text the config holds.
	 *
	 * @param text the text
	 */
	record Scalar(@NotNull String text) implements Node {
	}

	/**
	 * A sequence.
	 *
	 * @param items the items in order
	 */
	record Sequence(@NotNull List<Node> items) implements Node {
		/**
		 * Copies the items.
		 */
		public Sequence {
			items = List.copyOf(items);
		}
	}

	/**
	 * A mapping.
	 *
	 * @param entries the entries in the config's order
	 */
	record Mapping(@NotNull SequencedMap<String, Node> entries) implements Node {
		/**
		 * Copies the entries, keeping their order.
		 */
		public Mapping {
			entries = java.util.Collections.unmodifiableSequencedMap(new LinkedHashMap<>(entries));
		}

		/**
		 * A mapping of one entry.
		 *
		 * @param key   the key
		 * @param value the value
		 * @return the mapping
		 */
		public static @NotNull Mapping of(@NotNull String key, @NotNull Node value) {
			LinkedHashMap<String, Node> entries = new LinkedHashMap<>();
			entries.put(key, value);

			return new Mapping(entries);
		}

		/**
		 * A mapping without entries.
		 *
		 * @return the mapping
		 */
		public static @NotNull Mapping empty() {
			return new Mapping(new LinkedHashMap<>(Map.of()));
		}
	}
}
