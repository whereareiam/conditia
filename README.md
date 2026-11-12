# Conditia

**Conditia** checks requirements written in config files for JVM hosts such as Discord bots and Minecraft plugins. A host declares the typed parts of a place (a sender and a recipient, a member and an interaction), registers condition types, and binds each `requires:` list once at startup, so a typo or a condition about a part the place does not have stops startup with its config path. A check runs the list in order and returns the first condition that did not hold, with its reason.

```yaml
requires:
  - permission: chat.global
  - recipient:
      - permission: chat.receive
      - not: { permission: chat.deaf }
  - nearby: 50
```

## A condition type

A type is a class with plain typed parameters; Conditia picks the parts:

```java
public final class PermissionCondition implements ConditionType.Single<Player, PermissionCondition.Settings> {
    public record Settings(List<String> nodes) {}

    public String name() { return "permission"; }
    public Class<Player> subject() { return Player.class; }
    public Class<Settings> settings() { return Settings.class; }

    public Outcome holds(Player player, Settings settings) {
        return Outcome.when(settings.nodes().stream().allMatch(player::hasPermission));
    }
}
```

## A place

```java
public static final Part<Player> SENDER = Part.of("sender", Player.class);
public static final Part<Player> RECIPIENT = Part.of("recipient", Player.class);

Conditions conditions = new DefaultConditions(ConditionsOptions.builder()
        .reader(new ConfiguraSettings(configura.mapper()))
        .build());
conditions.register(new PermissionCondition());

Place place = conditions.place("channels.global.requires", channel.getRequires(), SENDER, RECIPIENT);
Verdict verdict = place.check(Situation.of(SENDER, sender).and(RECIPIENT, recipient));
```

A type that has to ask a service implements `ConditionType.Deferred`, and only a `DeferredPlace`, checked with a `CompletionStage`, takes it; a place that needs its answer at once, such as a chat message on a game server's chat thread, refuses it at startup.

## Documentation

- [Getting started](docs/content/developers/index.md)
- [Writing conditions](docs/content/developers/writing-conditions/index.md), for config files
- [Condition types](docs/content/developers/condition-types/index.md), for plugins and hosts that add types
- [Hosting Conditia](docs/content/developers/hosting/index.md): places, verdicts, scopes, Configura and Chassis
- [Contributing](docs/content/contributing/index.md)

## License

[MIT](LICENSE)
