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
| Spigot | `Best effort` | 現状の `bukkit` module が実質的な Spigot 互換実装。compile target は Spigot API `1.16.5`。 | GA 前に `spigot` module へ rename / move する。Bukkit API 互換を基準にし、Paper-specific API に依存しない。 |
| Paper | `Best effort` | 現状の Spigot 互換実装上で Paper も考慮している。`1.20.6+` 向け registration path がある。 | GA 前に Paper 専用 module を新設する。Paper 公式 command API と Adventure component を優先する。 |
| Folia | `Planned` | なし。 | scheduler、threading、command registration の安全性確認が必要。 |
| Mohist | `Best effort` | integration fixture: `1.16.5`, `1.20.1`。README に tested version として記載あり。 | Bukkit compatibility 上で動かす方針。Forge side との干渉確認が必要。 |
| Velocity | `Planned` | なし。 | proxy command model と Minecraft server command model の差分設計が必要。 |
| Forge | `Best effort` | `forge` module。現在は Forge `1.16.5`。 | Forge 1.16.5 は現行実装あり。新しい Forge 系は未対応。 |
| NeoForge | `Planned` | なし。 | Forge から分岐後の API 差分調査が必要。 |
| Fabric | `Planned` | なし。 | Fabric command registration と Brigadier integration の設計が必要。 |

## Version coverage

CommandLib は `1.16.5` から最新安定版までのサポートを目標にする。ただし、すべての platform で同じ version range を同時に保証するわけではない。

| Minecraft version | Current evidence | Target |
|---|---|---|
| `1.16.5` | Bukkit fixture、Mohist fixture、Forge module。 | Core supported baseline。 |
| `1.17.x` | 専用 fixture なし。NMS bridge の一部に `1.17` range がある。 | Best effort から tested へ引き上げる。 |
| `1.18.x` | 専用 fixture なし。NMS bridge の一部に `1.18` range がある。 | Best effort から tested へ引き上げる。 |
| `1.19.4` | Bukkit fixture。 | Tested。 |
| `1.20.1` | Bukkit fixture、Mohist fixture。 | Tested。 |
| `1.20.4` | Bukkit fixture。 | Tested。 |
| `1.20.5` | Bukkit fixture。 | Tested。 |
| `1.20.6` | Bukkit fixture。Paper 1.20.6+ registration path がある。 | Tested。Paper official API path の候補。 |
| `1.21.0` | Bukkit fixture。 | Tested。 |
| latest stable | 未固定。 | 各 release 時点の最新安定版を追従対象にする。 |

## Support policy

- 最低対応 Minecraft version は `1.16.5` とする。
- 最新安定版は、CommandLib の各 release 時点で確認し、compatibility table に明記する。
- Bukkit / Spigot / Paper は現状 `bukkit` module を基礎に扱うが、この module は実質的に Spigot 互換実装として扱う。
- GA 前に現行 `bukkit` module を `spigot` module へ rename / move する。
- GA 前に Paper 専用 module を新設する。
- Spigot module は Bukkit/Spigot API 互換を基準にし、Paper-specific API を使わない。
- Paper module は Paper 公式 API を基準にし、Paper の command API、Adventure component、lifecycle API を優先する。
- Folia は Paper 互換として扱わず、Paper/Folia の差分と threading model を別途確認する。
- Mohist は Bukkit compatibility を前提にしつつ、Forge 連携部分の干渉を別途確認する。
- Forge、NeoForge、Fabric は、それぞれ platform-specific module を追加または分離して扱う。
- unsupported version / platform でも偶然動く可能性はあるが、test と docs で確認するまでは supported としない。

## Module split direction

現状の `bukkit` module は、Bukkit/Spigot/Paper の共通実装というより、Spigot 互換を主軸にした Bukkit-family 実装である。Paper も考慮しているが、GA 前の module split では `bukkit` を `spigot` に rename / move し、Paper は公式 API と Adventure component を前提に新設する。

方針:

- GA 前に現行 `bukkit` module を `spigot` module へ rename / move する。
- GA 前に `paper` module を新設する。
- GA 後の public dependency は platform-specific artifact を基本にする。
- `bukkit` artifact は GA 前の暫定 artifact として扱い、`spigot` への rename / move 時に削除する。
- 分離後は platform-specific 実装を `bukkit` module に戻さない。

目標 module 構成:

| Module | Role |
|---|---|
| `common` | platform 非依存の command model、arguments、options、permissions、help、testing primitives。 |
| `spigot` | 現行 `bukkit` module の移行先。Bukkit/Spigot API を基準にした実装。Paper-specific API は使わない。 |
| `paper` | Paper API を基準にした新規実装。Paper command API、Adventure component、lifecycle API を優先する。 |
| `folia` | Folia の scheduler / threading model に合わせた Paper 系実装。Paper module と共通化できる部分は共有する。 |
| `mohist` | Bukkit compatibility と Forge 混在環境の差分を吸収する実装。必要なら `spigot` から分離する。 |
| `velocity` | proxy command model 向け実装。server-side command model と同じ API に載せるかは要設計。 |
| `forge` | Forge 実装。現行 module を維持しつつ、新 version 対応を整理する。 |
| `neoforge` | NeoForge 実装。Forge との差分を分離する。 |
| `fabric` | Fabric command registration / Brigadier integration 向け実装。 |

分離する理由:

- Paper が Spigot/Bukkit API 互換を保つとは限らない。
- Paper 1.20.6+ 以降の command registration は Spigot と別物として扱うほうが安全。
- Paper は Adventure component を標準的に扱える一方、Spigot は legacy text / Bukkit API compatibility を意識する必要がある。
- deprecated text component や NMS/reflection fallback を Paper module に残し続けると、modern Paper support の足かせになる。
- Spigot 向けの legacy compatibility と Paper 向けの modern API support を別々に進化させられる。

実施方針:

- 現行 `bukkit` module を `spigot` へ移す。
- Paper 1.20.6+ registration path は、Paper official API に置き換える前提で Paper module 側に再設計する。
- `common` への追加移動は原則行わず、platform-neutral な新機能が出た場合だけ検討する。
- 分離作業は GA 前にまとめて行い、分離途中の artifact layout を長期公開しない。
- README、sample、integration test fixtures、publishing settings を同じタイミングで更新する。

移行方針:

- GA 前の利用者には breaking change として `spigot` / `paper` artifact への移行を案内する。
- 新規利用者には `spigot` または `paper` artifact を選ばせる。
- `bukkit` artifact は `spigot` への rename / move と同時に削除する。
- README には platform ごとの dependency を分けて掲載する。

## Roadmap tasks

### P0

- README の supported versions をこの文書へ集約する。
- integration fixtures と実際に通る Gradle tasks を version table に紐づける。
- `1.17.x` と `1.18.x` の扱いを、NMS bridge の範囲だけでなく integration test で確認する。
- 現行 `bukkit` module を `spigot` に rename / move するための影響範囲を確認する。

### P1

- Paper 1.20.6+ で official Paper command API path を設計する。
- `/execute`、command block、function、datapack、tag 経由での挙動を version ごとに確認する。
- Mohist の Forge/Bukkit 混在時の command registration を確認する。
- `spigot` / `paper` module split の design doc を追加する。

### P2

- Folia 対応方針を決める。
- Velocity module の必要性と API shape を決める。
- NeoForge / Fabric module の分割方針を決める。
- GA 前に `bukkit` artifact を削除し、現行実装を `spigot` artifact へ移行する。

## Open questions

- Velocity を server-side command library と同じ API で扱うか、proxy command 用の別 API として扱うか。
- Fabric / NeoForge を `common` の同じ command model に完全に載せるか、platform-specific limitations を許容するか。
- Java baseline を Java 11 のまま維持するか、platform ごとに Java baseline を分けるか。
- `latest stable` を release ごとに固定する仕組みを README / docs / CI のどこに置くか。
- Paper module の message API は Adventure component を標準にするか、CommandLib 独自 abstraction を挟むか。
