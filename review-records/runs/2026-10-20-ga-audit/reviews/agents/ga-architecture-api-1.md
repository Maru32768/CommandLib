# CommandLib GA 監査レポート: アーキテクチャと公開 API

- 監査日: 2026-10-20
- 対象: `a65a9974`（master、作業ツリーは clean。監査用に一時追加した `common/src/test/.../GaAuditScratchTest.java` は
  `evidence/reproduction/` に保存して本体からは削除した）
- 実施者: Claude Fable 5.1（Claude Code）。実装を担当したエージェントとは独立した立場で、roadmap / README / docs /
  公開 API / 実装 / テストを読み、仮説は `common` の一時テストで再現した。annotation API と Kotlin DSL /
  testing artifact は同じ session の subagent（read-only）に読ませ、主要な指摘は source で再確認した。
- 対象外: `review-records/` 配下の既存記録（依頼により未参照）、NMS typed module の個別実装、実サーバーでの再実行。

## 1. 判定

**現状の公開 API をそのまま GA（安定契約）として凍結することは推奨しない。** 機能は揃っているが、次の 3 点が「API
利用者が正しく見えるコードで不正な状態を作れ、しかも診断が出ない」種類の欠陥であり、GA 前に修正か明示的な拒否が必要。

1. F01: 親 branch と argument child の引数名が衝突しても登録時に検証されず、実行時に型エラーまたは誤った値になる。
2. F02: `Command` モデルが登録後も可変で、登録後の変更が Spigot では無視され、Paper / Forge では次の reload で有効になる。
   同じ code が platform と時刻で異なる tree を作る。
3. F12: API の境界（internal / stable）と安定性ポリシーが無く、roadmap 自身が「API stability policy」を未着手としている。

加えて F03〜F09 は契約が未定義または platform 間で食い違う箇所で、GA の文書で「仕様」として宣言するか直す必要がある。
好みの範囲の指摘は §6 に分け、客観的欠陥と区別した。

既存テストの再実行は本監査では行っていない（直前の記録で全 module が通っている前提）。再現は
`evidence/reproduction/GaAuditScratchTest.java`（12 シナリオ）で、出力は `scratch-test-output.txt`。

## 2. 重大な指摘（客観的欠陥）

### F01 (High) 引数名の名前空間が経路全体で平坦なのに、検証は node 単位

**根拠**

- `CommonCommandContext.collectInputs`（`common/.../CommonCommandContext.java:57-93`）は親 context と子 context の
  node を 1 つの `name → input` map に入れ、同名は上書きする（variadic の隣接 repeat だけ連結）。
- `Arguments.parse`（`Arguments.java:42-56`）は `parsedArgMap` に名前で書く。`CommandRunner.runInContext`
  （`CommandRunner.java:139-163`）は継承 branch → 自 branch の順に parse するので、子の値が親の値を上書きする。
- Brigadier 自身の `CommandContext` も引数を名前で持つため、親の `IntegerArgumentType.getInteger(handle, "key")` は
  子が書いた String を見つけて `IllegalArgumentException` を投げる。
- `CommandNameValidator.validateChildren`（`CommandNameValidator.java:33-51`）は 1 command の直下の node だけを比較し、
  argument child の branch と親の branch を比較しない。`CommonCommand.validateArguments`（`:490-499`）も 1 branch 内だけ。

**再現**（`GaAuditScratchTest.h1_*` / `h1b_*`）

```java
new Command("config") {{
    argument(new IntegerArgument("key")).child(key -> new Command("set") {{
        argument(new StringArgument("key")).execute((value, ctx) -> key.get());
    }});
}}
```

- `/config 5 set abc` → `java.lang.IllegalArgumentException: Argument 'key' is defined as String, not int` が
  parse 段階（middleware 前）で発生し、runner の catch を抜けて Brigadier に伝播する（`CommandRunner.java:191-197`）。
  sender には platform の汎用エラー、console に stack trace。
- 両方 `StringArgument` にすると例外は出ず、`key.get()` が子の値 `inner` を返す（`parent=inner child=inner`）。
  `ctx.getInputs()` も `[config, inner, set]` となり親の入力が失われる。

annotation API でも同じ（subagent 指摘 1）: `@Subcommand("<team> add") void add(Team team, @Name("team") String note)`
は parse も登録も通る。parser は path の placeholder 名を全部知っている（`AnnotationParser.java:625-650`）ので
parse 時に拒否できる。

