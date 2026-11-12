package me.whereareiam.conditia;

import org.jetbrains.annotations.NotNull;

/**
 * The registrations of one owner, such as a plugin or a module. Types and named lists registered through it are
 * known as {@code <owner>:<name>} and all go when the scope closes, so an owner cannot forget one on unload.
 *
 * <pre>{@code
 * ConditionScope scope = conditions.scope("verification");
 * scope.register(new VerifiedCondition(accounts));                  // "verification:verified"
 * scope.name("verified", requirements);                            // "verification:verified" as a named list
 * ...
 * scope.close();                                                   // on unload
 * }</pre>
 */
public interface ConditionScope extends ConditionRegistrar, AutoCloseable {
	/**
	 * The owner this scope registers for.
	 *
	 * @return the owner's id
	 */
	@NotNull String owner();

	/**
	 * Removes every type and named list registered through this scope, then binds all places again. A place that
	 * still refers to one of them denies everyone and is logged with its path until the owner registers them again.
	 * Closing twice does nothing.
	 */
	@Override
	void close();
}
