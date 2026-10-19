# レビュー対応メモ（2c8ffd7a..HEAD）

各 finding の詳細は同じディレクトリの `round*-*.md` にある。ここでは判断だけを記録する。
状態: 対応 = 修正担当に割り当て（結果は後で commit を追記）/ 見送り = ROI が低い / Pending = 仕様判断が必要

## Round 1

### A1 common 解析・option・annotation

| id | 判断 | メモ |
|---|---|---|
| A1-1/A1-11 | 対応 | argument node を共有する branch で executor が上書きされる（high） |
| A1-2 | 対応 | word と greedy の StringArgumentType が同一扱い |
| A1-3 | 対応 | option 値の検証エラーの caret 位置 |
| A1-4 | 対応 | 後から追加した alias の重複チェック漏れ |
| A1-5 | 対応 | variadic() が optional/execute を黙って捨てる |
| A1-6 | 対応 | ArgumentParseException#getMessage が null |
| A1-7 | 対応 | CommandOption の public constructor |
| A1-8 | 対応 | Arguments の parent 付き overload 削除 |
| A1-9 | Pending | `requires(option, value)` が未指定時の default 値を受け入れる。`requires(option)` は指定を要求する。どちらに揃えるか |
| A1-12 | 対応 | 親クラスの @Execute 等が無視される → 継承をたどる |
| A1-13 | 対応 | custom annotation が @Flag 等で無視される → 例外 |
| A1-14 | 対応 | @Range の小数境界が int/long で切り捨て |
| A1-15 | 対応 | `<a b>` の placeholder 誤分割 |
| A1-16 | 対応 | エラーメッセージの argN 表記 |
| A1-17 | 対応（一部） | required=true と @Default/@Optional の併用を拒否。@Optional+@Default は冗長なだけなので見送り |
| A1-18 | 見送り | AnnotationParser 内の重複。ROI 低 |

### A2 common 実行・message・help

| id | 判断 | メモ |
|---|---|---|
| A2-1 | 対応 | ErrorFormatter が何も送らないと AssertionError が握りつぶされる |
| A2-2 | 対応 | addPreprocess(Consumer) を削除（Predicate と曖昧） |
| A2-3 | Pending | function を再実行すると confirm が成立しない。dispatch ごとの token で区別するか、function では confirm 不可と文書化するか |
| A2-4 | 後回し | permission 一覧の 2 重 walk。全 platform に触るので他の修正の取り込み後に対応 |
| A2-5 | 対応 | DefaultHelpFormatter の旧 padding 互換分岐 |
| A2-6 | 対応 | 未使用の UnknownCommandActor 削除 |
| A2-7 | Pending | `getInput(int)`/`getInputs()` が literal を含み、`getArgument(int)` と index がずれる（Paper では root literal の扱いも違う可能性）。API を削除するか、argument のみに限定するか |
| A2-8 | Pending | argument branch は親の uncaught exception handler を継承し、子 command は継承しない。揃えるか |
| A2-9 | 対応 | 引数エラーの usage に権限のない `[options]` が出る |
| A2-10 | 対応 | visibleWhen が例外を投げると unexpected error |

### B spigot / paper / nms

| id | 判断 | メモ |
|---|---|---|
| B-1 | 対応 | enable() 失敗時の rollback が no-op |
| B-2 | 対応 | Paper ChatColorArgument で `reset` が NPE（Spigot は受理） |
| B-3 | 対応 | Folia entity scheduler に retired callback がない |
| B-4 | 対応 | Folia の register/unregister が任意の region thread を許す |
| B-5 | Pending | `sendSuccess`（broadcastToOps）を async から呼ぶと player list を別 thread で走査する。sendSuccess 内で sync に移すか、sync 専用と文書化するか |
| B-6 | 対応 | ScoreboardDisplaySlotArgument の static init |
| B-7 | Pending | 未使用の NMSEntity を全 27 NMS module と codegen から削除。全 NMS module に触るため確認待ち |
| B-8 | 見送り | Paper の reflection lookup の cache 化・共通化。ROI 低 |
| B-9 | 対応 | `Collection<? extends Command>` |
| B-10 | 対応 | diagnostics() に main thread チェック |
| sendComponent | 対応 | common の未使用 `sendComponent(C)` と各 platform の override を削除（A2 担当） |

### C modded / velocity / kotlin

| id | 判断 | メモ |
|---|---|---|
| C-1 | 対応 | actor の hasPermission が非 player で常に false |
| C-2 | 対応 | 不明な source が CONSOLE 扱いで全権限 |
| C-3 | 対応 | docs/agents/modded.md の command block 記述（docs 担当でまとめて） |
| C-4 | 対応 | modded の allowLogging overload 削除 |
| C-5 | 対応 | LocationArgument の rotation |
| C-6 | 対応（一部） | Velocity の default を putIfAbsent に。unregister で default を消すかは Pending |

