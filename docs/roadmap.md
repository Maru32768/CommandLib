# CommandLib Roadmap

この Roadmap は、CommandLib を Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric を含む Minecraft
コマンドライブラリとして、CommandAPI、Cloud、Lamp、ACF と Paper 公式 API を利用する開発体験を凌駕するために、
不足機能、実装順、競合を上回ったと判断するための証拠を整理する。機能の同等化は途中の到達点であり、最終目標ではない。

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
| 型安全な argument callback              | `[x]` | `Unary` から `Hept` までの typed branch callback と、任意個の引数を `argument(List)` で受けて `ctx.getArgument(arg)` で型付きに読む形がある。 | — |
| Variadic / optional arguments       | `[x]` | 末尾の `optional(default)` / `optional(Extractor)` と `variadic([max])`（`List<T>`）がある。配置違反は登録時に `IllegalStateException`（[`docs/design/optional-variadic-arguments.md`](design/optional-variadic-arguments.md)）。実サーバーでは Spigot / Paper / modded fixture の `OptionalVariadicArgumentTest` が console から、省略時の default、指定値での上書き、`optional(Extractor)`、`variadic(3)` の順序、player 名と `@p` の variadic、空の optional variadic を確認する（spigot-1.21.8、paper-1.21.4、forge-1.20.1、fabric-1.21.1 で実行済み）。 | 上限なしの variadic、`Optional<T>` 形式、player からの実行、command tree の大きさ（`maxCount` × option の展開）の測定。 |
| Execution result                    | `[x]` | `ctx.setResult(int)` が `/execute store result` に、message のある失敗（例外・権限・prerequisite 等）が `store success 0` に反映される（message のない失敗は 0 を返すだけで、`store success` は 1）。spigot-1.16.5 / paper-26.3 の integration test あり。 | Forge / NeoForge の実サーバー検証。 |
| Async execution                     | `[x]` | `executeAsync(...)`（全 typed branch）と `ctx.supplyAsync` / `runAsync`、platform 別の `CommandScheduler` で sync thread へ復帰する（[`docs/execution.md`](execution.md)）。 | Folia の command block（region scheduler）経路の実サーバー検証（Bukkit / Forge / NeoForge と、Folia の player / console は 2026-10-16 に検証済み）。 |
| Dynamic command tree refresh        | `[x]` | `CommandLib.refresh(player)` / `refreshAll()` が Spigot / Paper / Forge / NeoForge / Fabric にあり、state による表示切替は `visibleWhen(...)` で書ける（[`docs/permissions.md`](permissions.md)）。Velocity は tree を再送する API が無いため `refresh` も無い。 | 実 client での検証。 |
| Vanilla-style feedback              | `[~]` | `ctx.sendSuccess(message)` が Spigot / Paper / Forge / NeoForge / Fabric で vanilla の `CommandSourceStack#sendSuccess` を通り、gamerule と silent source を尊重する。Paper は reflection で呼び、その method がない source（test の mock 等）には通常の message で送る。Velocity には gamerule がないため、常に sender に送る。operator への broadcast と target 数に応じた文言は呼び出し側の責務とし、提供しない。 | `sendCommandFeedback` gamerule と silent source の tests（unit / integration とも無い）。 |
| Bukkit / Spigot / Paper support     | `[~]` | `spigot` module（Spigot 1.16.5〜26.3 を Tested）と `paper` module（Paper 1.21〜26.3 を Tested、Folia は best effort）がある。 | Folia は smoke target `folia-26.3` のみで、Paper の suite は Folia で走らない。 |
| Paper 1.20.6+ registration          | `[x]` | `LifecycleEvents.COMMANDS` + `Commands registrar` + `CommandSourceStack` を使った公式 API 登録が実装済み。Adventure component を標準 message path として利用。 | bootstrap 登録の互換性保証・docs / tests の拡充（`CommandLib.bootstrap(BootstrapContext, ...)` は実装済み）。                                                   |
| Spigot / Paper module split         | `[x]` | `bukkit` を `spigot` へ rename / move し、`paper` module を新設済み。                                                                               | —                                                                          |
| Forge support                       | `[~]` | Forge 1.16.5 / 1.18.2 / 1.19.2 / 1.20.1 を `modded` の Stonecutter 単一ソースから build。 | loader 非依存層の分離（`[?]`）。4 version とも実サーバーで検証済み（2026-10-16）。 |
| Mohist support                      | `[~]` | README 上で tested version が記載されている。                                                                                                      | 1.20.1 で失敗 message が player に届かない件。1.16.5 / 1.20.1 は integration test 済み（2026-10-16）。                                           |
| Folia support                       | `[~]` | `paper` artifact が Folia を検出し、`FoliaCommandScheduler` で entity / region / global scheduler に戻す。`spigot` artifact は Folia で登録を拒否する。 | smoke target `folia-26.3` で登録、bot の実行と message、`executeAsync` の thread（player は region thread、console は global region thread）、補完を確認済み（2026-10-16）。command block の region 経路、`/execute as`、`spigot` artifact の拒否、region thread からの runtime 登録の guard は未検証。 |
| Velocity support                    | `[~]` | `velocity` / `velocity-testing` module（Velocity 3.4.0+、`PlayerArgument` / `ServerArgument`）がある。smoke target `velocity-3.4.0`（Velocity 3.4.0 の proxy、Paper 1.21.4 の backend、proxy 経由の bot）で command tree、実行と message、失敗 message、permission の拒否、`ServerArgument` の補完、`executeAsync` を確認済み（2026-10-17）。4.2.0 は手動確認のみ。`samples/velocity` がある。 | option（`OptionGraph`）、`PlayerArgument`、permission plugin、Velocity 4.x の実 proxy での確認。 |
| NeoForge support                    | `[~]` | NeoForge 1.21.1 / 1.21.4 を build（両方とも integration test 済み）。 | 1.21.5 以降（1.21.11 で `Identifier` / `PermissionSet` 等の大きな変更）、26.x（非難読化、Java 25、`loom-no-remap`）。 |
| Fabric support                      | `[~]` | `fabric-1.20.1` / `fabric-1.21.1` / `fabric-1.21.4` を build（`CommandRegistrationCallback` v2、fabric-permissions-api 任意対応）。`modded-testing` も全 node で通る。`fabric-1.20.1` / `fabric-1.21.1` の integration target で Forge / NeoForge と同じ suite が実サーバーで通る。 | `fabric-1.21.4` の integration target、fabric-permissions-api の実サーバー確認、Fabric 用 Kotlin module、1.21.5 以降と 26.x。 |
| Brigadier / NMS hiding              | `[~]` | 利用者 API から Brigadier / NMS を隠している。                                                                                                      | capability detection、公式 API 優先、fallback policy。                            |
| `/execute` integration              | `[~]` | README と Bukkit registration path で `/execute run` 連携を扱っている。spigot module と paper module の command は `/minecraft:reload` 後や runtime 登録後も data pack function から使える（`/minecraft:reload` 後は両 module に integration test あり）。paper module で plugin から登録した command は次 tick の function 再 compile 後に使える（bootstrap 登録は server の compile 時点から使える）。 | Forge / NeoForge / Fabric の function / tag の tests（command block と、Spigot / Paper の function tag は integration test あり）。 |
| Built-in arguments                  | `[~]` | 基本的な primitive、string、player、entity、location、item などがある。CommandAPI / Paper との挙動別 catalog は [`docs/arguments.md`](arguments.md)。 | catalog の missing 一覧（`resourceKey` 相当、angle / rotation、score holder 等）の補完。 |
| Argument validation                 | `[x]` | `validator(...)` と `ArgumentValidator` がある。                                                                                             | — |
| Argument transform                  | `[x]` | `transformer(...)` がある。                                                                                                                 | docs と examples。                                                           |
| Custom parsing fallback             | `[x]` | `additionalParser(...)` がある。                                                                                                            | behavior spec と tests。                                                     |
| Sync suggestions                    | `[x]` | `suggestionProvider(...)`、additional suggestions、`SuggestionBuilder#suggest` の String / Brigadier Message tooltip overload がある。                                                                                   | rich tooltip の platform 別保証、ranking、fuzzy suggestions。                                 |
| Async suggestions                   | `[~]` | `SuggestionBuilder#await(CompletionStage)` がある。                                                                                                      | deadlock policy、cache、invalidation、Paper native path。                      |
| Permission-aware execution          | `[x]` | command / argument branch / option の permission が実行時に使われ、`CommandDocs` で export できる。 | 祖先 command の実行時再検査（P0 の 18-F07）。 |
| Permission registration             | `[~]` | Bukkit / Forge の permission registration がある。                                                                                           | reload safety の integration tests、LuckPerms guide。                         |
| Permission-aware help / suggestions | `[~]` | argument branch permission を help / tab completion に反映する設計がある。                                                                          | 網羅 tests と docs。                                                           |
| Typed command options               | `[~]` | flag / value / count option、任意の argument を値にする `Options.of(...)`、default、dependency、repeatable、alias、required、`mutuallyExclusiveOptions(...)`、option 単位の permission、opt-in の `optionsAnywhere()`、`OptionParseException`、help の option section がある（[`docs/options.md`](options.md)）。 | subcommand への継承、固定 arity の multi-value（Pillar 4 の Required）、排他 group の専用 error message。annotation は `@Flag` / `@Option` / `@MutuallyExclusiveOptions` で対応済み。 |
| Generated usage / help              | `[x]` | 生成 help / usage に `HelpFormatter` / `UsageFormatter` と `examples(...)` があり、command ごとに継承される（[`docs/help.md`](help.md)）。`serverHelp(...)` は Bukkit 系の `/help` index に載る。 | usage line の RichText 化（hover / click）。 |
| Error taxonomy                      | `[~]` | parse / validation / prerequisite / option 系の例外があり、各 error で sender に見える message を [`docs/errors.md`](errors.md) で公開仕様化した。 | unknown subcommand の専用 error（Brigadier が CommandLib より先に報告するため `ErrorFormatter` に届かず、platform の hook が要る）、cooldown / confirmation / preprocess message の error formatter 経由化。missing argument は help を送る。 |
| Exception handling                  | `[x]` | uncaught exception handler と、middleware での exception mapping / result handling がある。 | global な exception handler。 |
| Preprocess / prerequisite           | `[~]` | `addPreprocess(...)`（middleware 化済み）、`addPrerequisite(...)`、`requirePlayer()` 等がある。名前付きの condition は annotation の `@Condition`（`AnnotationParser#registerCondition`）だけ。 | fluent / builder / Kotlin の reusable condition registry（現在は `Prerequisite` の値を使い回すだけ）、diagnostics。 |
| Middleware / interceptor            | `[x]` | `addMiddleware(CommandMiddleware)` で pre / post hook、exception mapping、result handling を合成でき、child と argument branch に継承される。`confirm()` / `cooldown()` / preprocess も middleware 上にある。 | — |
| Sender abstraction                  | `[x]` | `provide(Type, ctx -> ...)` で sender を plugin の型に変換し、`ctx.get(Type)` で cast なしに読める。middleware は `ctx.put` で値を足せる。登録単位の provider は `CommandDefaults`（[`docs/context-values.md`](context-values.md)）。この行の sender は「plugin の型への変換」の意味で、P5 の sender / message abstraction（platform 間の sender 型の差の吸収）とは別。 | — |
| Dependency injection                | `[x]` | context value の provider、登録単位の `CommandDefaults`、annotation API の `registerDependency` / `@Dependency` field がある。DI framework 非依存。 | `require(Class)` の短縮形。 |
| Annotation API                      | `[~]` | `AnnotationParser` と `@Command` / `@Subcommand` / `@Permission` / `@Optional` / `@Default` / `@Range` / `@Suggest` / `@Flag` / `@Option` / `@Confirm` / `@Cooldown` 等、custom annotation の拡張点が Spigot / Paper / Velocity / modded にある。Spigot / Paper / modded では fluent と同じ help / permission / 実行結果を snapshot で確認し、Velocity は実行と引数の parse だけを確認（`AnnotatedCommandsTest`、[`docs/annotations.md`](annotations.md)）。 | `java.util.Optional<T>` 引数、`requires(option, predicate)` の annotation、Velocity の snapshot、modded の guild case（placeholder、middleware、visibility、`@Requires`）。 |
| Kotlin DSL                          | `[~]` | `common-kotlin` + `spigot-kotlin` / `paper-kotlin` / `velocity-kotlin` に型推論の効く builder、delegated option、`executeSuspend`、suspend suggestions がある（[`docs/kotlin.md`](kotlin.md)）。 | Forge / NeoForge 版、Kotlin 関数の default 引数から command を作る形。 |
| Java builder API                    | `[x]` | `Command.builder(name)`（common の `CommandBuilder`）が全 platform にあり、fluent と同じ command tree / permission / 結果を snapshot で確認（[`docs/java-builder.md`](java-builder.md)）。 | — |
| Localization / captions             | `[~]` | `Caption` / `Captions` で組み込み message を key 化し、sender の locale で解決する（en / ja 同梱、override、ResourceBundle、`CaptionSender`。[`docs/localization.md`](localization.md)）。 | diagnostics / `CommandDocs` の文言、Fabric node での locale 取得の確認。 |
| Public testing framework            | `[~]` | `common-testing` / `spigot-testing` / `paper-testing` / `velocity-testing` / loader 別 `modded-testing` がある。`CommandTester`（`suggest()`、int を返す `execute`、`assertCommandTreeMatchesSnapshot`）、`FakeSender`、NMS mock、JUnit 5 の `CommandLibExtension` + `@Sender`（全 testing artifact）と downstream guide（[`docs/testing.md`](testing.md)）がある。 | testing artifact 間の共通化、gamerule / silent source の模擬。 |
| Adventure / MiniMessage             | `[~]` | platform 非依存の rich text 値型 `RichText`（色、装飾、hover text、click、translatable 引数）を全 platform で送れる（Phase 1 完了）。Paper / modded は native component の `sendSuccess/Warn/Failure` もある。 | rich help、rich suggestion tooltip の統一、MiniMessage。設計: [`docs/design/rich-text-messages.md`](design/rich-text-messages.md)。 |
| Multi-version integration tests     | `[~]` | `integration-test` aggregator と Spigot（1.16.5〜26.3 の全 release）/ Paper / Mohist / Forge / NeoForge fixtures がある。                                                             | CI matrix、`IntegrationTarget` への platform フィールド追加、result docs、test kit 化。  |
| NMS Reflection test strategy        | `[~]` | `CommandTester` が `Mockito.mockStatic` で `NMSClassRegistry` をスタブし、軽量な Mock 実装クラス方式で NMS なしテストを実現している。                 | Builder / mock 差し込みの保証拡充、新引数の Mock + Test ルールの継続適用。              |
| Documentation                       | `[~]` | README（Why CommandLib、比較表）、`docs/compatibility.md`、`arguments` / `errors` / `options` / `suggestions` / `permissions` / `testing` / `troubleshooting` と design docs がある。 | compatibility の結果公開・更新、documentation site。 |
| Migration guides                    | `[x]` | CommandAPI、Cloud、Lamp、ACF からの移行 guide が `docs/migration/` にある。 | 未実装機能（annotation、Kotlin、DI、locale 等）の実装に合わせた更新。 |
| Runtime diagnostics                 | `[x]` | `CommandLib.diagnostics()` と opt-in の `CommandLibDiagnosticsCommand` が Spigot / Paper / Forge / NeoForge にある（[`docs/diagnostics.md`](diagnostics.md)）。 | plugin 別 filter（実サーバー検証は 2026-10-16 に済み）。 |
| Generated docs / exports            | `[x]` | `CommandDocs` が command docs と permission tree を Markdown / JSON で出力する。 | 排他 option group と option の `requires` の出力、sender 依存の description（examples は出力済み）。 |

