# CommandLib Compatibility

この文書は、CommandLib の platform / Minecraft version 対応を「現在の実装」と「目標」に分けて整理する。

## 方針

CommandLib の長期目標は、Minecraft `1.16.5` から各リリース時点の最新安定版までをサポートすることである。

対象 platform の目標範囲:

- Spigot
- Paper
- Folia
- Mohist
- Velocity
- Forge
- NeoForge
- Fabric

## Status の定義

| Status | 意味 |
|---|---|
| `Supported` | public API としてサポート対象。通常の修正対象に含める。 |
| `Tested` | repository の test / integration test / fixture で確認している。 |
| `Best effort` | 動作を目標にするが、まだ十分な test coverage や platform-specific 実装がない。 |
| `Planned` | Roadmap 上の対応予定。現時点では未実装。 |
| `Unsupported` | 現時点では対応しない。 |

## 現在の実装状況

| Platform | Status | Current coverage | Notes |
|---|---:|---|---|
| Spigot | `Best effort` | `spigot` module。compile target は Spigot API `1.16.5`。typed NMS module により `1.16.5`〜`1.21.1` を扱う。 | Bukkit API 互換を基準にし、Paper-specific API に依存しない。 |
| Paper | `Best effort` | `paper` module と `paper-testing`。Paper 公式 command API と Adventure component を使う。 | Paper `1.16.5`〜`1.21.1` と `1.21.4` の integration target がある（`1.21.0` と `1.21.4` は paper module）。 |
| Folia | `Planned` | なし。 | scheduler、threading、command registration の安全性確認が必要。 |
| Mohist | `Best effort` | integration fixture: `1.16.5`, `1.20.1`。README に tested version として記載あり。 | Bukkit compatibility 上で動かす方針。Forge side との干渉確認が必要。 |
| Velocity | `Planned` | なし。 | proxy command model と Minecraft server command model の差分設計が必要。 |
| Forge | `Best effort` | `modded` の Stonecutter tree の `forge-1.16.5` / `forge-1.20.1` node（artifact: `forge-1.16.5` / `forge-1.20.1`）。`forge-testing-<version>` と integration target あり。 | 上記以外の Forge version は未対応。 |
| NeoForge | `Best effort` | `modded` の Stonecutter tree の `neoforge-1.21.1` node（artifact: `neoforge-1.21.1`）。`neoforge-testing-1.21.1` と integration target `neoforge-1.21.1` あり。 | Forge と同じソースを `//? if neoforge` で分岐。NeoForge 1.21.1 以外の version は未対応。 |
| Fabric | `Planned` | なし。 | Fabric command registration と Brigadier integration の設計が必要。 |

## Version coverage

CommandLib は `1.16.5` から最新安定版までのサポートを目標にする。ただし、すべての platform で同じ version range を同時に保証するわけではない。

| Minecraft version | Current evidence | Target |
|---|---|---|
| `1.16.5` | Paper fixture、Mohist fixture、Forge node。 | Core supported baseline。 |
| `1.17.x` | Paper `1.17.1` fixture、typed NMS module（`1.17`、`1.17.1`）。`1.17` は MCProtocolLib が 1.17 の protocol に未対応のため fixture なし（module のコンパイルのみで確認）。 | `1.17.1` は Tested、`1.17` は Supported。 |
| `1.18.x` | Paper `1.18` / `1.18.1` / `1.18.2` fixture、typed NMS module。 | Tested。 |
| `1.19.x` | Paper `1.19`〜`1.19.4` fixture、typed NMS module。 | Tested。 |
| `1.20` / `1.20.1` | Paper fixture、Mohist `1.20.1` fixture、typed NMS module（`1.20` は `1.20.1` の module）。 | Tested。 |
| `1.20.2` | Paper fixture、typed NMS module。 | Tested。 |
| `1.20.3` | Spigot fixture、typed NMS module。Paper 1.20.3 は存在しない。 | Tested。 |
| `1.20.4` | Paper / Spigot fixture、typed NMS module。 | Tested。 |
| `1.20.5` | Paper / Spigot fixture、typed NMS module。 | Tested。 |
| `1.20.6` | Paper / Spigot fixture、typed NMS module。Paper 1.20.6+ registration path がある。 | Tested。Paper official API path の候補。 |
| `1.21` / `1.21.1` | Paper `1.21.0` fixture（paper module）、Paper `1.21.1` fixture（spigot module）、Spigot `1.21` / `1.21.1` fixture、typed NMS module。 | Tested。 |
| `1.21.4` | Paper `1.21.4` fixture（paper module）。1.21.2 で変わった `ServerFunctionLibrary#reload` の引数など、paper module が reflection で読む内部を確認する。 | paper module で Tested。 |
| spigot module の `1.21.2+` | 未対応。`Attribute` / `Biome` / `Sound` が registry interface になり、spigot module の引数が動かない。typed NMS module もないため NMS を使う引数は `NMSClassNotFoundException` になる。Paper では paper module を使う。 | roadmap 参照。 |
| spigot module の `1.16.3` 以前 | 未対応。typed NMS module がない。 | 対応予定なし。 |
| latest stable | 未固定。 | 各 release 時点の最新安定版を追従対象にする。 |