### E modded / velocity / integration のテスト

| id | 判断 | メモ |
|---|---|---|
| E-1 | 対応 | velocity-testing が proxy の default 権限を再現しない（high） |
| E-2〜E-5 | 対応 | modded-testing FakeSender / CommandTester の不具合 |
| E-6〜E-10 | 対応 | integration test の空振りチェック、Velocity の不足ケース |
| E-11, E-12, E-14 | 対応 | modded の未テスト箇所 |
| E-13 | Pending | command block に OP default node は level 4 が必要（以前は許可、vanilla は level 2）。どの level にするか |
| E-15, E-16 | 対応 | Spigot のみのシナリオを Paper に移植、MessageArgument の selector |
| E-17〜E-20 | 対応 | テストの assertion 強化、tester の不具合 |
| E-21 | 対応（一部） | CloseableResource wrapper 削除。FakeSender の permissions/denyPermissions が置換か結合かの統一は Pending |
| E-22, E-23 | 対応 | 固定 tick 待ち、到達しない FAILED 記録 |
| E-24 | 対応 | jmhCompare の結果欠落を検出 |
| E-25 | 対応 | integration-test/README.md と modded.md の古い記述（docs 担当でまとめて） |
| E-26 | C-6 と同じ | |

### D common / spigot / paper のテスト

| id | 判断 | メモ |
|---|---|---|
| D-1 | 対応 | A2-10 と同じ（A2 担当） |
| D-2 | 対応 | 子の visibleWhen が例外 → 親 help 全体が失敗（A2 担当） |
| D-3 | 対応 | confirm 済みの fork 実行が最初の target だけ実行（A2 担当） |
| D-4 | 対応 | A2-3 と同じ。再利用される parse で confirm 不可が実証されたのでバグ扱いに変更（A2 担当） |
| D-5, D-26 | 対応（第2弾） | cooldown 予約の期限切れ・解放のテスト |
| D-6 | 対応（第2弾） | AnnotationParser のエラーパスのテスト。A1 修正の取り込み後 |
| D-7, D-9〜D-13, D-15〜D-17, D-19〜D-22, D-31, D-33 | 対応 | testing module の不具合（D-utils 担当）。D-15 の「grant と deny を別々に保持し、各 setter は自分の集合だけ置換」は E-21 の置換/結合問題にも適用する方針 |
| D-8, D-18, D-23〜D-25, D-28 | 対応（第2弾） | 本体ロジックのテスト不足 |
| D-14 | 対応（第2弾） | Paper の unregister に node identity チェックがない。B 修正の取り込み後 |
| D-27 | Pending | 予約中に弾かれた実行に「残り時間 = 全期間」と伝える現挙動。どう伝えるか |
| D-29 | 対応（一部、第2弾） | MessageArgument.resolveSelectors の CommandSyntaxException パスのみ。Spigot テスト群の Paper への移植は見送り（D-32 の重複を増やすため） |
| D-30 | 見送り | Paper FakeSender の翻訳 key を英語化。ROI 低 |
| D-32 | 見送り | spigot/paper-testing のテスト重複（約 850 行）の共通化。大規模で ROI 低 |
| D-34 | 見送り | 並列 JUnit 未使用のため潜在問題のみ |

### F docs / examples / samples / skill

| id | 判断 | メモ |
|---|---|---|
| F-1〜F-24, F-26〜F-34, F-36, F-37 | 対応 | docs 担当。誤記・古い記述・skill の platform 不足など。F-36 は flag が効くと確認できた場合のみ |
| C-3, E-25 | 対応 | docs 担当 |
| F-25 | 対応（第2弾） | modded ObjectiveArgument のエラーを他 platform と揃える（コード修正） |
| F-35 | Pending | samples / README の `latest.release` は 0.18.0 の tag 前は旧 0.17.2 API を解決する。意図通りか |

## Round 1 取り込み結果

- velocity（7be577e9, c3974d47, cfd1526d）: C-6（putIfAbsent）、E-1、E-17/E-18/E-19 の velocity 分を修正。`FakeSender.permissions(..)` は未列挙 node を UNDEFINED にし command の default が効くようになった（破壊的変更）。E-19 の `@BeforeAll` での tester 共有も実バグと確認・修正。spigot/paper-testing の同種問題は D-utils 担当に追加。
- docs（36 commit、66d12648 系 → rebase 後 808d147e まで）: F-1〜F-24, F-26〜F-34, F-36, F-37, C-3, E-25 をすべて修正。`mkdocs build --strict` 通過。例のバージョンは CHANGELOG に合わせ 0.18.0（tag 前なので javadoc リンクはリリースまで 404）。F-11 の Kotlin snippet は未 compile。
- spigot/paper（7 commit、2884c3bd まで）: B-1, B-3, B-6, B-9, B-10 を修正。
  - B-2: Paper の `reset` は NPE ではなく新 caption `NOT_A_COLOR` の構文エラーにした。**Pending**: Paper でも Spigot と同様に `reset` を受理するか（`NamedTextColor` では表現できないため戻り値型の変更が必要。`Optional<NamedTextColor>` や null=色なし など）。
  - B-3: 退出済み entity の follow-up を global region で実行。design doc の「捨てるのが意図」という記述を書き換えた（判断の反転なので要確認ポイント）。
  - B-4: `isGlobalTickThread` は onEnable/onDisable で false になるため、代わりに「region を tick 中の thread」を拒否する方式にした。Folia 実機では未確認。
