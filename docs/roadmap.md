# CommandLib Roadmap

この Roadmap は、CommandLib を Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric を含む Minecraft
コマンドライブラリの中で最有力の選択肢にするために、足りないものと実装順を整理する。

完了済み項目は [`docs/roadmap-history.md`](roadmap-history.md) に移動済み。

## 実装状況サマリ

凡例:

- `[x]` 実装済み。現行 API / docs / tests のいずれかで確認できる。
- `[~]` 部分実装。基礎はあるが、競合を上回るには不足がある。
- `[ ]` 未実装。Roadmap 上の新規実装項目。
- `[?]` 要調査。現状確認、仕様決定、または競合比較が必要。

| 領域                                  |    状況 | 現状                                                                                                                                      | 足りないもの                                                                     |
|-------------------------------------|------:|-----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Fluent / tree API                   | `[x]` | `Command`、`argument(...)`、`addChildren(...)` による command tree 定義がある。                                                                    | API stability policy と migration guide。                                    |
| 型安全な argument callback              | `[x]` | `Unary` から `Hept` までの typed branch callback がある。                                                                                        | 8 個以上の引数、optional/default/list 系の表現整理。                                     |
| Bukkit / Spigot / Paper support     | `[~]` | 現行 `bukkit` module は実質的な Spigot 互換実装。複数 version 向け NMS bridge がある。                                                                      | `spigot` への rename / move、Paper 公式 API module、Folia 対応。                    |
| Paper 1.20.6+ registration          | `[x]` | `LifecycleEvents.COMMANDS` + `Commands registrar` + `CommandSourceStack` を使った公式 API 登録が実装済み。Adventure component を標準 message path として利用。 | bootstrap compatibility。                                                   |
| Spigot / Paper module split         | `[x]` | `bukkit` を `spigot` へ rename / move し、`paper` module を新設済み。                                                                               | —                                                                          |
| Forge support                       | `[~]` | Forge 1.16.5 / 1.20.1 module がある（version ごとのソースコピー）。                                                                                                                | Architectury Loom + Stonecutter による単一ソース化、1.16.5+ 全 version 対応、Bukkit との差分整理。                                     |
| Mohist support                      | `[~]` | README 上で tested version が記載されている。                                                                                                      | integration test の明文化、非対応ケースの整理。                                           |
| Folia support                       | `[ ]` | なし。                                                                                                                                     | scheduler / threading model を踏まえた Paper 互換性確認。                             |
| Velocity support                    | `[ ]` | なし。                                                                                                                                     | proxy command model 向け module / API 設計（方針案: Pillar 11）。                                    |
| NeoForge support                    | `[ ]` | なし。                                                                                                                                     | Stonecutter matrix への追加（方針案: Pillar 11）。                                                   |
| Fabric support                      | `[ ]` | なし。                                                                                                                                     | Fabric command registration / Brigadier integration 設計、Stonecutter matrix への追加（方針案: Pillar 11）。                    |
| Brigadier / NMS hiding              | `[~]` | 利用者 API から Brigadier / NMS を隠している。                                                                                                      | capability detection、公式 API 優先、fallback policy。                            |
| `/execute` integration              | `[~]` | README と Bukkit registration path で `/execute run` 連携を扱っている。                                                                            | command block / function / tag / datapack での保証と tests。                     |
| Built-in arguments                  | `[~]` | 基本的な primitive、string、player、entity、location、item などがある。                                                                                | CommandAPI / Paper との argument catalog、NBT、registry、biome、sound 等の不足補完。    |
| Argument validation                 | `[x]` | `validator(...)` と `ArgumentValidator` がある。                                                                                             | reusable condition registry、error customization。                           |
| Argument transform                  | `[x]` | `transformer(...)` がある。                                                                                                                 | docs と examples。                                                           |
| Custom parsing fallback             | `[x]` | `additionalParser(...)` がある。                                                                                                            | behavior spec と tests。                                                     |
| Sync suggestions                    | `[x]` | `suggestionAction(...)` と additional suggestions がある。                                                                                   | tooltip support、ranking、fuzzy suggestions。                                 |
| Async suggestions                   | `[~]` | `asyncSuggestionAction(...)` がある。                                                                                                       | deadlock policy、cache、invalidation、Paper native path。                      |
| Permission-aware execution          | `[x]` | command / argument branch permission が実行時に使われる。                                                                                         | option permission、diagnostics、export。                                      |
| Permission registration             | `[~]` | Bukkit / Forge の permission registration がある。                                                                                           | reload safety の integration tests、LuckPerms guide。                         |
| Permission-aware help / suggestions | `[~]` | argument branch permission を help / tab completion に反映する設計がある。                                                                          | 網羅 tests と docs。                                                           |
| Typed command options               | `[~]` | flag / value option、default、dependency、`ctx.getOption(...)` がある。                                                                        | repeatable、multi-value、alias、required、mutual exclusion。                    |
| Generated usage / help              | `[~]` | README と snapshot tests に usage / help がある。                                                                                             | formatter API、theme、locale、MiniMessage / Adventure。                        |
| Error taxonomy                      | `[~]` | parse / validation / prerequisite 系の例外がある。                                                                                              | option error、unknown subcommand、missing argument などの公開仕様。                  |
| Exception handling                  | `[~]` | uncaught exception handler の仕組みがある。                                                                                                     | exception mapping、result handling、docs。                                    |
| Preprocess / prerequisite           | `[~]` | `addPreprocess(...)`、`addPrerequisite(...)`、`requirePlayer()` 等がある。                                                                     | reusable registry、middleware 化、diagnostics。                                |
| Middleware / interceptor            | `[ ]` | なし。                                                                                                                                     | execution pipeline API。                                                    |
| Sender abstraction                  | `[ ]` | platform context に依存。                                                                                                                   | sender mapper、custom sender、context enrichment。                            |
| Dependency injection                | `[ ]` | なし。                                                                                                                                     | constructor / method parameter injection、extension point。                  |
| Annotation API                      | `[ ]` | なし。                                                                                                                                     | `@Command`、`@Subcommand`、`@Permission`、`@Suggest`、`@Option` 等。             |
| Kotlin DSL                          | `[ ]` | なし。                                                                                                                                     | Kotlin-first builder、default args、coroutine suggestions。                   |
| Java builder API                    | `[ ]` | 現状は subclass / initializer / fluent API 中心。                                                                                             | explicit builder style。                                                    |
| Localization / captions             | `[ ]` | なし。                                                                                                                                     | locale-aware help / errors / suggestions。                                  |
| Adventure / MiniMessage             | `[ ]` | なし。                                                                                                                                     | rich help、hover、click、component output。                                    |
| Public testing framework            | `[~]` | `spigot-test` module と repository tests がある。`CommandTester` / `FakeSender` / NMS mock クラス群を整備済み。                                        | downstream guide、JUnit extension、assertion utilities、Builder パターン化。        |
| Multi-version integration tests     | `[~]` | `integration-test` aggregator と Spigot / Mohist fixtures（1.16.5〜1.21.0）がある。                                                             | CI matrix、`IntegrationTarget` への platform フィールド追加、result docs、test kit 化。  |
| NMS Reflection test strategy        | `[~]` | `CommandTester` が `Mockito.mockStatic` で `NMSClassRegistry` / `NMSReflection` をスタブし、軽量な Mock 実装クラス方式で NMS なしテストを実現している。                 | `CommandTester` Builder パターン化、新引数追加時の Mock + Test セット作成ルール整備。              |
| Documentation                       | `[~]` | README と design docs がある。                                                                                                               | compatibility、arguments、suggestions、permissions、migration、troubleshooting。 |
| Migration guides                    | `[ ]` | なし。                                                                                                                                     | CommandAPI、Cloud、Lamp、ACF からの移行 guide。                                     |
| Runtime diagnostics                 | `[ ]` | なし。                                                                                                                                     | command tree、permissions、platform capability report。                       |
| Generated docs / exports            | `[ ]` | なし。                                                                                                                                     | command docs、permission tree Markdown / JSON。                              |

