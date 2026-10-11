# Module Structure

This document is a design note for organizing CommandLib's module / artifact layout before GA.

## Assumptions

The current implementation is already fairly well separated into `common` / `bukkit` / `forge`. Not much more can be moved into `common` from here.

Key assumptions:

- The current `bukkit` module is effectively a **Spigot-compatible implementation**.
- Running on Paper is taken into account, but the design is centered on Bukkit/Spigot compatibility.
- The NMS util is a compatibility layer that supports Spigot / old Paper, with some consideration for Paper.
- Responsibilities are already separated between `common` and the platform modules, so the main goal of the pre-GA module split is not more abstraction.
- The main goal of the pre-GA module split is to settle the artifact names and platform contracts.

## Current State

| Module | Current state |
|---|---|
| `common` | Platform-independent command model, arguments, options, permissions, help, and Brigadier tree construction. |
| `spigot` | Bukkit-family implementation centered on Spigot compatibility. Includes the NMS bridge, the Bungee `BaseComponent` message path, and considerations for Paper 1.20.6+. |
| `forge` | Implementation for Forge 1.16.5. Includes Forge permissions and the Minecraft `ITextComponent` message path. |
| `spigot-test` | Bukkit-family command testing utilities. |
| `integration-test` | Multi-version integration fixtures for Bukkit / Paper / Mohist. |
| `sample` | Sample plugin / mod. |

## Goals

- Settle the platform-specific artifact boundaries before GA.
- Rename / move the current `bukkit` module to a `spigot` module.
- Remove the `bukkit` artifact before GA.
- Create the Paper module from scratch on top of the official Paper API and Adventure components, rather than as an extension of the Spigot implementation.
- Put the NMS fallback in `spigot` first.
- Keep an NMS fallback in the Paper module only when Paper needs one, and only for limited purposes.
- As a rule, do not move more code into `common`. Consider it only when a clearly platform-neutral new feature appears.
- Make the artifact layout easy to extend with Forge / NeoForge / Fabric / Velocity / Folia.

## Non-Goals

- Making `common` bigger is not a goal of the module split.
- Do not bring legacy behavior kept for Spigot compatibility into the Paper module.
- Do not bring Paper-only APIs into the Spigot module.
- Do not keep the `bukkit` artifact after GA.
- Do not assume every platform is fully supported at the same time as the module split.

## Target Module Layout

### Core

| Module | Published | Role |
|---|---:|---|
| `common` | Yes | The existing platform-neutral command model. No large-scale moves into it. |

What stays in `common`:

- `CommonCommand`
- `CommonArgument`
- `CommonCommandContext`
- command tree / branch model
- command options
- default permission model
- common help / usage model
- exception model
- platform-neutral suggestion model

What does not go into `common`:

- Bukkit / Spigot / Paper / Forge / Fabric / Velocity types
- platform command registration
- platform permission registration
- platform message component implementation
- NMS / reflection bridge
- Paper lifecycle API

### Server Platforms

| Module | Published | Role |
|---|---:|---|
| `spigot` | Yes | Successor of the current `bukkit` module. Based on Bukkit/Spigot API compatibility. |
| `paper` | Yes | New implementation based on the official Paper command API, lifecycle API, and Adventure components. |
| `folia` | Yes | Paper-family implementation adapted to the Folia threading / scheduler model. Whether it becomes a `paper` runtime mode or a separate artifact needs design. |
| `mohist` | Maybe | Added if Mohist-specific differences are large. For now, handled through `spigot` compatibility and integration tests. |
| `forge` | Yes | Keep the current Forge module while preparing for new version support. |
| `neoforge` | Yes | Implementation for NeoForge. Keeps differences from Forge separate. |
| `fabric` | Yes | Implementation for Fabric command registration / Brigadier integration. |

### Proxy Platforms

| Module | Published | Role |
|---|---:|---|
| `velocity` | Yes | Velocity proxy command implementation. Whether it shares the same API as the server-side command model needs design. |

### Test Modules

| Module | Published | Role |
|---|---:|---|
| `common-test` | Maybe | common parser / tree / help / option test helpers. |
| `spigot-test` | Yes | Candidate successor of the current `bukkit-test`. Spigot/Bukkit command testing utilities. |
| `paper-testing` | Yes | Testing utilities supporting the Paper command API / Adventure / lifecycle. |
| `forge-test` | Yes | Forge command testing utilities. |
| `integration-test` | No | Multi-platform integration test aggregator. |

The testing modules will be redesigned after the module split. Forge, Paper, and Folia will also be testing targets, so the public testing guide should not be fixed around `bukkit-test` at this point.

### Removed Module

| Module | Published | Role |
|---|---:|---|
| `bukkit` | No | Current interim module. Removed before GA after being renamed / moved to `spigot`. |

## Dependency Direction

Target dependency graph:

```text
common
  ^
  |
  +-- spigot
  +-- paper
  +-- folia
  +-- forge
  +-- neoforge
  +-- fabric
  +-- velocity

common-test -> common
spigot-test -> spigot
paper-testing -> paper
forge-test -> forge
integration-test -> platform modules
```