- common 解析（9 commit、fe7e7b5b 系 → rebase 後 1b8af338 まで）: A1-1〜A1-8, A1-11〜A1-17 をすべて修正。A1-5 は carry over ではなく例外（optional の default T は List<T> に移せないため）。A1-7 は constructor を package-private にしたが、Options.bool 等を Options.of 経由にするのは default/required の挙動が変わるため見送り。
- 作業ツリーの未コミット変更（ArgRef.java / OptionGraph.java の Javadoc 文言）は取り込み時に stash → pop で保持。念のため `user-uncommitted-backup.patch` に退避済み。
- modded（7 commit、40c71933 まで）: C-1, C-2, C-4, C-5, E-2〜E-5, E-11, E-12, E-14, E-17〜E-21 の modded 分を修正。9 node の modded-testing すべて通過。
  - E-11: Fabric の SERVER_STARTING で player list が null → NPE を修正。登録テストは Forge 1.16.5/1.18.2 では FML 外で EVENT_BUS.register が失敗するため Fabric と Forge 1.19+/NeoForge のみ。
  - E-12: fabric-1.21.4 の RawWordArgumentType と shaded/nested packaging のテストはコスト過大で見送り。
  - E-20: 「close() まで scope を開いたまま」ではなく、scheduler に渡した task が実行 thread で scope を開き直す方式（未 close の tester が後続を壊すため）。
  - C-2, E-11 は実サーバーで未確認。
- testing ユーティリティ（15 commit、a7ca25d8 まで）: D-7, D-9〜D-13, D-15〜D-17, D-19〜D-22, D-31, D-33（一部）と spigot/paper-testing の CommandLibExtension（名前衝突・@BeforeAll 共有）を修正。
  - D-7 は fail fast ではなく、scheduler task の thread で NMS static mock を開く方式で動くようにした。
  - D-13 は bridge を出荷すると Paper 内部実装・MockBukkit と衝突するため、docs/testing.md に設定方法を記載するだけにした。
  - D-15: `permissions()` は grant 集合を置換、`denyPermissions()` は累積（common-testing と builder に合わせた）。
  - rebase 時に B-2（Paper ChatColor を vanilla `color` に変更）と D-22（色名の正規化）が衝突 → paper-testing の `color` double に正規化を入れ、不要になった namedColor double を削除して解消。testing 3 module のテスト通過。
  - 新規発見: Brigadier の `getCompletionSuggestions` は suggestion future が 1 つ失敗すると完了しない → suggestion provider が例外を出すと client に何も返らない（実サーバーにも影響）。第2弾で修正。
- common 実行（8 commit、rebase 後 27eacadf..dfe47766）: A2-1, A2-2, A2-5, A2-6, A2-9, A2-10/D-1, D-2, D-3, sendComponent 削除を修正。rebase で HelpMessageAction が A1-8（parent 引数削除）と衝突 → 両方を合わせて解消。
  - D-4 (= A2-3) は **Pending** に戻した: dispatch ごとの token がコードに届かない（1.20.3 未満は build() が同じ nodes/arguments を返し、1.20.3 以降は compiled function が ContextChain を再利用）。案: (a) platform から server tick を token として渡す（PlatformAdapter に method 追加、同一 tick 内の 2 回実行は sibling 扱いのまま）、(b) function から再実行では confirm できないと文書化（[Confirm] ボタンは使える）。
  - 追加修正: VisibilityTest の空振り assertion（`<player>` は色付き出力に出ない）を修正。
- integration / benchmark（13 commit、master 取り込み後 HEAD まで）: E-6〜E-10, E-15, E-16, E-22〜E-24 を修正。docs/compatibility.md の Velocity 行の衝突は integration 側の記述を採用。
  - 強化したチェックで実バグ 2 件を発見・修正: (1) Spigot 1.20.3 で selector を含む MessageArgument / entity hover の ComponentArgument が JsonSyntaxException（bungeecord-chat 1.20-R0.1 が int 配列の UUID を読めない）→ BungeeChatJson で変換、codegen に 1.20.3 フラグ。(2) Paper の unregister が label 名で削除し、後の登録の command を消す（D-14 と同じ）→ 所有 label のみ削除。
  - 実サーバー: spigot-1.20.3 96/96、spigot-1.21.11 96/96、paper-1.21.11 95/95、velocity-3.4.0 PASS。folia / mohist / 古い paper は未実行。

