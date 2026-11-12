# AGENTS.md

## Conditia At A Glance

Conditia checks **requirements written in config files** for JVM hosts such as Discord bots and Minecraft plugins. A host declares the typed parts of each place where it checks (a sender and a recipient, a member and an interaction), registers condition types, and binds every `requires:` list once: unknown types, parts or named lists, unreadable settings and conditions no part fits stop startup with the config path. A check runs the list in order and completes with a sealed verdict naming the first condition that did not hold. Types and named lists live in a scope per owner and go when it closes; places are bound again after every change.

When changing behavior:

- keep Conditia host-agnostic: nothing in `conditia-api` or `conditia-common` knows a Discord member, a Minecraft player or a host's word for named lists
- keep `holds` typed: a type gets its parts as parameters, never by looking them up
- a check denies when in doubt: a type that throws, or a place that cannot be bound, is `Broken`, never `Passed`
- a `Place` never waits: it binds only types that answer at once, so a check runs on the caller's thread

---

## Repository Orientation

### Module Map

- `conditia-api`
  The contracts types and hosts compile against: `ConditionType` (`Plain`, `Single`, `Pair`, answering at once, and the same under `Deferred`, answering later), `Conditions`, `ConditionRegistrar`, `ConditionScope`, `Place`, `DeferredPlace`, `SettingsReader`; `model` (`Part`, `Situation`, `Requirements`, `Node`, `NoSettings`, `Names`, `Wording`); `result` (`Outcome`, `Reason`, `Verdict`); `exception` (`ConditionException`).
- `conditia-common`
  `DefaultConditions` is the door: it implements `Conditions`, serialises every change and binds all places again after it; `ConditionsOptions` is what a host tells it. Packages: `registration` (`Registrations`: types and named lists by owner), `binding` (`Binder`, `TestBinding`, `PartChoice`, `Focus`, `Check`: the startup checks and the bound tree), `evaluation` (`Evaluation`: running a check in order, at once or as a stage), `place` (`Places`, `BoundPlace`, `DefaultPlace`, `DefaultDeferredPlace`), `type` (`TimeCondition`, `DateCondition`, `ChanceCondition`).