## 優先度別バックログ

### P0: GA 前の欠陥と契約（レビュー由来）

2026-10-18 / 2026-10-19 / 2026-10-20 のレビューで見つかり、未対応だった指摘をまとめる。ID は各レビュー記録の ID
（`18-F01` は [2026-10-18 GA 監査](../review-records/runs/2026-10-18-ga-audit/reviews/agents/architecture-api.md)、
`20-F01` は [2026-10-20 GA 監査](../review-records/runs/2026-10-20-ga-audit/reviews/agents/ga-architecture-api-1.md)、
`R1-` / `R2-` は [import-review-1](../review-records/runs/2026-10-19-import-review-1/triage/findings.md) /
[import-review-2](../review-records/runs/2026-10-19-import-review-2/triage.md) の triage ID）。
「仕様判断不要」は正しい修正が一つに決まるもの、「要判断」は互換性のない選択肢から選ぶ必要があるもの。

#### 仕様判断不要

- `[ ]` modded の relocate した CommandLib jar を Forge / NeoForge / Fabric の実サーバーで読み込む integration target を足す
  （R1-D-11）。
- `[ ]` modded / modded-testing / buildSrc / test plugin の build script の依存 version も version catalog に移す
  （non-modded は移行済み。R1-C-12 の残り）。
