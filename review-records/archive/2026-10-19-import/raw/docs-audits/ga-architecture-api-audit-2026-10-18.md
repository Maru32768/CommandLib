# CommandLib GA アーキテクチャ・公開 API 監査

監査日: 2026-10-18（Asia/Tokyo）  
対象: `3cb2008de5751d6a16e01fc848cfba1d103333ee` と監査開始時の作業ツリー。`common/.../ArgRef.java`、`common/.../OptionGraph.java` に各1行の既存変更がある。`.claude/` も開始時から未追跡。これらを保持し、監査対象のソース・既存テスト・ビルド定義は変更していない。

## 1. 判定

**現状のまま公開 API の GA 安定性を約束することは推奨しない。** 機能数の不足より、型付き引数参照、権限の所有権、解析と実行の間の権限再検査、登録状態の契約に問題がある。

既存の focused test は **8モジュール、1,113件、失敗0、skip 0**。それとは独立した小さな再現プログラムで下記の不整合を確認した。テスト成功と、機能を合成した場合の契約の成立は区別する。

GA 判断では、少なくとも F01・F05・F06・F07 を修正し、C01 の登録後変更・再利用契約を決める必要がある。残る F 項目も、修正するか、API が明示的に拒否する状態として扱うべきである。将来機能として書かれた roadmap の全バックログを GA の必須条件とはしていない。

| ID | 優先度 | 分類 | 指摘 | 確認方法 |
|---|---|---|---|---|
| F01 | P1 | 客観的欠陥 | 親子の同名引数が親の値を上書きし、`ArgRef<T>` が誤った値を返す | common 実行再現 |
| F02 | P2 | 客観的欠陥 | コマンド名と同名の optional 引数を省略すると default にならず例外 | common 実行再現 |
| F03 | P2 | 客観的欠陥 | `addChildren` が循環を許し、構築時に `StackOverflowError` | common 実行再現 |
| F04 | P2 | 客観的欠陥 | 同じ child を同一親の別 branch に付けると既存 route の継承状態まで変わる | common 状態・permission export 再現 |
| F05 | P1 | 客観的欠陥 | 古い Bukkit 系登録の解除が、新しい登録の permission を削除する | 実 Bukkit permission registry + Paper の helper、両 module のコード追跡 |
| F06 | P1 | 客観的欠陥 | Velocity で解除後の `ALL → NONE` 再登録がアクセスを閉じない | 公開 register/unregister + mock proxy 再現 |
| F07 | P1 | 客観的欠陥 | 解析済み入力の実行時、祖先 command の permission を再検査しない | Brigadier ParseResults の再実行 |
| F08 | P2 | 客観的欠陥 | 権限で隠した literal がサーバー側の補完 API に残る | Brigadier completion と可視 tree の比較 |
| F09 | P2 | 客観的欠陥・仕様不一致 | Paper の初期登録は不正な定義でも成功を返し、後の lifecycle event で失敗する | 既存 lifecycle fixture を使った独立再現 |
| F10 | P2 | 客観的欠陥 | confirmation が parse の同一性を実行の同一性とみなし、再利用された parse を確認できない | 同一 ParseResults を3回実行 |

P1 は誤った対象への操作・権限境界の破綻を伴い、GA 前の修正を強く求めるもの。P2 は限定条件で契約が破綻するもの。P0 に相当する無条件・全環境の障害は、この監査では確認していない。

以下の例の `Command` / `StringArgument` / `IntegerArgument` は platform の公開 API を表す。common 単体再現では既存の `TestCommand` / `TestCommandRunner` と `CommonStringArgument` / `CommonIntegerArgument` の薄い subclass を使った。[再現コード・出力・実行方法](ga-audit-evidence-2026-10-18.md)も保存している。

## 2. 監査範囲と限界

確認した資料は README、roadmap と history、arguments/options/permissions/suggestions/errors/execution/context-values/annotations/java-builder/packaging/compatibility、および module-structure、argument-child-command、optional-variadic、execution-pipeline、command-registration-audit の設計文書。実装は common の定義・検査・tree 構築・解析・実行・補完・help/export、4 platform の登録入口と権限管理、Kotlin coroutine/ThreadLocal 橋渡し、annotation の fluent 変換、NMS 選択の境界を中心に追跡した。

