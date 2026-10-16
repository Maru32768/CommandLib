# CommandLib Roadmap History

完了済み roadmap エントリのアーカイブ。現在進行中の項目は `roadmap.md` を参照。

## 完了済みバックログ

### P0: 方針を固定する（完了）

- `[x]` `docs/compatibility.md` を追加し、supported / tested / best-effort / unsupported を分ける。
- `[x]` `docs/design/argument-catalog.md` を追加し、CommandLib / CommandAPI / Paper の argument 対応表を作る（2026-10-15 に behavior 別の `docs/arguments.md` へ置き換え）。
- `[x]` 現行 `bukkit` module は Spigot 互換実装として扱う方針に決める。
- `[x]` GA 前 module split plan を作り、`bukkit` artifact は削除する方針に決める。
- `[x]` spigot / paper 向け test module と integration-test の整理方針を決める。

### P1: GA 前 module split を実行する（完了）

- `[x]` 現行 `bukkit` module を `spigot` に rename / move する。
- `[x]` `bukkit` artifact を削除し、publishing を `spigot` artifact に切り替える。
- `[x]` `bukkit-test` を `spigot-test` に rename / move し、`:spigot` 依存へ変更する。
- `[x]` `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
- `[x]` integration target metadata に platform を追加する（`IntegrationTarget` に `Platform` enum フィールドを追加）。
- `[x]` sample / fixtures / docs / README の `bukkit` artifact 参照を `spigot` に置き換える。
- `[x]` `:spigot:compileJava`, `:spigot:test`, `:spigot-test:test` を通す。

### P2: Paper module を新設する（完了）

- `[x]` `paper` module を追加する。
- `[x]` Paper official command / lifecycle API を使った registration path を実装する。
- `[x]` Adventure component を Paper module の標準 message path にする。
- `[x]` Paper-specific suggestions / tooltip behavior を実装する。
- `[x]` Paper registry-backed arguments の設計を開始する。
- `[x]` `paper-testing` を追加する。
- `[x]` Paper integration targets を `integration-test` に追加する。

### P3: CommandAPI parity（完了分）

- `[x]` suggestions with tooltip を実装する。`SuggestionBuilder.suggest(String, Message)` を common に追加済み。Paper では
  `PaperMessages.toMessage(Component)` でリッチ tooltip が使える。
- `[x]` suggestions / tooltip の spigot-test coverage を追加する。`CommandTester.suggestions()` を追加し、tooltip
  テストを追加済み。
- `[x]` 公式資料を基準に argument / vanilla behavior の比較 catalog を作り、未対応と未確認を分けて追跡する
  （`docs/arguments.md`、2026-10-15）。
- `[x]` registry-backed arguments の入力形式を Paper native の registry key semantics に寄せる。Spigot の `SoundArgument`、
  `AttributeArgument`、`BiomeArgument`、`EntityTypeArgument` は Bukkit `Registry` による `minecraft:...` 入力・補完、
  `ScoreboardDisplaySlotArgument` / `ChatColorArgument` は vanilla 名に統一（Paper は `NamedTextColor` を返す、2026-10-15）。
- `[x]` execution result を返せるようにする。`ctx.setResult(int)` を `/execute store result` に、失敗を `store success 0` に
  反映する（spigot-1.16.5 / paper-26.3 の integration test、2026-10-15）。
- `[x]` vanilla-style feedback を追加する。`ctx.sendSuccess(message, broadcastToOps)` が `sendCommandFeedback` /
  `logAdminCommands` gamerule を尊重する（2026-10-15）。
- `[x]` `IntegerRangeArgument` / `DoubleRangeArgument`（vanilla `int_range` / `float_range`、Guava `Range`）と
  `DurationArgument`（`1h30m`、`20t`、`java.time.Duration`）を Spigot / Paper / Forge / NeoForge に追加する（2026-10-15）。
- `[x]` error を platform 間で揃える。実行中の resolve error は全 platform で vanilla の translatable component と context
  line、空の selector は Paper でも vanilla の `argument.entity.notfound.*`（2026-10-15）。
- `[x]` `BlockPositionArgument` / `ItemPredicateArgument` / `BlockPredicateArgument` と `LocationArgument` の centering 無効化を
  Spigot / Paper / Forge / NeoForge に追加する（Paper の `BlockPredicateArgument` は 1.21.8+、2026-10-15）。
- `[x]` `ComponentArgument` / `MessageArgument` / `NBTCompoundArgument` を Spigot / Paper / Forge / NeoForge / Fabric に追加する
  （2026-10-16）。
- `[x]` 汎用の `RegistryArgument`、vanilla `time` の `TimeArgument` を追加し、Spigot の `ScoreboardDisplaySlotArgument` /
  `ChatColorArgument` を vanilla の argument type で送る（2026-10-16）。
- `[x]` error taxonomy を公開仕様として整理する（`docs/errors.md`、2026-10-15）。
- `[x]` CommandAPI migration guide を追加する（`docs/migration/from-commandapi.md`、2026-10-15）。
- `[x]` Cloud、Lamp、ACF からの migration guide を追加する（`docs/migration/from-cloud.md` / `from-lamp.md` /
  `from-acf.md`、2026-10-15）。
- `[x]` `IntegerRangeArgument` / `DoubleRangeArgument` / `DurationArgument` を実サーバーで検証する（旧 target の
  MCProtocolLib bot が `int_range` / `float_range` を decode できるか、NMS range class の link）。2026-10-16 に 29 target で検証した。
- `[x]` `BlockPositionArgument` / `ItemPredicateArgument` / `BlockPredicateArgument` を実サーバーで検証する（新しい NMS class の
  link、Paper 1.21.8+ の `blockInWorldPredicate` の reflection、vanilla argument type の補完が client に届くこと）。2026-10-16 に 29 target で検証した。
- `[x]` `TimeArgument` / `RegistryArgument` と Spigot の vanilla 型の display slot / color を実サーバーで検証する（旧 bot の `minecraft:time` decode を含む）。2026-10-16 に 29 target で検証した。
- `[x]` `ComponentArgument` / `MessageArgument` / `NBTCompoundArgument` を実サーバーで検証する（2026-10-16）。
- `[x]` integration test で見つかった bug を直す（2026-10-16）: fixture plugin に common の resource（caption bundle）を同梱、Spigot 26.2+ の
  `TeamColorArgument` 改名、Mohist 1.16.5 での range argument の link、modded の sync executor が 1 task 遅れる件。

### P4: Developer experience（完了分）

- `[x]` options の repeatable / count / aliases / required / mutually exclusive、`OptionParseException`、help の option
  section を実装する（`docs/options.md`、2026-10-15）。
- `[x]` 末尾の optional argument（`optional(default)`）と variadic argument（`variadic()` で `List<T>`）を追加し、
  配置違反を登録時の runtime error にする（`docs/design/optional-variadic-arguments.md`、2026-10-15）。
- `[x]` testing artifact に JUnit 5 の `CommandLibExtension` / `@Sender`、`CommandTester#suggest`、command tree snapshot
  （`assertCommandTreeMatchesSnapshot`）を追加する（common / spigot / paper、2026-10-15）。