## Round 2

### C modded / velocity / kotlin（round2-C）

| id | 判断 | メモ |
|---|---|---|
| R2C-1 | 対応 | Round 1 の C-2 修正によるリグレッション（high）: data pack function の compile 用 source（CommandSource.NULL、server なし）が UNKNOWN 扱いになり、OP/NONE の command を呼ぶ function が読み込めない |
| R2C-2 | 対応 | Forge/NeoForge 1.18+ で未宣言 node は level 4 固定で permission handler に聞かない → Cooldown.bypassPermission を LuckPerms で付与できない |
| R2C-3 | Pending | 同じ node の default が複数登録で異なる場合、Velocity/modded は最初、Spigot/Paper は最後を採用。Velocity は unregister 後も古い default が残る。どの方針に揃えるか（C-6 の Pending 部分と同じ） |
| R2C-4 | Pending | console の扱いが platform で違う: modded/Velocity の console は NONE node を通過、Spigot/Paper は不可（docs は「operator も含め不可」）。modded の requireConsole は RCON を受理、Spigot/Paper は拒否。揃えるか文書化か |
| R2C-5 | 対応 | CHANGELOG に allowLogging overload 削除と actor 変更の記載がない |

### E docs（round2-E）

| id | 判断 | メモ |
|---|---|---|
| E2-1 | 対応 | CHANGELOG の Folia register/unregister 記述が B-4 修正と矛盾 |
| E2-2 | 対応 | R2C-5 と同じ（modded 担当） |
| E2-3 | 対応 | testing 各 artifact の FakeSender permissions/denyPermissions の意味が不統一。E-21 の Pending 部分を「grant と deny を別保持、deny 優先、各 setter は自分の集合のみ置換」（spigot/paper/common の方式）に統一する方針に決定（テスト用ユーティリティの一貫性であり製品仕様ではないため） |
| E2-4〜E2-9 | 対応 | docs の小さな誤り（velocity-testing の default、PlayerArgument 検証済み、modded の fake source の level、rich-text 設計 doc、@BeforeAll 拒否の artifact 一覧、skill の MockedStatic 記述） |
| 追加 | 対応 | common-testing の CommandLibExtension が @BeforeAll 注入を許す（他 4 artifact は修正済み） |

### B spigot / paper / nms（round2-B）

| id | 判断 | メモ |
|---|---|---|
| R2B-1 | 対応 | Spigot の unregister が namespaced `<plugin>:<label>` を所有確認なしで削除（Paper は修正済み）。fixture に namespaced label のケース追加 |
| R2B-2 | 対応 | E2-1 と同じ |
| R2B-3 | 対応 | onEnable で登録した command を実行時に unregister するシナリオが Paper の実サーバーテストにない |

### D テスト全般（round2-D）

| id | 判断 | メモ |
|---|---|---|
| D2-1 | 対応 | modded/velocity-testing の FakeSender の setter が互いに置換（E2-3 と同じ方針で統一） |
| D2-2 | 対応 | common-testing の @BeforeAll 拒否（round2-E の追加分と同じ） |
| D2-3 | 対応 | spigot/paper-testing の player/entity 名が大文字小文字を区別 |
| D2-4, D2-10 | 対応 | docs/testing.md の記述 |
| D2-5 | 対応 | velocity/modded-testing の未使用 permissionPrefix() 削除（D-33 の残り） |
| D2-6 | 対応 | velocity CommandLibTest が static DEFAULTS とテスト順に依存（mutation で確認） |
| D2-7 | 対応 | R2B-1 と同じ |
| D2-8, D2-9 | 対応 | modded fixture の固定 2 秒待ち、Velocity の log チェックが Error を見逃す |
| D2-11 | 対応 | spigot/paper-testing の @a/@p が実行者を含まない（vanilla と modded は含む） |
| D2-12〜D2-16 | 対応 | 空振りテスト・テスト不足（D-2 の branch 側、positive control、FabricPermissionTest、modded の refusal test、nearest player） |
| 追加 | 対応（後続） | R2C-2 の続き: A2-4 取り込み後に Cooldown bypass node を CommandTree の walk に入れ、Forge 1.18+ でも登録されるようにする |

### A common（round2-A）

