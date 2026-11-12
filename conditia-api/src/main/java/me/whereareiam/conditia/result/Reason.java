package me.whereareiam.conditia.result;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Why a condition failed, beyond that it did. A type declares its reasons as records nested in it and lists them in
 * {@link me.whereareiam.conditia.ConditionType#reasons()}:
 *
 * <pre>{@code
 * public record Missing(List<String> products) implements Reason {
 *     public Map<String, String> values() { return Map.of("products", String.join(", ", products)); }
 * }
 * }</pre>
 */
public interface Reason {
	/**
	 * The values a host's text of this reason may show, by placeholder name.
	 *
	 * @return the values, empty by default
	 */
	default @NotNull Map<String, String> values() {
		return Map.of();
	}

	/**
	 * The key of a reason's texts: the condition's name and the record's name in lower case with dashes, such as
	 * {@code owns.missing} or {@code products:owns.missing}.
	 *
	 * @param condition the name configs use for the condition's type
	 * @param reason    the reason's class
	 * @return the key
	 */
	static @NotNull String key(@NotNull String condition, @NotNull Class<? extends Reason> reason) {
		String name = reason.getSimpleName().replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(java.util.Locale.ROOT);

		return condition + "." + name;
	}
}