既存テストでは構造、optional/variadic、options/redirect、permission/visibility、middleware/async、context、annotation/builder、登録 lifecycle/rollback を確認した。各引数・メッセージ・tester の既存テストも下記の Gradle 実行に含む。ただし、全公開メソッド・全 generated NMS 実装を一行ずつ監査したという意味ではない。

実サーバー/Docker の integration matrix、全 modded node の再コンパイル、sample 全ビルド、JMH、japicmp、Kotlin suite は今回再実行していない。実サーバーの到達範囲は repository の fixture と記録を確認した範囲であり、新たに実証したものではない。競合製品の現行仕様・将来 Minecraft version の妥当性は本監査の対象外。先行エージェントのレビュー結果を結論の根拠にはしていない。

実行コマンド（PowerShell）:

```powershell
.\gradlew.bat -g C:/Users/maru3/.gradle '-Pcommandlib.typedNms=false' --offline :common:test :paper:test :velocity:test :spigot:test
.\gradlew.bat -g C:/Users/maru3/.gradle '-Pcommandlib.typedNms=false' --offline :paper-testing:test :velocity-testing:test :spigot-testing:test :common-testing:test
```

2つ目の実行には、監査用クラスパスを書き出す外部 init script と `gaAuditClasspath` tasks も付加した。製品のビルド定義への変更はない。初回は sandbox 側の cache 不足、続く試行は PowerShell の `-P` 引数解釈で失敗したが、既存 cache と引用符付き引数で解消し、上記2組は成功した。`typedNms=false` は unit test 用であり、この出力の Spigot jar を配布可能と判断していない。

| Module | Tests | Failures / Errors / Skipped |
|---|---:|---|
| common | 449 | 0 / 0 / 0 |
| spigot | 138 | 0 / 0 / 0 |
| paper | 12 | 0 / 0 / 0 |
| velocity | 6 | 0 / 0 / 0 |
| common-testing | 37 | 0 / 0 / 0 |
| spigot-testing | 227 | 0 / 0 / 0 |
| paper-testing | 204 | 0 / 0 / 0 |
| velocity-testing | 40 | 0 / 0 / 0 |

## 3. 再現済みの指摘

### F01 — 親子の同名引数で型付き参照が別の値を返す [P1]

```java
Command root = new Command("root");
root.argument(new StringArgument("value")).child(parent -> {
    Command set = new Command("set");
    set.argument(new StringArgument("value")).execute((value, ctx) ->
        ctx.sendMessage("parent=" + parent.get() + ",child=" + value));
    return set;
});
// /root A set B
```

期待: `parent=A,child=B`、または構築時に同名 route を拒否。実測: **`parent=B,child=B`**。独立した引数 instance と型付き `ArgRef` を使っていても防げない。親の値が対象 ID、子の値が変更後 ID なら誤った対象に操作し得る。同名で異なる型なら parse/cast 例外にもつながる。

根拠:

- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:490` の重複検査は一つの branch 内だけ。
- `CommonCommandContext.java:31,57,165,683` は入力・解析済み値とも名前を key とし、typed lookup も `argument.name()` に戻る。
- `ChainedCommandContext.java:72` 以降も同名引数を後方優先で取得する。
- `ArgRef.java:55` 以降は context の typed lookup に委譲する。

改善方向: 最小修正は、継承する引数を含む route 全体で名前衝突を登録前に拒否すること。長期的には引数 occurrence/route の identity で格納し、表示名と lookup key を分離する。後者を GA 後に変えると既存の名前 lookup と shadowing の契約を壊すため、今決めるべきである。

回帰テスト: 同名/異名、同型/異型、alias、options redirect、nested child、補完、`ctx.getArgument(arg)` と `ArgRef.get(ctx)` の各経路。

### F02 — literal を引数の存在判定に使い、optional 省略が壊れる [P2]

```java
Command count = new Command("count");
count.argument(new IntegerArgument("count").optional(7))
     .execute((n, ctx) -> ctx.sendMessage(n));