**なぜ欠陥か**: 合法に見える定義が診断なしに壊れる。`docs/design/argument-child-command.md` は「名前で読む」設計を述べるが、
名前の一意性が経路全体に必要であることをどこにも書いていない。

**修正方向**: `addArgumentChildren` / `CommandNodeCreator.build` で継承 chain 全体の引数名を検証して `IllegalArgumentException`
にする（最小）。本質的には `parsedArgMap` / 入力 map を branch 単位に分けるか、`ArgRef` が instance 同一性で解決する。

### F02 (High) モデルが登録後も可変で、platform ごとにスナップショット時点が違う

**根拠**

- `CommonCommand` の全 setter は登録後も有効。`CommandNodeCreator.build()` が Brigadier node と `CommandRunner` を作る時点で
  executor / middleware / providers / options を読み取る（`CommandRunner` コンストラクタ `:44-67`、
  `CommonCommand.middlewares()` `:740-763`）。
- Spigot は node を 1 回作って保持する（`spigot/.../CommandLib.java:360-367`、再利用 `:313`, `:386`）。
- Paper は `COMMANDS` event のたびに `commands.buildNodes()` で作り直す（`paper/.../CommandLib.java:664-676`、
  `addNodes` `:696-712`）。`/minecraft:reload` のたびに現在のモデルから再構築される。
- modded は `RegisterCommandsEvent` / `CommandRegistrationCallback` のたびに再構築する（`modded/.../CommandLib.java:360-370`）。
- Velocity は 1 回。

**再現**（`h3_*`, `h10_*`）

- 登録後に `command.addChildren(new Command("late"))` → dispatcher は `/a late` を `Incorrect argument` で拒否するが、
  `CommandTreeRenderer.render`（= `CommandTester.commandTree()` / `CommandDocs.tree()` / diagnostics）は `late` を表示する。
- 登録後に `command.provide(String.class, ...)` → `visibleWhen` の predicate は値を見る（`isVisibleTo` が `providers()` を
  都度評価、`CommonCommand.java:162-165`）が、executor は見ない（runner が構築時に snapshot、`CommandRunner.java:66`）。

**状態遷移**: plugin が `Command` の参照を保持し `onEnable` 後に `cooldown(...)` を足した場合、Spigot では永久に無効、
Paper では次の data reload から有効、Forge では次の `/reload` から有効。同一 command instance を 2 回 `register` した場合、
`CommandDefaults.applyTo` が `defaults` を上書きし（`CommandDefaults.java:81-83`、Javadoc も明記）、Paper の reload 後は
両方の登録が最後の defaults で再構築される。

**なぜ欠陥か**: 「登録はスナップショットである」とも「モデルは生きている」とも定義されておらず、platform で実挙動が違う。
testing artifact の snapshot assertion はモデル側を描画するため、dispatcher が知らない tree に対して green になる。

**修正方向**: (a) 登録時に command tree を凍結し、以後の setter を `IllegalStateException` にする（`registered` flag を root から
伝播）。(b) Paper / modded でも最初に build した node を再利用する、または「再構築は同じ tree になる」ことを凍結で保証する。
(c) どちらにしても docs に契約を書く。

### F03 (Medium) 権限 node が経路位置を含まないため、別経路が同一 node を共有し default が衝突する

**根拠**: `CommonCommand.permissionNameWithoutPrefix()`（`:923-928`）は literal 名だけを連結し `parentArguments` を無視する。
`docs/permissions.md` はこれを仕様として表に載せている（`/config <key> get` → `config.get`）。しかし直下の子 `b` と
`<a>` の下の子 `b` は別の Brigadier node なので validator は両方許し、同じ node `prefix.cmd.b` を 2 回、異なる default で
登録する。Spigot は `removePermissionCompletely` → `addPermission` で後勝ち（`spigot/.../CommandLib.java:821-839`）、
Paper の `CommandPermissions.add` も同じ（`paper/.../CommandPermissions.java:49-58`）。

**再現**（`h7_*`）: `CommandTree.permissionConfigs` が `p.cmd.b=NONE, p.cmd.b.c=NONE, p.cmd.b=ALL, p.cmd.b.c=ALL` を返す。
登録順は branch の子 → 直下の子なので、`NONE` と宣言した経路が `ALL` で登録される。

同じ生成規則により `--silent` の option node `prefix.cmd.option.silent` と、子 command `option` の子 `silent` の node も衝突する。

**修正方向**: `CommandTree.permissionConfigs` で同一 node に異なる `PermissionConfig` があれば登録時に拒否する。
経路に引数名を含めるかは仕様判断（互換性に影響）。

