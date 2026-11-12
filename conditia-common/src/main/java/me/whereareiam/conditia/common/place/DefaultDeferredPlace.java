package me.whereareiam.conditia.common.place;

import me.whereareiam.conditia.DeferredPlace;
import me.whereareiam.conditia.common.binding.Check;
import me.whereareiam.conditia.common.binding.Timing;
import me.whereareiam.conditia.common.evaluation.Evaluation;
import me.whereareiam.conditia.model.Part;
import me.whereareiam.conditia.model.Requirements;
import me.whereareiam.conditia.model.Situation;
import me.whereareiam.conditia.result.Verdict;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A place that waits for types answering later.
 */
final class DefaultDeferredPlace extends BoundPlace implements DeferredPlace {
	DefaultDeferredPlace(String path, Requirements requires, List<Part<?>> parts, Places places, Check check) {
		super(path, requires, parts, Timing.DEFERRED, places, check);
	}

	@Override
	public @NotNull String path() {
		return super.path();
	}

	@Override
	public @NotNull CompletionStage<Verdict> check(@NotNull Situation situation) {
		return check(situation, check -> Evaluation.later(check, situation), CompletableFuture::completedStage);
	}

	@Override
	public @NotNull SequencedMap<String, String> wording() {
		return super.wording();
	}

	@Override
	public void close() {
		forget();
	}
}
