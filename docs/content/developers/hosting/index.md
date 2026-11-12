---
title: Hosting Conditia
description: Places, verdicts, named lists, scopes, Configura and Chassis.
---

## Places and parts

A place is where a host checks one `requires:` list: a command, a button, a chat channel. `conditions.place(path, requires, parts...)` binds the list to the parts the place supplies; the first part is the main participant. Create places after your plugins have registered their types, so a place that names a type nobody registered stops startup.

A place stays bound until it is closed: after every registration, removal or closed scope Conditia binds all places again. Close the places of a config you reload.

## Verdicts

A check completes with a sealed `Verdict` and never exceptionally:

| Verdict | Means |
| --- | --- |
| `Passed` | every condition held |
| `Failed(path, condition, reason)` | the first condition that did not hold; a failed `any` or `not` reports itself without a reason |
| `Broken(path, condition, error)` | a type threw, or the place cannot be bound since something it names was removed |

`failed.key()` gives the key of the reason's texts, and `failed.reason().get().values()` its placeholder values. Without a reason, show the place's own text, such as a command's `denied`.

## Named lists

A host that wants reusable lists gives Conditia its word for them and registers them by name:

```java
Conditions conditions = new DefaultConditions(ConditionsOptions.builder()
        .reader(reader)
        .namedLists(Optional.of("audience"))
        .build());
conditions.name("moderator", audiences.get("moderator"));
```

```yaml
requires:
  - audience: moderator
  - target:
      - not: { audience: moderator }      # asked of the target
```

A failed named list reports its reference (`audience moderator`) with the reason of the condition inside that failed. `name` replaces a list of the same name, and `forget` removes one, so a host can reload its file of lists.

## Scopes

`conditions.scope(owner)` opens the registrations of a plugin or module. Types and lists registered through it are named `<owner>:<name>` and all go when the scope closes; a place that still names one denies everyone and is logged with its path until the owner registers it again.

## Configura

`conditia-adapter-configura` lets a config model hold `Requirements` fields and reads settings with Configura's mapper, so settings take durations, times and enums:

```java
Configura configura = Configura.builder()
        .feature(new ConditiaFeature())
        .build();
SettingsReader reader = new ConfiguraSettings(configura.mapper());
```

An absent `requires:` reads as `Requirements.none()`, and written files keep numbers as numbers.

## Chassis

`conditia-adapter-chassis` gives each Chassis extension a scope named after its id, closed by Chassis when the extension stops:

```java
ExtensionConditions extensionConditions = new ExtensionConditions(conditions);

ConditionScope scope = extensionConditions.open(context);   // in the extension's enable
scope.register(new VerifiedCondition(accounts));             // "verification:verified"
```
