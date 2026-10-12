# Testing Module Structure

This document defines how the test modules and the integration-test module are organized after the pre-GA `spigot` / `paper` module split.

## Assumptions

- The current `bukkit` module is effectively a Spigot-compatible implementation.
- The current `bukkit-test` depends on `:bukkit` and provides Spigot/Bukkit command testing utilities and NMS mocks.
- The current `bukkit-integration-test` handles Bukkit / Paper / Mohist fixtures for multiple Minecraft versions in a single module.
- Forge, Paper, and Folia will also be testing targets, so a public guide built around `bukkit-test` should not be fixed in advance.

## Goals

- Separate public testing artifacts from repository-only integration tests.
- Migrate the current `bukkit-test` to `spigot-test`.
- Handle Paper-only behavior in `paper-testing` and Paper integration targets.
- Leave room to add a Forge testing story later.
- Keep the aggregator approach for integration tests at first, and make platform targets explicit.

## Non-Goals

- Do not keep the `bukkit-test` artifact after GA.
- Do not publish the integration-test module as a public artifact.
- Do not mix Paper-specific tests into the Spigot testing utilities.
- Do not fold Forge testing into the Spigot/Paper testing utilities.

## Target Test Modules

### Public test artifacts

| Module | Published | Successor of | Role |
|---|---:|---|---|
| `common-test` | Maybe | none | Test helpers for the `common` parser / command tree / options / help / permission model. Whether to publish it needs discussion. |
| `spigot-test` | Yes | `bukkit-test` | Command testing utilities for Spigot/Bukkit plugins. Includes NMS mocks and a fake sender. |
| `paper-testing` | Yes | none | Testing utilities for the Paper command API, Adventure components, Paper suggestions, and Paper registry arguments. |
| `forge-testing-<version>` | Yes | none | Forge command testing utilities. One artifact per Minecraft version, like `forge`. |

### Repository-only integration modules

| Module | Published | Role |
|---|---:|---|
| `integration-test` | No | Multi-platform integration test aggregator. Candidate rename target for the current `bukkit-integration-test`. |
| `spigot-integration-test` | No | Future split target if the aggregator grows too large. |
| `paper-integration-test` | No | Future split target if Paper official API / Adventure / latest Paper tests grow. |
| `mohist-integration-test` | No | Future split target if Mohist-specific differences grow. |
| `forge-integration-test` | No | Integration tests for the Forge server/mod environment. |

Recommended:

- Before GA, use the `integration-test` aggregator approach as the baseline.
- Rename `bukkit-integration-test` to `integration-test`.
- Make `spigot`, `paper`, and `mohist` explicit in target IDs / fixture names.
- Split out `paper-integration-test` once Paper-only fixtures grow and the aggregator becomes hard to manage.

## Current Module Mapping

| Current | Target | Notes |
|---|---|---|
| `bukkit-test` | `spigot-test` | The current `bukkit` moves to `spigot`, so the test artifact is renamed too. |
| `bukkit-integration-test` | `integration-test` | Renamed as a multi-platform aggregator. |
| `bukkit-integration-test/fixtures/test-plugin-*` | `integration-test/fixtures/spigot-*` or target metadata | Make the platform target explicit, not just the fixture name. |
| `bukkit-integration-test/fixtures/*-mohist` | `integration-test/fixtures/mohist-*` | Make it explicit as a Mohist target. |
| `test-plugin-common` | `common` | Kept as the shared fixture source for Spigot/Paper/Mohist. |

## `spigot-test`

`spigot-test` is the successor of the current `bukkit-test`.

Responsibilities:

- fake Bukkit sender / command sender.
- Spigot command execution assertions.
- Spigot tab completion / suggestion assertions.
- Bukkit permission model mocks.
- NMS mock classes for Spigot-compatible arguments.
- public testing utilities for downstream Spigot plugins.

Dependencies:

- `api(project(":spigot"))`
- Spigot API compileOnly / testImplementation.
- Brigadier.
- Mockito / AssertJ / JUnit where needed.

Migration from `bukkit-test`:

- Rename / move the `bukkit-test` module to `spigot-test`.
- Change `api(project(":bukkit"))` to `api(project(":spigot"))`.
- Keep the public package `dev.marulabs.commandlib`.
- Keep the NMS mock package for now, aligned with the NMS util in the Spigot module.
- README / downstream docs point to the `spigot-test` artifact.

## `paper-testing`

`paper-testing` is a new module.

Responsibilities:

- Adventure component message assertions.
- Paper suggestion / tooltip assertions.
- Paper registry argument fake / adapter.
- Paper `ArgumentTypes` / selector resolver fake.
- public testing utilities for downstream Paper plugins.

Not responsibilities:

- Verifying Paper command API registration / the lifecycle API. This is guaranteed by the real Paper server targets in `integration-test`.
- Verifying the Spigot legacy command map fallback.
- Bungee `BaseComponent` compatibility.
- Mocks for the Spigot NMS fallback in general.

Open questions:

