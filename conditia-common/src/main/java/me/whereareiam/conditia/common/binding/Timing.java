package me.whereareiam.conditia.common.binding;

/**
 * When a place needs its answer, which decides the types its conditions may be of.
 */
public enum Timing {
	/**
	 * The place needs its answer at once, so it takes only types that answer at once.
	 */
	IMMEDIATE,

	/**
	 * The place waits for its answer, so it takes types that answer at once and types that answer later.
	 */
	DEFERRED
}
