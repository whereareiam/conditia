package me.whereareiam.conditia.common.place;

import me.whereareiam.conditia.Place;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.common.binding.Timing;
import me.whereareiam.conditia.common.evaluation.Evaluation;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * A place that needs its answer at once; it is bound with types that answer at once only.
 */
final class DefaultPlace extends BoundPlace implements Place {
	DefaultPlace(String path, Requirements requires, List<Part<?>> parts, Places places, Check check) {
		super(path, requires, parts, Timing.IMMEDIATE, places, check);
	}

	@Override
	public @NotNull String path() {
		return super.path();
	}

	@Override
	public @NotNull Verdict check(@NotNull Situation situation) {
		return check(situation, check -> Evaluation.now(check, situation), Function.identity());
	}

	@Override
	public void close() {
		forget();
	}
}