### F04 (Medium) `CommonArgument.execute` が作る中間実行点が help / docs / snapshot に出ない

**根拠**: executor は branch（`Arguments.executor`）と argument instance（`CommonArgument.executor`）の 2 箇所に置ける。
`Arguments.executorFor`（`Arguments.java:141-151`）は branch 末尾でなければ instance の executor にフォールバックする。
`CommandNodeCreator` の `runsAt` 検査（`:141-157`）はこれを実行点として数えるが、`HelpMessageAction.help` は
`Arguments.endsUsage()`（branch 全体）しか usage に出さず、`CommandTreeRenderer` / `CommandDocs` も `BranchEntry.executable`
だけを持つ。

**再現**（`h8_*`）: `n.execute(...)` した `IntegerArgument n` を `argument(n, s).execute(...)` に置くと `/cmd 1` が n の
executor を実行するが、描画は `<n> <s>` の 1 行だけ。instance を別 command で再利用すると executor も一緒に移る。

**修正方向**: positional 引数の instance executor を廃止して branch だけにするか、中間実行点を help / docs / tree に出し、
`runsAt` と同じ規則で文書化する。

### F05 (Medium) index ベースの `getInput(int)` / `getArgument(int)` の意味が食い違い、platform 依存

**根拠**: `getInputs()` は literal を含む（`collectInputs` は option 名以外の全 node を入れる）、`getArguments()` は parse 済み引数
＋省略 optional の default。Javadoc は `getInput(int)` を「the input of the argument at the given index」と書く
（`CommonCommandContext.java:109-117`）。さらに `HelpMessageAction.java:196-197` のコメント通り Paper 1.21+ は root literal を
node に含めないため、`getInput(0)` の意味が platform で変わる。

**再現**（`h2_*`）: `/a sub foo` で `inputs=[a, sub, foo] arguments=[foo, 7] input0=a argument0=foo`。

**修正方向**: `getInputs()` を引数の入力だけに限定して index を `getArguments()` と揃えるか、index API を非推奨にして
名前 / instance ベースに一本化する。`SuggestionBuilder.getInput(int)` も同じ。

### F06 (Medium) `getOption` が他 command の option を黙って default で返す

**根拠**: `CommonCommandContext.getOption`（`:178-190`）は `option.registered`（どこかの command に登録済みか）だけを見る。
実行中の command の `OptionGraph` に無い option は `optionValues` に無いので default を返す。

**再現**（`h6_*`）: command A の `force` を command B の executor で読むと `value=false has=false`、例外なし。Kotlin DSL では
delegated property が lexical scope で見えるため `command("heal") { val silent by flag(...); subcommand("self") { execute { if (!silent) ... } } }`
が自然に書けてしまい、`/heal self --silent` は構文エラー、`/heal self` は常に false（subagent 指摘 A1、`Dsl.kt:239-242`）。

**修正方向**: `CommandOption` に所有 command を持たせ、実行中 command（親を含めない）の option 以外は `IllegalArgumentException`。

### F07 (Medium) testing artifact の保証が docs と一致しない

- snapshot / `commandTree()` は executor の有無、cooldown bypass node、`require` 前提、`visibleWhen`、description を含まない
  （`CommandTreeRenderer.java` に `executable` / `cooldown` の参照なし）。`docs/testing.md:309-311` は「tree、permission node、
  alias の変更でテストが失敗する」と約束するが、`cooldown(...)` を消しても `execute(...)` を消しても通る。
- `docs/testing.md:58` は全 artifact の `CommandTester` に `suggestions(input, sender)` があるとするが、`velocity-testing` には
  `suggest` しかない（`velocity-testing/.../CommandTester.java:142,149`）。
- 同じ `Command` instance から 2 つ tester を作ると `defaults(...)` が後勝ちで両方に効く（F02 の帰結、
  `common-testing/.../CommandTester.java:184`）。
- `FakeSender.player(...)` は locale 未設定だと JVM default locale で caption を解決するが、実 player は常に client locale を
  持つ。OS が `ja_JP` の開発者と `en_US` の CI で結果が変わる。

### F08 (Medium) 登録 API の失敗時点・取り消し・衝突の扱いが platform で違い、契約が未定義

