---
title: Hosting Conditia
description: Places, verdicts, named lists, scopes, Configura and Chassis.
---

## Places and parts

A place is where a host checks one `requires:` list: a command, a button, a chat channel. Binding fits the list to the parts the place supplies; the first part is the main participant. Create places after your plugins have registered their types, so a place that names a type nobody registered stops startup.

| Bound with | Check | Takes | For |
| --- | --- | --- | --- |
| `conditions.place(path, requires, parts...)` | `Place.check(situation)` returns a `Verdict` on the caller's thread | types that answer at once | a place that may not wait, such as a chat message on a game server |
| `conditions.deferredPlace(path, requires, parts...)` | `DeferredPlace.check(situation)` completes a `CompletionStage<Verdict>` | types that answer at once and later | a place that may wait, such as a command that asks a database |

An immediate place that names a deferred type, directly or inside a named list, stops startup with the condition's path. If a type is later registered again under the same name answering later, the immediate place denies everyone and is logged, until a type answering at once takes the name again.

A place stays bound until it is closed: after every registration, removal or closed scope Conditia binds all places again. Close the places of a config you reload.

## Verdicts

A check gives a sealed `Verdict` and never throws or completes exceptionally:

| Verdict | Means |
| --- | --- |
| `Passed` | every condition held |
| `Failed(path, condition, parts, reason)` | the first condition that did not hold, with the parts of the place it was about; a failed `not` reports the condition inside it that held, with the reason it held for, and a failed `any` reports itself without a reason |
| `Broken(path, condition, error)` | a type threw, or the place cannot be bound since something it names was removed |

`failed.key()` gives the key of the reason's texts, and `failed.reason().get().values()` its placeholder values. Without a reason, show the place's own text, such as a command's `denied`.

`failed.about(part)` tells whose wording to show where a place has several participants:

```java
if (verdict instanceof Verdict.Failed failed) {
    Map<String, String> wording = failed.about(Chat.RECIPIENT) ? texts.recipient() : texts.sender();
    deny(sender, failed.key().map(wording::get).orElse(texts.denied()), failed);
}
```

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

A failed named list reports the condition inside it that failed, with its own reason and key, at a path that runs through the reference (`commands.ban.requires[0].audience[1].permission`). `name` replaces a list of the same name, and `forget` removes one, so a host can reload its file of lists.

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