| id | 判断 | メモ |
|---|---|---|
| A-1 | Pending | D-3 修正のリグレッション: confirm 後の entry が残り、再利用される parse（function の再実行）が timeout まで確認なしで実行される。dispatch token がないため D-4 と同じ問題。D-4 の方針（tick を token にする / function では confirm 不可と文書化）と合わせて決める必要がある |
| A-2 | 対応 | A2-9 修正のリグレッション: 継承した親 branch の usage の `[options]` を子の option で判定 |
| A-3 | 対応 | A1-1 修正のリグレッション: 引数必須 branch の executor を bare `/x` が借りて実行しエラー（以前は help） |
| A-4 | 対応 | A1-12 修正のリグレッション: generic な親メソッドを override すると bridge method が二重に読まれる |
| A-5 | 対応 | A1-2 の残り: equals のない設定付き型（EntityArgument の player/entities 等）が同一 node に merge |
| A-6, A-7 | 対応 | A1-6, A1-16 の残り |
| A-8 | 対応 | 登録後の `repeatable(n)` が未登録 option を返し、getOption が黙って `[]` |
| A-9 | 対応 | interface の default method の @Execute 等が無視される |
| A-10 | 対応 | 共有 argument node で最初の branch の suggestion だけが使われる |
| A-11 | 対応 | @Default / 暗黙の 0 が @Range 外でも受理 |
| A-12 | 対応 | `List<? extends T>` が拒否される（Kotlin の `List<Player>`） |
| A-13 | 対応 | A2-1 修正の副作用: silent な ErrorFormatter + AssertionError で実サーバーに空の赤行 |
| A-14, A-15 | 対応 | CHANGELOG の sendComponent 移行説明、CommandError の javadoc |
| A-16 | 対応 | annotation 由来の builder エラーに class/method 名がない |
| A-17 | Pending | option は位置引数より先に parse されるため、option の validator が getArgument を読むと unexpected error。parse 順を変えるか文書化か |
| A-18 | Pending | `/config -f <key> set` で親の `-f` が受理された後に捨てられる（docs/options.md と矛盾）。どちらの挙動にするか |
| A-19 | 対応 | 親クラスの class-level annotation が無視される（A1-12 と同じ方針で継承をたどる） |
| A-20 | Pending | Options の型別 factory が Options.of と重複し default の意味も違う。@Range option は int のみ。残す/拡張/削除のどれか |
| A-21 | Pending | `addPreprocess(Predicate)` が addMiddleware / addPrerequisite と重複。buffered message の挙動が必要なければ削除 |
| 範囲外 | 対応 | VelocityIntegrationTest の log 走査（D2-9）、compatibility.md の Velocity 行の run stamp、ArgumentIntegrationCoverageTest の存在しない path |

## Round 2 取り込み結果

- 第2弾テスト（7 commit、72f0c2b0 まで）: D-5, D-6（24 ケース）, D-8, D-18, D-23（一部）, D-24, D-25, D-26, D-28, D-29（一部）のテストを追加。各テストはコードを壊すと失敗することを確認済み。D-23 の ownsLabel / vanilla root の同一性チェック / Folia 拒否は NMS dispatcher の mock や Folia 判定の切替が必要でコスト過大のため見送り。
- spigot unregister + docs（3 commit）: R2B-1/D2-7（Spigot の namespaced label も所有確認）、R2B-3（onEnable 登録 → 実行時 unregister のシナリオ）、E2-1, E2-5, E2-7 を修正。spigot-1.21.11 98/98、paper-1.21.11 97/97、paper-1.21.1（spigot artifact）98/98 通過。

## 中断時点の状況（利用上限）
- 実行中だった修正担当: 第2弾 platform 横断（A2-4・suggestion hang・F-25・modded 全 node 検証）、R2C-1/R2C-2/R2C-5（modded）、R2 testing utils 統一（D2-*/E2-*）、R2 common リグレッション修正（A-*）。完了したら rebase → master 取り込み。
- 残り: 上記の取り込み、Cooldown bypass node の登録（A2-4 後）、Round 3 レビュー、ponytail-review を 2〜3 回、`mise run jev-review 2c8ffd7aa17181325ba6f4759586a2bfb94e86ed`。
- 第2弾 platform 横断（3 commit、c5fc6581 まで）: A2-4（全 platform の permission 登録を CommandTree の 1 回の walk に統一、`PermissionConfig` を package-private 化）、suggestion の非同期 stage 失敗で future が完了しない問題、F-25（modded ObjectiveArgument のエラーを他 platform と統一）を修正。modded 全 9 node の compile と modded-testing 全 9 node のテスト通過。
- modded リグレッション（3 commit、b81e4fd6 まで）: R2C-1（function compile 用 source = server なしの CommandSource.NULL を全通過に。6 バージョンの jar で確認）、R2C-5（CHANGELOG）、R2C-2 は docs のみ（Forge/NeoForge 1.18+ では未登録 node を handler が付与できない旨）。modded 全 node 通過。実サーバーの function は未確認。
- testing ユーティリティ統一（14 commit）: D2-1〜D2-16, E2-3, E2-4, E2-6, E2-8, E2-9 と範囲外 2 件を修正。全 testing artifact の FakeSender を「grant/deny 別保持・deny 優先」に統一（E-21 の Pending を解消）。追加修正: common/spigot/paper-testing の重複 node で例外、modded fake player の entity type が null、mock overworld の feature flags が null（1.20.1 で @a が NPE）。
- cooldown bypass node（d23ee1db）: CommandTree の walk に default OP で追加し全 platform で登録・export（kind `cooldown-bypass`）。Forge 1.18+ でも permission plugin で付与可能に。
- common リグレッション（16 commit）: A-2〜A-16, A-19 を修正。A-5 は reflection で設定 field を比較（ResourceArgument の registry key は未比較、shortcut コメントあり）。A-10 は reject ではなく merge（同一の @Suggest も別 instance になるため）。A-13 は tester だけが AssertionError を投げ直す thread-local 方式で、実サーバーでは silent のまま。リスク: A-8 で未宣言 option の getOption が例外に（共有 middleware などで影響の可能性）。