| 観点 | Spigot | Paper | modded | Velocity |
|---|---|---|---|---|
| 構造エラー（optional 配置、option 数など）が出る時点 | `register()` 内（`enable()` が build） | `register()` 内（`CommandSet.of` は top-level のみ、`buildNodes()` は handler / `addNodes` 内）。初回登録が event 待ちだと **handler 内で SEVERE log** になり `register()` は成功して返る（`paper/.../CommandLib.java:612-620`） | **`RegisterCommandsEvent` 内**。`register()` は `validateTopLevel` だけ（`modded/.../CommandLib.java:93-100`、build は `:367-370`） | `register()` 内 |
| `unregister()` | あり | あり（bootstrap はなし） | **なし**。`REGISTRATIONS` と `DEFAULTS` は増える一方 | あり |
| 同名 label が他 plugin にある場合 | 黙って置き換え（`knownCommands.remove`, `:386-401`） | Paper registrar が他 plugin の label を残す（`:668-669`） | Brigadier merge | Velocity CommandManager の規則 |
| 初期化後の初回登録 | 可（Paper 1.20.6+ も `ServerResourcesReloadedEvent` 経由、`:172-195`） | `IllegalStateException`（`:354-357`） | 可 | 可 |
| 権限 default の決まり方 | 登録ごとに上書き | 登録ごとに上書き | **最初の登録が勝つ**（`DEFAULTS.putIfAbsent`, `:96-97`） | 登録ごと |

これらは個々には理由があるが、`docs/` に横断表が無く、`CommandLib.register` の Javadoc にも `@throws` が無い。
特に modded は「登録時に runtime error」という roadmap の約束（`docs/roadmap.md:267`）が event 内でしか成立しない。

### F09 (Medium, 要決定) `/execute as` 後の actor が platform で違い、cooldown / confirmation / permission の対象も変わる

`docs/vanilla-integration.md` が差分を表にしており、roadmap は `[?]`。帰結として `Cooldown.throttledId` は actor の UUID を使う
（`Cooldown.java:189-195`）ので、`/execute as @a run buy` は Spigot / Paper では console 扱いで cooldown なし、Forge / NeoForge /
Fabric では entity ごとに throttle される。`Confirmation` の key、`ctx.getActor().hasPermission` も同様。
「同じ public command model」を掲げる GA では、どちらかに揃えるか、`getActor()` と `getExecutor()` を分けて両方を公開仕様に
するかを決める必要がある。

## 3. 中程度・軽微な指摘（客観的だが影響が限定的）

- **F10 (Low)** 成立しない option 制約を登録が受け入れる: `required()` 同士を `mutuallyExclusive` にすると必ず
  `Missing required option` で失敗する（`h9_*`）。同じ子を `addChildren` に 2 回渡すと help の usage が重複する（`h5_*`、
  `CommandNameValidator.java:87-89` は「harmless」とするが help には出る）。
- **F11 (Low, 要決定)** cooldown は executor の直前に開始するため、executor が `ctx.fail(...)` で失敗しても消費される
  （`h4_*`、`CommonCommand.java:756-761`）。README / execution.md は「全 middleware を通過したら開始」と書いており挙動通りだが、
  「失敗した実行は cooldown を消費する」とは明言していない。confirmation は失敗時に再要求されるので非対称。
- **F12 (Medium)** API 境界と安定性ポリシーの欠如:
  - `@ApiStatus.Internal` / `Experimental` が 1 つも無い。public だが利用者向けでない型: `PlatformAdapter`（static `get()` を
    持つ SPI）、`ArgumentBranchDelegate`（design doc 自身が open question）、`CommonCommandContext.getHandle()`、
    `util.function.*`、`util.ChatColorUtil` / `StringUtil`、`TextRichText` / `TranslatableRichText`。
  - `build.gradle.kts` の version は `0.17.2`、CHANGELOG は `0.18.0 Unreleased`、japicmp の baseline は旧 package の 0.17.2
    （`build.gradle.kts:311-315`）で比較が成立しない。README は「0.x は minor で破壊的変更あり」と宣言。
  - roadmap 自身が Fluent API の「足りないもの」に API stability policy を挙げたまま `[x]`。GA が何を安定させるかの定義が無い。