- `[ ]` 同じ revision の typed NMS module（`spigot-1.20.3` と `spigot-1.20.4` は package 名だけが違う）をまとめられるか
  `TypedNmsTest` で確かめる（R1-C-10 の残り。`generate.py --check` は実装済み）。

#### 要判断

- `[?]` Spigot / Paper で最後の登録を実行時に `unregister()` し、同じ tick に plugin が disable されると、data pack function の
  再 compile が行われず function が外した command を呼び続ける（以前からの挙動。`unregister()` が disable listener も外すため）。
  案: plugin ごとに登録より長く生きる disable listener を持つ、または command 実行中でなければ `unregister()` 内で同期 compile する
  （2026-10-21 review の CR2-2 の残り）。
- `[?]` permission node の所有者を登録時に記録する設計（現在は解除時に identity 比較と全登録の再走査で決める）。下の「複数の登録が
  同じ node を宣言したときの規則」と併せて決める（2026-10-21 review の CR2-8）。
- `[?]` `spigot-testing` の `CommandTester.Builder.mockNmsClass(Class<T extends NMSClass>, ...)` は docs に載る公開 API だが、
  引数の型は内部 package（`dev.marulabs.commandlib.internal.nms`）に移った。公開 API として残すか、内部扱いにするか
  （2026-10-21 の package 整理で判明）。
- `[?]` `ctx.getOption(option)` / `hasOption` が実行中の command に無い option（他 command、親 command の option）を黙って
  default / false で返す。Kotlin DSL の delegated property では親の option を子の `execute` で自然に読めてしまう。ただし
  `6142b594` で「親や `CommandDefaults` の middleware が一部の子にしか無い option を読むと default を得る」ために意図して
  緩めた経緯があり、互換性のない選択になる。案: (a) どこでも例外（共有 middleware の書き方が使えなくなる）、
  (b) command 自身の executor の中だけ例外、middleware / prerequisite は default（実行段階の flag が要り、async の続きの
  扱いが曖昧）、(c) 現挙動を docs に書くだけ。補完と `visibleWhen` では option が parse されず default になることは
  Javadoc に記載済み（18-C02、20-F06）。