- `[x]` confirmation / cooldown を標準機能として追加する（`confirm()`、`cooldown(Duration)`、bypass permission、
  差し替え可能な message と clock、2026-10-15）。
- `[x]` sender mapper / context enrichment / dependency injection hooks を追加する。`ContextKey` による typed context value、
  `provide` / `ctx.put` / `ctx.get`、annotation の `registerDependency` と `@Dependency`（`docs/context-values.md`、2026-10-16）。
- `[x]` help / usage / error formatter API と command の `examples(...)`、opt-in の syntax advice を追加する（`docs/help.md`、
  2026-10-16）。
- `[x]` annotation に argument-first path（`@Subcommand("<team> add")`）、`@Middleware`、`@VisibleWhen`、`@OptionsAnywhere`、`@Requires`、
  `@MutuallyExclusive`、`@Examples`、`@CaptionBundle`、`@Variadic(max)`、option の `@Permission` を追加する（2026-10-16）。
- `[x]` 任意の argument を option の値にする `Options.of(...)`、option 単位の permission、positional argument の後に option を置く
  `optionsAnywhere()`、state で表示を切り替える `visibleWhen(...)` を追加する（2026-10-16）。
- `[x]` option の順列展開を redirect で抑える。player ごとの tree は short flag 6 個で 491,607 → 5,490 node、argument node を共有
  （`docs/design/command-options.md`、2026-10-16、実サーバー未検証）。
- `[x]` modded に Bukkit 側の argument（World / Sound / Biome / Attribute / EntityType / Advancement / Recipe / LootTable / Objective /
  ScoreboardDisplaySlot / GameMode / ChatColor）、`AnnotatedCommands`、modded-testing の `CommandLibExtension` を追加する（2026-10-16）。
- `[x]` registration 全体の middleware / provider（`CommandDefaults`）、cooldown の `remaining` / `reset`、typed executor からの
  `ctx.fail(...)`、annotation の `@Condition` による名前付き prerequisite、selector 向けの `ctx.sendSuccessFor(...)` を追加する（2026-10-16）。
- `[x]` middleware / interceptor API（`addMiddleware`）と async execution（`executeAsync`、`CommandScheduler` による sync thread
  への復帰）を追加し、preprocess / confirm / cooldown を middleware 上に載せる（`docs/execution.md`、
  `docs/design/execution-pipeline.md`、2026-10-15）。