- Whether CommandLib should have its own Adventure component assertions or rely on Kyori test utilities.
- Whether to include Folia-safe behavior in `paper-testing` or create a separate `folia-test`.

## `forge-testing`

`forge-testing` uses the same Stonecutter setup as `forge` and publishes `forge-testing-<version>` per Minecraft version.

Responsibilities:

- Forge command source / sender fake.
- Forge permission assertions.
- Forge text component assertions.
- Forge argument parsing helpers.
- public testing utilities for downstream Forge mods.

Decisions:

- Like `forge`, the testing API is split into per-version artifacts. The shape of the public API (`CommandTester` / `FakeSender`) is
  the same across all versions.
- Run Minecraft's `Bootstrap` to initialize the registries, and parse vanilla argument types with the real implementations. The
  `NetworkHooks.init` called by `Bootstrap` on Forge 1.17+ assumes FML's class transformer, so it is mocked only during bootstrap.
- `MinecraftServer` / `PlayerList` / `GameProfileCache` are Mockito mocks. Fake players can be resolved by name.
- Permission nodes are answered by the running `FakeSender` (1.18+ mocks `CommandLib#hasPermission`, 1.16.5 mocks
  `PermissionAPI#hasPermission`, only during execution).

Open questions:

- Whether to build test utilities that can be shared with NeoForge.
- Whether to fake arguments that need a world / scoreboard (such as `TeamArgument`).

## `integration-test` Aggregator

The current `bukkit-integration-test` is already close to an aggregator with multi-version targets.

Keep:

- one Gradle module that owns Docker / server bootstrap / MCProtocolLib wiring.
- target list with Minecraft version, Java version, fixture name, protocol configuration.
- shared fixture source.
- report file naming per target.

Change:

- module name: `bukkit-integration-test` -> `integration-test`.
- add the platform to the target metadata.
- include the platform in fixture names.
- dependencies in fixtures should target `spigot`, `paper`, or `mohist` explicitly.
- task names should include platform where useful.

Suggested target metadata:

```kotlin
data class IntegrationTarget(
    val platform: Platform,
    val id: String,
    val fixtureName: String,
    val minecraftVersion: String,
    val reportFileName: String,
    val javaVersion: Int,
    val protocolConfiguration: Configuration,
    val serverDirectory: String = "server",
    val serverJarName: String = "server.jar",
    val requiresMohistBootstrap: Boolean = false,
)

enum class Platform {
    SPIGOT,
    PAPER,
    MOHIST,
    FORGE,
    NEOFORGE,
    FABRIC,
}
```

Suggested fixture naming:

| Current | Target |
|---|---|
| `test-plugin-1.16.5` | `1.16.5-paper` |
| `test-plugin-1.16.5-mohist` | `1.16.5-mohist` |
| `test-plugin-1.20.1-mohist` | `1.20.1-mohist` |
| `test-plugin-common` | `common` |

Fixtures are now named `{version}-{server}` (e.g. `1.20.6-paper`). Fixtures using the paper CommandLib module set `extra["commandlibModule"] = "paper"` explicitly (1.20.6+).

## Split Decision

Use aggregator first.

Reasons:

- Existing integration tests already share server bootstrap logic.
- Docker / server setup is expensive to duplicate.
- MCProtocolLib version isolation is already centralized.
- Paper-specific fixtures are not yet large enough to justify a separate module.
- Mohist needs special bootstrap but can still be represented as a target.

Split later when:

- Paper official API tests become large enough to require separate lifecycle setup.
- Forge / NeoForge / Fabric need different bootstrap models.
- Gradle task graph becomes too hard to maintain in one module.
- CI needs separate jobs with independent caching and failure reporting.

## Migration Plan

### Step 1: after module split

- Rename `bukkit-test` to `spigot-test`.
- Update dependency from `:bukkit` to `:spigot`.
- Keep public package names stable.
- Update root `publishedProjects`.
- Update README / docs references.

### Step 2: integration module rename

- Rename `bukkit-integration-test` to `integration-test`.
- Add `platform` to integration target metadata.
- Rename fixtures or add explicit platform metadata.
- Update Gradle task names and reports where useful.

### Step 3: Paper testing

- Add `paper-testing` after `paper` module public API exists.
- Add Paper-specific integration targets.
- Add Adventure component assertions.
- Add Paper command API registration tests.

### Step 4: Forge testing

- Add `forge-testing` after Forge module version strategy is defined.
- Add Forge integration targets only when server/mod bootstrap is reliable.

## Roadmap Checklist

- `[x]` Rename `bukkit-test` to `spigot-test`.
- `[x]` Rename `bukkit-integration-test` to `integration-test`.
- `[ ]` Add platform metadata to integration targets.
- `[ ]` Decide fixture naming convention.
- `[x]` Add `paper-testing` design after Paper module API is drafted.
- `[x]` Add `forge-testing` after Forge support strategy is updated.
- `[x]` Update README dependency snippets for test artifacts.
- `[ ]` Publish `spigot-testing` and `paper-testing`; keep `integration-test` unpublished.
