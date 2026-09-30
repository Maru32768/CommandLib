# Testing Module Structure

この文書は、GA 前の `spigot` / `paper` module split 後に、test module と integration-test module をどう整理するかを定義する。

## 前提

- 現行 `bukkit` module は実質的な Spigot 互換実装である。
- 現行 `bukkit-test` は `:bukkit` に依存し、Spigot/Bukkit command testing utilities と NMS mock を提供している。
- 現行 `bukkit-integration-test` は、Bukkit / Paper / Mohist の複数 Minecraft version fixtures を 1 module で扱っている。
- Forge、Paper、Folia も testing 対象になるため、`bukkit-test` 前提の public guide を先に固定しない。

## Goals

- public testing artifacts と repository-only integration tests を分離する。
- 現行 `bukkit-test` を `spigot-test` に移行する。
- Paper 専用 behavior は `paper-testing` と Paper integration targets で扱う。
- Forge testing story を後から追加できる余地を残す。
- integration test は最初は aggregator 方式を維持し、platform target を明示する。

## Non-Goals

- `bukkit-test` artifact を GA 後に残さない。
- integration-test module を public artifact として公開しない。
- Paper-specific tests を Spigot testing utilities に混ぜない。
- Forge testing を Spigot/Paper testing utilities に寄せない。

## Target Test Modules

### Public test artifacts

| Module | Published | Successor of | Role |
|---|---:|---|---|
| `common-test` | Maybe | none | `common` の parser / command tree / options / help / permission model の test helpers。公開するかは要検討。 |
| `spigot-test` | Yes | `bukkit-test` | Spigot/Bukkit plugin 向け command testing utilities。NMS mock と fake sender を含む。 |
| `paper-testing` | Yes | none | Paper command API、Adventure component、Paper suggestions、Paper registry arguments の testing utilities。 |
| `forge-test` | Yes | none | Forge command testing utilities。Forge module の testing story が決まってから追加する。 |

### Repository-only integration modules

| Module | Published | Role |
|---|---:|---|
| `integration-test` | No | multi-platform integration test aggregator。現行 `bukkit-integration-test` の rename 先候補。 |
| `spigot-integration-test` | No | 将来、aggregator が肥大化した場合の分割先。 |
| `paper-integration-test` | No | 将来、Paper official API / Adventure / latest Paper tests が増えた場合の分割先。 |
| `mohist-integration-test` | No | 将来、Mohist 固有差分が増えた場合の分割先。 |
| `forge-integration-test` | No | Forge server/mod environment の integration tests。 |

Recommended:

- GA 前は `integration-test` aggregator 方式を基本にする。
- `bukkit-integration-test` は `integration-test` に rename する。
- target ID / fixture 名で `spigot`, `paper`, `mohist` を明示する。
- Paper 専用 fixture が増えて aggregator が扱いづらくなった時点で `paper-integration-test` を分離する。

## Current Module Mapping

| Current | Target | Notes |
|---|---|---|
| `bukkit-test` | `spigot-test` | 現行 `bukkit` が `spigot` に移るため、test artifact も rename する。 |
| `bukkit-integration-test` | `integration-test` | multi-platform aggregator として rename する。 |
| `bukkit-integration-test/fixtures/test-plugin-*` | `integration-test/fixtures/spigot-*` or target metadata | fixture 名だけでなく platform target を明示する。 |
| `bukkit-integration-test/fixtures/*-mohist` | `integration-test/fixtures/mohist-*` | Mohist target として明示する。 |
| `test-plugin-common` | `common` | Spigot/Paper/Mohist 共通 fixture source として維持する。 |

## `spigot-test`

`spigot-test` は現行 `bukkit-test` の後継である。

Responsibilities:

- fake Bukkit sender / command sender。
- Spigot command execution assertions。
- Spigot tab completion / suggestion assertions。
- Bukkit permission model mocks。
- NMS mock classes for Spigot-compatible arguments。
- downstream Spigot plugin 向け public testing utilities。

Dependencies:

- `api(project(":spigot"))`
- Spigot API compileOnly / testImplementation。
- Brigadier。
- Mockito / AssertJ / JUnit where needed。

Migration from `bukkit-test`:

- `bukkit-test` module を `spigot-test` に rename / move。
- `api(project(":bukkit"))` を `api(project(":spigot"))` に変更。
- public package は `net.kunmc.lab.commandlib` を維持する。
- NMS mock package は当面維持し、Spigot module の NMS util に合わせる。
- README / downstream docs は `spigot-test` artifact を案内する。

## `paper-testing`

`paper-testing` は新設する。

Responsibilities:

- Adventure component message assertions。
- Paper suggestion / tooltip assertions。
- Paper registry argument fake / adapter。
- Paper `ArgumentTypes` / selector resolver fake。
- downstream Paper plugin 向け public testing utilities。

Not responsibilities:

- Paper command API registration / lifecycle API の検証。これは `integration-test` の実 Paper server targets で保証する。
- Spigot legacy command map fallback の検証。
- Bungee `BaseComponent` compatibility。
- Spigot NMS fallback の mock 全般。

Open questions:

- Adventure component assertion を CommandLib 独自に持つか、Kyori test utilities に寄せるか。
- Folia-safe behavior を `paper-testing` に含めるか、`folia-test` を別に作るか。

## `forge-test`

`forge-test` は Forge module 境界が固まってから追加する。

Responsibilities:

- Forge command source / sender fake。
- Forge permission assertions。
- Forge text component assertions。
- Forge argument parsing helpers。
- downstream Forge mod 向け public testing utilities。

Open questions:

- Forge 1.16.5 と新しい Forge の testing API を同じ artifact に入れるか。
- NeoForge と共有できる test utilities を作るか。
- server bootstrap なしでどこまで command execution を fake できるか。

## `integration-test` Aggregator

現行 `bukkit-integration-test` は、すでに multi-version targets を持つ aggregator に近い。

Keep:

- one Gradle module that owns Docker / server bootstrap / MCProtocolLib wiring。
- target list with Minecraft version, Java version, fixture name, protocol configuration。
- shared fixture source。
- report file naming per target。

Change:

- module name: `bukkit-integration-test` -> `integration-test`。
- target metadata に platform を追加する。
- fixture names に platform を含める。
- dependencies in fixtures should target `spigot`, `paper`, or `mohist` explicitly。
- task names should include platform where useful。

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

- Existing integration tests already share server bootstrap logic。
- Docker / server setup is expensive to duplicate。
- MCProtocolLib version isolation is already centralized。
- Paper-specific fixtures are not yet large enough to justify a separate module。
- Mohist needs special bootstrap but can still be represented as a target。

Split later when:

- Paper official API tests become large enough to require separate lifecycle setup。
- Forge / NeoForge / Fabric need different bootstrap models。
- Gradle task graph becomes too hard to maintain in one module。
- CI needs separate jobs with independent caching and failure reporting。

## Migration Plan

### Step 1: after module split

- Rename `bukkit-test` to `spigot-test`。
- Update dependency from `:bukkit` to `:spigot`。
- Keep public package names stable。
- Update root `publishedProjects`。
- Update README / docs references。

### Step 2: integration module rename

- Rename `bukkit-integration-test` to `integration-test`。
- Add `platform` to integration target metadata。
- Rename fixtures or add explicit platform metadata。
- Update Gradle task names and reports where useful。

### Step 3: Paper testing

- Add `paper-testing` after `paper` module public API exists。
- Add Paper-specific integration targets。
- Add Adventure component assertions。
- Add Paper command API registration tests。

### Step 4: Forge testing

- Add `forge-test` after Forge module version strategy is defined。
- Add Forge integration targets only when server/mod bootstrap is reliable。

## Roadmap Checklist

- `[x]` Rename `bukkit-test` to `spigot-test`。
- `[x]` Rename `bukkit-integration-test` to `integration-test`。
- `[ ]` Add platform metadata to integration targets。
- `[ ]` Decide fixture naming convention。
- `[x]` Add `paper-testing` design after Paper module API is drafted。
- `[ ]` Add `forge-test` design after Forge support strategy is updated。
- `[x]` Update README dependency snippets for test artifacts。
- `[ ]` Publish `spigot-testing` and `paper-testing`; keep `integration-test` unpublished。
