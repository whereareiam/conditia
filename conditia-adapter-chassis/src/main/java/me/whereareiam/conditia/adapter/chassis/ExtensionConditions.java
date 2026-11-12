package me.whereareiam.conditia.adapter.chassis;

import lombok.RequiredArgsConstructor;
import me.whereareiam.chassis.ExtensionContext;
import me.whereareiam.conditia.ConditionScope;
import me.whereareiam.conditia.Conditions;
import org.jetbrains.annotations.NotNull;

/**
 * Gives each Chassis extension a Conditia scope named after its id, closed by Chassis when the extension stops, so
 * its condition types and named lists go with it. A host offers it to its extensions through their context:
 *
 * <pre>{@code
 * ExtensionConditions extensionConditions = new ExtensionConditions(conditions);
 * ...
 * ConditionScope scope = extensionConditions.open(context);       // in the extension's enable
 * scope.register(new VerifiedCondition(accounts));                  // "verification:verified"
 * }</pre>
 */
@RequiredArgsConstructor
public final class ExtensionConditions {
	private final @NotNull Conditions conditions;

	/**
	 * Opens the scope of an extension for its current load.
	 *
	 * @param context the extension's context
	 * @return the scope, named after the extension's id and closed when the extension stops
	 * @throws IllegalStateException when the extension's scope is open already
	 */
	public @NotNull ConditionScope open(@NotNull ExtensionContext context) {
		ConditionScope scope = conditions.scope(context.descriptor().getId());
		context.onClose(scope);

		return scope;
	}
}