## Support policy

- 最低対応 Minecraft version は `1.16.5` とする。
- 最新安定版は、CommandLib の各 release 時点で確認し、compatibility table に明記する。
- Bukkit family は `spigot` module と `paper` module に分かれている。`bukkit` module / artifact は存在しない（"Bukkit" は API family 名としてのみ使う）。
- Spigot module は Bukkit/Spigot API 互換を基準にし、Paper-specific API を使わない。
- Paper module は Paper 公式 API を基準にし、Paper の command API、Adventure component、lifecycle API を優先する。
- Folia は Paper 互換として扱わず、Paper/Folia の差分と threading model を別途確認する。
- Mohist は Bukkit compatibility を前提にしつつ、Forge 連携部分の干渉を別途確認する。
- Forge と NeoForge は `modded` module（Stonecutter node）で扱う。Fabric は platform-specific module または loader の追加で扱う。
- unsupported version / platform でも偶然動く可能性はあるが、test と docs で確認するまでは supported としない。

## Module layout

`bukkit` module の `spigot` / `paper` への分割は完了している。詳細は [design/module-structure.md](design/module-structure.md) を参照。

現在の module:

| Module | Role |
|---|---|
| `common` | platform 非依存の command model、arguments、options、permissions、help。 |
| `spigot` | Bukkit/Spigot API を基準にした実装。typed NMS module（`nms/*`）を同梱する。Paper-specific API は使わない。 |
| `paper` | Paper 公式 command API、Adventure component、lifecycle API を使う実装。 |
| `modded` | Forge / NeoForge 実装。Stonecutter node は `forge-1.16.5` / `forge-1.20.1` / `neoforge-1.21.1`。 |
| `common-testing` / `spigot-testing` / `paper-testing` / `modded-testing` | 各 platform 向けの公開 testing artifact。 |
| `integration-test` | 非公開の実サーバー integration test aggregator。 |

未実装の module（`folia` / `velocity` / `fabric`）は Planned。Mohist は専用 module を持たず、`spigot` 互換と integration target で扱う。

方針:

- public dependency は platform-specific artifact を基本にする。
- platform-specific 実装を `common` に戻さない。`common` への追加は platform-neutral な新機能が出た場合だけ検討する。
- `paper` は `spigot` に依存しない。

分離した理由:

- Paper が Spigot/Bukkit API 互換を保つとは限らない。
- Paper 1.20.6+ 以降の command registration は Spigot と別物として扱うほうが安全。
- Paper は Adventure component を標準的に扱える一方、Spigot は legacy text / Bukkit API compatibility を意識する必要がある。
- Spigot 向けの legacy compatibility と Paper 向けの modern API support を別々に進化させられる。

## Roadmap tasks

### P0

- README の supported versions をこの文書へ集約する。
- integration fixtures と実際に通る Gradle tasks を version table に紐づける。
- `1.17.x` と `1.18.x` の扱いを、NMS bridge の範囲だけでなく integration test で確認する。

### P1

- Paper 1.20.6+ の official Paper command API path の挙動を version ごとに確認する。
- `/execute`、command block、function、datapack、tag 経由での挙動を version ごとに確認する。
- Mohist の Forge/Bukkit 混在時の command registration を確認する。

### P2

- Folia 対応方針を決める。
- Velocity module の必要性と API shape を決める。
- NeoForge / Fabric module の分割方針を決める。

## Open questions

- Velocity を server-side command library と同じ API で扱うか、proxy command 用の別 API として扱うか。
- Fabric / NeoForge を `common` の同じ command model に完全に載せるか、platform-specific limitations を許容するか。
- Java baseline を Java 11 のまま維持するか、platform ごとに Java baseline を分けるか。
- `latest stable` を release ごとに固定する仕組みを README / docs / CI のどこに置くか。
- Paper module の message API は Adventure component を標準にするか、CommandLib 独自 abstraction を挟むか。
