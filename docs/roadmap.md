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
| 型安全な argument callback              | `[x]` | `Unary` から `Hept` までの typed branch callback と、任意個の引数を `argument(a, b, ..., i)` / `argument(List)` で受けて `ctx.getArgument(arg)` で型付きに読む形がある。 | Kotlin DSL の 8 引数以上の overload。 |
| Variadic / optional arguments       | `[x]` | 末尾の `optional(default)` / `optional(Extractor)` と `variadic([max])`（`List<T>`）がある。配置違反は登録時に `IllegalStateException`（[`docs/design/optional-variadic-arguments.md`](design/optional-variadic-arguments.md)）。 | 上限なしの variadic、`Optional<T>` 形式、実サーバーでの検証。 |
| Execution result                    | `[x]` | `ctx.setResult(int)` が `/execute store result` に、失敗（例外・権限・prerequisite 等）が `store success 0` に反映される。spigot-1.16.5 / paper-26.3 の integration test あり。 | Forge / NeoForge の実サーバー検証。 |
| Async execution                     | `[x]` | `executeAsync(...)`（全 typed branch）と `ctx.supplyAsync` / `runAsync`、platform 別の `CommandScheduler` で sync thread へ復帰する（[`docs/execution.md`](execution.md)）。 | 実サーバーでの scheduler 検証（Bukkit / Forge / Folia）。 |
| Dynamic command tree refresh        | `[x]` | `CommandLib.refresh(player)` / `refreshAll()` が Spigot / Paper / Forge / NeoForge にある（[`docs/permissions.md`](permissions.md)）。 | permission 以外の state による表示切替 predicate、実 client での検証。 |
| Vanilla-style feedback              | `[x]` | `ctx.sendSuccess(message, broadcastToOps)` が全 platform で vanilla の `CommandSourceStack#sendSuccess` を通り、gamerule と silent source を尊重する。 | selector で複数 target に適用した場合の集計 message、gamerule の tests。 |
| Bukkit / Spigot / Paper support     | `[~]` | `spigot` module（Spigot 1.16.5〜26.3 を Tested）と `paper` module（Paper 1.21〜26.3 を Tested、Folia は best effort）がある。 | Folia の実サーバー検証。 |
| Paper 1.20.6+ registration          | `[x]` | `LifecycleEvents.COMMANDS` + `Commands registrar` + `CommandSourceStack` を使った公式 API 登録が実装済み。Adventure component を標準 message path として利用。 | bootstrap 登録の互換性保証・docs / tests の拡充（`CommandLib.bootstrap(BootstrapContext, ...)` は実装済み）。                                                   |
| Spigot / Paper module split         | `[x]` | `bukkit` を `spigot` へ rename / move し、`paper` module を新設済み。                                                                               | —                                                                          |
| Forge support                       | `[~]` | Forge 1.16.5 / 1.20.1 を Architectury Loom + Stonecutter による `modded` の単一ソースから build。                                                                                                                | 1.16.5+ の対象 version 拡充、loader 非依存層の分離、Bukkit との差分整理。                                     |
| Mohist support                      | `[~]` | README 上で tested version が記載されている。                                                                                                      | integration test の明文化、非対応ケースの整理。                                           |
| Folia support                       | `[~]` | `paper` artifact が Folia を検出し、`FoliaCommandScheduler` で entity / region / global scheduler に戻す。`spigot` artifact は Folia で登録を拒否する。 | 実 Folia server の integration target（Folia 用 fixture が必要）、region thread からの runtime 登録の guard。 |
| Velocity support                    | `[~]` | `velocity` / `velocity-testing` module（Velocity 3.4.0+、`PlayerArgument` / `ServerArgument`）がある。Velocity 3.4.0 / 4.2.0 の proxy で手動 smoke test 済み。 | 実 proxy + backend + client の integration target、Velocity sample。 |
| NeoForge support                    | `[~]` | NeoForge 1.21.1 を Forge と共有の `modded` Stonecutter tree から build（`neoforge-1.21.1`、`neoforge-testing-1.21.1`、integration target あり）。 | 1.21.1 以外の NeoForge version。 |
| Fabric support                      | `[~]` | `modded` の Stonecutter tree から `fabric-1.20.1` / `fabric-1.21.1` を build（`CommandRegistrationCallback` v2、fabric-permissions-api 任意対応）。`modded-testing` も全 node で通る。 | 実サーバーの integration target、Fabric sample、Fabric 用 Kotlin module、metadata の `minecraft` version range。 |
| Brigadier / NMS hiding              | `[~]` | 利用者 API から Brigadier / NMS を隠している。                                                                                                      | capability detection、公式 API 優先、fallback policy。                            |
| `/execute` integration              | `[~]` | README と Bukkit registration path で `/execute run` 連携を扱っている。spigot module と paper module の command は `/minecraft:reload` 後や runtime 登録後も data pack function から使える（`/minecraft:reload` 後は両 module に integration test あり）。paper module で plugin から登録した command は次 tick の function 再 compile 後に使える（bootstrap 登録は server の compile 時点から使える）。 | command block / tag での保証と tests。 |
| Built-in arguments                  | `[~]` | 基本的な primitive、string、player、entity、location、item などがある。CommandAPI / Paper との挙動別 catalog は [`docs/arguments.md`](arguments.md)。 | catalog の missing 一覧（`resourceKey` 相当、angle / rotation、score holder 等）の補完。 |
| Argument validation                 | `[x]` | `validator(...)` と `ArgumentValidator` がある。                                                                                             | — |
| Argument transform                  | `[x]` | `transformer(...)` がある。                                                                                                                 | docs と examples。                                                           |
| Custom parsing fallback             | `[x]` | `additionalParser(...)` がある。                                                                                                            | behavior spec と tests。                                                     |
| Sync suggestions                    | `[x]` | `suggestionProvider(...)`、additional suggestions、`SuggestionBuilder#suggest` の String / Brigadier Message tooltip overload がある。                                                                                   | rich tooltip の platform 別保証、ranking、fuzzy suggestions。                                 |
| Async suggestions                   | `[~]` | `SuggestionBuilder#await(CompletionStage)` がある。                                                                                                      | deadlock policy、cache、invalidation、Paper native path。                      |
| Permission-aware execution          | `[x]` | command / argument branch permission が実行時に使われる。                                                                                         | option permission、diagnostics、export。                                      |
| Permission registration             | `[~]` | Bukkit / Forge の permission registration がある。                                                                                           | reload safety の integration tests、LuckPerms guide。                         |
| Permission-aware help / suggestions | `[~]` | argument branch permission を help / tab completion に反映する設計がある。                                                                          | 網羅 tests と docs。                                                           |
| Typed command options               | `[~]` | flag / value / count option、default、dependency、repeatable（`List<T>`）、alias、required、`mutuallyExclusive(...)`、`OptionParseException`、help の option section がある（[`docs/options.md`](options.md)）。 | subcommand への継承、positional argument の後の option、固定 arity の multi-value、排他 group の専用 error message。 |
| Generated usage / help              | `[x]` | 生成 help / usage に `HelpFormatter` / `UsageFormatter` と `examples(...)` があり、command ごとに継承される（[`docs/help.md`](help.md)）。`serverHelp(...)` は Bukkit 系の `/help` index に載る。 | usage line の RichText 化（hover / click）、`CommandDocs` への examples の出力。 |
| Error taxonomy                      | `[~]` | parse / validation / prerequisite / option 系の例外があり、各 error で sender に見える message を [`docs/errors.md`](errors.md) で公開仕様化した。 | unknown subcommand、missing argument の専用 error、cooldown / confirmation / preprocess message の error formatter 経由化。 |
| Exception handling                  | `[x]` | uncaught exception handler と、middleware での exception mapping / result handling がある。 | global な exception handler。 |
| Preprocess / prerequisite           | `[~]` | `addPreprocess(...)`（middleware 化済み）、`addPrerequisite(...)`、`requirePlayer()` 等がある。 | diagnostics。 |
| Middleware / interceptor            | `[x]` | `addMiddleware(CommandMiddleware)` で pre / post hook、exception mapping、result handling を合成でき、child と argument branch に継承される。`confirm()` / `cooldown()` / preprocess も middleware 上にある。 | — |
| Sender abstraction                  | `[x]` | `provide(Type, ctx -> ...)` で sender を plugin の型に変換し、`ctx.get(Type)` で cast なしに読める。middleware は `ctx.put` で値を足せる（[`docs/context-values.md`](context-values.md)）。 | platform 単位の provider 登録。 |
| Dependency injection                | `[x]` | context value の provider と、annotation API の `registerDependency` / `@Dependency` field がある。DI framework 非依存。 | platform 単位の provider 登録、`require(Class)` の短縮形。 |
| Annotation API                      | `[~]` | `AnnotationParser` と `@Command` / `@Subcommand` / `@Permission` / `@Optional` / `@Default` / `@Range` / `@Suggest` / `@Flag` / `@Option` / `@Confirm` / `@Cooldown` 等、custom annotation の拡張点が Spigot / Paper / Velocity / modded にある。fluent と同じ help / permission / 実行結果を snapshot で確認（[`docs/annotations.md`](annotations.md)）。 | middleware / option 依存（`requires`・排他）/ argument-first child の annotation。 |
| Kotlin DSL                          | `[~]` | `common-kotlin` + `spigot-kotlin` / `paper-kotlin` / `velocity-kotlin` に型推論の効く builder、delegated option、`executeSuspend`、suspend suggestions がある（[`docs/kotlin.md`](kotlin.md)）。 | Forge / NeoForge 版、Kotlin 関数の default 引数から command を作る形、suspend middleware。`common-kotlin` は package-private の `CurrentCommandContext` を同じ package から読むため `common` と同じ class loader が必要。 |
| Java builder API                    | `[x]` | `Command.builder(name)`（common の `CommandBuilder`）が全 platform にあり、fluent と同じ command tree / permission / 結果を snapshot で確認（[`docs/java-builder.md`](java-builder.md)）。 | — |
| Localization / captions             | `[~]` | `Caption` / `Captions` で組み込み message を key 化し、sender の locale で解決する（en / ja 同梱、override、ResourceBundle、`CaptionSender`。[`docs/localization.md`](localization.md)）。 | help / usage formatter API、diagnostics / `CommandDocs` の文言、Fabric node での locale 取得の確認。 |
| Public testing framework            | `[~]` | `common-testing` / `spigot-testing` / `paper-testing` / loader 別 `modded-testing` がある。`CommandTester`（`suggest()`、int を返す `execute`、`assertCommandTreeMatchesSnapshot`）、`FakeSender`、NMS mock、JUnit 5 の `CommandLibExtension` + `@Sender`（velocity-testing 以外）と downstream guide（[`docs/testing.md`](testing.md)）がある。 | velocity-testing の JUnit extension、testing artifact 間の共通化、gamerule / silent source の模擬。 |
| Adventure / MiniMessage             | `[~]` | platform 非依存の rich text 値型 `RichText`（色、装飾、hover text、click、translatable 引数）を全 platform で送れる（Phase 1 完了）。Paper / modded は native component の `sendSuccess/Warn/Failure` もある。 | rich help、rich suggestion tooltip の統一、MiniMessage。設計: [`docs/design/rich-text-messages.md`](design/rich-text-messages.md)。 |
| Multi-version integration tests     | `[~]` | `integration-test` aggregator と Spigot（1.16.5〜26.3 の全 release）/ Paper / Mohist / Forge / NeoForge fixtures がある。                                                             | CI matrix、`IntegrationTarget` への platform フィールド追加、result docs、test kit 化。  |
| NMS Reflection test strategy        | `[~]` | `CommandTester` が `Mockito.mockStatic` で `NMSClassRegistry` をスタブし、軽量な Mock 実装クラス方式で NMS なしテストを実現している。                 | Builder / mock 差し込みの保証拡充、新引数の Mock + Test ルールの継続適用。              |
| Documentation                       | `[~]` | README（Why CommandLib、比較表）、`docs/compatibility.md`、`arguments` / `errors` / `options` / `suggestions` / `permissions` / `testing` / `troubleshooting` と design docs がある。 | compatibility の結果公開・更新、documentation site。 |
| Migration guides                    | `[x]` | CommandAPI、Cloud、Lamp、ACF からの移行 guide が `docs/migration/` にある。 | 未実装機能（annotation、Kotlin、DI、locale 等）の実装に合わせた更新。 |
| Runtime diagnostics                 | `[x]` | `CommandLib.diagnostics()` と opt-in の `CommandLibDiagnosticsCommand` が Spigot / Paper / Forge / NeoForge にある（[`docs/diagnostics.md`](diagnostics.md)）。 | 実サーバーでの検証、plugin 別 filter。 |
| Generated docs / exports            | `[x]` | `CommandDocs` が command docs と permission tree を Markdown / JSON で出力する。 | 排他 option group と option の `requires` の出力、sender 依存の description。 |