- `[?]` 登録後の command model の変更をどう扱うか。現在は Spigot / Velocity が登録時の tree を保持し、Paper / modded は reload
  ごとに現在の model から作り直すため、登録後の `addChildren` / `cooldown` / `provide` が platform と時刻で違う結果になる。
  案: (1) 登録時に freeze して以後の setter を例外にする（推奨）、(2) 登録時の完全 snapshot、(3) 明示的な rebuild API。同じ
  command の複数登録、`CommandDefaults` の上書き、tester 間の共有も併せて決める（18-C01、20-F02、20-F07）。
- `[?]` index ベースの `getInput(int)` / `getArgument(int)` の意味。`getInputs()` は literal を含み `getArguments()` は含まないため
  index が揃わず、Paper 1.21+ は root literal を含まない。案: index API を削除、または `getInputs()` を引数だけにする（20-F05）。
- `[?]` `CommonArgument#execute` で引数 instance に付けた executor は branch の途中を実行点にするが、help / docs / tree に出ない。
  案: positional 引数の instance executor を削除して branch だけにする、または help / docs に出す（20-F04）。
- `[?]` 失敗の `/execute store success` が message の有無で変わる（message の無い失敗は 0 を返すだけで success 1）。失敗状態・表示・
  result を独立させるか、現挙動を契約にするか（18-C04）。
- `[?]` cooldown は executor の直前に始まるため、executor が `ctx.fail(...)` で失敗しても消費される。confirmation は失敗時に再要求
  される（20-F11）。
- `[?]` 同じ parse（function 等の cached parse）を再実行すると confirmation が「同じ fork の別 target」と判定し、二度と確認できない。
  dispatch 単位の identity を作るか、confirmation の対象経路を直接入力に限るか（18-F10）。
- `[?]` 複数の登録（別 plugin、shade した別 copy を含む）が同じ permission node を宣言したときの規則。現在は Bukkit 系が置き換え、
  Velocity / modded が first-wins（18-C03、20-F08）。
- `[?]` annotation で `@Subcommand` method の argument branch に description / permission / visibleWhen を付ける手段（fluent では
  branch ごとに書ける）（20-F14）。
- `[?]` 非同期処理の寿命（plugin disable、unregister、終わらない stage）と、validator / transformer / provider が補完や option /
  variadic の再 parse で複数回呼ばれることの契約（純粋性・副作用）（18-C06）。
- `[?]` GA の定義: 安定を保証する API、release gate（japicmp の baseline、必要な検証）、roadmap の `[x]` が「実装済み」と
  「検証済み」のどちらを意味するか（18 の roadmap 1・2、20-F12、20-R1）。
- `[?]` `requires(option, value)` は `ctx.getOption` を見るため未指定の default でも成立し、`requires(option)` は指定を要求する。
  value 版も明示的な指定を要求するか（R2-A1-9）。
- `[?]` argument branch は command の `UncaughtExceptionHandler` を継承するが、子 command は親のものを継承しない（middleware は両方
  継承する）。揃えるか docs に書くか（R2-A2-8）。
- `[?]` option は位置引数より先に parse されるため、option の validator / transformer / default から `getArgument` を読むと
  unexpected error になる。parse 順を変えるか制約として明示するか（R2-A-17）。
- `[?]` `/config -f <key> set` で親 command の option が受理された後に捨てられる（`docs/options.md` は動くと書き、別の節は継承しない
  と書く）。拒否するか子に見せるか。options の継承（P3）と併せて決める（R2-A-18）。
- `[?]` `Options.bool / integer / longValue / floatValue / doubleValue / string` が `Options.of` と重複し、default の意味も違う。
  annotation の `@Range` option は int だけ。typed factory を削除するか、long / float / double の範囲付きを足すか（R2-A-20）。
- `[?]` `addPreprocess(Predicate)` が `addMiddleware` / `addPrerequisite` と役割で重なる。GA 前に削除するか、役割を明記して残すか
  （R2-A-21）。
- `[?]` modded では command block（level 2）が `OP` default の node に level 4 を要する（vanilla の command は 2）。level を
  vanilla に合わせるか（R2-E-13）。
- `[?]` console の扱いが platform で違う: modded / Velocity の console は `NONE` の node も通るが Spigot / Paper は通らない、modded の
  `requireConsole` は RCON を受理し Spigot / Paper は拒否する。揃えるか docs に書くか（R2-R2C-4）。
- `[?]` cooldown の予約中に弾かれた実行に、残り時間として cooldown の全期間を伝えている。専用 caption にするか、予約からの経過で
  計算するか（R2-D-27）。
- `[?]` Spigot / Paper は `plugin.yml` で宣言済みの command / option / argument の permission を置き換え、default・children・
  description が消える（cooldown bypass node だけ保護）。どちらを優先するか。上の「複数の登録が同じ node を宣言したときの規則」と
  併せて決める（R2-R3B-1）。
- `[?]` Paper 1.21.4+ で実行時に登録した label は plugin の metadata の無い plain node になり、`/help` の分類と `dispatchCommand`
  の permission（`minecraft.command.<label>`）が変わる。reflection で metadata を付けるか docs に書くか（R2-CME）。
- `[?]` testing artifact の API を揃える: `with*` の命名（modded-testing は `withPlayer` / `withLevel` / `withServer`、他は
  `withFakePlayer` / ...）、common の main にある `Snapshots` の置き場所、`CommandLibExtension` の共通化（R1-C-06）。
- `[?]` `FakeSender` は既定で全 permission を持ち `DefaultPermission` を適用しない（velocity-testing を除く）。登録の default と
  `isOp` を適用する opt-in mode を足すか。integration test の bot も常に operator で、`OP` default が非 op player を拒否することは
  実サーバーで未検証（R1-B1-08、R1-B2-09）。

### P3: Minecraft 連携で CommandAPI を上回る

- `[~]` missing built-in arguments を catalog に基づいて実装する（NMS 依存引数は `MockNMSArgumentXxx` + `XxxArgumentTest`
  をセットで追加する）。`GameModeArgument`（Spigot / Paper）、`BiomeArgument`（Spigot / Paper）、`WorldArgument`（Spigot /
  Paper）、`NamespacedKeyArgument`（Spigot / Paper）、`ResourceLocationArgument`（Forge）、`SoundArgument`（Spigot /
  Paper）、`EntityTypeArgument`（Spigot / Paper）、`AttributeArgument`（Spigot / Paper）、`AdvancementArgument`（Spigot /
  Paper）、`LootTableArgument`（Spigot / Paper）、`RecipeArgument`（Spigot / Paper）、`ObjectiveArgument`（Spigot /
  Paper）、`ScoreboardDisplaySlotArgument`（Spigot / Paper）、`ChatColorArgument`（Spigot / Paper）を追加済み。