// /count
```

期待: `7`。実測: **`IllegalArgumentException: No such argument 'count' exists on this command`**。通常の callback には到達しない。

`CommonCommandContext.java:57–86` は command literal も `argumentNameToInputArgMap` に入れる。`Arguments.java:44` の `hasInput("count")` が true になり、省略時 default の代わりに存在しない Brigadier argument を読む。constructor と登録検査はこの名前を許す。F01 と関連するが、親子を使わない最小コマンドでも発生する別の故障である。

改善方向: literal を含む route 入力と、引数 occurrence の有無を分離する。公開 `getInputs()` の既存挙動を変えるなら互換性方針も必要。回帰テストには command/alias/祖先 literal と同名の required・optional 引数を含める。

### F03 — 公開 API が循環した command tree を作れる [P2]

```java
Command loop = new Command("loop");
loop.addChildren(loop); // 成功してしまう
// register/build -> StackOverflowError
```

実測は `addChildren` が成功した後、tree 構築で **`StackOverflowError`**。`a.addChildren(b); b.addChildren(a)` も検査の対象外である。

`CommonCommand.java:167–184` は「別の parent が既にあるか」だけを調べ、自身・祖先への接続を拒否しない。permission の親たどりや providers/middleware 継承、tree/export は木を仮定している。構築の不正な入力を明示的に拒否すべきであり、木という抽象化で stack overflow を公開エラーにする理由はない。

改善方向: 接続前に ancestor identity を検査し、失敗時に状態を一切変えない。self cycle、2/3 node cycle、同一 child の同一位置への重複（既存 API が許容）を区別してテストする。

### F04 — 同一 child の別 branch への再利用で既存 route が変化する [P2]

```java
Command root = new Command("root");
Command show = new Command("show");
root.argument(new StringArgument("first"))
    .permission(DefaultPermission.ALL).child(ref -> show);
root.argument(new StringArgument("second"))
    .permission(DefaultPermission.NONE).child(ref -> show);