## 優先度別バックログ

### P3: Minecraft 連携で CommandAPI を上回る

- `[~]` missing built-in arguments を catalog に基づいて実装する（NMS 依存引数は `MockNMSArgumentXxx` + `XxxArgumentTest`
  をセットで追加する）。`GameModeArgument`（Spigot / Paper）、`BiomeArgument`（Spigot / Paper）、`WorldArgument`（Spigot /
  Paper）、`NamespacedKeyArgument`（Spigot / Paper）、`ResourceLocationArgument`（Forge）、`SoundArgument`（Spigot /
  Paper）、`EntityTypeArgument`（Spigot / Paper）、`AttributeArgument`（Spigot / Paper）、`AdvancementArgument`（Spigot /
  Paper）、`LootTableArgument`（Spigot / Paper）、`RecipeArgument`（Spigot / Paper）、`ObjectiveArgument`（Spigot /
  Paper）、`ScoreboardDisplaySlotArgument`（Spigot / Paper）、`ChatColorArgument`（Spigot / Paper）を追加済み。
- `[~]` `/execute`、command blocks、functions、tags の挙動を検証して docs / tests に落とす。spigot-1.16.5（75/0）、spigot-26.3（76/0）、
  paper-26.3（74/0）、paper-1.20.4（76/0）で integration test が通った（range / block position / predicate / `/execute store` を含む）。
  残りの Spigot / Paper target、mohist、forge / neoforge と、component / message / NBT の fixture は未実行。