- `[~]` `/execute`、command blocks、functions、tags の挙動を検証して docs / tests に落とす。2026-10-17（`89b9503f`）に全 78 target で
  integration test が全て通った（[`docs/compatibility.md`](compatibility.md)、[`integration-test/README.md`](../integration-test/README.md)）。
  Spigot / Paper の command block と function tag（`VanillaExecutionTest` / `DataPackFunctionTest`）、modded の command block
  （`ActorTest` / `PermissionTest`）は integration test がある。残り: Forge / NeoForge / Fabric の function / tag、未実行の patch release。
- `[?]` `/execute as` 後の actor を platform 間で揃えるか決める。Spigot / Paper では sender / actor は元の sender（console）のまま、
  Forge / NeoForge では実行 entity に従う（permission 判定も含む）。
- `[ ]` Paper で resolve error の translatable component が実 client で翻訳されること、`MessageComponentSerializer` が
  Paper 1.21.3〜26.x で動くことを integration test（`CommandSyntaxExceptionTest`）で確認する。
- `[ ]` optional / variadic argument の command tree の大きさ（`maxCount` × option の展開）を確認する。実サーバーでの parse と実行は
  `OptionalVariadicArgumentTest`（Spigot / Paper / modded fixture）で確認済み。
- `[ ]` cooldown の永続化（restart 後に復元する hook）、argument branch 単位の confirm / cooldown / `@Condition`。
- `[~]` option の redirect 化（`OptionGraph`）を実サーバーで検証する。Spigot / Paper / Mohist / Forge / NeoForge は 2026-10-16 に検証済み、Velocity が残り。
  argument child（argument の下の command）は redirect を使わない tree のままで、同じ state の path が子 node を共有する。
  option が多すぎて tree が大きくなる command は登録時に `IllegalStateException` で拒否する。
- `[ ]` Mohist 1.20.1 は player の構文エラーを Bukkit の "Unknown command" で返すため、CommandLib の失敗 message（caption を含む）が player に届かない。回避できるか調べる。
- `[ ]` caption の locale を実サーバーで確かめる test が bot の既定 `en_us` だけなので、他 locale の bot を追加する。
- `[ ]` CommandLib 組み込み message の他言語 bundle を増やす（現在は en / ja）。annotation の `java.util.Optional<T>` 引数（core が null default を許さない）。
- `[ ]` option の redirect 復元の変更後に `ExecuteBenchmark` を idle な環境で baseline と比較する。
- `[ ]` Kotlin DSL を Forge / NeoForge に広げ（module 化には Stonecutter の Loom node 5 つ分が必要）、Kotlin 関数の default 引数から
  command を作る形を追加する。
- `[~]` options の継承と配置拡張（subcommand への継承、固定 arity の multi-value）を実装する。
  repeatable / count / aliases / required / 排他と、positional argument の後の option（opt-in の `optionsAnywhere()`）は実装済み。
- `[ ]` `sendSuccess` が `sendCommandFeedback` gamerule と silent source（function 等）を vanilla と同じに扱うことを integration test で
  確認する。Paper は `sendSuccess` を reflection で呼び、見つからないと通常の message に
  切り替わるため、実サーバーで vanilla の経路を通ることも確かめる。
- `[?]` fluent / builder / Kotlin にも名前付きの reusable condition registry を用意するか決める。現在は annotation の `@Condition` だけ。
- `[ ]` rich help（hover / click の usage line）と MiniMessage を追加する。
- `[?]` 構文エラー時の usage 表示（`ErrorFormatter.SYNTAX_ADVICE`）を既定にするか決める。現在は opt-in で、既定にすると全 module の
  argument error の出力が変わる。

### P5: Platform expansion

