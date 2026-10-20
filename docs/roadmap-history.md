# CommandLib Roadmap History

完了済み roadmap エントリのアーカイブ。現在進行中の項目は `roadmap.md` を参照。

## 完了済みバックログ

### P0: GA 前の欠陥と契約（完了分）

2026-10-18 / 2026-10-19 / 2026-10-20 のレビューの指摘のうち、仕様判断の要らないものを 2026-10-21 に修正した（`3279d890` 以降の
commit、各 commit の本文に指摘 ID）。TeamColor 処理の共有（R2-P2C）は、Spigot は NMS wrapper の helper、Paper は
paper-testing が `mockStatic` で差し替える factory 呼び出しで、約 5 行の共有が結合を増やすため見送った。

- `[x]` route 全体で引数名の一意性を登録時に検証する。親 branch と argument child の同名引数は、同型なら親の `ArgRef` が子の値を
  返し、異型なら実行時に `IllegalArgumentException` になる。annotation の placeholder と子の引数の同名も parse 時に拒否する
  （18-F01、20-F01）。
- `[x]` command literal と同名の optional 引数を省略すると `No such argument` になる。入力 map の「引数の有無」を argument node
  だけで判定する（18-F02）。
- `[x]` `addChildren` が自身・祖先の追加（循環）を許し、build で `StackOverflowError` になる。接続前に拒否する（18-F03）。
- `[x]` 同じ child instance を同じ親の別の位置（直下と argument branch、別の branch）に付けると `parentArguments` が上書きされ、既存
  route の default permission と help の path が変わる。同じ位置への重複追加も help の usage を重複させる。別位置への再接続と
  重複追加を拒否する（18-F04、20-F10）。
- `[x]` Spigot / Paper で古い登録の `unregister()` が、同じ node を置き換えた新しい登録の permission を削除する。登録時の
  `Permission` object と現在の registry の object を比較して解除する（18-F05）。
- `[x]` Velocity の `unregister()` が permission default を残し、`ALL` で登録・解除した node を `NONE` で再登録しても開いたままに
  なる。default の寿命を live な登録に揃える（18-F06）。
- `[x]` 実行時の権限再検査が終端 command と argument branch だけで、祖先 command の node を見ない。cached parse（function 等）では
  祖先の権限を失った sender が実行できる。route 上の全 node を再検査する（18-F07）。
- `[x]` server 側の補完 API は権限で隠した literal も返す（client tree からは消える）。`CommandTester#suggest(ions)` を client と
  同じく requirement で絞り、docs の保証範囲を「client の command tree」と明記する（18-F08）。
- `[x]` Paper（plugin 初期化中の登録）と modded は command tree の構築が event まで遅れ、optional 配置違反などの定義エラーが
  `register()` で例外にならず、event 内の log になる。platform 非依存の検証を `register()` 内で行う（18-F09、20-F08）。
- `[x]` 1 回の登録の中で、同じ permission node に異なる default / description が付いている（直下の子と argument child の同名など）と
  後勝ちで登録される。登録時に拒否する（18-C03 のうち登録内、20-F03）。
- `[x]` `required()` の option 同士を `mutuallyExclusive(...)` にした、必ず失敗する定義を登録時に拒否する（20-F10）。
- `[x]` executor の二重設定（`execute` を 2 回、`require(...).execute` と `execute` の併用）が黙って後勝ちになる。Kotlin DSL の
  `require { execute {} }` は docs と違い command の executor を置き換える。二重設定を `IllegalStateException` にする（20-F13、
  20-F15）。
- `[x]` testing artifact の docs と実装を揃える: `velocity-testing` に `suggestions(input, sender)` を足す、command tree snapshot に
  executor の有無と cooldown bypass node を含めるか docs の保証範囲を直す、`FakeSender.player(...)` の既定 locale を実 client と
  同じ `en_us` にする（20-F07）。
- `[x]` annotation API: `@Range` の境界が型の範囲外なら黙って飽和せず parse error にする、root command の名前エラーに class 名を
  付ける、`@Suggest(method=)` の accessibility 失敗を `ParseException` で包む、Kotlin の `-java-parameters` を案内する、
  `docs/design/annotation-api.md` の stale な "Not done: inherited methods" を直す（20-F14）。