- `[?]` `/execute as` 後の actor を platform 間で揃えるか決める。Spigot / Paper では sender / actor は元の sender（console）のまま、
  Forge / NeoForge では実行 entity に従う（permission 判定も含む）。
- `[ ]` `IntegerRangeArgument` / `DoubleRangeArgument` / `DurationArgument` を実サーバーで検証する（旧 target の
  MCProtocolLib bot が `int_range` / `float_range` を decode できるか、NMS range class の link）。
- `[ ]` `BlockPositionArgument` / `ItemPredicateArgument` / `BlockPredicateArgument` を実サーバーで検証する（新しい NMS class の
  link、Paper 1.21.8+ の `blockInWorldPredicate` の reflection、vanilla argument type の補完が client に届くこと）。
- `[ ]` `ComponentArgument` / `MessageArgument` / `NBTCompoundArgument` を実サーバーで検証する（Paper 1.21.3〜26.x での message の
  reflection と vanilla NBT type、旧 bot の parser decode）。modded-testing の fake source は level を持たないため selector を解決できず、
  `docs/arguments.md` の「real selectors」の記述を見直す。
- `[ ]` `TimeArgument` / `RegistryArgument` と Spigot の vanilla 型の display slot / color を実サーバーで検証する（旧 bot の `minecraft:time` decode を含む）。
- `[ ]` Paper で resolve error の translatable component が実 client で翻訳されること、`MessageComponentSerializer` が
  Paper 1.21.3〜26.x で動くことを integration test（`CommandSyntaxExceptionTest`）で確認する。
