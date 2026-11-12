---
title: Contributing
description: Build, test and change Conditia.
---

## Building

```bash
./gradlew test build
```

The build uses a Java 21 toolchain; Gradle downloads one when the machine has none. Versions of dependencies live in `gradle/libs.versions.toml`, shared build conventions in `build-logic/` (`module-java`, `module-api`, `packaging-publication`).

## Modules

| Module | Holds |
| --- | --- |
| `conditia-api` | the contracts types and hosts compile against; no behavior |
| `conditia-common` | `DefaultConditions` and its packages: `registration` (types and named lists by owner), `binding` (the startup checks and the part choice), `evaluation` (running a bound check, at once or as a stage), `place` (immediate and waiting places, bound again after changes), `type` (`time`, `date`, `chance`) |
| `conditia-adapter-configura` | reading `Requirements` and settings through Configura; Configura is `compileOnly` |
| `conditia-adapter-chassis` | a scope per Chassis extension; Chassis is `compileOnly` |
| `conditia-testing` | `TestRequirements` and `TestSettings` |

Adapters depend on `conditia-api` only and keep their library `compileOnly`, so the host chooses its version.

## Testing

Tests mirror the class they test (`DefaultConditionsTest`). The engine's tests run against a small chat host in `conditia-common`'s tests (`Chat`) with requirements written as YAML; a change to binding or evaluation gets a test there with the path the verdict or error names.