- `[x]` 8 引数以上を raw types / casts なしで扱える表現（varargs / List の `argument(...)` と `ctx.getArgument(arg)`）と explicit Java
  builder API（`Command.builder(name)`）を追加する。`ArgumentBuilder` は削除（`docs/java-builder.md`、2026-10-16）。
- `[x]` annotation API を設計・実装する（reflection で fluent model に変換、Spigot / Paper / Velocity、`docs/annotations.md`、
  2026-10-16）。
- `[x]` Kotlin DSL を設計・実装する（Spigot / Paper / Velocity、coroutine の `executeSuspend`、`docs/kotlin.md`、2026-10-16）。
- `[x]` dynamic command tree refresh を追加する。`CommandLib.refresh(player)` / `refreshAll()`（Spigot / Paper / Forge /
  NeoForge、2026-10-15）。

### P5: Platform expansion（完了分）

- `[x]` Velocity support を追加する。`velocity` / `velocity-testing` module（`BrigadierCommand` 登録、Adventure message、
  `PlayerArgument` / `ServerArgument`、diagnostics）。Velocity 3.4.0 / 4.2.0 で手動確認（2026-10-15）。
- `[x]` Fabric support を追加する。`modded` の単一ソースから `fabric-1.20.1` / `fabric-1.21.1` と `fabric-testing-*` を build
  （2026-10-16、実サーバー未検証）。
- `[x]` modded の version matrix に forge-1.18.2 / forge-1.19.2 / neoforge-1.21.4 / fabric-1.21.4 を追加する（2026-10-16、実サーバー未検証）。
- `[x]` Folia support 方針を決め、実装する。`paper` artifact で Folia を検出して entity / region / global scheduler を使い、
  `spigot` artifact は Folia を拒否する（2026-10-15、mock test のみ）。

### P6: Ecosystem と tooling（完了分）

- `[x]` Spigot 1.21.2 以降に対応する。spigot module の `AttributeArgument` / `BiomeArgument` / `SoundArgument` を Bukkit
  `Registry` を使う実装に作り直し（入力は Paper 版と同じ NamespacedKey）、1.21.3〜26.3 の typed NMS module と、Spigot
  1.16.5〜26.3 の全 release、Paper 1.21.3〜26.3（paper module）の integration target を追加した。
- `[x]` command tree documentation generator と permission tree exporter（`CommandDocs` の Markdown / JSON）、runtime
  platform capability diagnostics と opt-in の diagnostics command を追加する（`docs/diagnostics.md`、2026-10-15）。
- `[x]` downstream 向けの packaging 方針を決めて docs に書く。全 platform で shade + relocate を推奨し、JarJar / `include()` は
  minor version を固定する代替として記載（`docs/packaging.md`、2026-10-16）。
- `[x]` example plugin collection を整備する（`samples/` の spigot / paper / paper-kotlin / velocity / forge / fabric、2026-10-16）。
- `[x]` Error Prone / NullAway を typed NMS module（27 module、generator 側で修正）と modded / modded-testing の node に
  広げ、modded node に javadoc jar と doclint を追加する（2026-10-15）。

## 完了済み Release Phases

### Phase 0: Freeze Direction（完了）

目的: GA 前に module / artifact / platform support の方針を固定する。

- compatibility 方針を文書化する。
- argument catalog を作る。
- module split 方針を文書化する。
- testing module 方針を文書化する。
- `bukkit` artifact を削除し、現行実装を `spigot` に移す方針を固定する。

Exit criteria（達成済み）:

- `docs/compatibility.md` が存在する。
- `docs/design/argument-catalog.md` が存在する。
- `docs/design/module-structure.md` が存在する。
- `docs/design/testing-module-structure.md` が存在する。

### Phase 1: GA Module Split（完了）

目的: GA 前に `bukkit` artifact を削除し、Spigot / Paper の platform contract を分ける。

- `bukkit` module を `spigot` に rename / move する。
- `bukkit-test` を `spigot-test` に rename / move する。
- `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
- `paper` module を新設する。
- `paper-testing` を新設する。
- samples / fixtures / README / publishing を platform-specific artifacts に更新する。

Exit criteria（達成済み）:

- `:spigot:compileJava` と `:spigot:test` が通る。
- `:spigot-test:test` が通る。
- `integration-test` aggregator に platform metadata がある。
- README が `bukkit` artifact を案内していない。
- `paper` module の最小 registration path がある。

## Immediate Next Actions（完了）

1. `[x]` `bukkit` module を `spigot` module に rename / move する。
2. `[x]` `bukkit` artifact を削除し、publishing を `spigot` artifact に切り替える。
3. `[x]` `bukkit-test` を `spigot-test` に rename / move する。
4. `[x]` `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
5. `[x]` `paper` module の最小 skeleton を追加する。
6. `[x]` `IntegrationTarget` に `Platform` enum フィールドを追加する。
7. `[x]` `CommandTester` を Builder パターン化し、Mock クラスの外部登録を可能にする。