- `[ ]` optional / variadic argument を実サーバー（Spigot / Paper / modded の integration test）で検証する。各 token を
  platform の `ArgumentType#parse` で読み直す実装のため、command tree の大きさ（`maxCount` × option の展開）も確認する。
- `[ ]` cooldown の永続化（restart 後に復元する hook）、argument branch 単位の confirm / cooldown / `@Condition`。
- `[ ]` permission 以外の state で command の表示を切り替える visibility predicate を追加する（現在は permission を付け外しして
  `CommandLib.refresh(player)` する方式を docs で案内）。
- `[ ]` option の redirect 化（`OptionGraph`）を実サーバーで検証する（Spigot 1.16.5〜26.x、Paper の `ApiMirrorRootNode`、Velocity、modded）。
  argument child と primitive 以外の value option を持つ command は従来の tree のままで、順列で増える。
- `[ ]` 比較で劣った点を埋める: positional argument の後の option、option 単位の permission、primitive / string 以外の value option、
  annotation での argument-first tree、CommandLib 組み込み message の他言語 bundle。
- `[ ]` middleware / option の `requires`・排他 / argument-first child 用の annotation を追加する。
- `[ ]` Kotlin DSL を Forge / NeoForge に広げ、suspend middleware と Kotlin 関数の default 引数からの command 生成を追加する。
- `[~]` options の継承と配置拡張（subcommand への継承、positional argument の後の option、固定 arity の multi-value）を実装する。
  repeatable / count / aliases / required / 排他は実装済み。
- `[ ]` rich help（hover / click の usage line）と MiniMessage を追加する。
- `[?]` 構文エラー時の usage 表示（`ErrorFormatter.SYNTAX_ADVICE`）を既定にするか決める。現在は opt-in で、既定にすると全 module の
  argument error の出力が変わる。

### P5: Platform expansion