- **F13 (Low)** 契約の細部:
  - 構造エラーの例外型が `IllegalArgumentException`（名前衝突、`addChildren`）と `IllegalStateException`（optional 配置、
    option 数、Paper 遅延登録）に分かれ、`register` の Javadoc に `@throws` が無い。
  - `CommandLib.diagnostics()` は Spigot だけ main thread を要求する。
  - Spigot の `functionCompiler` は 1 tick timer として plugin の生存中ずっと走る（全登録を `unregister` しても止まらない）。
  - Paper `addIfCommandsEventMissed` は reflection 失敗時に `Bukkit.reloadData()` へフォールバックする（`:537-539`）。
    「commands を登録したら datapack 全体が reload された」という副作用で、user docs には無い（design doc のみ）。
  - `execute(...)` / `require(...).execute(...)` / `executeAsync(...)` は最後の呼び出しが黙って勝つ（`CommonCommand.java:704-706`）。
    Kotlin DSL の `require { execute {} }` は branch ではなく command の executor を置き換える（subagent A2、`docs/kotlin.md:159-170`
    の説明と食い違う）。
- **F14 (Low)** annotation API（subagent 指摘を source で確認）:
  - `@Range` の境界が型の範囲外だと黙って飽和する（`AnnotationParser.java:169-174` の `(int) min(p)`）。
  - `@Subcommand` method に引数があるとき、branch 単位の description / permission / visibleWhen を書けず、同一 path の 2 method に
    `@Permission` を付けると parse error（`:737-760`）。fluent と同じ tree を書けない。
  - root command の名前エラーは class 名で修飾されない（`:335-338` が `at(...)` の外）。
  - `docs/design/annotation-api.md` の "Not done: inherited methods" は実装済みで stale。
  - parameter 名の案内が Kotlin（`-java-parameters`）に触れない。
- **F15 (Low)** Kotlin DSL / docs: `docs/kotlin.md:214-216` の「sync thread で再開」は Velocity では calling thread
  （`VelocityCommandScheduler.java:21-24`）。「parent job なし」の記述は `CoroutineScope(ctx)` が Job を作るので不正確。

## 4. roadmap の整合性（質問 1）

- **R1** GA の完了条件が無い。Phase 0 / 1 に「GA 前」という語はあるが、Phase 2〜5 の exit criteria のどれが GA 必須かは
  書かれていない。0.18.0 は「0.17.x と binary 非互換」の release とされ、CI / japicmp / Maven Central は「0.18.0 の後」。
  0.18.0 = GA なら安定性の道具が GA の後に来る。
- **R2** 「Fluent / tree API `[x]`、足りないもの: API stability policy」と Pillar 7「fluent / tree API を stable core として扱う」
  が同居している。F12 参照。
- **R3** Pillar 6 Required「unknown subcommand、missing argument を区別した error」は、Brigadier が CommandLib の前に報告する
  現構成では `ErrorFormatter` に届かない（`ErrorFormatter.java:21-23`、`docs/help.md`）。`[~]` ではなく、platform hook が要る
  設計課題として扱うべき。
- **R4** 「Permission-aware execution `[x]` 足りないもの: option permission」は options.md / `CommandOption.permission` が
  実装済みで stale。
- **R5** 「Sender abstraction `[x]`」（`provide` による型変換）と P5「sender / message abstraction … sender 側は未着手」が
  同じ語を別の意味で使っている。F09 と合わせ、sender / actor / executor の用語を統一すべき。
- **R6** 「Dynamic command tree refresh `[x]`」だが Velocity には `refresh` が無い（permissions.md は明記）。summary は platform
  限定を書くべき。
- **R7** `/execute as` の actor は `[?]` のまま。目標「同じ public command model」と矛盾する（F09）。
- **R8** README の「Tested Versions」に Fabric `1.21.4` が載るが注記は「実サーバー未実行」。compatibility.md の `Tested` 定義と
  食い違う。
- **R9** P6 の「`CommonCommandContext` の可変状態の棚卸し `[?]`」は本監査で具体的な害（F01 / F05、variadic parse 中の
  `replaceHandle` による `getHandle()` の一時差し替え）が確認できたので、GA 前の項目に格上げする価値がある。
- **R10** Pillar 2「optional / variadic 違反は登録時に runtime error」は modded では event 内（F08）。

## 5. 不変条件の確認（質問 3）

成立しているもの（根拠付き）:

- 同名 literal / 引数の衝突は node 単位で拒否（`CommandNameValidator`）。optional / variadic / greedy の配置、bare input の二重実行、
  同じ終端で走る 2 branch は `CommandNodeCreator` で拒否。
- option の名前重複、`requires` 先の未登録、`mutuallyExclusive` の未登録は登録時に拒否。`OptionGraph` の node 上限 50,000。
- 実行時の権限再検査（command、option、各 branch）、`visibleWhen` の再検査、cooldown の予約、option の redirect 復元
  （`OptionGraph.recover`、alias / namespace / `execute run` の prefix も処理）。
