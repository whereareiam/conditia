package me.whereareiam.conditia.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The line of a config a condition stands on, named by its path, such as {@code chats.global.requires[1].cooldown}.
 * A {@link me.whereareiam.conditia.ConditionType.Recording Recording} type keys what it records by it, so two lines
 * of the same type keep apart, and the same line keeps its record as long as the config does not move it.
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Line {
	/**
	 * The condition's path.
	 */
	private final @NotNull String path;

	/**
	 * Names a line by its path.
	 *
	 * @param path the condition's path
	 * @return the line
	 */
	public static @NotNull Line of(@NotNull String path) {
		return new Line(path);
	}
}
