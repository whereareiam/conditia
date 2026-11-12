---
title: Writing conditions
description: The form of a requires list in a config file.
---

A `requires:` list holds conditions, all of which must hold. Each condition is one `name: value` line.

```yaml
requires:
  - permission: chat.global
  - recipient:
      - permission: chat.receive
      - not: { permission: chat.deaf }
  - nearby: 50
```

## Values

A bare value fills a type's one setting; a list fills a setting that takes several values, and a single value does too. A type with several settings takes a mapping:

```yaml
- permission: chat.global
- permission: [chat.global, chat.colors]       # both
- time: { from: "08:00", to: "22:00" }
```

## Which part a condition is about

A place has parts, such as `sender` and `recipient`; the first is the main participant.

- A condition about one part is about the main participant when its type fits, otherwise about the only part that fits.
- A condition about two parts takes the first fitting part and the next one.
- A part's name opens a block: the conditions inside are about that part.
- `of` names the parts where the defaults are not what you mean.

```yaml
- recipient:
    - permission: chat.receive                  # about the recipient
- permission: { nodes: chat.receive, of: recipient }
- nearby: { blocks: 50, of: [recipient, sender] }
```

A type that takes no settings reads its value as the parts: `above: [actor, target]`, or nothing (`above:`) for the default parts.

## Combining

| Line | Holds when |
| --- | --- |
| a list | every condition in it holds, checked in order |
| `any: [...]` | one condition in it holds |
| `not: {...}` or `not: [...]` | the condition, or the whole list, does not hold |
| a host's word for named lists, such as `audience: moderator` | the named list holds, about the same parts |

## Built-in types

| Type | Holds | Example |
| --- | --- | --- |
| `time` | from `from` until before `to` each day; a range past midnight holds overnight | `time: { from: "22:00", to: "06:00" }` |
| `date` | on the days from `from` to `to`, both included | `date: { from: 2026-12-20, to: 2027-01-06 }` |
| `chance` | for a share of checks, drawn each time | `chance: 0.25` |

## When something is wrong

Every problem names the config path, such as `commands.clear.requires[1].target[0].not`:

- at startup: an unknown type, part or named list, settings that cannot be read, a condition no part fits or one where several fit;
- later: a condition whose type or named list was removed with its plugin denies everyone and is logged, until the plugin loads again.
