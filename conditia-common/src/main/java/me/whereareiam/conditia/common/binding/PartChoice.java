package me.whereareiam.conditia.common.binding;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.exception.ConditionException;
import me.whereareiam.conditia.model.Part;

import java.util.List;
import java.util.Optional;

/**
 * Decides which parts of a place a condition is about. A single takes the main part when its type fits, else the
 * only part that fits; a pair takes the first fitting part and the next one. Inside a participant block the block's
 * part is the main one. {@code of} names the parts instead.
 */
@RequiredArgsConstructor
final class PartChoice {
	private final List<Part<?>> parts;

	Part<?> single(String path, Class<?> subject, List<String> of, Focus focus) {
		if (parts.isEmpty()) throw new ConditionException(path, "the place has no parts");
		if (!of.isEmpty()) return named(path, of, 1).getFirst().fitting(path, subject);

		Part<?> main = focus.block().orElse(parts.getFirst());
		if (fits(main, subject)) return main;

		List<Part<?>> fitting = parts.stream().filter(part -> fits(part, subject)).toList();
		if (fitting.size() == 1) return fitting.getFirst();
		if (fitting.isEmpty()) throw new ConditionException(path, "no part of the place is a " + subject.getSimpleName());

		throw new ConditionException(path, "several parts are a " + subject.getSimpleName() + " " + names(fitting) + "; name one with `of`");
	}

	List<Part<?>> pair(String path, Class<?> first, Class<?> second, List<String> of, Focus focus) {
		if (parts.isEmpty()) throw new ConditionException(path, "the place has no parts");
		if (!of.isEmpty()) {
			List<Named> named = named(path, of, 2);

			return List.of(named.get(0).fitting(path, first), named.get(1).fitting(path, second));
		}

		Part<?> main = focus.block().orElse(parts.getFirst());
		Part<?> one = fits(main, first) ? main : firstFitting(path, first, Optional.empty());

		return List.of(one, firstFitting(path, second, Optional.of(one)));
	}

	private Part<?> firstFitting(String path, Class<?> type, Optional<Part<?>> taken) {
		return parts.stream()
				.filter(part -> taken.map(other -> !other.equals(part)).orElse(true))
				.filter(part -> fits(part, type))
				.findFirst()
				.orElseThrow(() -> new ConditionException(path, "no further part of the place is a " + type.getSimpleName()));
	}

	private List<Named> named(String path, List<String> of, int count) {
		if (of.size() != count) throw new ConditionException(path, "`of` names " + count + (count == 1 ? " part" : " parts"));

		return of.stream()
				.map(name -> parts.stream()
						.filter(part -> part.getName().equals(name))
						.findFirst()
						.map(Named::new)
						.orElseThrow(() -> new ConditionException(path, "the place has no part `" + name + "`; it has " + names(parts))))
				.toList();
	}

	private static boolean fits(Part<?> part, Class<?> type) {
		return type.isAssignableFrom(part.getType());
	}

	private static String names(List<Part<?>> parts) {
		return parts.stream().map(Part::getName).toList().toString();
	}

	private record Named(Part<?> part) {
		Part<?> fitting(String path, Class<?> type) {
			if (!fits(part, type)) throw new ConditionException(path, "`" + part.getName() + "` is no " + type.getSimpleName());

			return part;
		}
	}
}
