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
| Spigot | `Tested` | `spigot` module。compile target は Spigot API `1.16.5`。typed NMS module により `1.16.4`〜`26.3` を扱う。Spigot integration target は `1.16.5`、`1.17.1`、`1.18`〜`26.3` の全 release。 | Bukkit API 互換を基準にし、Paper-specific API に依存しない。Paper 上でも `1.16.5`〜`1.21.1` を integration target で確認している。 |
| Paper | `Tested` | `paper` module と `paper-testing`。Paper 公式 command API と Adventure component を使う。paper module の integration target は `1.21`、`1.21.3`〜`1.21.11`、`26.1.1`〜`26.3`（Paper `1.21.2` と `26.1` の build はない）。 | Paper `1.20.6` 以前は spigot module を使い、`1.16.5`〜`1.21.1` の Paper integration target で確認している。それより新しい Paper で spigot module を使うと paper artifact を案内する `NMSClassNotFoundException` になり、`paper-26.3-spigot-jar` で確認している。 |
| Folia | `Best effort` | `paper` module が実行時に Folia を検出し、async command の sync executor を entity / region / global region scheduler、async executor を async scheduler にする。`paper-testing` の `FoliaCommandSchedulerTest` で mock した scheduler に対して確認。 | 実 Folia server の integration target はまだない（fixture が Bukkit scheduler を使うため Folia 用 fixture が必要）。`spigot` module は Folia では `CommandLib.register` が `UnsupportedOperationException` になる。plugin 側で `folia-supported: true` を宣言する。Folia は function を実行せず reload もないため、function の再 compile はしない。詳細は [execution.md](execution.md#folia)。 |
| Mohist | `Best effort` | integration fixture: `1.16.5`, `1.20.1`。README に tested version として記載あり。 | Bukkit compatibility 上で動かす方針。Forge side との干渉確認が必要。 |
| Velocity | `Best effort` | `velocity` module と `velocity-testing`。Velocity API `3.4.0`（Java 17）に対して compile し、command を `BrigadierCommand` で登録する。unit test は Velocity の Brigadier fork 上で実行。 | repository に実 proxy の integration target はない。2026-10-15 に Velocity `3.4.0`（Java 17）と `4.2.0`（Adventure 5、Java 25）の実 proxy で、plugin から登録・実行・失敗 message・permission default・RichText の click・補完・登録解除を client なしの command source で手動確認した。player client からの command tree 送信は未確認。argument は Brigadier の基本型、`DurationArgument`、`LiteralArgument` / `EnumArgument` / `ObjectArgument` / `NameableObjectArgument` と proxy 用の `PlayerArgument`（online player）、`ServerArgument`（`RegisteredServer`）に限る。world / entity / location 系 argument は提供しない。Velocity には operator も permission の default 登録もないため、`DefaultPermission.ALL` 以外は permission plugin が付与しない限り player に許可されない（console は常に許可）。command tree を再送する API がないため `CommandLib.refresh` はない。 |
| Forge | `Best effort` | `modded` の Stonecutter tree の `forge-1.16.5` / `forge-1.20.1` node（artifact: `forge-1.16.5` / `forge-1.20.1`）。`forge-testing-<version>` と integration target あり。 | 上記以外の Forge version は未対応。 |
| NeoForge | `Best effort` | `modded` の Stonecutter tree の `neoforge-1.21.1` node（artifact: `neoforge-1.21.1`）。`neoforge-testing-1.21.1` と integration target `neoforge-1.21.1` あり。 | Forge と同じソースを `//? if neoforge` で分岐。NeoForge 1.21.1 以外の version は未対応。 |
| Fabric | `Planned` | なし。 | Fabric command registration と Brigadier integration の設計が必要。 |

## Version coverage

CommandLib は `1.16.5` から最新安定版までのサポートを目標にする。ただし、すべての platform で同じ version range を同時に保証するわけではない。

| Minecraft version | Current evidence | Target |
|---|---|---|
| `1.16.5` | Spigot / Paper / Mohist fixture、Forge node、typed NMS module。 | Core supported baseline。Tested。 |
| `1.17.x` | Spigot / Paper `1.17.1` fixture、typed NMS module（`1.17`、`1.17.1`）。`1.17` は MCProtocolLib が 1.17 の protocol に未対応のため fixture なし（module のコンパイルのみで確認）。 | `1.17.1` は Tested、`1.17` は Supported。 |
| `1.18.x` | Spigot / Paper `1.18`〜`1.18.2` fixture、typed NMS module。 | Tested。 |
| `1.19.x` | Spigot / Paper `1.19`〜`1.19.4` fixture、typed NMS module。 | Tested。 |
| `1.20` / `1.20.1` | Spigot / Paper fixture、Mohist `1.20.1` fixture、typed NMS module（`1.20` は `1.20.1` の module）。 | Tested。 |
| `1.20.2` | Spigot / Paper fixture、typed NMS module。 | Tested。 |
| `1.20.3` | Spigot fixture、typed NMS module。Paper 1.20.3 は存在しない。 | Tested。 |
| `1.20.4`〜`1.20.6` | Spigot / Paper fixture（spigot module）、typed NMS module。 | Tested。 |
| `1.21` / `1.21.1` | Spigot fixture、Paper `1.21` fixture（paper module）、Paper `1.21.1` fixture（spigot module）、typed NMS module。 | Tested。 |
| `1.21.2` / `1.21.3` | Spigot fixture、Paper `1.21.3` fixture（paper module）、typed NMS module（`spigot-1.21.3`）。Paper 1.21.2 は存在しない。1.21.2 で attribute id の `generic.` prefix がなくなった。 | Tested。 |
| `1.21.4`〜`1.21.11` | Spigot / Paper fixture（Paper は paper module）、typed NMS module（`1.21.6` と `1.21.7`〜`1.21.8`、`1.21.9`〜`1.21.10` はそれぞれ 1 module）。1.21.11 で `ResourceLocation` が `Identifier` になり、CraftBukkit の function compile 用 source が `NullCommandSender` を返すようになった。 | Tested。 |
| `26.1`〜`26.3` | Spigot fixture（`26.1`、`26.1.1`、`26.1.2`、`26.2`、`26.3`）、Paper fixture（`26.1.1`〜`26.3`、paper module）、typed NMS module（`spigot-26.1.2` が全 26.x を扱う）。Spigot も Mojang 名で動き、CraftBukkit の package に version が付かない。Java 25 が必要。 | Tested。 |
| spigot module on Paper `1.21.2+` | 未対応。typed NMS module は Spigot 用のみ。Paper では paper module を使う。 | 対応予定なし。 |
| spigot module の `1.16.3` 以前 | 未対応。typed NMS module がない。 | 対応予定なし。 |
| latest stable | `26.3`（2026-10 時点）。 | 各 release 時点の最新安定版を追従対象にする。 |

## Support policy

- 最低対応 Minecraft version は `1.16.5` とする。
- 最新安定版は、CommandLib の各 release 時点で確認し、compatibility table に明記する。
- Bukkit family は `spigot` module と `paper` module に分かれている。`bukkit` module / artifact は存在しない（"Bukkit" は API family 名としてのみ使う）。
- Spigot module は Bukkit/Spigot API 互換を基準にし、Paper-specific API を使わない。
- Paper module は Paper 公式 API を基準にし、Paper の command API、Adventure component、lifecycle API を優先する。
- Folia は専用 module を持たず `paper` module で扱う。Paper の command / lifecycle API はそのまま使い、thread が分かれる部分（async command の scheduler、function の再 compile、data reload）だけ実行時に切り替える。`spigot` module は Folia を拒否する。
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
| `velocity` | Velocity の `BrigadierCommand`、permission API、Adventure component を使う proxy 用実装。 |
| `common-testing` / `spigot-testing` / `paper-testing` / `velocity-testing` / `modded-testing` | 各 platform 向けの公開 testing artifact。 |
| `integration-test` | 非公開の実サーバー integration test aggregator。 |

未実装の module（`fabric`）は Planned。Mohist は専用 module を持たず、`spigot` 互換と integration target で扱う。Folia も専用 module を持たず、`paper` module で扱う。

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
- `/execute`、command block、function、datapack、tag 経由での挙動を version ごとに確認する。挙動は
  [vanilla-integration.md](vanilla-integration.md) に記載。Bukkit 系 fixture の `VanillaExecutionTest`（`execute as` /
  `positioned` / `rotated`、`@s` と `distance` selector、command block の sender と `SuccessCount`）と
  `DataPackFunctionTest`（function、function tag、`#minecraft:load` が reload 後に 1 回だけ実行されること）、
  Forge / NeoForge fixture の `ActorTest`（`execute as` と `positioned ... rotated`）を追加済み。2026-10-15 時点では
  Docker が使えず実サーバーでは未実行のため、各 target で通るまで Tested としない。Forge / NeoForge の command block と
  function は未確認。
- Mohist の Forge/Bukkit 混在時の command registration を確認する。

### P2

- Folia の実サーバー integration target を追加する（Folia の scheduler を使う fixture が必要）。
- Velocity の実 proxy integration target を追加する。
- NeoForge / Fabric module の分割方針を決める。

## Open questions

- Fabric / NeoForge を `common` の同じ command model に完全に載せるか、platform-specific limitations を許容するか。
- Java baseline を Java 11 のまま維持するか、platform ごとに Java baseline を分けるか。
- `latest stable` を release ごとに固定する仕組みを README / docs / CI のどこに置くか。
- Paper module の message API は Adventure component を標準にするか、CommandLib 独自 abstraction を挟むか。