## Round 3

| id | 判断 | メモ |
|---|---|---|
| R3B-1 | 対応 | cooldown bypass 登録（d23ee1db）のリグレッション: Spigot/Paper で plugin.yml 宣言済みの同名 permission を上書きし（default・children・description が消える）、unregister で名前ごと削除する。既存 permission がある場合は登録しない・自分が追加したものだけ削除する |
| D3-1 | 対応 | CHANGELOG の modded permission 項目が 0.17.2 ではなく未リリースの round 1 変更との差分を書いている（0.17.2 は全 source を level で判定、getActor は 0.17.2 に存在しない） |
| D3-2 | 対応 | 子/alias の名前重複の即時拒否が Fixed にあるが、0.17.2 は拒否していなかったので Breaking changes に移す |
| D3-3 | 対応 | CHANGELOG の Folia 記述（onEnable/onDisable thread も可、region thread のみ拒否） |
| D3-4 | 対応 | spigot-testing MockEntitySelector のエラー文の rename 残骸 |
| C3-1 | 対応 | D3-4 と同じ |
| C3-2 | 対応 | spigot/paper/velocity/modded tester の CommandFailure.replayed を外しても失敗するテストがない（mutation で確認） |
| C3-3 | 対応 | 登録済み実行者が @a/@e に重複しない guard のテストがない |
| C3-4 | 対応 | modded-testing の player key が大文字小文字を区別（他 tester は区別しない） |
| C3-5 | 対応 | withLevel で渡した overworld に enabledFeatures が null のままだと 1.20.1 で NPE 再発 |
| C3-6 | 対応 | 実 vanilla EntityArgument での shared-node 設定比較のテストを modded-testing に |
| C3-7 | 対応 | Velocity の log チェックの pattern を velocity-3.4.0 で実行して確認 |
| R3A-1 | 対応 | A-5 の reflection 比較が 1.21.5+ の ItemPredicateArgument 等の per-instance record を設定とみなし、正当な共有を拒否（リグレッション） |
| R3A-2 | 対応 | A-10 の suggestion merge が argument instance で dedupe し最初の branch の requirement だけ見る |
| R3A-3 | 対応 | 同期的に throw する suggestion provider が sibling の候補ごと消す。round 2 で残した「同期例外は伝播」を改め、捕捉して報告し続行する |
| R3A-4 | 対応 | merge した候補が tooltip 付きだと重複 |
| R3A-5 | 対応 | 共変戻り値の bridge を override と誤認 |
| R3A-6 | 対応 | getOption の未宣言 option 例外（A-8）は親/CommandDefaults の middleware が子の option を読むケースを壊す。`!option.registered` の判定だけにし declaredOptions を削除する方針に決定（A-8 の目的は満たせ、挙動変更が小さい） |
| R3A-7 | 対応 | repeatable() の登録後拒否を撤回（R3A-6 の判定で十分、登録順依存を避ける） |
| R3A-8 | 対応 | sub-interface の annotation が super-interface より優先されない |
| R3A-9 | R3B-1 と同じ | |

## Round 3 取り込み結果

- bypass permission + CHANGELOG（3 commit）: R3B-1（既存 permission があれば bypass node を登録しない・自分が追加したものだけ削除）、D3-1〜D3-4 を修正。
  - **Pending**: command / option / argument の permission node も、plugin.yml で宣言済みなら同様に上書きされる（children・default・description が消える）。plugin.yml を優先して API の default/description を無視するか、現状の上書きを維持するか。
- common（4 commit）: R3A-1〜R3A-8 を修正。各テストは修正前のコードで失敗することを確認済み。getOption は未登録 option の場合のみ例外（declaredOptions 削除）、repeatable() の登録後拒否は撤回。挙動変更: 同期的に Exception を投げる suggestion provider は出力してスキップ（Error は従来どおり伝播）。
- tester テスト不足（5 commit）: C3-2〜C3-6 を修正（各テストは mutation で失敗を確認）。C3-7 は velocity-3.4.0 の実 proxy で log pattern が誤検知しないことを確認、変更なし。