## 優先度別バックログ

### P3: CommandAPI parity を埋める

- `[~]` missing built-in arguments を catalog に基づいて実装する（NMS 依存引数は `MockNMSArgumentXxx` + `XxxArgumentTest`
  をセットで追加する）。`GameModeArgument`（Spigot / Paper）、`BiomeArgument`（Spigot / Paper）、`WorldArgument`（Spigot /
  Paper）、`NamespacedKeyArgument`（Spigot / Paper）、`ResourceLocationArgument`（Forge）、`SoundArgument`（Spigot /
  Paper）、`EntityTypeArgument`（Spigot / Paper）、`AttributeArgument`（Spigot / Paper）、`AdvancementArgument`（Spigot /
  Paper）、`LootTableArgument`（Spigot / Paper）、`RecipeArgument`（Spigot / Paper）、`ObjectiveArgument`（Spigot /
  Paper）、`ScoreboardDisplaySlotArgument`（Spigot / Paper）、`ChatColorArgument`（Spigot / Paper）を追加済み。
- `[ ]` registry-backed arguments の入力形式を Paper native の registry key semantics に寄せる。`SoundArgument`、
  `EntityTypeArgument`、`AttributeArgument` などは Paper の `ArgumentTypes.resource(RegistryKey...)` と同等の
  `minecraft:...` 入力・補完を基準にし、Spigot 側も可能な限り同じ public behavior に合わせる。
