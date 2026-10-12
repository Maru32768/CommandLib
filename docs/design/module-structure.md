# Module Structure

This document describes CommandLib's module / artifact layout and the rules that keep it stable. The pre-GA split of the old `bukkit` module into `spigot` and `paper` is done; this document records the result and what is still open.

## Principles

- `common` holds the platform-neutral command model. Platform modules depend on `common`; `common` depends on no platform module.
- Do not move more code into `common` unless a clearly platform-neutral new feature appears.
- `spigot` is based on Bukkit/Spigot API compatibility and does not use Paper-only APIs.
- `paper` is based on the official Paper command API, lifecycle API, and Adventure components. It does not depend on `spigot`, so Spigot-first registration, message, and NMS assumptions do not leak into it.
- Bukkit-family code that would blur platform contracts if shared stays duplicated until a truly neutral abstraction appears.
- "Bukkit" remains the name of the API family (Bukkit API compatibility); it is not a module or artifact name.

## Current Layout

| Module | Published | Role |
|---|---:|---|
| `common` | Yes | Platform-independent command model, arguments, options, permissions, help, and Brigadier tree construction. |
| `spigot` | Yes | Bukkit/Spigot implementation. Includes the typed NMS bridge (bundles the `nms:*` modules), the Bungee `BaseComponent` message path, and the legacy command registration path. |
| `paper` | Yes | Paper implementation on the official Paper command API and Adventure components. |
| `modded` | Yes | Forge and NeoForge implementation. One shared source tree built as Stonecutter nodes `forge-1.16.5`, `forge-1.20.1`, and `neoforge-1.21.1` (Gradle paths `:modded:<node>`). |
| `nms:*` | No | Typed NMS modules bundled into `spigot`, one per directory under `nms/`. |
| `common-testing` | Yes | Test helpers for commands built against `common`. |
| `spigot-testing` | Yes | `CommandTester` / `FakeSender` and NMS mocks for downstream Spigot plugins. |
| `paper-testing` | Yes | Testing utilities for the Paper command API, Adventure components, and Paper registry arguments. |
| `modded-testing` | Yes | Serverless testing utilities for downstream Forge / NeoForge mods, built per node. |
| `integration-test` | No | Multi-platform real-server integration aggregator (Spigot, Paper, Mohist, Forge, NeoForge targets). |
| `samples` | No | Sample plugins / mods for Spigot, Paper, and Forge. |

What stays in `common`:

- `CommonCommand`, `CommonArgument`, `CommonCommandContext`
- command tree / branch model, command options
- default permission model
- common help / usage model
- exception model
- platform-neutral suggestion model

What does not go into `common`:

- Bukkit / Spigot / Paper / Forge / Fabric / Velocity types
- platform command registration and permission registration
- platform message component implementation
- NMS / reflection bridge
- Paper lifecycle API

## Artifact Names

| Gradle project | Artifact ID |
|---|---|
| `:common` | `common` |
| `:spigot` | `spigot` |
| `:paper` | `paper` |
| `:modded:forge-1.16.5` | `forge-1.16.5` |
| `:modded:forge-1.20.1` | `forge-1.20.1` |
| `:modded:neoforge-1.21.1` | `neoforge-1.21.1` |
| `:common-testing` | `common-testing` |
| `:spigot-testing` | `spigot-testing` |
| `:paper-testing` | `paper-testing` |
| `:modded-testing:forge-1.16.5` | `forge-testing-1.16.5` |
| `:modded-testing:forge-1.20.1` | `forge-testing-1.20.1` |
| `:modded-testing:neoforge-1.21.1` | `neoforge-testing-1.21.1` |

No `bukkit` artifact exists. `integration-test` is not published.

## Dependency Direction

```text
common
  ^
  |
  +-- spigot
  +-- paper
  +-- modded (forge-* / neoforge-* nodes)

common-testing   -> common
spigot-testing   -> spigot
paper-testing    -> paper
modded-testing   -> modded
integration-test -> platform modules
```

## Public API Shape

### Spigot

The `spigot` artifact exposes `dev.marulabs.commandlib.Command`, `CommandContext`, `CommandLib`, and the Bukkit/Spigot argument classes.

Responsibilities:

- Bukkit/Spigot command registration and permission registration.
- Bukkit sender/context bridge.
- NMS fallback where no official API exists (Spigot `1.16.5` to `1.21.1`).
- Legacy text / Bungee `BaseComponent` compatibility.
- Old Paper compatibility as long as it matches Spigot-oriented behavior.

### Paper

The `paper` artifact provides the same high-level command model without being constrained by Spigot internals.

Responsibilities:

- Paper official command API / lifecycle registration.
- Adventure component message path.
- Paper registry-backed arguments, predicates, and Adventure arguments.
- Paper-native suggestion behavior.
- NMS fallback only where Paper official APIs do not cover the required behavior.

### Forge / NeoForge

`modded` exposes the same command model where practical, using platform-native registration, permission, message, and registry APIs. Public API exposes each Minecraft version's native types. See `forge-version-support.md`.

## Not Yet Implemented

These platforms have no module. Adding one should follow the rules above (platform module depends on `common`, no code moved into `common`).

| Platform | Notes |
|---|---|
| Folia | Paper-family; the scheduler / threading model must be handled. |
| Velocity | Proxy command model; whether it shares the server-side API needs design. |
| Fabric | Command registration / Brigadier integration; may become another `modded` loader or a separate module. |

Mohist has no module of its own. It is handled through `spigot` compatibility and the Mohist integration targets.

## Open Questions

- Should `paper` share any source with `spigot`, or should duplication be accepted until a true neutral abstraction appears?
- Should `folia` be a separate artifact or a Paper runtime mode?
- Should `mohist` stay tested under `spigot`, or become its own artifact if its differences grow?
- Should message APIs expose CommandLib's own abstraction, Adventure, or platform-native components?
- Should `modded` be split into a Minecraft-version module and loader modules once Fabric is added (see `docs/roadmap.md`)?
