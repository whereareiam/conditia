package me.whereareiam.conditia.common.binding;

import me.whereareiam.conditia.model.Part;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where binding stands: the part a participant block is about, and the named lists being bound, so a list that
 * refers to itself is found.
 *
 * @param block the block's part, empty outside blocks
 * @param lists the named lists entered, outermost first
 */
record Focus(Optional<Part<?>> block, List<String> lists) {
	static Focus start() {
		return new Focus(Optional.empty(), List.of());
	}

	Focus about(Part<?> part) {
		return new Focus(Optional.of(part), lists);
	}

	Focus enter(String list) {
		List<String> entered = new ArrayList<>(lists);
		entered.add(list);

		return new Focus(block, List.copyOf(entered));
	}
}