- `[ ]` `/execute`、command blocks、functions、tags の挙動を検証して docs / tests に落とす。
- `[ ]` error taxonomy を公開仕様として整理する。
- `[ ]` CommandAPI migration guide を追加する。

### P4: Cloud / Lamp に対抗する developer experience を追加する

- `[ ]` annotation API を設計・実装する。
- `[ ]` Kotlin DSL を設計・実装する。
- `[ ]` middleware / interceptor API を追加する。
- `[ ]` dependency injection hooks を追加する。
- `[ ]` sender mapper / context enrichment を追加する。
- `[ ]` localization / caption system を追加する。

### P5: Platform expansion

- `[ ]` Folia support 方針を決める。
- `[~]` Forge testing strategy を決め、`forge-testing` を追加する。実サーバーの integration targets（`forge-1.16.5`、`forge-1.20.1`）は追加済み。サーバーなしの `forge-testing` は未着手。
- `[~]` NeoForge / Fabric / Forge の module 方針を決める。方針案は
  [11. Platform Architecture](#11-platform-architecture) を参照。
- `[~]` Velocity support 方針を決める。方針案は [11. Platform Architecture](#11-platform-architecture) を参照。
- `[x]` Architectury Loom + Stonecutter の PoC として、`forge:1.16.5` と `forge:1.20.1` を単一ソースから build する。
- `[ ]` mod loader 系 module を `minecraft`（MC version 依存・loader 非依存）と `loader/*`（loader 依存）に分割する。
- `[ ]` sender / message abstraction（Adventure と vanilla `Component` の差分吸収）を Velocity 着手前に設計する。
- `[ ]` public API の binary compatibility check（japicmp 等）を CI に追加する。
- `[ ]` downstream mod 向けに JarJar / `include()` と shade + relocate の推奨方針を決めて docs に書く。
- `[ ]` capability-based registration に移行する。
- `[ ]` runtime platform capability diagnostics を追加する。

### P6: Ecosystem と tooling

- `[ ]` command tree documentation generator を追加する。
- `[ ]` permission tree exporter を追加する。
- `[ ]` runtime diagnostics command を追加する。
- `[ ]` example plugin collection を整備する。
- `[ ]` documentation site を作る。

## 目標

CommandLib は、plugin / mod 開発者が 1 つの型安全なコマンドモデルでコマンドを定義でき、platform、Minecraft
version、Brigadier、NMS の差分を意識しなくて済む状態を目指す。

最終的な目標は次の状態である。

- CommandAPI に対して、vanilla command 連携、argument coverage、`/execute` 対応、suggestions、permissions、documentation、testing
  で同等以上になる。
- Cloud / Lamp に対して、拡張性、annotation、Kotlin support、dependency injection、execution pipeline customization で同等以上になる。
- 利用可能な環境では Paper 公式 command API を使い、旧 Bukkit / Spigot / Paper 互換も維持する。
- Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric を、可能な限り同じ public command model に揃える。
- downstream plugin が安全に shade / relocate できる状態を維持する。

## 競合ベースライン

### CommandAPI

CommandAPI は最も直接的な競合である。vanilla command registration、多数の built-in arguments、`/execute`、functions /
tags、tooltips、Kotlin DSL、annotation support、testing utilities、documentation、Minecraft 最新版への追従が強い。

CommandLib に足りないもの:

- Minecraft version ごとの compatibility matrix。
- CommandAPI と比較できる argument catalog。
- `/execute`、command blocks、datapack functions、tags に対する明確な保証。
- public testing utilities と利用ガイド。
- Kotlin DSL と annotation API。
- migration guide と troubleshooting guide。
- Minecraft version support policy。

### Cloud

Cloud は汎用 JVM command framework として強い。builder、annotation、Kotlin、execution coordinators、sender mapping、platform
modules、pipeline extension points を備えている。

CommandLib に足りないもの:

- middleware、interceptor、postprocessor。
- sender abstraction と custom sender mapping。
- platform capability detection。
- localization / caption support。
- 現在の fluent API と併存する annotation / builder API。
- command lifecycle hooks。

### Lamp

Lamp は annotation-driven な developer experience が強い。dependency injection、custom annotations、flags /
switches、multiple command variants、Kotlin default arguments、広い platform support を備えている。

CommandLib に足りないもの:

- annotation-first API。
- dependency injection。
- custom annotation extension points。
- optional / default argument の使いやすさ。
- Kotlin-native な API design。

### ACF

ACF は長い採用実績があり、annotations、dependency injection、validation、tab completion、help、syntax advice、conditions を扱える。

CommandLib に足りないもの:

- reusable condition / prerequisite registry。
- syntax advice。
- error message customization。
- help formatter / theme customization。
- locale-aware messages。
- annotation usage の成熟度。

### Paper Command API

Paper の command API は、modern Paper server で Brigadier integration を行う最も自然な方法である。

CommandLib に足りないもの:

- Paper 1.20.6+ では Paper 公式 command / lifecycle API を優先すること。
- Spigot と Paper を GA 前に module と artifact の単位で分けること。
- Paper module では Adventure component を標準 message path として扱うこと。
- bootstrap registration support。
- modern Paper における datapack function compatibility。
- Brigadier suggestions と async completions を capability に応じて切り替える仕組み。
- Folia compatibility。

## Product Pillars

### 1. Compatibility First

CommandLib は、複数の Minecraft version と platform をまたいで 1 つの command model を使いたい利用者に選ばれるべきである。

Required:

- `docs/compatibility.md` を追加し、Bukkit、Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric ごとに
  supported、tested、best-effort、unsupported を明示する。
- supported modules に対する focused compile / test matrix を作る。
- integration test の結果を docs に掲載する。
- Paper 1.20.6+ では Paper 公式 command API を優先する。
- 可能な限り version-string branching より capability detection を優先する。

Stretch:

- Folia support。
- Velocity support。
- Fabric / NeoForge support。
- Mojang-mapped server regression tests。

### 2. Argument Supremacy

CommandLib は、利用者が Brigadier や platform internals に降りなくても必要な引数を標準で扱える状態にする。

Required:

- CommandLib、CommandAPI、Paper の argument 対応表を追加する。
- missing arguments を実際の plugin 需要と parity gap に基づいて優先付けする。
- すべての argument に parse、suggestion、validation、error のテストを追加する。
- range、optional、default、list、map、greedy、quoted string、namespaced key、registry value、enum alias の挙動を標準化する。
- Bukkit と Forge の同じ概念の argument は common API で揃える。

High-priority arguments:

- NamespacedKey / ResourceLocation。
- Registry-backed values。
- Advancement。
- Biome。
- Sound。
- Loot table。
- Recipe。
- Objective / scoreboard。
- GameMode。
- Entity type。
- Potion effect。
- Attribute。
- NBT / SNBT。
- Component / JSON text。
- Time / duration。
- Angle / rotation。
- Predicate arguments。

### 3. Best-In-Class Suggestions

Suggestions は高速で、型安全で、permission-aware で、tooltip-capable であり、legacy Bukkit と modern Paper の両方で安全に動く必要がある。

Required:

- suggestion API を sync、async、default、additional、replacement の観点で整理する。
- permission filtering を suggestions と help に一貫して適用する。
- suggestions with tooltips を実装する。
- 利用可能な Paper では native Brigadier suggestions を優先する。
- legacy platform で deadlock しない async completion policy を定義する。
- suggestion cache と invalidation API を追加する。

Stretch:

- Fuzzy suggestions。
- Context-aware examples。
- Suggestion ranking。
- Locale-aware suggestions。

### 4. Command Options And Flags

CommandLib には typed command options がある。これは二次的な機能ではなく、主力機能として伸ばす。

Required:

- repeatable options。
- multi-value options。
- option aliases。
- option groups / mutually exclusive options。
- required options。
- positional arguments の後にも options を置ける opt-in mode。
- generated help に dedicated option section を追加する。
- option parse errors と argument parse errors を分ける。

Stretch:

- subcommand 向け inherited options。
- global options。
- environment / config-backed defaults。

### 5. Permissions And Visibility

permission-aware command tree は CommandLib の強い差別化要素である。root command、subcommand、argument branch、option の
permission を予測可能で可視化しやすいものにする。

Required:

- permission node generation rules を固定して文書化する。
- root commands、subcommands、argument branches、options で permission を一貫して扱う。
- platform ごとの default permission behavior を文書化する。
- permission registration、unregistering、reload、tab completion、help visibility の integration tests を追加する。
- LuckPerms guide を追加する。

Stretch:

- permission export command。
- generated permission tree の Markdown / JSON 出力。
- runtime permission diagnostics。

### 6. Help, Usage, And Errors

CommandLib は、初期設定でも有用な help と正確な error を出し、plugin 側が style を合わせられる formatter hooks を持つべきである。

Required:

- public help formatter API。
- public usage formatter API。
- public error formatter API。
- locale / translation key support。
- permission-aware help。
- option-aware help。
- command path examples。
- syntax advice。
- unknown subcommand、missing argument、invalid argument、invalid option、failed prerequisite、permission denied を区別した
  error。

Stretch:

- MiniMessage / Adventure component support。
- hover / click 対応の rich help。
- per-sender locale。
- command tree からの generated command documentation。

### 7. Multiple API Styles

現在の fluent API を弱めず、複数の authoring style を提供する。

Required:

- 現在の fluent / tree API を stable core として扱う。
- annotation API を追加する。
- Kotlin DSL を追加する。
- Java builder API を追加する。
- API style 間の migration guide を用意する。

Annotation API goals:

- `@Command`。
- `@Subcommand`。
- `@Permission`。
- `@Description`。
- `@Optional`。
- `@Default`。
- `@Range`。
- `@Suggest`。
- `@Flag` / `@Option`。
- custom annotation extension point。

Kotlin DSL goals:

- 型推論が効く command builders。
- Kotlin default argument support。
- Bukkit、Paper、Forge types 向け extension functions。
- coroutine-aware async suggestions。

### 8. Execution Pipeline

Cloud の大きな強みは pipeline customization である。CommandLib にも小さく実用的な同等機能が必要である。

Required:

- middleware / interceptor API。
- pre-execution validation hooks。
- post-execution hooks。
- exception mapping。
- result handling。
- dependency injection hooks。
- sender mapper。
- context enrichment。

Stretch:

- transaction-like command execution。
- cooldown module。
- confirmation module。
- rate limit module。
- metrics hooks。

### 9. Testing Framework

CommandLib は、この repository 自体だけでなく downstream plugin authors にとっても command behavior をテストしやすい状態にする。

詳細は `docs/design/testing-strategy.md` を参照。

#### テスト層の方針

- **Layer 1 (unit)**: `spigot:test` / `common:test` — NMS 非依存ロジックを検証。
- **Layer 2 (command-level)**: `spigot-test` の `CommandTester` — `Mockito.mockStatic` で `NMSClassRegistry` /
  `NMSReflection` をスタブし、実 NMS なしでコマンド parse・実行を検証。CommandAPI の「実 NMS + `Bootstrap.bootStrap()` +
  Mockito スパイ」方式は採用しない。
- **Layer 3 (integration)**: `integration-test` — Testcontainers + 実 Minecraft サーバー + MCProtocolLib で E2E 検証。

新引数追加時は **Mock 実装クラス（`MockNMSArgumentXxx`）と引数テスト（`XxxArgumentTest`）を必ずセットで作成する**。

Required:

- `spigot-test` を public testing framework として文書化する。
- common parser tests。
- help、usage、errors の snapshot tests。
- fake sender、fake player、fake permission model。
- suggestion assertion utilities。
- command execution assertion utilities。
- `IntegrationTarget` に `platform` フィールドを追加し、multi-version integration test guide を整備する。
- `CommandTester` を Builder パターン化し、外部から Mock クラスを差し込めるようにする。

Stretch:

- JUnit extension（`@ExtendWith(CommandLibExtension.class)`）。
- golden command tree snapshots。
- downstream plugin authors 向け compatibility test kit。
- Paper 公式 test-framework（`io.papermc:paper-api-test-framework`）の利用調査。

### 10. Documentation And Migration

利用者が source を読まなくても採用判断できるだけの documentation を用意する。

Required docs:

- `docs/compatibility.md`。
- `docs/arguments.md`。
- `docs/suggestions.md`。
- `docs/options.md`。
- `docs/permissions.md`。
- downstream users 向け `docs/testing.md`。
- `docs/migration/from-commandapi.md`。
- `docs/migration/from-acf.md`。
- `docs/migration/from-cloud.md`。
- `docs/migration/from-lamp.md`。
- `docs/troubleshooting.md`。

Required README additions:

- Why CommandLib。
- comparison table。
- Bukkit / Paper quick start。
- Forge quick start。
- compatibility matrix への link。
- testing guide への link。

### 11. Platform Architecture

1.16.5 以降の全 Minecraft version と、Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric を含む全環境を
同じ public command model で支える。`common` が Brigadier のみに依存する現行構成を前提に、platform を性質で 2 種類に分けて扱う。

| 分類          | 対象                                                         | version の扱い                                  | 構成                                                  |
|-------------|------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------|
| 安定 API 型    | Spigot、Paper、Folia、Velocity（将来: Sponge、BungeeCord、Minestom） | platform API が互換性を保つため、1 module で複数 version に対応できる | 現行 Spigot と同じ単一 module 方式。NMS 差分は module 内の bridge で吸収する |
| MC 直結型      | Forge、NeoForge、Fabric                                       | Minecraft 本体に直接 compile するため、MC version × loader の matrix になる | Architectury Loom + Stonecutter による単一ソース・複数 build          |

#### MC 直結型（Forge / NeoForge / Fabric）

Spigot のように単一 jar で複数 version に対応することは目指さない。理由:

- Bukkit のような安定 API 層がなく、Minecraft 本体に直接 compile する。
- mapping と runtime 名が version ごとに異なる（1.16.5 は MCP / SRG、1.17+ Forge は Mojang mappings / SRG、
  NeoForge 1.20.5+ は runtime も Mojang 名）。reflection での吸収は現実的でない。
- 1.17 の package 再編など、class 名・package 構成が大きく変わる。
- ForgeGradle / NeoGradle は 1 project 1 MC version 前提で、必要な Java version も 8 → 16 → 17 → 21 と変わる。
- `mods.toml` / `neoforge.mods.toml` / `fabric.mod.json` などの metadata も loader・version ごとに異なる。

現行の `forge:1.16.5` と `forge:1.20.1` は同じ file 構成のコピーで、約 1,600 行中約 540 行の diff がある。
diff の大半は MCP 名と Mojang 名の差・import 変更などの機械的なもので、対象 version と loader が増えるとコピー方式は破綻する。

方針:

- **Architectury Loom** で Fabric、Forge（1.16.5+）、NeoForge を同一 toolchain で build し、1.16.5 を含む全 target を
  Mojang mappings で記述する。これで diff の大半（mapping 差）が消え、実 API 差分だけが残る。1.16.5 の MCP snapshot
  mappings は廃止する。
- **Stonecutter** で単一ソースに `//? if >=1.19 {` 形式の条件コメントを書き、MC version × loader ごとの build を生成する。
- module は変更軸で分ける:

  ```
  common/          platform 非依存（現行のまま）
  minecraft/       Brigadier 連携・argument 型など。MC version 依存、loader 非依存（コードの大半）
  loader/forge     RegisterCommandsEvent、entrypoint、mods.toml
  loader/neoforge  同上
  loader/fabric    CommandRegistrationCallback（1.19 で v1 → v2）、fabric.mod.json
  ```

- `Argument` 実装は可能な限り vanilla class のみで `minecraft/` に書き、loader 依存層は command 登録・permission・metadata
  程度に最小化する。
- 破壊的変更が集中する version 境界: 1.17（package 再編、Java 16）、1.19（Component API、Fabric command API v2）、
  1.20.5（item components による `ItemStackArgument` への影響、Java 21）、1.21.x の各 drop。
- 互換性のある patch version（例: 1.21 / 1.21.1）は metadata の version range で 1 jar にまとめる。まとめられる範囲は
  integration test で裏付ける。
- artifact 名は `commandlib-<loader>-<mcVersion>` 形式を基本とし、公開前に確定する。

#### 安定 API 型（Velocity など）

Velocity:

- `BrigadierCommand` で Brigadier をネイティブに扱えるため、`common` の `CommandNodeCreator` をほぼそのまま利用できる。
- world / entity / block の概念がないため、`LocationArgument`、`EntityArgument` などは提供しない。
- client に送れる argument type は Brigadier の基本型に限られるため、Player / Server などは string + 独自 parse +
  suggestion（`CommonNameableObjectArgument` 方式）で実装する。
- Spigot / Paper 間の argument parity 方針は Velocity には適用しない。platform ごとの argument 提供範囲を
  `docs/compatibility.md` で明文化する。

Brigadier を持たない環境（BungeeCord、Minestom など）を対応する場合は、CommandLib 内部に `CommandDispatcher` を持ち、
文字列入力を自前で dispatch する adapter を用意して `common` を変更せずに済ませる。

Sender / message abstraction は Velocity 着手前に設計する。Velocity と Paper は Adventure、Forge / NeoForge / Fabric は
vanilla `Component` が native であり、platform が増えるほど後からの変更コストが大きくなる。

#### Mod loader の互換性リスク

Forge 系で「マイナーアップデートで mod が動かなくなる」原因は次のように分解でき、それぞれ対策を持つ。

1. **MC マイナー version の破壊的変更**（最多）: 近年の Mojang は 1.20.5、1.21.2、1.21.4、1.21.5 などでも大きな変更を入れる。
   argument 型や `Component` 周りが直撃するため、version 境界ごとに build を分けて Stonecutter で吸収する。
2. **metadata の version range による起動拒否**: `versionRange` を狭く書くと binary 互換があっても load されない。
   互換範囲を test で確認した上で適切な range を宣言する。
3. **loader 自身の API 変更**: 同一 MC version 内の loader build 更新でも API が変わることがある
   （`[?]` Forge 1.21.6+ の EventBus 刷新による listener 登録方法の変更を要確認）。loader 依存層を最小化し、
   CI で各 MC version の最古・最新 loader build の両方をテストする。
4. **JarJar / `include()` による CommandLib の version 衝突**（library として最重要）: Forge / NeoForge の JarJar と
   Fabric の `include()` は、複数 mod が異なる CommandLib version を同梱した場合に最新の 1 つだけを load する。
   古い CommandLib 向けに compile された mod が新しい CommandLib 上で動くため、`NoSuchMethodError` などが起きうる。
   - public API の binary compatibility を維持し、japicmp 等で CI チェックする。
   - downstream 向けに JarJar / `include()` と shade + relocate のどちらを推奨するかを決めて docs に書く。

## Release Phases

Phase 0（Freeze Direction）と Phase 1（GA Module Split）は完了済み。詳細は [`docs/roadmap-history.md`](roadmap-history.md) を参照。

### Phase 2: CommandAPI Parity

目的: 最大の直接競合である CommandAPI との差を埋める。

- priority arguments を実装する。
- tooltip suggestions を追加する。
- `/execute`、command block、function、tag behavior を検証する。
- error taxonomy を改善する。
- options syntax を拡張する。
- CommandAPI からの migration guide を追加する。

Exit criteria:

- CommandAPI の主要 argument categories に対して、CommandLib equivalent または deferred reason がある。
- integration tests が主要 supported Spigot / Paper versions をカバーしている。
- 最小限の CommandAPI migration example が存在する。

### Phase 3: Cloud And Lamp Developer Experience

目的: modern users が期待する authoring styles と extension points を追加する。

- annotation API。
- Kotlin DSL。
- middleware / interceptor API。
- sender mapper。
- dependency injection。
- locale / caption system。
- custom annotation extension point。

Exit criteria:

- 同じ example command を fluent、annotation、Kotlin DSL の 3 style で書ける。
- cross-cutting behavior を middleware として実装できる。
- Kotlin examples が casts や raw types なしで書ける。

### Phase 4: Platform Expansion

目的: Spigot / Paper 以外の platform support を広げる。

- Folia support。
- Forge testing strategy。
- NeoForge support。
- Fabric support。
- Velocity support。
- capability-based registration。
- expanded integration test matrix。

Exit criteria:

- Folia の対応方針が決まっている。
- Forge testing module の方針が決まっている。
- NeoForge / Fabric / Velocity の module 方針が決まっている（[11. Platform Architecture](#11-platform-architecture)）。
- Forge / NeoForge / Fabric が Stonecutter による単一ソースから build されている。
- public API の binary compatibility check が CI で動いている。
- runtime platform capability diagnostics が使える。

### Phase 5: Ecosystem And Tooling

目的: CommandLib を command registration library だけでなく、開発と運用の ecosystem にする。

- command tree documentation generator。
- permission tree exporter。
- runtime diagnostics command。
- Gradle plugin / annotation processor feasibility study。
- example plugin collection。
- documentation site。

Exit criteria:

- downstream plugins が command / permission documentation を生成できる。
- server admins が registered commands、permissions、platform capabilities を確認できる。

## Non-Goals

当面やらないこと:

- Bukkit、Spigot、legacy Paper compatibility を捨てて latest-Paper-only にする。
- fluent API を annotation-only API で置き換える。
- 競合ライブラリの API を完全にコピーする。
- Minecraft parity より先に non-Minecraft CLI / JDA support を優先する。
- supported legacy versions を壊してまで reflection / NMS を完全排除する。

## Success Metrics

- README だけで、新規 plugin が CommandLib を選ぶ理由を説明できる。
- CommandAPI、Cloud、Lamp、ACF からの migration guides がある。
- supported Minecraft versions が focused tests または integration tests で裏付けられている。
- breaking changes が release notes で管理されている。
- downstream plugins が execution、permissions、suggestions、help をテストできる。
- Minecraft release 後、latest Paper support を短期間で更新できる。