## Round 4（round 3 修正分のレビュー）

| id | 判断 | メモ |
|---|---|---|
| R4B-1 / A4-1 | 対応（20d89461 系の docs commit 16926819） | docs/suggestions.md の例外の記述が R3A-3 と矛盾 |
| R4B-2 | 対応（16926819） | CHANGELOG の Forge bullet: `/execute as` の source は実行者の level を保つ、function は server として動くものと compile のみ全通過 |
| A4-2 | 対応（20d89461） | bypass node が別 command の node と同名だと登録順で command 側が消える → permissionConfigs で共有される bypass entry を除外し、Spigot/Paper は全 command を 1 回の walk で登録 |
| A4-3 | 対応（16926819、docs のみ） | tester の suggest/suggestions は provider の例外で失敗しない（サーバーと同じ）旨を docs/testing.md に追記。tester で投げ直す案は見送り |

Round 4 は low のみ → レビュー/修正ループは収束と判断。master 7a88ae5e で modded 全 9 node compile + modded-testing 全 node 通過を確認済み。

## ponytail-review 1回目

### D modded / velocity（ponytail1-D）

| id | 判断 | メモ |
|---|---|---|
| P1D-1 | 対応 | A4-2 の修正が Forge/NeoForge に届いていない（command ごとの walk）。全 command の walk に変更 |
| P1D-2 | 対応 | velocity と paper の RichTexts（と test）がコメント 1 行以外同一 → 共有 source dir に（両方で compile できる場合） |
| P1D-3 | 対応 | modded register の prefix null/空チェックと list の copy（他 platform と揃える） |
| P1D-4 | 対応 | Stonecutter 分岐内の同一メソッドの重複 |
| P1D-5 | 対応 | velocity-kotlin の冗長な compileOnly |

### C spigot / paper / nms（ponytail1-C）

| id | 判断 | メモ |
|---|---|---|
| P1C-1 | 対応（high） | B-2 修正のリグレッション: Paper の ChatColorArgument が `ColorArgument` を名前で探すが 26.2 で `TeamColorArgument` に改名 → Paper 26.2/26.3 で構築時に失敗し command 登録ごと失敗 |
| P1C-2 | 対応 | 26.2 以降 vanilla に RESET がないため Spigot も reset を拒否する。javadoc と fixture を修正。**B-2 の Pending はこれで解消**: vanilla 自体が 26.2 で reset を廃止したので、Paper は reset を拒否したままにする |
| P1C-3 | 対応 | NMS 生成物と generator の一致を CI で確認する手順を roadmap に追記 |

### B common 実行（ponytail1-B）: バグなし

| id | 判断 | メモ |
|---|---|---|
| P1B-1 | 対応 | `PlatformAdapter.hasPermission(command, ctx, prefix)` は呼び出し 1 箇所のみ → 削除して 2 引数版に統一（約 -25 行） |
| P1B-2 | 対応 | CommandRunner の permission-denied fail の 3 重複 → helper |
| P1B-3 | 見送り | Captions/Caption の properties 読み込み重複（-8 行、shading の相対 path 考慮が必要で ROI 低） |

### A common 解析（ponytail1-A）

| id | 判断 | メモ |
|---|---|---|
| P1A-1 | 対応 | short flag 10 個以上で node 上限チェック前に順列を全生成し起動時 OOM |
| P1A-2 | 対応 | A-11 修正のリグレッション: `@Option(required=true) @Range(min=1) int` が宣言不能（暗黙 0 が範囲外、@Default は required と併用不可） |
| P1A-3 | 対応 | `-` で始まる command/alias/argument 名が option token と衝突 |

### E テスト（ponytail1-E）

| id | 判断 | メモ |
|---|---|---|
| P1E-1 | 対応 | common-testing の executeAndGetContext / latest() が visibleWhen の空 context を返す（probe で確認） |
| P1E-2 | 対応 | integration-test の spigot/paper fixture で byte 同一の 13 class（約 1,280 行）を shared/bukkit に移す |
| P1E-3 | 対応 | common-testing の CommandLibExtension の no-op close wrapper 削除 |
| P1E-4 | 対応 | paper-testing の readDouble wrapper 削除 |

## ponytail 1回目 取り込み結果