- `[ ]` Folia の integration target を追加する（共有 paper fixture は `Bukkit.getScheduler()` を使うため Folia 用 fixture が必要）。
- `[x]` Forge testing strategy を決め、`forge-testing` を追加する。実サーバーの integration targets（`forge-1.16.5`、`forge-1.20.1`）と、サーバーなしの `forge-testing-<version>`（`CommandTester` / `FakeSender`）を追加済み。
- `[~]` NeoForge / Fabric / Forge の module 方針を決める。方針案は
  [11. Platform Architecture](#11-platform-architecture) を参照。
- `[ ]` Velocity の integration target（proxy + backend + bot）を追加する。`ServerArgument` の補完で quote が必要な名前を quote する。
- `[x]` Architectury Loom + Stonecutter の PoC として、`forge:1.16.5` と `forge:1.20.1` を単一ソースから build する。
- `[?]` mod loader 系 module を `minecraft`（MC version 依存・loader 非依存）と `loader/*`（loader 依存）に分割する。Fabric 追加時点では
  loader 依存部分が登録・permission・server 取得・reflection 2 箇所と metadata だけで、分割は不要だった。
- `[ ]` Fabric の integration target（test mod と server setup の Fabric 対応）を追加し、`samples/fabric` の shade jar を実サーバーで読み込む。
- `[~]` sender / message abstraction（Adventure と vanilla `Component` の差分吸収）を Velocity 着手前に設計する。
  message 側は `RichText` と platform ごとの変換（`RichTexts.toBungee` / `toAdventure` / `toVanilla`）で Phase 1 を実装済み
  （[`docs/design/rich-text-messages.md`](design/rich-text-messages.md)）。sender 側は未着手。
- `[~]` public API の binary compatibility check（japicmp 等）を CI に追加する。`:common:japicmp` / `:spigot:japicmp` は
  root `build.gradle.kts` の `apiBaselines` にある JitPack の release と比較できる。基準の 0.17.2 は `net.kunmc.lab`
  package のままなので、0.18.0 の release 後に基準を移し、paper と `*-testing` を加え、CI で実行する。
- `[ ]` capability-based registration に移行する。

### P6: Ecosystem と tooling

- `[ ]` documentation site を作る。
- `[ ]` CI を整備する。typed NMS module（`nms/*`）は初回 build で BuildTools を version ごとに実行し（1 version あたり
  10 分前後）、paperweight の dev bundle も取得するため、`~/.m2` の Spigot artifact と paperweight cache を CI 間で
  保持する方法を決める。cache がない job や PR では `-Pcommandlib.typedNms=false` で module を外して unit test だけを
  実行することも検討する（その spigot jar は NMS 実装を持たないので配布しない）。integration test の CI matrix もここで扱う。
  CI では `check`（Spotless、compile 時の Error Prone / NullAway、doclint）と japicmp を実行する。jev-lint を CI で
  使う場合は、verdict cache（`.jev-lint/baseline.json`）を commit して key なしで lint する運用に切り替える。
- `[ ]` 0.18.0 を release する。`AbstractCommandContext` の削除や `net.kunmc.lab` から `dev.marulabs` への package 移動など、
  0.17.x と binary 互換でない変更を含む。
- `[ ]` JitPack から Maven Central に移行する。javadoc jar と doclint は Bukkit 側の module と modded / modded-testing の
  node に設定済み。
- `[ ]` integration test 用 mod（`integration-test/shared/modded`）にも Error Prone / NullAway を広げる。
- `[?]` （優先度低・候補）config-driven customization。server admin が YAML 等で command 名、alias、無効化、message を
  再コンパイルなしで上書きできるようにする。需要を見て採否を決める。

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
| Cloud | execution coordinators、future handlers、pipeline、sender mapping、DI、captions、複数 API / platform | tree と callback が直結。async suggestions と preprocess / prerequisite はあるが、async execution・汎用 pipeline は未実装 | 非同期・thread 復帰・例外処理・DI・sender mapping を合成でき、同じ Minecraft ユースケースを少ない設定と型付き API で実装・検証できる。 |
| Lamp | annotation、DI、custom annotations、optional / default、Kotlin default args、flags、複数 platform | fluent / tree、argument-first child、typed options を提供。annotation / Kotlin DSL は未実装 | 同じ example を fluent / builder / annotation / Kotlin で自然に書け、default / variadic / options と独自 annotation の拡張が同じモデル・help・tests に反映される。 |
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

1. **現在地と配布を固める**: 実装済み API の docs / tests を揃え、0.18.0 の移行、CI、binary compatibility、Maven Central を整える。
   P6 の配布・品質項目は Phase 5 まで待たず、各 phase の公開前に必要な範囲を実施する。
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

- JUnit extension（`CommandLibExtension`。common / spigot / paper の testing artifact は実装済み、modded-testing は未実装）。
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
- Maven Central 配布、documentation site、競合からの migration guides と downstream compatibility test kit が利用できる。
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
