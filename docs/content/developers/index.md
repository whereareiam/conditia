---
title: Getting started
description: Check requirements from config files in a bot, plugin or mod with Conditia.
---

Conditia checks `requires:` lists written in a host's config files. The host declares the typed parts of each place where it checks (a sender and a recipient, a member and an interaction), registers the condition types it knows, and binds every list once at startup. A list that names an unknown type, a part the place does not have, or settings that cannot be read stops startup with its config path. A check runs the list in order and returns the first condition that did not hold.

This page sets up a host. For the config form, see [Writing conditions](./writing-conditions/index.md); to change Conditia itself, see [Contributing](../contributing/index.md).

## Dependencies

Conditia is published to the [public Maven registry](https://registry.whereareiam.me/maven/packages) under the group `me.whereareiam.conditia`, and needs Java 21.

| Artifact | Who depends on it | What it holds |
| --- | --- | --- |
| `conditia-api` | condition types, through the host's API | `ConditionType`, `Conditions`, `ConditionScope`, `Place`, `Part`, `Situation`, `Requirements`, `Outcome`, `Reason`, `Verdict` |
| `conditia-common` | the host, shipped inside it | `DefaultConditions`, `ConditionsOptions`, the types `time`, `date` and `chance` |
| `conditia-adapter-configura` | a host whose configs are read by Configura | `ConditiaFeature`, `ConfiguraSettings` |
| `conditia-adapter-chassis` | a host whose plugins are Chassis extensions | `ExtensionConditions` |
| `conditia-testing` | tests of hosts and types | `TestRequirements`, `TestSettings` |

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam.conditia:conditia-common:<version>")
    implementation("me.whereareiam.conditia:conditia-adapter-configura:<version>")

    testImplementation("me.whereareiam.conditia:conditia-testing:<version>")
}
```

## A first place

Declare the parts once, create the conditions with the reader of your config library, register your types, and bind each list after your plugins have registered theirs:

```java
public final class Chat {
    public static final Part<Player> SENDER = Part.of("sender", Player.class);
    public static final Part<Player> RECIPIENT = Part.of("recipient", Player.class);
}

Configura configura = Configura.builder()
        .feature(new ConditiaFeature())
        .build();

Conditions conditions = new DefaultConditions(ConditionsOptions.builder()
        .reader(new ConfiguraSettings(configura.mapper()))
        .build());
conditions.register(new PermissionCondition());
conditions.register(new NearbyCondition());

ChannelConfig channel = configura.read(folder.resolve("channels/global.yml"), ChannelConfig.class);
Place place = conditions.place("channels.global.requires", channel.getRequires(), Chat.SENDER, Chat.RECIPIENT);
```

A check gets a value for every part and completes with a verdict:

```java
place.check(Situation.of(Chat.SENDER, sender).and(Chat.RECIPIENT, recipient))
        .thenAccept(verdict -> {
            switch (verdict) {
                case Verdict.Passed passed -> deliver(message, recipient);
                case Verdict.Failed failed -> deny(sender, failed);
                case Verdict.Broken broken -> logger.warn("Cannot check {}", broken.path(), broken.error());
            }
        });
```

Next: [Writing conditions](./writing-conditions/index.md) shows what the list may say, [Condition types](./condition-types/index.md) how to add a type, and [Hosting Conditia](./hosting/index.md) how verdicts, named lists and scopes work.