Rules:

- platform modules depend on `common`; `common` depends on no platform module.
- `spigot` is the successor of the current `bukkit` module.
- `paper` should not depend on `spigot` if that would import Spigot-first registration, message, or NMS assumptions.
- shared code should move to `common` only when it is truly platform-neutral.
- Bukkit-family-specific shared code should stay duplicated initially if extracting it would blur platform contracts.

## Artifact Names

| Module | Artifact ID |
|---|---|
| `common` | `common` |
| `spigot` | `spigot` |
| `paper` | `paper` |
| `folia` | `folia` |
| `forge` | `forge` |
| `neoforge` | `neoforge` |
| `fabric` | `fabric` |
| `velocity` | `velocity` |
| `spigot-test` | `spigot-test` |
| `paper-testing` | `paper-testing` |
| `forge-test` | `forge-test` |

No `bukkit` artifact is published for GA.

## Public API Shape

### Spigot

The `spigot` artifact should preserve the current Bukkit-family public API shape as much as possible:

- `net.kunmc.lab.commandlib.Command`
- `net.kunmc.lab.commandlib.CommandContext`
- `net.kunmc.lab.commandlib.CommandLib`
- current Bukkit/Spigot argument classes

Responsibilities:

- Bukkit/Spigot command registration.
- Bukkit permission registration.
- Bukkit sender/context bridge.
- NMS fallback for `1.16.5+` where no official API exists.
- legacy text / Bungee `BaseComponent` compatibility where needed.
- old Paper compatibility as long as it is compatible with Spigot-oriented behavior.

### Paper

The `paper` artifact should provide the same high-level command model, but should not be constrained by Spigot internals.

Responsibilities:

- Paper official command API / lifecycle registration.
- Adventure component message path.
- Paper registry-backed arguments.
- Paper-specific predicates / Adventure arguments.
- Paper-native suggestion behavior.
- NMS fallback only where Paper official APIs do not cover the required behavior.

### Forge / NeoForge / Fabric

Each modded platform module should expose the same command model where practical, while using platform-native registration, permission, message, and registry APIs.

## Migration Model

### What the split really means

The split is not primarily:

- moving more code into `common`;
- making one generic Bukkit-family abstraction;
- making Paper depend on Spigot;
- preserving the `bukkit` artifact.

The split is primarily:

- renaming/moving current `bukkit` implementation to `spigot`;
- deleting the `bukkit` artifact before GA;
- creating a Paper-first implementation in `paper`;
- updating docs, samples, fixtures, and publishing to use platform-specific artifacts.

### Step 1: rename current Bukkit implementation to Spigot

- Add `spigot` module.
- Move current `bukkit` source into `spigot` with minimal behavior changes.
- Keep package names stable unless conflicts require otherwise.
- Update published projects from `bukkit` to `spigot`.
- Update samples and README dependency snippets.

### Step 2: remove Bukkit artifact

- Remove `bukkit` from `settings.gradle.kts`.
- Remove `bukkit` from publishing.
- Update docs to stop referencing `bukkit` as an artifact.
- Keep “Bukkit API compatibility” as a concept under the `spigot` artifact, not as an artifact name.

### Step 3: create Paper module

- Add `paper` module.
- Start with the same public model as `spigot` where useful.
- Implement registration through Paper official APIs.
- Use Adventure component as the standard message path.
- Add Paper-specific argument and suggestion behavior.
- Decide explicitly which NMS utilities, if any, Paper still needs.

### Step 4: redesign testing modules

- Rename or replace `bukkit-test` with `spigot-test`.
- Add `paper-testing` after Paper module behavior is defined.
- Add `forge-test` when Forge testing requirements are clear.
- Update integration fixtures to declare whether they test `spigot`, `paper`, or `mohist` behavior.

### Step 5: update migration docs

- Add pre-GA migration notes from `bukkit` artifact to `spigot` / `paper`.
- Update compatibility docs with artifact names.
- Update argument catalog platform columns.
- Update README install snippets.

## Open Questions

- Should `paper` share any source with `spigot`, or should duplication be accepted until a true neutral abstraction appears?
- Should `folia` be a separate artifact or a Paper runtime mode?
- Should `mohist` be a separate artifact or tested under `spigot`?
- Should message APIs expose CommandLib's own abstraction, Adventure, or platform-native components?
- Are there any public `bukkit` APIs that need source-compatible replacements in both `spigot` and `paper`?
- Should test artifacts be published for every platform, or only for platforms with stable fake server support?

## Recommended GA Gate

Before GA:

- `bukkit` module has been renamed/moved to `spigot`.
- `bukkit` artifact is removed.
- `paper` module exists.
- README no longer recommends `bukkit` as an artifact.
- platform-specific examples exist for Spigot and Paper.
- compatibility docs list artifact names per platform.
- integration fixtures target explicit platform artifacts.
- module dependency graph follows the direction rules in this document.