```

両方の接続が成功する。後の接続が `show.parentArguments` を `second` に上書きし、permission export では **`/root <first> show` も default NONE** になる。`first` branch の ALL を継承するという契約が後の接続で失われる。help の経路復元もこの単一 parentArguments を使う。

根拠は `CommonCommand.java:167–184,474–487,935` 以降。command parent が同じなら許すが、実際の親は「command + branch」で一意でなければならない。F03 の cycle と違い、非循環でも壊れる。

改善方向: 接続位置の所有権まで検査して別位置への再利用を拒否するか、定義から route ごとの独立 node を作る。API は factory を既に持つため、新しい child instance を返す制約を示す選択も合理的。単なる factory の使い方の好みではなく、共有した状態が他 route の意味を変えることが欠陥である。

### F05 — 古い登録の解除が新しい permission を削除する [P1]

```java
CommandLib a = CommandLib.register(plugin, "p", commandA); // permission p.shared
CommandLib b = CommandLib.register(plugin, "p", commandB); // 同じ node を置換
a.unregister(); // b の command は残るが、b の permission が削除され得る
```

`commandA` と `commandB` の `permission("p.shared", ...)` を明示しても、同じ command 名で再登録しても起こる。状態遷移は `A owns P1 → B replaces with P2 → unregister(A) removes name(P1) → P2 missing`。

- Spigot `CommandLib.java:821–866` は登録 permission の名前を記録し、解除時にその名前の**現在の** permission を削除する。
- Paper `CommandLib.java:586` と `CommandPermissions.java:61–65` は古い `Permission` を `removePermission(Permission)` に渡す。Bukkit の除去はその permission の名前による。
- command label 側は Spigot `CommandLib.java:891,988` と Paper `CommandLib.java:565` で所有権を確認するため、残った command と消えた permission の組合せになる。

再現では Paper の実 `CommandPermissions.add/remove` と、Spigot API の実 `SimplePluginManager` permission registry を組み合わせ、`P2` が live であることを確認してから `remove(P1)` を呼び、**registry が null になる**ことを確認した。実 Minecraft サーバーでの再現ではない。Spigot の削除経路は上記コードから同じ結論になる。

影響: ALL の grant が失われる、NONE の登録が消えて platform の未登録 node の扱いに戻るなど、後の登録が宣言した権限制御を維持できない。具体的な最終 grant は sender と permission plugin に依存する。

改善方向: 現在の registry object と登録時 object の identity を比較して解除する。共有 permission を正式に支えるなら、同じ default/config を要求し参照数・所有権を管理する。shade した別 copy による置換も想定する。古い登録解除、逆順解除、foreign permission の置換、rollback、disable をテストする。

### F06 — Velocity の解除済み default が再登録の制限を無効にする [P1]

```java
Command open = new Command("audit");
open.permission(DefaultPermission.ALL);
CommandLib first = CommandLib.register(plugin, proxy, "audit", open);
first.unregister();
Command closed = new Command("audit");
closed.permission(DefaultPermission.NONE);
CommandLib second = CommandLib.register(plugin, proxy, "audit", closed);
```

permission plugin が node を `UNDEFINED` とする player に対して、実測は **`declared default=NONE` なのに `allowed=true`**。

`VelocityPermissions.java:26–27` は JVM 内で最初の default を `putIfAbsent` する。`CommandLib.java:181–185` の unregister は command と診断一覧を削除するだけで DEFAULTS を除去しない。同時に live な登録の競合を first-wins にする既存テストはあるが、解除済み登録まで永久に勝つことは別問題である。OP→ALL を抑止する意図は ALL→NONE の制限失敗を防がない。

改善方向: default の lifetime を live registration の lifetime に揃える。共有 node の競合は構築/登録エラーにするか、所有権・合成規則を定める。単純な `put` への置換だけでは、他の live registration を開放する逆問題が残る。

### F07 — 祖先 permission の実行時再検査が欠ける [P1]

再現の状態遷移:

1. `root` と `root child` の permission を許可して Brigadier で `root child` を parse。
2. `root` だけを失効させる。
3. 同じ `ParseResults` を execute。

実測: **result 1、child executor が実行される**。新たに parse すれば root で拒否される。

`docs/permissions.md` は route の全 step の node を必要とする。`CommandRunner.java:110` は終端 command のみを再検査し、143付近から inherited argument branch は再検査するが、通常の ancestor command は再検査しない。一方 `checkVisible`（93–105）は ancestor をたどる。permission と visibility の保証が非対称である。

解析結果の再利用、解析元と実行元 source が違う場合、function 等の事前コンパイル経路が影響候補。今回実証したのは共通 Brigadier 経路であり、各サーバーの function/source 変換で同じ問題が到達可能かは integration test が必要。通常の新規 parse を毎回行う chat 入力が無条件に突破されるとは主張しない。

改善方向: executable route の ancestor command・argument branch・option の必要権限を一つの集合として保持し、実行時に全て再検査する。cached parse で root/中間/leaf を各々 revoke するテストと、source を切り替えるテストを追加する。

### F08 — literal の補完結果が permission/visibility の可視 tree と一致しない [P2]

`root` だけ許可し、`root secret` を許可しない sender で再現:

```text
visibleChildren("root") = [help]
suggest("root ")        = [help, secret]
```

`CommandNodeCreator.java:107,228` 以降は literal に `requires` を付けるが、補完候補の filter は付けない。Brigadier の server completion は literal の requirement をこの場で評価しない。`ArgumentCommandNodeCreator.java:164–175`は argument suggestion に独自 permission filter があるため、literal と argument で契約が異なる。公開 `common-testing/CommandTester.java:89–91` も dispatcher の completion をそのまま返す。

これは server-side completion API での再現であり、全 client が通常の tab 操作で必ず secret を表示するという意味ではない。送信済み client tree から除外される場合は表示されない。公開 docs の「補完されない」という無条件の保証、testing API と実行許可の整合性は成立していない。

改善方向: 実際の suggestion response を requirement/visibility で filter するか、client tree と server completion の保証範囲を明示する。非権限 literal、alias、option literal、visibleWhen=false、補完 request を直接送る integration test が必要。

### F09 — Paper の不正な定義は register で fail-fast しない [P2]

```java
Command broken = new Command("broken");
broken.argument(new IntegerArgument("optional").optional(1),
                new IntegerArgument("required")).execute((a, b, ctx) -> {});
