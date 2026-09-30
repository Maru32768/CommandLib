# Module Structure

この文書は、GA 前に CommandLib の module / artifact 構成を整理するための設計メモである。

## 前提

現行実装は、すでに `common` / `bukkit` / `forge` にかなり分離されている。ここからさらに大きく `common` へ移動できるものは多くない。

重要な前提:

- 現在の `bukkit` module は、実質的には **Spigot 互換実装** である。
- Paper 上でも動かすことを考慮しているが、設計の主軸は Bukkit/Spigot compatibility である。
- NMS util は Spigot / old Paper を支える compatibility layer であり、一部 Paper も考慮している。
- `common` と platform modules の責務分離はすでに進んでいるため、GA 前 module split の主目的は追加抽象化ではない。
- GA 前 module split の主目的は、artifact 名と platform contract を確定することである。

## 現状

| Module | 現状 |
|---|---|
| `common` | platform 非依存の command model、argument、options、permissions、help、Brigadier tree construction。 |
| `spigot` | Spigot 互換を主軸にした Bukkit-family implementation。NMS bridge、Bungee `BaseComponent` message path、Paper 1.20.6+ 向け考慮を含む。 |
| `forge` | Forge 1.16.5 向け implementation。Forge permission、Minecraft `ITextComponent` message path を含む。 |
| `spigot-test` | Bukkit-family command testing utilities。 |
| `integration-test` | Bukkit / Paper / Mohist の multi-version integration fixtures。 |
| `sample` | sample plugin / mod。 |

## Goals

- GA 前に platform-specific artifact 境界を確定する。
- 現行 `bukkit` module を `spigot` module へ rename / move する。
- `bukkit` artifact は GA 前に削除する。
- Paper module は Spigot 実装の延長ではなく、Paper 公式 API と Adventure component を前提に新設する。
- NMS fallback はまず `spigot` 側に置く。
- Paper 側で NMS fallback が必要な場合だけ、用途を限定して Paper module に持つ。
- `common` への追加移動は原則行わない。明らかに platform-neutral な新機能が出た場合だけ検討する。
- Forge / NeoForge / Fabric / Velocity / Folia を追加しやすい artifact 構成にする。

## Non-Goals

- `common` をさらに大きくすることを module split の目的にしない。
- Spigot 互換のための legacy behavior を Paper module に持ち込まない。
- Paper-only API を Spigot module に持ち込まない。
- `bukkit` artifact を GA 後に残さない。
- すべての platform support を module split と同時に完成させることを前提にしない。

## Target Module Layout

### Core

| Module | Published | Role |
|---|---:|---|
| `common` | Yes | 既存の platform-neutral command model。大規模な追加移動はしない。 |

`common` に残すもの:

- `CommonCommand`
- `CommonArgument`
- `CommonCommandContext`
- command tree / branch model
- command options
- default permission model
- common help / usage model
- exception model
- platform-neutral suggestion model

`common` に入れないもの:

- Bukkit / Spigot / Paper / Forge / Fabric / Velocity type
- platform command registration
- platform permission registration
- platform message component implementation
- NMS / reflection bridge
- Paper lifecycle API

### Server Platforms

| Module | Published | Role |
|---|---:|---|
| `spigot` | Yes | 現行 `bukkit` module の移行先。Bukkit/Spigot API compatibility を基準にする。 |
| `paper` | Yes | Paper 公式 command API、lifecycle API、Adventure component を基準にした新規 implementation。 |
| `folia` | Yes | Folia threading / scheduler model に合わせた Paper-family implementation。`paper` runtime mode にするか別 artifact にするかは要設計。 |
| `mohist` | Maybe | Mohist 固有差分が大きい場合に追加する。まずは `spigot` compatibility と integration test で扱う。 |
| `forge` | Yes | 現行 Forge module を維持しつつ、新 version support に備える。 |
| `neoforge` | Yes | NeoForge 向け implementation。Forge との差分を分離する。 |
| `fabric` | Yes | Fabric command registration / Brigadier integration 向け implementation。 |

### Proxy Platforms

| Module | Published | Role |
|---|---:|---|
| `velocity` | Yes | Velocity proxy command implementation。server-side command model と同じ API に載せるかは要設計。 |

### Test Modules

| Module | Published | Role |
|---|---:|---|
| `common-test` | Maybe | common parser / tree / help / option test helpers。 |
| `spigot-test` | Yes | 現行 `bukkit-test` の移行先候補。Spigot/Bukkit command testing utilities。 |
| `paper-testing` | Yes | Paper command API / Adventure / lifecycle 対応の testing utilities。 |
| `forge-test` | Yes | Forge command testing utilities。 |
| `integration-test` | No | multi-platform integration test aggregator。 |