- `[x]` Forge testing strategy を決め、`forge-testing` を追加する。実サーバーの integration targets（`forge-1.16.5`、`forge-1.20.1`）と、サーバーなしの `forge-testing-<version>`（`CommandTester` / `FakeSender`）を追加済み。
- `[~]` NeoForge / Fabric / Forge の module 方針を決める。方針案は
  [11. Platform Architecture](#11-platform-architecture) を参照。
- `[x]` Architectury Loom + Stonecutter の PoC として、`forge:1.16.5` と `forge:1.20.1` を単一ソースから build する。
- `[?]` mod loader 系 module を `minecraft`（MC version 依存・loader 非依存）と `loader/*`（loader 依存）に分割する。Fabric 追加時点では
  loader 依存部分が登録・permission・server 取得・reflection 2 箇所と metadata だけで、分割は不要だった。
- `[ ]` modded を 1.21.5〜1.21.11 と 26.x に広げる。1.21.11 は `ResourceLocation` → `Identifier`、permission level → `PermissionSet`、
  `GameProfile` の record 化などで約 50 箇所が壊れ、26.x は `dev.architectury.loom-no-remap` と Java 25 が必要。
- `[~]` Fabric の integration target（test mod と server setup の Fabric 対応）を追加し、`samples/fabric` の shade jar を実サーバーで読み込む。
  `fabric-1.20.1` / `fabric-1.21.1` target は追加済み（2026-10-17）。残りは `samples/fabric` の shade jar と `fabric-1.21.4` target。
- `[~]` sender / message abstraction（Adventure と vanilla `Component` の差分吸収）を Velocity 着手前に設計する。
  message 側は `RichText` と platform ごとの変換（`RichTexts.toBungee` / `toAdventure` / `toVanilla`）で Phase 1 を実装済み
  （[`docs/design/rich-text-messages.md`](design/rich-text-messages.md)）。sender 側は未着手。
- `[~]` public API の binary compatibility check（japicmp 等）を CI に追加する。`:common:japicmp` / `:spigot:japicmp` は
  root `build.gradle.kts` の `apiBaselines` にある JitPack の release と比較できる。基準の 0.17.2 は `net.kunmc.lab`
  package のままなので、0.18.0 の release 後に基準を移し、paper と `*-testing` を加え、CI で実行する。
- `[ ]` capability-based registration に移行する。

### P6: Ecosystem と tooling

- `[?]` `CommonCommandContext` の可変状態と変更経路を棚卸しし、責務を分割する。解析済みの入力・引数などの読み取り用データと、
  実行結果・失敗・出力などの実行中の状態を分離し、初期化後の変更を減らす。option 復元時の handle 差し替え、遅延解決の cache、
  非同期実行で共有される状態を含めて検討し、既存 API への影響と段階的な移行方法を設計する。
- `[ ]` override したメソッドの戻り値・引数に `@NotNull` / `@Nullable` などの annotation が欠けていないか全 module を点検する。
  親の契約と各 annotation の継承・tool による解釈を確認し、必要な明示を揃える。生成ソースは generator 側で対応する。
- `[?]` override 時の annotation 欠落・契約の不一致を検出する linter を選定・設定する。既存の Error Prone / NullAway で
  検出できる範囲と明示 annotation を要求する規則の差を確認し、対象 annotation、例外、Gradle / CI への組み込み方を決める。
- `[~]` documentation site を作る。MkDocs Material の設定（`mkdocs.yml`、`mkdocs build --strict` が通る）はあるが、公開（GitHub Pages 等）は
  CI / 配布方針が決まるまで保留。JitPack の javadoc URL が実際に表示されるかは未確認。
- `[ ]` CI を整備する。typed NMS module（`nms/*`）は初回 build で BuildTools を version ごとに実行し（1 version あたり
  10 分前後）、paperweight の dev bundle も取得するため、`~/.m2` の Spigot artifact と paperweight cache を CI 間で
  保持する方法を決める。cache がない job や PR では `-Pcommandlib.typedNms=false` で module を外して unit test だけを
  実行することも検討する（その spigot jar は NMS 実装を持たないので配布しない）。integration test の CI matrix もここで扱う。
  CI では `check`（Spotless、compile 時の Error Prone / NullAway、doclint）と japicmp を実行する。jev-lint を CI で
  使う場合は、verdict cache（`.jev-lint/baseline.json`）を commit して key なしで lint する運用に切り替える。
  生成済み typed NMS source の手編集や再生成漏れを検出するため、`tools/nms-codegen` で `uv run generate.py --all` を実行し、
  `git status --porcelain -- nms spigot/src/main/java/dev/marulabs/commandlib/internal/nms/TypedNmsModules.java` が空でなければ
  失敗させる step も入れる（`git diff --exit-code` は新規の未追跡ファイルを検出しないため）。
- `[?]` （候補）compile 用の API jar を build で自動生成する。公開 class だけを抜き出した jar を `compileOnly`、
  全 class を含む jar を runtime / shade 用に配布すれば、内部 API は補完に出ず compile も通らない。両 jar に同じ
  binary の公開 class を持たせる方法、shade 時の class の重複、Gradle の variant と公開 metadata を検証する必要がある。
  上の整理の後に、需要を見て採否を決める。
- `[ ]` 0.18.0 を release する。`AbstractCommandContext` の削除や `net.kunmc.lab` から `dev.marulabs` への package 移動など、
  0.17.x と binary 互換でない変更を含む。
- `[ ]` JitPack から Maven Central に移行する。javadoc jar と doclint は Bukkit 側の module と modded / modded-testing の
  node に設定済み。
- `[ ]` integration test 用 mod（`integration-test/shared/modded`）にも Error Prone / NullAway を広げる。
- `[?]` （優先度低・候補）config-driven customization。server admin が YAML 等で command 名、alias、無効化、message を
  再コンパイルなしで上書きできるようにする。需要を見て採否を決める。

### 将来検討: Minecraft 外の CLI 対応

- `[?]` 通常の CLI 向け module の実現性と API を設計する。`main(String[] args)` による単発実行と対話式 console を区別し、
  token 境界・空文字・引用符、stdout / stderr、終了コード、help、補完、非同期処理の完了待ちを検討する。
  `CommonXXXArgument` は Brigadier の入力・context に結び付いた共通実装なので、その流用や追加の抽象化を前提にしない。
  CLI 独自の argument model を含む小さな PoC で共有可能な範囲を判断する。Minecraft 対応を優先する既存方針は維持する。
- `[?]` 一般的な Unix / Linux の command line 定義・慣習と CommandLib の差分を調査する。POSIX の規約と GNU の拡張・慣習を
  区別し、短縮 option の結合、`--name=value`、`--`、option と operand の順序、負数、繰り返し・複数値、subcommand、
  help / usage、終了コード・標準入出力などを比較表にする。shell が処理する引用符・展開・pipe とライブラリの責務も分け、
  Minecraft 内でも改善できる項目と CLI 固有の項目、互換性への影響を整理する。

## 目標

CommandLib は、plugin / mod 開発者が 1 つの型安全なコマンドモデルでコマンドを定義でき、platform、Minecraft
version、Brigadier、NMS の差分を意識しなくて済む状態を目指す。

最終的な目標は次の状態である。

- CommandAPI に対して、vanilla command 連携、argument coverage、`/execute` 対応、suggestions、permissions、documentation、testing
  で上回る。
- Cloud / Lamp に対して、拡張性、annotation、Kotlin support、dependency injection、execution pipeline customization で上回る。
- ACF に対して、reusable conditions、help / error customization、localization、syntax advice で上回る。
- 利用可能な環境では Paper 公式 command API を使い、旧 Bukkit / Spigot / Paper 互換も維持する。
  公式 API を直接使う場合と同じ vanilla semantics を保ち、定義・テスト・運用の負担を減らす。
- Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric を、可能な限り同じ public command model に揃える。
- downstream plugin が安全に shade / relocate できる状態を維持する。

## 競合ベースラインと勝利条件

比較基準は 2026-10-14 に確認した公式資料と現行ソース。競合の更新に合わせて release ごとに再確認する。
「未確認」は「非対応」と区別し、競合が型安全な API、複数 platform、testing を持つことも前提に比較する。

| 対象 | 競合の強み | 現行 CommandLib | 上回るための到達条件 |
|---|---|---|---|
| CommandAPI | vanilla arguments、result executors、functions / tags、tooltips、annotation / Kotlin、test toolkit | typed callback、options、広い Spigot / Paper matrix、公開 testing、tooltip と bootstrap の基礎あり | 引数を名前でなく parse / suggestion / error / registry semantics で比較し、主要カテゴリの同等性と型付き callback・options・test coverage の優位性を示す。result / success、feedback、reload、command block / function / tag を実サーバーで保証する。 |
| Cloud | execution coordinators、future handlers、pipeline、sender mapping、DI、captions、複数 API / platform | `executeAsync` と platform 別の `CommandScheduler`、middleware、sender mapping（`provide`）、context value の DI、caption がある | 非同期・thread 復帰・例外処理・DI・sender mapping を合成でき、同じ Minecraft ユースケースを少ない設定と型付き API で実装・検証できる。 |
| Lamp | annotation、DI、custom annotations、optional / default、Kotlin default args、flags、複数 platform | fluent / tree、argument-first child、typed options、custom annotation を足せる annotation API、Kotlin DSL がある。Kotlin 関数の default 引数から command を作る形はない | 同じ example を fluent / builder / annotation / Kotlin で自然に書け、default / variadic / options と独自 annotation の拡張が同じモデル・help・tests に反映される。 |
| ACF | conditions、validation、completion、help、syntax advice、DI、annotation | validator / prerequisite、generated help、branch permissions の基礎あり | reusable conditions と formatter / locale を備え、権限・options・入力位置に整合した案内を出し、snapshot と移行例で確認できる。 |
| Paper Command API / Brigadier | 公式 lifecycle、bootstrap、native arguments、vanilla semantics と直接制御 | 独立 paper module、Adventure、lifecycle / bootstrap、function 再 compile 対応あり | result / success、source / executor、registry、reload の意味を保ち、型付き callback・help・options・testing・診断で開発負担を減らす。必要な native extension point を提供する。 |

公式資料:

- [CommandAPI](https://docs.commandapi.dev/)、[result executors](https://docs.commandapi.dev/create-commands/executors/resulting-executors)、[testing](https://docs.commandapi.dev/test/intro)。
- [Cloud Core](https://cloud.incendo.org/core/)、[Minecraft platforms](https://cloud.incendo.org/minecraft/)、[Fabric / NeoForge](https://cloud.incendo.org/minecraft/modded/)。
- [Lamp](https://github.com/Revxrsal/Lamp)、[ACF](https://github.com/aikar/commands)。
- [Paper command registration](https://docs.papermc.io/paper/dev/command-api/basics/registration/)、[Brigadier](https://github.com/Mojang/brigadier)。

## 実装順と完了判定

backlog は作業項目、Product Pillars は仕様の詳細、Release Phases は公開可能なまとまりを示す。
同じ機能の状態は実装状況サマリを基準に更新し、完了の証拠は roadmap-history に残す。

1. **現在地と配布を固める**: 実装済み API の docs / tests を揃え、0.18.0 の移行と release notes（`CHANGELOG.md`）を整える。
   CI、CI での binary compatibility check、Maven Central への移行は 0.18.0 の release 後に回す（2026-10-17 決定）。
   それ以外の P6 の配布・品質項目は Phase 5 まで待たず、各 phase の公開前に必要な範囲を実施する。
2. **vanilla semantics を完成させる**: argument catalog、result / success、feedback、command block / function / tag の保証を優先する。
3. **共通モデルを拡張する**: optional / variadic、8 引数以上の型安全な表現、options、sender / message、thread / async / middleware を設計する。
4. **定義方法と platform を広げる**: 共通モデル上に builder / annotation / Kotlin、DI、locale と Folia / Velocity / Fabric、modded version matrix を載せる。
5. **競合優位を証明する**: 共通 example、移行ガイド、公開 test kit、diagnostics、exports、benchmark を揃える。

機能の完了には public API、使用例、仕様、focused tests が必要。platform の対応完了には実サーバーの証拠が必要。
fixture の存在、設計方針の決定、compile 成功だけでは全挙動の保証完了としない。

- [x] 競合比較用の共通 example suite を作る（`examples/` module。7 use case を fluent / builder / annotation / Kotlin で書き、
  同一の transcript を snapshot で確認）。
- [x] 各 example の定義量、取得キー / cast、設定手順、拡張のしやすさ、テスト可能範囲を比較し、差分と制約を公開する
  （[`docs/comparison.md`](comparison.md)。競合側の snippet は未 compile、一部未検証）。
- [~] parse / suggestion の p50 / p95 latency、throughput、allocation と起動 / 登録コストを同条件で計測する。JMH の `benchmark` module、
  事前に決めた回帰許容値と `:benchmark:jmhCompare` がある（[`docs/benchmarks.md`](benchmarks.md)）。初回 baseline は高負荷下で取ったため、
  時間指標で gate する前に idle な環境で取り直す。platform tester / async / 競合比較の計測は未実施。
- [ ] 競合の各主要領域で同等性を確認し、少なくとも一つの具体的な優位性を example / test / measurement で示す。
  未対応カテゴリの deferred reason は中間 release の判断材料であり、最終的な勝利条件の代わりにはしない。
- [ ] platform 固有の制約と対象外用途を公開する。CLI / JDA 等への拡張は Minecraft 領域の完成後に別 phase として判断する。

## Product Pillars

### 1. Compatibility First

CommandLib は、複数の Minecraft version と platform をまたいで 1 つの command model を使いたい利用者に選ばれるべきである。

Required:

- 実装済み `docs/compatibility.md` を更新し、Bukkit、Spigot、Paper、Folia、Mohist、Velocity、Forge、NeoForge、Fabric ごとに
  supported、tested、best-effort、unsupported を明示する。
- supported modules に対する focused compile / test matrix を作る。
- integration test の結果を docs に掲載する。
- Paper 1.20.6+ では Paper 公式 command API を優先する。
- 可能な限り version-string branching より capability detection を優先する。

Required platform expansion（Phase 4）:

- Folia support。
- Velocity support。
- Fabric support と Forge / NeoForge version matrix 拡充。
- Mojang-mapped server regression tests。

### 2. Argument Supremacy

CommandLib は、利用者が Brigadier や platform internals に降りなくても必要な引数を標準で扱える状態にする。

Required:

- CommandLib、CommandAPI、Paper の argument 対応表を追加する。
- missing arguments を実際の plugin 需要と parity gap に基づいて優先付けする。
- すべての argument に parse、suggestion、validation、error のテストを追加する。
- range、optional、default、list、map、greedy、quoted string、namespaced key、registry value、enum alias の挙動を標準化する。
- optional / variadic argument は argument 列の末尾にのみ置けるものとし、違反は登録時に runtime error にする。
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
- Range（int / double）。
- Angle / rotation。
- Predicate arguments。

### 3. Best-In-Class Suggestions

Suggestions は高速で、型安全で、permission-aware で、tooltip-capable であり、legacy Bukkit と modern Paper の両方で安全に動く必要がある。

Required:

- suggestion API を sync、async、default、additional、replacement の観点で整理する。
- permission filtering を suggestions と help に一貫して適用する。
- 実装済み String / Brigadier Message tooltip の保証を platform 別に検証し、rich tooltip の統一 API を整える。
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
- permission / state 変化時に command tree を再送する dynamic refresh API を追加する。

Required tooling（Phase 5）:

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

Required developer experience / tooling（Phase 3 / 5）:

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

Cloud の大きな強みは pipeline customization である。CommandLib は同等の拡張性を備え、型安全性・thread 安全性・テスト容易性で上回る。

Required:

- middleware / interceptor API。
- pre-execution validation hooks。
- post-execution hooks。
- exception mapping。
- result handling（int result / success を `/execute store` に反映）。
- async execution と main / region thread への復帰。
- cooldown module。
- confirmation module。
- dependency injection hooks。
- sender mapper。
- context enrichment。

Stretch:

- transaction-like command execution。
- rate limit module。
- metrics hooks。

### 9. Testing Framework

CommandLib は、この repository 自体だけでなく downstream plugin authors にとっても command behavior をテストしやすい状態にする。

詳細は `docs/design/testing-strategy.md` を参照。

#### テスト層の方針

- **Layer 1 (unit)**: `spigot:test` / `common:test` — NMS 非依存ロジックを検証。
- **Layer 2 (command-level)**: `spigot-testing` の `CommandTester` — `Mockito.mockStatic` で `NMSClassRegistry` を
  スタブし、実 NMS なしでコマンド parse・実行を検証。CommandAPI の「実 NMS + `Bootstrap.bootStrap()` +
  Mockito スパイ」方式は採用しない。
- **Layer 3 (integration)**: `integration-test` — Testcontainers + 実 Minecraft サーバー + MCProtocolLib で E2E 検証。

新引数追加時は **Mock 実装クラス（`MockNMSArgumentXxx`）と引数テスト（`XxxArgumentTest`）を必ずセットで作成する**。

Required:

- common / Spigot / Paper / modded の公開 testing framework を文書化し、引数別の実 parse / mock / 未対応範囲を公開する。
- common parser tests。
- help、usage、errors の snapshot tests。
- fake sender、fake player、fake permission model。
- suggestion assertion utilities。
- command execution assertion utilities。
- `IntegrationTarget` に `platform` フィールドを追加し、multi-version integration test guide を整備する。
- 実装済み `CommandTester` Builder と mock 差し込みの使用例・互換性・保証範囲を整える。

Required downstream testing（Phase 5）:

- JUnit extension（`CommandLibExtension`。common / spigot / paper / velocity / modded の testing artifact で実装済み）。
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

旧 Forge module の version 別コピーは廃止済み。現行は `modded/src` の単一ソースから
Forge 1.16.5 / 1.20.1 と NeoForge 1.21.1 を build する。残る作業は version matrix の拡充、Fabric 追加、
MC 依存層と loader 依存層の分離である。以下の module 分割は目標構成であり、現行の directory 構成ではない。

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

### Phase 2: Vanilla Integration Beyond CommandAPI

目的: CommandAPI との parity を通過し、vanilla 連携・型安全性・検証容易性で上回る。

- priority arguments を実装する。
- 実装済み tooltip suggestions を検証し、rich tooltip の API を整える。
- `/execute`、command block、function、tag behavior を検証する。
- error taxonomy を改善する。
- options syntax を拡張する。
- execution result と `/execute store` 連携を追加する。
- vanilla-style feedback を追加する。
- CommandAPI からの migration guide を追加する。

Exit criteria:

- CommandAPI の主要 argument categories を挙動別に比較し、equivalent の tests がある。未対応は明記し、最終完遂まで追跡する。
- result / success、feedback、command block / function / tag、reload を integration tests で確認する。
- 共通 example で typed callback / options / testing の具体的な優位性を示す。
- integration tests が主要 supported Spigot / Paper versions をカバーしている。
- 最小限の CommandAPI migration example が存在する。

### Phase 3: Developer Experience Beyond Cloud, Lamp And ACF

目的: Cloud / Lamp / ACF の主要な開発体験を満たし、型安全性・合成容易性・テスト容易性で上回る。

- variadic / optional argument。
- async execution。
- confirmation / cooldown。
- dynamic command tree refresh。
- annotation API。
- explicit Java builder API。
- Kotlin DSL。
- middleware / interceptor API。
- sender mapper。
- dependency injection。
- locale / caption system。
- custom annotation extension point。

Exit criteria:

- 同じ example command を fluent、Java builder、annotation、Kotlin DSL の 4 style で書け、help / permissions / options / tests が一致する。
- cross-cutting behavior を middleware として実装できる。
- Kotlin examples が casts や raw types なしで書ける。
- async execution の timeout / cancellation / exception / thread 復帰を検証する。同期の vanilla result と非同期完了の扱いを明文化する。
- DI、custom annotations、conditions、locale / formatters の共通 example と競合からの移行例がある。

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

- Folia / Velocity / Fabric の public module / API が実装され、登録・実行・補完・権限を対象環境で検証している。
- Folia の entity / region thread 復帰と安全な scheduler 利用を実サーバーで検証している。
- Forge / NeoForge の対象 version matrix と公開 testing artifact の保証範囲を文書化している。
- Forge / NeoForge / Fabric が Stonecutter による単一ソースから build されている。
- public API の binary compatibility check が CI で動いている（0.18.0 の後。CI 整備を 0.18.0 の後に回したため、0.18.0 の
  release 条件にはしない）。
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
- Maven Central 配布、documentation site、競合からの migration guides と downstream compatibility test kit が利用できる
  （Maven Central は 0.18.0 の後）。
- 競合比較 suite と再現可能な benchmark 結果を公開し、未解決の差分と regression を追跡できる。

## Non-Goals

当面やらないこと:

- Bukkit、Spigot、legacy Paper compatibility を捨てて latest-Paper-only にする。
- fluent API を annotation-only API で置き換える。
- 競合ライブラリの API を完全にコピーする。
- Minecraft parity より先に non-Minecraft CLI / JDA support を優先する。
- supported legacy versions を壊してまで reflection / NMS を完全排除する。
- unknown subcommand / 値に対する error 時の「もしかして」提案。入力補完で十分にカバーできるため。

## Success Metrics

最終目標の達成は機能数だけで判定せず、競合ベースラインの各勝利条件を証拠付きで満たすことを必要とする。

- README だけで、新規 plugin が CommandLib を選ぶ理由を説明できる。
- CommandAPI、Cloud、Lamp、ACF からの migration guides がある。
- supported Minecraft versions が focused tests または integration tests で裏付けられている。
- breaking changes が release notes で管理されている。
- downstream plugins が execution、permissions、suggestions、help をテストできる。
- Minecraft release 後、latest Paper support を短期間で更新できる。対応までの日数を release ごとに記録し、追従目標を公開する。
- 競合の主要ユースケースを共通 example suite で再現でき、型安全性・開発負担・拡張性・検証範囲の優位性を説明できる。
- 性能・メモリ・登録コストの再現可能な測定と回帰検知がある。未測定の性能優位を主張しない。