CommandLib registration = CommandLib.register(plugin, "audit", broken);
```

plugin 初期化中の再現で `register` は正常に handle を返す。後で `COMMANDS` event を発火すると `IllegalStateException` を log に出し、label は null のまま。呼出し側は `register` の try/catch で捕捉できない。

- `CommandSet.java:16–34` は top-level 名だけ検査し、`buildNodes` は別操作。
- `CommandLib.java:329–355` は permission を登録し、未発火 handler に登録を積んで return。
- `CommandLib.java:616–618,664–680` で初めて構築し、失敗はログに留める。
- `Arguments.java:67` の配置検査はこの構築まで遅れる。

roadmap summary、history、`CommonArgument.optional/variadic` の「登録時に拒否」という契約と、Spigot の build-before-side-effect と一致しない。platform の dispatcher 自体が後でしか得られないことと、platform 非依存の定義検査を遅らせることは別である。

改善方向: platform 非依存の validation を `register` 内で行う。runtime platform failure は `Pending/Live/Failed/Unregistered` の観測 API または callback とし、権限・部分登録・reload 再試行方針を定める。modded も command build が event に遅れる経路を持つため、同じ契約決定を適用する（modded の実再現は未実施）。

### F10 — confirmation の実行 identity に parse identity を流用している [P2]

`confirm()` を設定した root の同じ `ParseResults` を timeout 内に3回 execute した結果:

```text
1回目: Run the command again within 10s to confirm. [Confirm]
2回目: messageなし、executor未実行
3回目: messageなし、executor未実行
```

`CommandRunner.java:76` は `context.getNodes()` を parse identity として保存する。`Confirmation.java:90–93` は同一 identity を同一 fork の別 target と判定し、前回の `confirmed=false` を返す。別の dispatch でも compiled parse を再利用すれば identity が同じである。

「同じ入力の次の実行で確認」と「一つの fork の target 同士では確認しない」の両方を parse identity だけで表現できない。前者と後者を区別する dispatch identity が必要。独立再現は Brigadier の parse 再利用まで。実サーバーの command block/function がどの形で context を再利用するかは version ごとの確認が必要である。

改善方向: dispatch ごとの token を fork の target だけで共有するか、対象 platform で確認機能が保証する source/経路を制限して明示する。既存の fork confirmation テストに、同じ parse の別 invocation を追加する。

## 4. GA 前に決める必要のある公開契約

この節は、現在の実装が意図している可能性を含む。F 節と違い、選択肢の一方だけを正解とはしない。

### C01 — 定義の可変性、登録後の変更、再登録・再利用

現在の command は可変 builder であると同時に live runtime の参照先でもある。

- executor、middleware list、providers は `CommandRunner` 構築時に取得される。
- captions/error formatter/visibility、argument の validator/transformer 等は実行時に可変 object を読む。
- help/export は command の現在の children/arguments を読む。
- `CommandDefaults.applyTo` は登録ごとの defaults を command object に保存する。別登録はこれを上書きし、Paper の reload 時には再構築が起こる。

再現では登録後 `execute(newExecutor)` を呼んでも old executor が動き、`addChildren(added)` 後の新 route は parse されない。一方変更が即時反映される property もある。`refresh()` は client tree の再送であって定義の再構築ではない。

GA では、次のどれかを公開契約として選ぶべきである。

1. 登録した定義は freeze し、setter を拒否。動的に変えてよい predicate の外部状態等を明記する。
2. 登録時に完全 snapshot を取り、元定義の変更は将来の登録だけに適用する。
3. update/rebuild を明示した API を用意し、permission/tree/help を原子的に更新する。

同じ command を複数 registration に渡すこと、子に付けた command を top-level でも登録すること、同一 option/argument instance の再利用範囲も併せて決める。現状を「更新対応」と説明するのも「不変 snapshot」と説明するのも不正確である。

### C02 — typed handle の scope と suggestion context

`getArgument(argument)` は identity でなく名前に束縛される（F01）。`getOption(option)` は「この context の command に登録されたか」ではなく、`option.registered`（どこかの command が登録したか）だけを検査する。別 command の option を読んでも factory default が返り、誤参照を検出しない。

また補完 context には option values を parse する段階がない。`root -f ` の argument suggestion から `getOption(flag)` を読む再現では **false** となった。これを実行 context と同じに見せるか、「補完では option は未解決」とするかを明示する必要がある。単に `C extends CommonCommandContext` を共有していることは、値の利用可能時点が同じことを意味しない。

`ArgRef.get()`、context providers、default extractor、validator、suggestion provider、async completion のそれぞれで、利用可能な値・thread・再入時の意味を一枚の契約表にすることを推奨する。

### C03 — permission node 競合と platform 差

明示 node を複数 route が共有することは自然な利用例である。しかし permission config の衝突を common は解決せず、Bukkit 系は置換、Velocity/modded は first-wins、Paper bootstrap default は map の更新に委ねる。default と description が異なる同名 node をどう扱うかは API の意味論であって内部実装詳細ではない。

同一 default の共有と、異なる default の競合を分ける。後者は早期エラーにすると platform 差を小さくできる。Bukkit に既に存在する permission を通常 node は置換し cooldown bypass は保持する、という現仕様も所有権の契約として表にするべきである。

### C04 — failure の論理結果が message の有無で変わる

`CommandRunner.fail` は failure に message が無ければ return 0、あれば throw する。したがって silent preprocess や message を出さない `ErrorFormatter` は `store success=1` にできる。`docs/errors.md` はこの差を説明しているが、`CommonCommandContext.setResult` Javadoc の「失敗で result/success とも0」は例外を明示していない。

これは隠れた実装バグと断定しない。ただし error presentation の変更が論理 success を変える設計は、formatter の責務として予測しにくい。GA 前に「失敗状態」「表示する message」「Brigadier result/success」を独立にするか、現在の制約を安定契約として受け入れるか判断すべきである。async の遅延結果が vanilla の同期 result を変えられない点は既に文書化されており、それ自体を欠陥とはしていない。

### C05 — public / extension / internal の境界と互換性

公開 Java modifier と安定 API の境界が明確でない。例: `PlatformAdapter`、`CommonArgument` の Brigadier 型を含む protected constructor、`CommonCommandContext.getHandle()`、public な `util.nms` wrappers/registry、testing framework の hook。

README の「Brigadier/NMS を意識せず使える」は通常の command 定義には成立するが、custom argument/SPI/native escape hatch には当てはまらない。これらを削除すべきという指摘ではなく、GA の互換性保証をどこまで適用するかの問題である。安定 API、拡張 SPI、内部 API を明示し、binary/source/behavior compatibility を区別する。Kotlin の same-classloader 制約は packaging に明記されており、既知の制約として扱える。

### C06 — 非同期処理の寿命、再解析、副作用

async result と sync thread 復帰は既存文書・テストが比較的明確。ただし plugin disable/unregister、entity retirement、never-completing stage、cooldown reservation 期限、confirmation の lifetime を統一した状態表にする必要がある。`CommandLib.unregister()` が command の新規到達を止めることと、実行中の stage を止めることは別である。

validators/transformers/providers は実行以外に補完でも呼ばれ、variadic/OptionGraph は入力を再解析する。custom parser の純粋性・呼出し回数・副作用禁止範囲を定めないと、同じ公開引数が通常 branch と option/variadic で違う意味になる。反射による `CommandNameValidator.sameSettings` は plain values 以外を比較しないため、任意の custom `ArgumentType` の同等性証明にはならない。共有 prefix に使える custom parser の条件を明示する必要がある。

## 5. roadmap と実装の対応

以下は summary の領域をまとめた対応表。`存在` は API/実装/テストの存在を確認した意味であり、全 platform の GA 保証が完了したという意味ではない。

| roadmap 領域 | 観察・関連根拠 | GA 評価 |
|---|---|---|
| Fluent/tree、typed callbacks、Java builder、8+ arguments | CommonCommand、branch、CommandBuilder、CommandStructureTest、CommandBuilderTest | 存在。F01–04、C01–02 が stable core の前提を崩す |
| Optional/variadic | Arguments、VariadicArgument、OptionalVariadicArgumentTest | 配置検査は存在。F02、F09。上限・展開コストは公開制約として固定が必要 |
| Execution result | CommandRunner、CommandResultTest、errors.md | 存在。C04 の message-dependent success を要決定 |
| Async execution | CommandScheduler、各 scheduler、AsyncExecutionTest | 存在。Folia の未検証経路と C06 を GA の保証から分離 |
| Dynamic refresh | 各 platform の refresh、VisibilityTest | refresh は実装済み。F07–08 と C01 により tree 再送だけでは保証できない |
| Vanilla feedback | sendSuccess/送信 bridge | API は存在。gamerule/silent/broadcast の証拠不足は現 roadmap 自身が認める。history の完了は検証完了とは読めない |
| Bukkit/Spigot/Paper support、module split | 独立 module、typed NMS、paper lifecycle | 分離は妥当。F05・F09。全 version の今回の動作認証はしていない |
| Paper registration/bootstrap | CommandLib/CommandSet/PluginRegistrationState、Lifecycle/BootstrapTest | bootstrap は解除不能と明示。pending/failure の公開契約が不足 |
| Forge/NeoForge/Fabric | Stonecutter 単一 source、CommandLib events/defaults | module は存在。unregister 不在・first-wins・検証時点の差を明示して共通契約の限界にする |
| Folia | FoliaCommandScheduler、mock と smoke fixture | 専用 artifact 不要の分離は合理的。command block、execute-as、共有 state の並行性は未保証 |
| Mohist | spigot 経路、compatibility の例外記述 | failure message が Unknown command になる既知例外を「同一 error」保証から除外すべき |
| Velocity | BrigadierCommand、VelocityPermissions、smoke fixture | 存在。F06。sync の意味、operator 不在、tree 更新時点の platform 差は公開済み |
| Brigadier/NMS hiding、capabilities | PlatformAdapterHolder、NMSClassRegistry、PaperDispatcher | 通常利用の隠蔽は成立。C05、reflection fallback policy は未完 |
| `/execute`/function/reload | 登録 bridge、function recompilation、fixture | 実装は存在。F07・F10 の cached execution を matrix に加えるべき |
| Built-in arguments、validation、transform、additionalParser | CommonArgument、argument catalog、各 argument tests | 基盤は存在。additionalParser は native parse が通った後だけ。C06 の再解析・副作用契約が必要 |
| Sync/async suggestions、tooltips | ArgumentCommandNodeCreator、SuggestionBuilder | 存在。F08、C02。timeout/cache/rich tooltip の未実装を未完として維持 |
| Permission execution/registration/help/suggestions | permissionConfigs、CommandRunner、各 platform registry | F04–08。route が表示・解析・実行・解除を通じ同じ権限を持つ保証は未成立 |
| Typed options | CommandOption、OptionGraph、OptionsAnywhere/OptionRedirect/Permission tests | 機能は存在。repeatable/count の有界性は妥当。C02–03、child route の非 redirect 展開を要検証 |
| Generated usage/help、error taxonomy、exception handling | HelpMessageAction、formatters、snapshot tests | 存在。C01/C04。native syntax error と wrapper error の責務境界は文書化されている |
| Preprocess/prerequisite、middleware | CommandRunner、CommonCommand.middlewares、CommandMiddlewareTest | 基盤は共通化済み。named condition registry の fluent 版は任意の機能判断で、欠陥ではない |
| Sender abstraction、context values、DI | CommandActor、ContextKey、CommandDefaults、ContextValueTest | 存在。summary の「platform 登録 provider 不足」は registration defaults の実装と整理が必要 |
| Annotation | AnnotationParser が fluent model へ変換、AnnotationParserTest、style snapshots | 変換層に留める方向は妥当。共通層の F 指摘は annotation からも回避できるとは限らない |
| Kotlin DSL | DSL、Coroutine dispatcher、CurrentCommandContexts | 共通定義への変換は妥当。modded 未対応は明記済み。今回 Kotlin suite 未実行 |
| Localization/captions | Captions、Caption、locale docs/tests | 存在。suggestion context の captions/providers 初期化範囲を C02 の表に含める |
| Public testing / NMS mocks | 5 testing 系統、JUnit extension、snapshot | 実行した4系統と common は成功。mock の成功と native semantics の保証は分ける |
| Adventure/RichText/MiniMessage | RichText と platform converters | RichText は存在。rich help/tooltip の統一、MiniMessage は未完。GA必須か選択可能 |
| Multi-version integration | targets/shared fixtures、Gradle platform property | 仕組みは存在。F の組合せケースは現在の成功記録から保証できない |
| Docs/migration/tooling | README、各 guide、MkDocs、japicmp設定 | 文書量は十分増えているが、完了状態と契約の正本を統一する必要がある |
| Diagnostics/export | CommandDocs、CommandTree、diagnostics | 実装あり。可変定義の現在値と live tree/defaults が異なると診断も一致しない（C01、F06） |

### roadmap 自体の矛盾・未定義

1. **「実装済み」の基準が二つある。** 冒頭 `[x]` は API/docs/tests の「いずれか」で確認できる状態。一方「実装順と完了判定」は public API・使用例・仕様・focused tests を全て要求し、platform 対応は実サーバー証拠が必要としている。history の機能追加完了と、summary の保証未完を同じ checkbox で読めない。実装済み・契約確定・検証済みを別列にするべき。
2. **GA と 0.18.0 と長期競合目標の exit が混在する。** CI/japicmp/Maven Central の 0.18.0 後への延期は明示決定であり、その延期自体は欠陥ではない。しかし1.0/GAに適用する別の gate がない。競合を「上回る」ことと、自身の契約を維持できることを別条件にする。
3. **古い未完記述が残る。** Dynamic refresh の不足欄は state predicate を挙げるが `visibleWhen` は実装済み。sender/DI の登録単位 provider は `CommandDefaults` が実装済み（platform 全体 registry と同一ではない）。Pillar 11 は Fabric 追加を今後の作業として書く箇所があるが現行 matrix には存在する。
4. **integration metadata の状態が食い違う。** history は `IntegrationTarget` の Platform enum 完了、現 roadmap は追加を未完としている。現コードの integration runner は `commandlib.platform` property と文字列判定を使う。歴史的記述か現行要件かを明示する。
5. **Paper の最小範囲が名称上混在する。** summary/Pillar は「Paper 1.20.6+ registration」、README と公開 `paper/CommandLib` は1.21+。旧 Paper に Spigot artifact を使う現方針に合わせ、公式API導入時点と artifact の対応範囲を分ける。
6. **platform 全体の Tested と挙動別の保証が同じではない。** README は注記を持つが、compatibility が best-effort とする Folia/Mohist/Velocity、unit-only の Fabric1.21.4 を同じ Tested Versions 欄に含める。注記を残しつつ、artifact/version/経路/挙動/証拠commitを対応させるべき。

## 6. 責務分離と複雑性の評価

### 維持する価値がある構造

- common に command/branch/context/options を集め、platform 登録と sender/message/native argument を外に置く方向は明確。Paper を Spigot の adapter に押し込まない判断も合理的である。
- builder/annotation/Kotlin が共通モデルへ変換されるため、4つの実行エンジンを持たない点はよい。独立した実行意味論を増やさず、共通層の修正を全 style に効かせられる。
- 明示的な有界 variadic/repeatable、async の即時 result 制約、platform 別 thread、shade/relocate の方針は実装制約を隠さず示している。
- command label の所有権、rollback、copy-on-write root、mock と real-server fixture の分離には具体的な目的がある。複雑だからという理由だけでは削除を提案しない。

### 問題と結び付く複雑性

定義の `CommonCommand/Arguments`、描画 snapshot の `CommandTree`、実行用 `CommandNodeCreator/OptionGraph`、入力復元の `ChainedCommandContext`、help の path 復元がそれぞれ route を解釈する。F01/F02/F04/C01 はこの複数の解釈が一致しない例である。

優先すべきは抽象化の追加そのものではなく、**登録前検査済みの route・argument identity・必要権限・option定義を共通の意味情報として固定すること**。まず common の validation pass と lifetime 契約を揃え、必要なら小さな immutable registration plan を導入する。全面的な書換えや既存 fluent API の廃止は、この監査結果からは要求しない。

`OptionGraph` の bounded state graph は factorial な順序展開を抑える一方、実行時 reparse と native tree の二つの parser を維持する。新構文を加える前に「native parse → recover → option occurrences → typed args → suggestions」で同じ入力の意味が一致するテストが必要。50,000 の guard は token 数を中心とするもので、client へ展開する全 node/packet size/variadic 深さの一般的な上限証明として扱わない。

module 重複の解消、thread-local の明示 context への全面置換、annotation parser の分割、method 名の好みは**任意の改善**であり、それだけをGA阻害理由にはしない。`PlatformAdapterHolder` の単一 classloader・固定実装名も現 packaging の範囲では成立し得るが、任意 platform を差し込める汎用 SPI として保証すべきではない。

## 7. GA 前の修正・検証順

1. F01/F02 の引数 scope、F03/F04 の tree 所有権を修正し、検査を共通化する。不正状態を利用者へ渡す前に拒否する。
2. F05/F06 の permission lifetime を修正し、F07 の route 全体の実行時認可を追加する。live registration の置換・解除・失効を一つのテストシナリオにする。
3. C01–C06 の契約を短い public API policy として確定する。特に definition freeze、handle scope、pending registration、failure result は後で変更しにくい。
4. F08/F09/F10 の regression test を追加する。literal/argument/option の completion、事前 validation と lifecycle failure、fork と cached dispatch を別々に検査する。
5. 少数の代表コマンドで組合せテストを行う。optional/variadic × options × argument-child × permission/visibility × alias/redirect、同一 instance の再利用、registration A/B の置換・解除を含む。既存 snapshot のみでなく、同じ入力の parse/execute/help/suggestion が一致することを比較する。
6. 実サーバーで cached function/source の権限失効、複数 shaded copy の置換/解除、Folia の command block/execute-as、Velocity の再登録を確認する。既知の best-effort 範囲は広い保証で覆わない。
7. GA 対象 API の互換性 baseline と release gate を設定する。CI の導入時期と、release 前に必要な検証を手動で行うかは運用判断だが、証拠を残すこと自体は必要。

本レポートは監査結果と提案であり、修正を実施した報告ではない。
