# Testing Module Structure

This document defines how the test modules and the integration-test module are organized now that the `spigot` / `paper` module split is done.

## Assumptions

- `spigot-testing` depends on `:spigot` and provides Spigot/Bukkit command testing utilities and NMS mocks.
- `integration-test` handles Spigot / Paper / Mohist / Forge / NeoForge fixtures for multiple Minecraft versions in a single module.
- Forge, Paper, and Folia are also testing targets, so the public testing guide is not built around Spigot alone.

## Goals

- Separate public testing artifacts from repository-only integration tests.
- Handle Paper-only behavior in `paper-testing` and Paper integration targets.
- Provide a serverless Forge / NeoForge testing story (`modded-testing`).
- Keep the aggregator approach for integration tests at first, and make platform targets explicit.

## Non-Goals

- Do not publish the integration-test module as a public artifact.
- Do not mix Paper-specific tests into the Spigot testing utilities.
- Do not fold Forge testing into the Spigot/Paper testing utilities.

## Target Test Modules

### Public test artifacts

| Module | Published | Role |
|---|---:|---|
| `common-testing` | Yes | Test helpers for commands built against the platform-neutral `common` API. |
| `spigot-testing` | Yes | Command testing utilities for Spigot/Bukkit plugins. Includes NMS mocks and a fake sender. |
| `paper-testing` | Yes | Testing utilities for the Paper command API, Adventure components, Paper suggestions, and Paper registry arguments. |
| `velocity-testing` | Yes | Velocity command testing utilities: Mockito `Player` / `ConsoleCommandSource` senders and a Mockito `ProxyServer` that knows fake players and servers. Proxy registration is not reproduced. |
| `modded-testing` | Yes | Forge / NeoForge / Fabric command testing utilities, published as `forge-testing-<version>`, `neoforge-testing-<version>` and `fabric-testing-<version>` (one artifact per node, like `modded`). |

### Repository-only integration modules

| Module | Published | Role |
|---|---:|---|
| `integration-test` | No | Multi-platform integration test aggregator. |
| `spigot-integration-test` | No | Future split target if the aggregator grows too large. |
| `paper-integration-test` | No | Future split target if Paper official API / Adventure / latest Paper tests grow. |
| `mohist-integration-test` | No | Future split target if Mohist-specific differences grow. |
| `modded-integration-test` | No | Future split target for the Forge / NeoForge server/mod environment. |

Current approach:

- Use the `integration-test` aggregator as the baseline (renamed from `bukkit-integration-test`).
- The `spigot`, `paper`, `mohist`, and modded platforms are explicit in target IDs / fixture names.
- Split out `paper-integration-test` once Paper-only fixtures grow and the aggregator becomes hard to manage.

## `spigot-testing`

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

The public package is `dev.marulabs.commandlib`. The NMS mock package stays aligned with the NMS util in the Spigot module.

## `paper-testing`

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

## `modded-testing`

`modded-testing` uses the same Stonecutter setup as `modded` and publishes `forge-testing-<version>` / `neoforge-testing-<version>` / `fabric-testing-<version>` per node.

Responsibilities:

- Forge command source / sender fake.
- Forge permission assertions.
- Forge text component assertions.
- Forge argument parsing helpers.
- public testing utilities for downstream Forge mods.

Decisions:

- Like `modded`, the testing API is split into per-version artifacts. The shape of the public API (`CommandTester` / `FakeSender`) is
  the same across all versions.
- Run Minecraft's `Bootstrap` to initialize the registries, and parse vanilla argument types with the real implementations. The
  `NetworkHooks.init` called by `Bootstrap` on Forge 1.17+ assumes FML's class transformer, so it is mocked only during bootstrap.
- `MinecraftServer` / `PlayerList` / `GameProfileCache` are Mockito mocks. Fake players can be resolved by name.
- Permission nodes are answered by the running `FakeSender` (1.18+ mocks `CommandLib#hasPermission`, 1.16.5 mocks
  `PermissionAPI#hasPermission`, only during execution).

Open questions:

- Whether to fake arguments that need a world / scoreboard (such as `TeamArgument`).

## `integration-test` Aggregator

`integration-test` is an aggregator with multi-version targets.

Keep:

- one Gradle module that owns Docker / server bootstrap / MCProtocolLib wiring.
- target list with Minecraft version, Java version, fixture name, protocol configuration.
- shared fixture source.
- report file naming per target.

Remaining changes:

- platform is currently a string property (`"paper"`, `"mohist"`, `"spigot"`) in the shared Bukkit-family script; the modded targets use a separate shared script.
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

## Remaining Work

- Decide whether the platform metadata should become a typed model instead of the current per-script `platform` property.
- Add a `fabric-1.21.4` integration target. `fabric-1.20.1`, `fabric-1.21.1`, `folia-26.3` and `velocity-3.4.0`
  exist; `folia-26.3` and `velocity-3.4.0` are smoke targets.
- Split `paper-integration-test` or `modded-integration-test` out of the aggregator when the "Split later when" conditions above hold.

## Roadmap Checklist

- `[x]` Rename `bukkit-test` to `spigot-testing` and `bukkit-integration-test` to `integration-test`.
- `[x]` Add `paper-testing`, `common-testing`, and `modded-testing`.
- `[x]` Decide fixture naming: `{version}-{server}` (for example `1.20.6-paper`).
- `[x]` Update README dependency snippets for test artifacts.
- `[ ]` Typed platform metadata for integration targets.
- `[ ]` Publish `spigot-testing` and `paper-testing` to the final repository; keep `integration-test` unpublished.
