package me.whereareiam.conditia.common;

import me.whereareiam.conditia.ConditionScope;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An owner's scope: its registrations carry the owner's prefix and go when it closes.
 */
final class OwnerScope extends OwnedRegistrar implements ConditionScope {
	private final String owner;
	private final DefaultConditions conditions;
	private final AtomicBoolean closed = new AtomicBoolean();

	OwnerScope(String owner, DefaultConditions conditions) {
		super(Optional.of(owner), conditions);
		this.owner = owner;
		this.conditions = conditions;
	}

	@Override
	public @NotNull String owner() {
		return owner;
	}

	@Override
	public void close() {
		if (closed.getAndSet(true)) return;

		conditions.close(owner);
	}
}