testing module は module split 後に再設計する。Forge、Paper、Folia も testing 対象になるため、現時点で `bukkit-test` 前提の public testing guide を固定しない。

### Removed Module

| Module | Published | Role |
|---|---:|---|
| `bukkit` | No | 現行暫定 module。`spigot` へ rename / move した後、GA 前に削除する。 |

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

- platform modules depend on `common`; `common` depends on no platform module。
- `spigot` is the successor of the current `bukkit` module。
- `paper` should not depend on `spigot` if that would import Spigot-first registration, message, or NMS assumptions。
- shared code should move to `common` only when it is truly platform-neutral。
- Bukkit-family-specific shared code should stay duplicated initially if extracting it would blur platform contracts。

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

No `bukkit` artifact is published for GA。

## Public API Shape

### Spigot

The `spigot` artifact should preserve the current Bukkit-family public API shape as much as possible:

- `net.kunmc.lab.commandlib.Command`
- `net.kunmc.lab.commandlib.CommandContext`
- `net.kunmc.lab.commandlib.CommandLib`
- current Bukkit/Spigot argument classes

Responsibilities:

- Bukkit/Spigot command registration。
- Bukkit permission registration。
- Bukkit sender/context bridge。
- NMS fallback for `1.16.5+` where no official API exists。
- legacy text / Bungee `BaseComponent` compatibility where needed。
- old Paper compatibility as long as it is compatible with Spigot-oriented behavior。

### Paper

The `paper` artifact should provide the same high-level command model, but should not be constrained by Spigot internals.

Responsibilities:

- Paper official command API / lifecycle registration。
- Adventure component message path。
- Paper registry-backed arguments。
- Paper-specific predicates / Adventure arguments。
- Paper-native suggestion behavior。
- NMS fallback only where Paper official APIs do not cover the required behavior。

### Forge / NeoForge / Fabric

Each modded platform module should expose the same command model where practical, while using platform-native registration, permission, message, and registry APIs。

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

- Add `spigot` module。
- Move current `bukkit` source into `spigot` with minimal behavior changes。
- Keep package names stable unless conflicts require otherwise。
- Update published projects from `bukkit` to `spigot`。
- Update samples and README dependency snippets。

### Step 2: remove Bukkit artifact

- Remove `bukkit` from `settings.gradle.kts`。
- Remove `bukkit` from publishing。
- Update docs to stop referencing `bukkit` as an artifact。
- Keep “Bukkit API compatibility” as a concept under the `spigot` artifact, not as an artifact name。

### Step 3: create Paper module

- Add `paper` module。
- Start with the same public model as `spigot` where useful。
- Implement registration through Paper official APIs。
- Use Adventure component as the standard message path。
- Add Paper-specific argument and suggestion behavior。
- Decide explicitly which NMS utilities, if any, Paper still needs。

### Step 4: redesign testing modules

- Rename or replace `bukkit-test` with `spigot-test`。
- Add `paper-testing` after Paper module behavior is defined。
- Add `forge-test` when Forge testing requirements are clear。
- Update integration fixtures to declare whether they test `spigot`, `paper`, or `mohist` behavior。

### Step 5: update migration docs

- Add pre-GA migration notes from `bukkit` artifact to `spigot` / `paper`。
- Update compatibility docs with artifact names。
- Update argument catalog platform columns。
- Update README install snippets。

## Open Questions

- Should `paper` share any source with `spigot`, or should duplication be accepted until a true neutral abstraction appears?
- Should `folia` be a separate artifact or a Paper runtime mode?
- Should `mohist` be a separate artifact or tested under `spigot`?
- Should message APIs expose CommandLib's own abstraction, Adventure, or platform-native components?
- Are there any public `bukkit` APIs that need source-compatible replacements in both `spigot` and `paper`?
- Should test artifacts be published for every platform, or only for platforms with stable fake server support?

## Recommended GA Gate

Before GA:

- `bukkit` module has been renamed/moved to `spigot`。
- `bukkit` artifact is removed。
- `paper` module exists。
- README no longer recommends `bukkit` as an artifact。
- platform-specific examples exist for Spigot and Paper。
- compatibility docs list artifact names per platform。
- integration fixtures target explicit platform artifacts。
- module dependency graph follows the direction rules in this document。
