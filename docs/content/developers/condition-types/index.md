---
title: Condition types
description: Add a kind of condition that configs can name.
---

A condition type is a class implementing one of three shapes, chosen by how many parts of a place it is about:

| Shape | About | `holds` | Examples |
| --- | --- | --- | --- |
| `ConditionType.Plain<S>` | no part | `holds(S settings)` | `time`, `chance` |
| `ConditionType.Single<T, S>` | one part | `holds(T subject, S settings)` | `permission`, `role`, `channel-type` |
| `ConditionType.Pair<A, B, S>` | two parts | `holds(A first, B second, S settings)` | `nearby`, `above` |

Conditia decides at startup which parts a condition is about, so `holds` gets plain typed values and never looks anything up. A part fits when its type is the type's class or a subtype.

## Answering at once or later

The shapes above answer at once: `holds` returns an `Outcome` from what the host already holds in memory, and every place takes them. A type that has to ask a service, such as a database or another server, implements the same shape under `ConditionType.Deferred` (`Deferred.Plain`, `Deferred.Single`, `Deferred.Pair`), whose `holds` returns a `CompletionStage<Outcome>`.

Only a `DeferredPlace`, which waits for its verdict, takes a deferred type. A `Place` needs its answer on the thread that asks, such as a chat message on a game server's chat thread, and stops startup with the condition's path when one is named there. Keep a type immediate whenever its owner can hold the answer in memory, such as playtime loaded when a player joins, so every place can use it.

## Settings

`settings()` names the class a condition's settings are read into, usually a record nested in the type. A bare value fills its only component; a type whose settings have several names the one a bare value fills with `main()`:

```java
public final class CooldownCondition implements ConditionType.Single<Member, CooldownCondition.Settings> {
    public record Settings(Duration duration, String group) {}

    public Optional<String> main() { return Optional.of("duration"); }
    ...
}
```

A type that takes no settings returns `NoSettings.class`; its value then names the parts.

## Outcomes and reasons

`holds` answers with `Outcome.pass()`, `Outcome.fail()` or `Outcome.fail(reason)`; `Outcome.when(boolean)` covers a plain yes or no. A deferred type returns its service's stage, or `outcome.completed()` when it can sometimes decide at once. A type that throws, or whose stage completes exceptionally, makes the check broken, which denies.

A reason is a record nested in the type, listed in `reasons()` so a host can check that every reason has a text:

```java
public final class OwnsCondition implements ConditionType.Deferred.Single<Member, OwnsCondition.Settings> {
    public record Settings(List<String> products) {}

    public record Missing(List<String> products) implements Reason {
        public Map<String, String> values() { return Map.of("products", String.join(", ", products)); }
    }

    public Set<Class<? extends Reason>> reasons() { return Set.of(Missing.class); }

    public CompletionStage<Outcome> holds(Member member, Settings settings) {
        return store.missing(member, settings.products())
                .thenApply(missing -> missing.isEmpty() ? Outcome.pass() : Outcome.fail(new Missing(missing)));
    }
    ...
}
```

The key of a reason's texts is the type's name and the record's in lower case with dashes: `owns.missing`, or `products:owns.missing` for a type registered by the owner `products`.

## Registering

A host registers its own types under plain names; a plugin registers through its scope, so its types carry its prefix and go when it unloads:

```java
conditions.register(new PermissionCondition());           // "permission"

ConditionScope scope = conditions.scope("products");
scope.register(new OwnsCondition(store));                 // "products:owns"
```

## Testing a type

`conditia-testing` reads requirements written as YAML and settings without a config library:

```java
TestSettings settings = new TestSettings()
        .read(OwnsCondition.Settings.class, node -> new OwnsCondition.Settings(TestSettings.texts(node, "products")));
Conditions conditions = new DefaultConditions(ConditionsOptions.builder().reader(settings).build());
conditions.register(new OwnsCondition(store));

DeferredPlace place = conditions.deferredPlace("test", TestRequirements.yaml("- owns: vip"), MEMBER);
```