- 一時テスト `h11_*`: option の引数名と positional 引数名が同じでも `parseNode` の handle 差し替えと復元で正しく動く。

破綻するもの: F01（名前空間）、F02（スナップショット時点）、F03（権限 node の重複）、F04（隠れた実行点）、F06（他 command の option）。

## 6. 複雑さ・状態・責務（質問 6。設計判断を含むため客観的欠陥とは分ける）

- `CommonCommandContext` は parse 結果、option 値、失敗 message の capture buffer、scheduler、captions、provider cache、
  result、variadic 用の handle 差し替えを 1 class に持つ（713 行）。roadmap P6 の棚卸しに同意。特に `captureFailure` と
  `replaceHandle` は「読み取り専用」とされる context を実行中に書き換える。
- executor の置き場が 2 つ（branch と argument instance、F04）、sender 要件の表現が 3 つ（`requirePlayer()`、`require(playerRequirer())`、
  `provide` + `ctx.get`）、annotation では `@Sender`。統合候補。
- static 可変状態: `PlatformAdapterHolder.instance`、`Captions.DEFAULT`、Spigot `INSTANCES` / `functionCompiler` /
  `functionLoadErrorFilter`、Paper `serverDispatcher` / `REGISTRATION_STATES` / `BOOTSTRAP_REGISTRATIONS`、modded `DEFAULTS` /
  `PERMISSIONS` / `REGISTRATIONS`、Velocity `CommandContext.proxy` / `REGISTRATIONS`。shade + relocate 前提なら許容だが、
  Velocity の tester は並列実行で cross-talk する（subagent B5）。
- `OptionGraph.recover` は redirect 後の context を補うために入力文字列を空白で自前走査して再 parse する（`:435-617`）。
  Brigadier の parse を二重化しており、失敗時は `IllegalStateException`。現行テストは通るが、保守コストが高い箇所。
- `CommandOption.registered` は「どこかに登録された」だけの boolean（F06）。
- `sendSuccessFor` の廃止方針（roadmap P6）に同意。
- `ArgumentBranchDelegate` が public なのは `ArgumentBranch` の public constructor のため。`@ApiStatus.Internal` で十分。

## 7. テストが無い振る舞い

- F01（親子同名引数）、F03（重複 node）、F04（中間実行点の help / docs）、F05（index API の意味）、F06（他 command の option）、
  F10（成立しない制約）。
- 登録後の変更が各 platform でどう扱われるか（F02）。Paper の reload 再構築で異なる tree になるケース。
- modded の構造エラーが event 内で出るときの挙動。
- `CommandTester` の snapshot がモデル変更を見逃すケース、同一 instance の tester 2 つ、locale 未設定 player。
- Kotlin: `require {}` と `execute {}` の併用、subcommand 内での親 option property の読み取り、suspension 後の
  `CommandPrerequisiteException`。
- annotation: `@CaptionBundle`、`@Suggest(method=)` の builder 形、`@Range` 範囲外、placeholder と子引数の同名。

## 8. 未確認事項と限界

- 実サーバーでの再現はしていない。F02 の Paper / modded reload 時の再構築は code reading（`buildNodes()` の呼び出し位置）による。
- NMS typed module の個別実装、`integration-test` の各 fixture、`CommandDocs` の出力形式は読んでいない。
- 既存テスト suite の再実行はしていない（一時テストのみ `:common:test --tests GaAuditScratchTest` で実行）。
- subagent の指摘のうち本文に入れたものは該当行を再確認したが、Kotlin coroutine の dispatch 詳細（`Coroutines.kt`）は
  subagent の読解に依存している。

## 9. 推奨する GA 前の順序

1. F01 を登録時検証で塞ぐ（最小: 継承 chain 全体の引数名重複を `IllegalArgumentException`）。annotation parser にも同じ検査。
2. F02 の契約を決める。推奨: 登録時に tree を凍結し、以後の構造変更を例外にする。Paper / modded の再構築は凍結済みモデルからなので
   同一 tree が保証される。
3. F03 / F06 / F10 を登録時検証に追加する（いずれも小さい）。
4. F12: `@ApiStatus.Internal` を付け、公開面を宣言し、0.18.0 を baseline にして japicmp を回す。GA の定義を roadmap に 1 節書く。
5. F05 / F04 / F08 / F09 / F11 は「仕様として宣言する」か「直す」かを決めて docs に書く。
6. F07 の docs と tester の不一致を直す（`velocity-testing` の `suggestions`、snapshot の対象、locale）。