- common（5 commit、934cc3a1 まで）: P1A-1〜P1A-3, P1B-1, P1B-2 を修正。P1A-3 は `-x` 形式の名前を拒否する破壊的変更。
- テスト（4 commit）: P1E-1〜P1E-4 を修正。P1E-2 は共有 dir を targets 側だけに追加していて shared fixture project（`:integration-test:shared:{spigot,paper}-fixture`）の compile が壊れていたため、両 build.gradle.kts にも srcDir を追加して squash。
- Paper ChatColor（3 commit）: P1C-1〜P1C-3 を修正。paper-26.2 / spigot-26.2 / paper-1.21.11 の実サーバーで通過。docs/arguments.md の vanilla エラー文（`Unknown color`）も訂正。
- **新規発見（未修正）**: paper-26.2 と paper-1.21.11 で 1 回ずつ、実行時の register/unregister 直後に Paper の非同期 command tree 送信（`Commands.sendAsync` → `fillUsableCommands`）で ConcurrentModificationException。fixture は全通過、再実行では再現せず。root 直下の子 node の children map を変更中に Paper の別 thread が走査している race と推定（`CommandNodeChildren.replace` は root の map だけ保護）。ponytail 2 回目で調査・修正する。
- modded / velocity（4 commit）: P1D-1〜P1D-5 を修正。RichTexts は `common/src/adventure` に共有化（integration-test の paper target 用 srcDir も追加、rebase 時に P1E-2 の bukkit srcDir と衝突 → 両方残して解消）。modded 全 9 node compile + modded-testing 全 node 通過。
  - 残るリスク: 別々の `CommandLib.register` 呼び出し間（別 mod など）では bypass node が他方の node を上書きし得る（Spigot も同じ）。

## ponytail-review 2回目

| id | 判断 | メモ |
|---|---|---|
| P2D-1 | 対応 | modded register で CommandNameValidator.validateTopLevel を呼ぶ（Paper と同じく即時に失敗させる） |
| P2D-2 | 対応 | velocity-testing の冗長な compileOnly velocity-api |
| P2B-1 | 対応 | P1B-1 の削除で PlatformAdapter の型引数 U が未使用に → 削除、HelpMessageAction の unchecked cast も削除 |
| P2B-2 | 対応 | 「sender が使える option」の計算が CommandRunner と HelpMessageAction に重複（既に一度ずれた）→ 1 つに統合 |
| P2C | 指摘なし | spigot/paper/nms: 生成物と generator の一致を確認（`generate.py --all` で差分なし）、26.2/26.3 jar で NMS module と Paper の名前ベース lookup がすべて解決することを確認。Spigot/Paper の TeamColor 二重化（約 5 行）と paper-kotlin の Bukkit DSL テスト不足は ROI 低で見送り |
| P2E-1 | 対応 | integration fixture の ScenarioTest / VanillaExecutionTest（572 行）は Spigot 1.20.3 判定以外同一 → shared/bukkit に統合（約 -570 行） |
| P2A-1 | 対応 | CHANGELOG に `-` で始まる名前の拒否（P1A-3）を追記 |

### Paper 非同期 command tree の CME（調査結果）

- 修正（429e6a2b）: 原因は CommandLib 側。Paper 1.21.4+ では `PluginCommandNode` が存在せず、`PaperDispatcher.PaperInternals.find()` が丸ごと null を返して root の children map を in-place 変更する fallback に落ちていた。Paper の非同期 builder は `execute run` の redirect 経由で live な root map を走査するため CME。constructor の lookup を分離し、mirror root があれば常に map を copy して差し替えるよう修正。paper-testing ではなく paper の test に stand-in の ApiMirrorRootNode を置いた再現テストあり（旧コードで CME、修正後通過）。paper-1.21.11 / paper-26.2 で各 2 回実行し CME なし。1.21.4〜1.21.10 / 26.1.x / 26.3 は未実行（原因は同じ）。
- **Pending**: Paper 1.21.4+ で実行時に登録した label は plugin metadata のない plain node になり、command map 上は `minecraft.command.<label>` permission の VanillaCommandWrapper になる（修正前からの挙動）。`pluginCommandMeta`（1.21.4）/ `apiCommandMeta`（1.21.5+）を付けて plugin の command として扱うか（/help の配置と Bukkit.dispatchCommand の permission が変わる）。

## ponytail 2回目 取り込み結果

- 6 commit（477e21fa まで）: P2A-1, P2B-1, P2B-2, P2D-1, P2D-2, P2E-1 を修正。modded 全 node、spigot-1.20.3 / paper-1.21.11 の実サーバー通過（CME なし）。

## ponytail-review 3回目（02fbf5f9..477e21fa のみ）

| id | 判断 | メモ |
|---|---|---|
| P3B | 指摘なし | 新テストが失敗し得ること（paper は mutation で確認）、stand-in ApiMirrorRootNode が出荷物に入らないこと、統合 fixture が 1.20.3 の quirk を保持していることを確認 |
| P3B-N1 | 対応（コメント修正のみ） | PaperDispatcher.java:162 のコメント「Not a Paper server, as in tests」が不正確に |
| P3A | 指摘なし | 83e54f34 を Paper 1.21.0〜26.3 の実 server jar（javap / decompile）で確認し正しいと判断。spigot artifact の経路も問題なし |

ponytail-review は 3 回目で指摘なし → 終了。コメント修正 abc43ba0 系。