- `conditia-adapter-configura`
  `ConditiaFeature` (reads and writes `Requirements` through Configura) and `ConfiguraSettings` (reads settings with Configura's mapper). Configura and Jackson are `compileOnly`.
- `conditia-adapter-chassis`
  `ExtensionConditions`: a scope per Chassis extension, closed through the extension's context. `chassis-api` is `compileOnly`.
- `conditia-testing`
  `TestRequirements` (requirements written as YAML) and `TestSettings` (settings read by functions), for hosts' and types' tests.
- `build-logic/`
  Shared Gradle conventions. `module-java` (group `me.whereareiam.conditia`, Java 21 toolchain, Lombok, JetBrains annotations, JUnit, sources and Javadoc jars), `module-api` (`module-java` plus publication), `packaging-publication` (Maven publication).
- `docs/content/`
  Documentation for hosts, type authors and contributors.

Not in the repository yet: platform families such as `conditia-platform/platform-paper`. Do not document them as existing.

### Where To Look First

- For contracts and public models, start in `conditia-api`.
- For what a config may say and the errors it gets, start in `binding`; for how a check runs, in `evaluation`.
- For registrations and scopes, start in `DefaultConditions` and `registration`.
- For build behavior or publication, check `build-logic/` and `gradle/libs.versions.toml`.

---

## Architecture Rules

- Contracts go into `conditia-api`, behavior into `conditia-common`.
- Adapters depend on `conditia-api` only and keep their library `compileOnly`, so the host chooses its version.
- No reflection in Conditia's own code: settings are read by the host's `SettingsReader`, and parts are matched with `Class.isAssignableFrom` and `Class.cast` only.
- Public API code must have complete Javadocs for public classes, interfaces, enums, and public methods. Public API changes are compatibility-sensitive.
- Error messages are read by administrators: they begin with the config path and say what is wrong in their words (`no part of the place is a Member`).

---

## Dependencies

- Use Java 21-compatible code and APIs.
- Follow the version catalog in `gradle/libs.versions.toml` when adding or changing dependencies.
- `conditia-api` and `conditia-common` have no runtime dependencies.
- When a new dependency is required, check the latest available version instead of relying on memory.

---

## Build, Test, And Validation

### Common Commands

- `./gradlew test`
- `./gradlew test build` (what the pull request checks run)
- `./gradlew :conditia-common:test --tests 'me.whereareiam.conditia.common.DefaultConditionsTest'`
- `VERSION=<version> ./gradlew publishToMavenLocal` (to try a build in a host)

The version comes from the `VERSION` environment variable and defaults to `dev`. The build uses a Java 21 toolchain and downloads one when the machine has none.

### Verification Guidance

- Prefer targeted Gradle tasks for the module you changed before running broad root verification.
- Add or update automated tests when behavior changes, in the closest existing test: `DefaultConditionsTest` (binding and evaluation through the door, against the small chat host in `Chat`), `PartChoiceTest`, `BuiltInTypesTest`, `ConditiaFeatureTest`, `ExtensionConditionsTest`, and the model tests of `conditia-api`.
- A test of a failure asserts the path the verdict or error names.

---

## Branching And Pull Requests

### Branch Flow

- Build each feature, fix, or isolated piece of work in its own `feature/...` branch.
- Merge feature work into `dev` through a pull request. Pull requests are merged by rebase: each commit lands on `dev` as written, so a branch holds clean conventional commits and no fixups.
- Releases are cut from `dev`: publishing the release draft tags the current `dev`.

### Pull Request Checks

- Every pull request gets the metadata check (title and label) and the checks: `./gradlew test build`.
- The `dev` ruleset requires these checks, so no pull request merges with a failing or missing check unless an administrator bypasses it.
- Dependabot groups minor and patch updates into one pull request per ecosystem, and `dependabot-auto-merge.yml` turns on auto-merge for it, so it merges once the checks pass; a major update gets a pull request of its own and waits for review. The merge goes through a GitHub App, because a merge by `GITHUB_TOKEN` would not start the workflows of the push to `dev`; its `MERGE_APP_CLIENT_ID` and `MERGE_APP_PRIVATE_KEY` are Dependabot secrets, since a run Dependabot starts gets no Actions secrets. When the checks gain or rename a job, update the required checks of the ruleset.
- Label a pull request with exactly one of `feature`, `change`, `bug`, `dependencies`, or with `skip-changelog`; `major` may accompany a category.
- A run keeps `test-results` as an artifact when a test failed.
- Development builds are started manually and publish a branch-qualified version. A published GitHub release publishes the Maven artifacts.
- Entry workflows only select a trigger and call `reusable-verify.yml` and `reusable-publish.yml`; shared steps come from `whereareiam/devops` and `whereareiam/blockops`.
- The Gradle build cache is on. Pull requests only read caches, so `build-cache.yml` compiles the project and its tests on every push to `dev` and writes the Gradle cache; when the checks start running other tasks before their tests, add those tasks to its command.

### Issues

- A suggestion opens with `suggestion`, the label of an idea not decided on yet. An accepted one trades it for `feature` or `change`; a declined one is closed as not planned.
- A bug report opens with `bug`.

### Release Notes

A merged pull request lands in the release draft as `* <title> ([#<number>](<url>))`, under the category of its label. The draft is written for the developers of hosts and condition types, so the title and the label decide what they read.

#### Titles

- use the strict format `Area: Description`, with a past-tense description of what changed for the reader: `Introduced`, `Changed`, `Fixed`, `Removed`
- describe the effect, not the implementation: `Binding: Fixed a block's part being ignored inside a named list`, not `Binding: Passed the focus to Binder.reference`
- the draft sorts by the exact area, so spell an area exactly as listed below

Areas for behavior hosts and condition types notice:

`Binding`, `Evaluation`, `Configuration`, `Named lists`, `Scopes`, `Built-in types`, `Configura`, `Chassis`, `Testing`

- reuse an existing area whenever one fits
- add a new area only for something readers recognise by that name, such as a new adapter or platform, and add it to this list, to the issue template's areas and to the pull request template in the same pull request
- never use class names or internal packages as an area: no `DefaultConditions`, `Common`, `Binder`

`API:` is for changes to the contracts hosts and condition types compile against: types, methods and their meaning in `conditia-api` and the public types of the other modules. The drafter moves `API:` changes out of their category into a closing "For developers" section. A change that is both behavior and API takes the behavior area; mention the API part in the pull request body.

#### Labels

| Label | Use for | Draft section |
| --- | --- | --- |
| `feature` | something new for hosts or condition types | What’s new |
| `change` | changed behavior or API | Enhancements |
| `bug` | a fix for a bug that shipped in a release | Fixes |
| `dependencies` | a library update | Dependencies |
| `skip-changelog` | anything a reader of the release would not notice | not listed |

Use `skip-changelog` for:

- internal refactors with no effect on hosts, extensions, or the public API
- build, CI, test, and documentation-only changes
- a fix for a bug that never shipped: when the broken code was introduced after the last release, the fix belongs to the work that introduced it, not to the release notes; compare against the last release tag (`git grep <symbol> <tag>`, `git log <tag>..HEAD -- <path>`) to decide
- changes to something that was itself added after the last release and never shipped

Add `major` next to the category when hosts or condition types must change on upgrade: an incompatible public API change, or a config form that changed meaning. One `major` pull request makes the draft a major version.

#### Release notes file

The drafter rebuilds the draft on every push to `dev`, so text typed into the draft is lost. Everything written by hand, such as the subtitle, the summary, upgrade notes, and security callouts, lives in `.github/release-notes.md` and is changed through pull requests like any other file.

```markdown
---
version: 0.2.0
---

# Update 0.2.0 - Typed Parts

One or two sentences about the release.

## Upgrading

* What a host or a condition type has to change.
```

- the content replaces the subtitle and summary placeholders of the draft only while `version` matches the version the draft resolves to; notes of a published release therefore never reach the next draft
- when a change needs a note, add it to this file in the same pull request; if the file names an already published version, start the notes of the next release by replacing the whole file and setting `version` to the version the draft shows
- keep the heading as `# Update <version> - <subtitle>`
- bullets in this file are not sorted into the changelog

#### Publishing a release

- fix the changelog part of the draft by editing titles and labels of merged pull requests, then rerun the `Release drafter` workflow
- the subtitle and summary come from `.github/release-notes.md`; the release workflow stops while a placeholder is left
- the summary names what hosts and condition types must change on upgrade (everything labelled `major`) and calls out security fixes
- publishing the release verifies the tagged commit and publishes the Maven artifacts to `https://registry.whereareiam.me/maven/packages`; nothing is attached to the release and nothing goes to plugin stores

---

## Documentation Rules

### Structure

`docs/content/` is split into two sections, one per reader, each a root folder (`"root": true` in its `meta.json`) that Scriptorium shows as its own sidebar: `developers/` (Using Conditia) and `contributing/`. The docs open on `developers/index.md`. `scriptorium.project.json` configures the site; there is no published site yet.

- **Using Conditia** (`developers/`): setting up a host, the config form, writing condition types, places, verdicts, named lists, scopes, Configura and Chassis.
- **Contributing** (`contributing/`): building, modules and where tests go.

- Internal classes of `conditia-common` that hosts never call (`Binder`, `PartChoice`, `Evaluation`, `Places`) belong in Contributing.
- Each page is `<name>/index.md`; each folder with several pages has a `meta.json` that orders them. Only the two section folders carry an `icon`.
- Links between pages are relative and point at the page file, such as `../hosting/index.md`.

### Content

- When changing behavior hosts or types notice, update the relevant documentation in the same work.
- Documentation describes the **current state** of the library; no "used to" text outside release notes.
- Every code example uses real symbols of the current source; check it against the code.

---

## Working Style

- Match the conventions already established in the surrounding module.
- Prefer small, scoped changes over broad cleanup unless cleanup is part of the task.
- If a change touches binding or evaluation, be especially careful: a regression there can let a check pass that should deny.
