package me.whereareiam.conditia.common;

import lombok.RequiredArgsConstructor;
import me.whereareiam.conditia.ConditionRegistrar;
import me.whereareiam.conditia.ConditionType;
import me.whereareiam.conditia.model.Requirements;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Registers for the host (no owner) or for one owner, then has every place bound again.
 */
@RequiredArgsConstructor
class OwnedRegistrar implements ConditionRegistrar {
	private final Optional<String> owner;
	private final DefaultConditions conditions;

	@Override
	public @NotNull String register(@NotNull ConditionType<?> type) {
		return conditions.change(() -> conditions.registrations().type(owner, type));
	}

	@Override
	public @NotNull String name(@NotNull String name, @NotNull Requirements requires) {
		return conditions.change(() -> conditions.registrations().list(owner, name, requires));
	}

	@Override
	public void forget(@NotNull String name) {
		conditions.change(() -> {
			conditions.registrations().forget(owner, name);

			return name;
		});
	}
}