- `[x]` docs の不一致を直す: Kotlin DSL の再開 thread（Velocity は calling thread）と "no parent job"、`register` の `@throws`、
  platform ごとの登録契約（失敗時点、unregister の有無、label 衝突、default の決まり方）の横断表、Paper の
  `Bukkit.reloadData()` fallback、補完 context では option が未解決であること（18-C02、20-F08、20-F13、20-F15）。
- `[x]` Spigot の function 再 compile task が全登録の解除後も毎 tick 走り続ける。登録が無くなったら止める（20-F13）。
- `[x]` 使われていない `NMSEntity`（27 の typed NMS module の実装と codegen の entry）と `NMSClass#getServerClass` を削除する
  （R2-B-7）。
- `[x]` annotation API: 引数に `@Optional` と `@Default` を併用した冗長な定義を拒否する（R2-A1-17）、`suspend fun` の method を
  分かりにくい `Continuation` のエラーでなく明示的な parse error にする（R1-A3-05）。
- `[x]` 共有 node の設定比較（`CommandNameValidator#sameSettings`）が `ResourceArgument` の registry key など equals の無い設定を
  比較しない。registry key を比較する（R2-A-5、`shortcut:` コメントあり）。
- `[x]` 重複を減らす: `spigot` / `paper` の main に同一 file が 11 個（`Argument`、`BukkitCommandActor`、primitive / enum / literal
  などの argument。R1-C-05）、`spigot-testing` / `paper-testing` の test に同一 file が 11 個（R1-C-07、R2-D-32）、Spigot / Paper の
  TeamColor 処理（R2-P2C）、`Captions` と `Caption` の properties 読み込み（R2-P1B-3）、Paper の reflection lookup の cache
  （R2-B-8）、`AnnotationParser` 内の重複（R2-A1-18）。共有 source dir か共通 helper にする。
- `[x]` non-modded の build script の依存 version を `gradle/libs.versions.toml` の version catalog に集める（`brigadier:1.0.18` が
  13 file に直書きだった。R1-C-12。modded 側は roadmap に残す）。
- `[x]` `tools/nms-codegen/generate.py` に生成済み source との一致を検査する `--check` mode を足す（CI step の前提。R1-C-10。module の統合の確認は roadmap に残す）。
- `[x]` テストを足す: Spigot / Paper の `LocationArgument(name, false)` の unit test（R1-D-15）、Velocity の optional / variadic
  （R1-D-09）、argument child の subtree に redirect が無いことの構造 test と、redirect の有無だけを見る弱い assertion の置き換え
  （R1-D-06、R1-B1-07）、英語の assertion が JVM の locale に依存しないよう test の locale を固定する（R1-B1-08）、Spigot の
  `ownsLabel` / vanilla root の同一性 / Folia 拒否（R2-D-23）、fabric-1.21.4 の `RawWordArgumentType`（R2-E-12）。
- `[x]` 権限拒否の失敗が `/execute store success 0` になる integration case を足す（R1-D-16。data pack function 経由、paper-1.21.4 で
  確認）。
- `[x]` `integration-test/README.md` に、Bukkit の suite の大半は console から、一部は bot から実行することを書く（R1-B2-11）。

- `[x]` integration test の期待ケース ID を実行結果と独立して管理し、全76 positive shared-runner target の
  suite / builder 登録漏れ、ケースの重複・置き換わり、失敗・skip、集計値の不整合を検出する。バージョン除外も明示した。
  回帰テスト8件と Paper 26.3 実サーバーの97ケースで確認（2026-10-20、
  [IT-001 対応記録](../review-records/runs/2026-10-20-integration-test-completeness/implementation.md)）。

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
- `[x]` execution result を返せるようにする。`ctx.setResult(int)` を `/execute store result` に、message のある失敗を `store success 0` に
  反映する（spigot-1.16.5 / paper-26.3 の integration test、2026-10-15）。
- `[x]` vanilla-style feedback を追加する。`ctx.sendSuccess(message, broadcastToOps)` が `sendCommandFeedback` /
  `logAdminCommands` gamerule を尊重する（2026-10-15）。
