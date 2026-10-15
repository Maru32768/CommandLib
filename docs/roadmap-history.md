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
- `[x]` error taxonomy を公開仕様として整理する（`docs/errors.md`、2026-10-15）。
- `[x]` CommandAPI migration guide を追加する（`docs/migration/from-commandapi.md`、2026-10-15）。

### P4: Developer experience（完了分）

- `[x]` options の repeatable / count / aliases / required / mutually exclusive、`OptionParseException`、help の option
  section を実装する（`docs/options.md`、2026-10-15）。
- `[x]` 末尾の optional argument（`optional(default)`）と variadic argument（`variadic()` で `List<T>`）を追加し、
  配置違反を登録時の runtime error にする（`docs/design/optional-variadic-arguments.md`、2026-10-15）。
- `[x]` testing artifact に JUnit 5 の `CommandLibExtension` / `@Sender`、`CommandTester#suggest`、command tree snapshot
  （`assertCommandTreeMatchesSnapshot`）を追加する（common / spigot / paper、2026-10-15）。
- `[x]` confirmation / cooldown を標準機能として追加する（`confirm()`、`cooldown(Duration)`、bypass permission、
  差し替え可能な message と clock、2026-10-15）。
- `[x]` dynamic command tree refresh を追加する。`CommandLib.refresh(player)` / `refreshAll()`（Spigot / Paper / Forge /
  NeoForge、2026-10-15）。

### P6: Ecosystem と tooling（完了分）

- `[x]` Spigot 1.21.2 以降に対応する。spigot module の `AttributeArgument` / `BiomeArgument` / `SoundArgument` を Bukkit
  `Registry` を使う実装に作り直し（入力は Paper 版と同じ NamespacedKey）、1.21.3〜26.3 の typed NMS module と、Spigot
  1.16.5〜26.3 の全 release、Paper 1.21.3〜26.3（paper module）の integration target を追加した。
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
