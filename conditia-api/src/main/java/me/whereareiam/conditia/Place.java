package me.whereareiam.conditia;

import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.SequencedMap;

/**
 * Requirements bound to the parts a host supplies at a place that needs its answer at once, such as a chat message
 * on its way to the players who see it. Its conditions are all of types that answer at once, so a check runs on the
 * caller's thread, never waits and never hands over to another thread.
 */
public interface Place extends AutoCloseable {
	/**
	 * Where the requirements stand in the host's config.
	 *
	 * @return the path
	 */
	@NotNull String path();

	/**
	 * Checks the requirements in order and stops at the first that does not hold.
	 *
	 * @param situation a value for every part of the place
	 * @return the verdict
	 * @throws IllegalArgumentException when the situation lacks a part of the place
	 */
	@NotNull Verdict check(@NotNull Situation situation);

	/**
	 * The texts the requirements' lines carry under the host's words, by the path of each, such as
	 * {@code chat.requires[0].denied}, in the config's order, so a host can check at startup what they refer to.
	 * Empty while the place cannot be bound.
	 *
	 * @return the texts by their paths
	 */
	@NotNull SequencedMap<String, String> wording();

	/**
	 * Forgets the place, so it is no longer bound again. A host closes the places of a config it reloads.
	 */
	@Override
	void close();
}