- `[x]` `IntegerRangeArgument` / `DoubleRangeArgument`（vanilla `int_range` / `float_range`、Guava `Range`）と
  `DurationArgument`（`1h30m`、`20t`、`java.time.Duration`）を Spigot / Paper / Forge / NeoForge に追加する（2026-10-15）。
- `[x]` error を platform 間で揃える。実行中の resolve error は全 platform で vanilla の translatable component（context line は
  例外が入力と cursor を持つときだけ付く。vanilla が resolve 中に投げる例外は入力を持たないため付かない。`docs/errors.md`）、
  空の selector は Paper でも vanilla の `argument.entity.notfound.*`（2026-10-15）。
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
- `[x]` option の順列展開を redirect で抑える。player ごとの tree は short flag 6 個で 491,607 → 5,490 node（`OptionTreeBenchmark` の
  計測。`OptionTreeSizeTest` は 6,000 未満であることだけを確認）、argument node を共有（`docs/design/command-options.md`、2026-10-16、
  実サーバー未検証）。
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

- `[x]` `ServerArgument` の補完で quote が必要な名前を quote する（`a761dffa`、`velocity-testing` の `CommandTesterTest`）。
- `[x]` modded-testing の fake source が level と op を持ち、`@p` / `@a` / `@r` を fake player で解決する（`TesterServerTest`、
  `UUIDArgumentTest`）。`docs/arguments.md` の「real selectors」はこれで裏付けられた。
- `[x]` Velocity support を追加する。`velocity` / `velocity-testing` module（`BrigadierCommand` 登録、Adventure message、
  `PlayerArgument` / `ServerArgument`、diagnostics）。Velocity 3.4.0 / 4.2.0 で手動確認（2026-10-15）。
- `[x]` Fabric support を追加する。`modded` の単一ソースから `fabric-1.20.1` / `fabric-1.21.1` と `fabric-testing-*` を build
  （2026-10-16、実サーバー未検証）。
- `[x]` modded の version matrix に forge-1.18.2 / forge-1.19.2 / neoforge-1.21.4 / fabric-1.21.4 を追加する（2026-10-16、追加時点では
  実サーバー未検証。forge-1.18.2 / forge-1.19.2 / neoforge-1.21.4 は同日の 29 target の integration test で検証、fabric-1.21.4 は target なし）。
- `[x]` Fabric の integration target `fabric-1.20.1` / `fabric-1.21.1` を追加する。Forge / NeoForge と同じ test mod を Fabric
  Loader 0.16.14 と Fabric API の実サーバーで動かし、intermediary に remap した CommandLib jar を jar-in-jar で読み込む。
  2 target とも 65 case が全て通った（2026-10-17）。fabric-permissions-api と `fabric-1.21.4` は対象外（command block は直後の commit
  `41bc1954` で suite に加わった）。
- `[x]` Folia support 方針を決め、実装する。`paper` artifact で Folia を検出して entity / region / global scheduler を使い、
  `spigot` artifact は Folia を拒否する（2026-10-15、mock test のみ）。
- `[x]` Folia の integration target `folia-26.3`（Folia 26.3 build 3）を追加する。Folia 用の smoke fixture
  （`integration-test/shared/folia`）で、登録、bot の command 実行と message、bot への補完、`executeAsync` の work が
  sender を持つ thread の外で走り、続きが player では region thread、console では global region thread で走ることを確認する
  （2026-10-16）。command block、`/execute as`、`spigot` artifact の拒否は対象外。
- `[x]` Velocity の integration target `velocity-3.4.0`（Velocity 3.4.0 build 566、Paper 1.21.4 の backend）を追加する。
  `VelocityIntegrationTest` が proxy と backend の container を起動し、bot は proxy 経由で接続する。bot が受け取る command tree の
  CommandLib の node（`DefaultPermission.OP` の node は除かれる）、`ServerArgument` の補完、子 command と argument の実行 message、
  失敗 message、`OP` の command の拒否、`executeAsync` の message を確認する（2026-10-17）。option、`PlayerArgument`、
  permission plugin、Velocity 4.x は対象外。

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
