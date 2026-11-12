package me.whereareiam.conditia.exception;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Requirements cannot be bound to a place: a condition names an unknown type, list or part, its settings cannot be
 * read, or no part fits it. The message names the config path.
 */
@Getter
public class ConditionException extends RuntimeException {
	/**
	 * Where in the host's config the problem is.
	 */
	private final @NotNull String path;

	/**
	 * Creates the exception.
	 *
	 * @param path    where in the host's config the problem is
	 * @param problem what is wrong
	 */
	public ConditionException(@NotNull String path, @NotNull String problem) {
		super(path + ": " + problem);
		this.path = path;
	}

	/**
	 * Creates the exception with its cause.
	 *
	 * @param path    where in the host's config the problem is
	 * @param problem what is wrong
	 * @param cause   the cause
	 */
	public ConditionException(@NotNull String path, @NotNull String problem, @NotNull Throwable cause) {
		super(path + ": " + problem, cause);
		this.path = path;
	}
}
