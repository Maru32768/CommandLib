# CommandLib Property-Based Testing / 不具合探索レポート

- 実施日: 2026-11-03
- 対象: `33850895`（master）。作業ツリーには利用者の未コミット変更（`README.md`、`mkdocs.yml`、`docs/use-cases/`）があるが、
  いずれも文書で、本調査の対象外。本番コードは変更していない。
- 実施者: Claude Opus 5.5（Claude Code）。既存テストの棚卸しと platform 差分の洗い出しは同じ session の read-only subagent
  （Sonnet / Opus）に任せ、本レポートで使った主張はすべて source を読み直すか再現テストで確かめた。
- 再現: `evidence/reproduction/PbtAuditScratchTest.java`（`:common` の一時テスト、12 本）と
  `evidence/reproduction/PbtAuditPaperScratchTest.java`（`:paper-testing` の一時テスト、1 本）。出力は同じ directory の
  `scratch-test-output.txt` / `paper-probe-output.txt`。手順は `evidence/reproduction-notes.md`。
- 対象外: 実サーバー（Spigot / Paper / Forge / Fabric / Velocity）での実行、`integration-test`、modded と Velocity の tester。

## 1. 要約

Property-based / metamorphic test を 5 本（生成入力 計約 2 万件）、仮説ごとの再現テストを 8 本書いた。option の解析
（順序・綴り・結合 short flag・別名・`optionsAnywhere`・redirect 後の再解析）は、生成した 3,000 入力すべてで仕様どおりに
動き、option token の補完も健全かつ完全だった。一方で、次の 9 件を実際の入力で確認した。

| ID | 重大度 | 証拠 | 概要 |
|---|---|---|---|
| F1 | Medium | 再現 | 生成される `help` literal が第 1 引数の値 `help` を奪う。`/msg help hi` は構文エラー、`/msg -f help hi` は通る |
| F2 | Medium-Low | 再現 | variadic branch と第 1 引数を共有する branch では、`optionsAnywhere` の option が黙って variadic の値になる |
| F3 | Low | 再現 | `:` を含む top-level command 名に redirect する option を付けると `IllegalStateException` |
| F4 | Low | 再現 | 経路上に同名の node が 2 つあると `getInputs()` から token が消える（20-F05 の新しい症状） |
| F5 | Low | 再現 | Object / NameableObject / Literal 引数の補完が、option token と同じ key では別の意味で解析され、escape 入力後は消える |
| F6 | Low-Medium | 再現（Paper の登録形をモデル化） | Paper の label redirect 下では、`getInputs()` に label が入るかが option の綴り方で変わる |
| F7 | Low | 再現 | 非 ASCII 名の enum 定数（`高速` など）は補完されるが入力できない |
| F8 | Info | 再現 | Duration が「Duration に収まるが long に収まらない数」を拒否する（コメントと不一致） |
| F9 | 要判断 | 一部再現（Paper） | Paper の `OfflinePlayersArgument` / `UUIDsArgument` は selector 権限の無い sender にも `@a` / `@r` を許す |

実証できなかった候補は §5 に分けた。

## 2. 方法と不変条件

既存テストは固定例が中心で、option の順序入れ替え、quoted 値、補完が解析に戻ることを確かめるテストは無かった
（subagent の棚卸し。`CommandOptionTest`、`OptionRedirectTest`、`OptionsAnywhereTest` などを確認）。そこで、仕様または
コードに根拠のある不変条件だけを採用し、乱数生成した入力で検査した。乱数は seed 固定（`20261103` 起点）で、失敗した
case は入力が短くなる方向にだけ縮小する。

| # | 不変条件 | 根拠 | 手法 | 結果 |
|---|---|---|---|---|
| I1 | 同じ option 集合は、順序・long/short/alias・結合 short flag（`-fv`、`-vvv`）のどの書き方でも同じ値になる | `docs/options.md` Kinds / Aliases | P1、metamorphic | 成立（3,000 件） |
| I2 | `optionsAnywhere()` では option を引数の前後どこに置いても同じ値になる | `docs/options.md` Options After the Arguments | P1 | 成立。ただし F1、F2 |
| I3 | redirect の有無（`OptionGraph.recover` を通るか）で値と `getInputs()` が変わらない | `OptionGraph` の class Javadoc | P1、P1b、H6 | 値は成立。`getInputs()` は F4、F6 |
| I4 | 入力済みの option token の途中で補完すると、その token が候補に入る | 補完と解析は同じ tree を使う | P2 | 成立 |
| I5 | 補完候補を適用した入力は最後まで解析できる | 同上 | P2 | 成立 |
| I6 | quotable 引数の補完候補は、適用すると元の名前に解析される | `QuotableSuggestions` Javadoc「so that the suggestion parses back to the name」 | P5 | 不成立（F5） |
| I7 | quotable 引数は入力を（開き quote を除き、大文字小文字を無視して）含む名前をすべて補完する | 同 Javadoc | P5 | 不成立（F5） |
| I8 | variadic の値は入力順・transformer 適用済みで返り、上限を超えると拒否される | `CommonArgument#variadic` Javadoc | P3 | 成立（3,000 件） |
| I9 | Duration は Javadoc の文法どおりに受理・拒否され、補完候補は必ず解析できる | `CommonDurationArgument` Javadoc | P4、独立 oracle | ほぼ成立（F8） |
| I10 | enum 引数の補完候補は入力できる | 補完と解析の整合 | H7 | 不成立（F7） |

P1 の生成器は、command の形（top-level、`tool scan`、同名の親子 `scan scan`）、別名 `sc`、前置き（`run ` や
`as <name> go ` という root への redirect）、flag・count・範囲付き integer・quoted string の repeatable・`Options.of`
の option、quoted 位置引数（空文字、空白、`"`、`\`、`'`、`-f`、command 名を含む）と optional な integer 引数を組み合わせる。
3,000 件を 1 つの runner で続けて実行したので、実行をまたぐ状態の持ち越しがあれば検出される。持ち越しは観測されなかった。

## 3. 確認した不具合

### F1 (Medium) 生成される `help` literal が第 1 引数の値 `help` を奪う

- 最小再現: option `-f` と引数 `<target:word> <text:greedy>` を持つ `msg`（H1）。

  | 入力 | 実際 |
  |---|---|
  | `msg -f help hi` | `target=help text=hi f=true` |
  | `msg help hi` | 構文エラー `Incorrect argument for command at position 9: msg help <--[HERE]` |
  | `msg help` | help を表示 |

- 期待される動作: `help` も他の値と同じく `<target>` に入る。少なくとも、直前に option があるかどうかで値の意味が変わらない
  （I2）。`docs/help.md` は `/<command> help` が help を出すと書くが、第 1 引数が値 `help` を受け取れなくなることは
  どの文書にも無い。
- 原因: `CommandNodeCreator.java:46-47` が command node に literal `help` を足す。条件は `shouldAddHelpChild`
  （`:55`）で、子・引数・option のどれかがあり、`help` という名前の子も第 1 引数も無いこと。Brigadier は token 全体が
  literal に一致すると引数 node を試さない（literal 優先）。option の hub（`OptionGraph.build`）には `help` が無いので、
  option の後では値として通る。
- 影響範囲: Spigot / Paper / modded はどれも `CommandNodeCreator` で tree を作る（`spigot/.../CommandLib.java`、
  `paper/.../CommandSet.java`、`modded/.../CommandLib.java`）。第 1 引数を持つ command のほぼすべてで値 `help` が
  失われる。例として、名前が `help` の player（Minecraft の名前として有効）を `/ban help` で指定できない。小文字名が
  `help` になる enum 定数 `HELP` も選べず、H7 では enum 引数の補完に値のような顔で `help` が並んだ。

### F2 (Medium-Low) variadic branch と第 1 引数を共有すると、後ろの option が variadic の値になる

- 最小再現: `optionsAnywhere()`、flag `-f`、branch `<who...>`（word の `variadic(3)`）と `<who> <level:int>` を持つ
  `tp`（H2）。

  | 入力 | 実際 |
  |---|---|
  | `tp -f bob 5` | `who=bob level=5 f=true` |
  | `tp bob 5 -f` | `who=[bob, 5, -f] f=false` |
  | `tp bob -f 5` | `who=[bob, -f, 5] f=false` |

- 期待される動作: `docs/options.md` は `optionsAnywhere()` で option が各引数の後に置けるとし、例外を「variadic の値の
  後」だけとしている。`<who> <level>` の branch では `-f` は flag になり、3 入力とも `who=bob level=5 f=true` になる。
- 原因: `OptionGraph.isVariadic`（`OptionGraph.java:364`）は「同じ名前の引数子を持つ node」を variadic とみなす。同名の
  branch が 1 つの node に merge されるので、共有された `who` node は variadic 側の 2 つ目の `who` を子に持ち、variadic
  と判定される。その結果 `copy`（`:319`）が node を複製せず、option token が後ろに付かない。残った variadic の word が
  `-f` を値として受け取る。
- 影響範囲: `optionsAnywhere` と variadic と、同名の第 1 引数を持つ別 branch がそろう command だけ。ただしエラーにならず、
  flag が消えて値が増えるので、利用者は気付きにくい。

### F3 (Low) `:` を含む top-level command 名では redirect した option で例外になる

- 最小再現: flag `-a` / `-b` と引数 `<target>` を持つ `ns:scan`（H3）。`ns:scan -a -b x` も `ns:scan -b -a x` も
  `IllegalStateException: Cannot read the options before position 14 of 'ns:scan -a -b x'` を投げる。short flag は
  long 名の hub へ redirect するので、どちらの順でも `recover` を通る。
- 期待される動作: `x a=true b=true` を実行する。または登録時に名前を拒否する。
- 原因: `OptionGraph.findRegion` は path が 1 段のとき token の最後の `:` までを namespace として捨て
  （`OptionGraph.java:526-528`）、`prefix` も同じ処理をする（`:655`）。名前自体が `:` を含むと一致しない。
  `CommonCommand` の constructor は空でない名前をすべて受け付ける（`CommonCommand.java:63-69`）。
- 影響範囲: `:` を含む名前だけ。各 platform がその名前で登録を許すかは確かめていない。例外は `CommandRunner.run` で
  unexpected error として log され、sender には失敗が返る。

### F4 (Low、20-F05 の新しい症状) 同名の node が経路上に 2 つあると `getInputs()` から token が消える

- 最小再現:
  - 親子とも `scan` の command で `scan scan a` を実行すると `getInputs()` は `[scan, a]` になる（P1 の shape 2 で最初に
    見つかった）。
  - `config <key:word>` の下に literal 子 `key` を置き、`config motd key` を実行すると `[config, key]` になり、`motd`
    が消える（H4）。
  - `getInput("key")` と `getArgument("key")` はどちらも `motd` で正しい。
- 期待される動作: `getInputs()` は入力された token を順に返す（既存テストは `inputs=[ban, Steve]` のように literal も含める
  前提で書かれている）。
- 原因: `CommonCommandContext.collectInputs` は node 名を key にした `LinkedHashMap` に入れる（`CommonCommandContext.java:96`）。
  同名の literal は前の値を上書きする。`validateNamesAlongRoute` は引数同士の衝突しか検査しない。
- 影響範囲: index ベースの `getInput(int)` / `getArgument(int)` の意味は roadmap P0 の 20-F05 で要判断になっている。
  ここでの 2 例は、その判断で扱うべき追加の case として記録する。

### F5 (Low) quotable 引数の補完が、解析と食い違う

`ObjectArgument`、`NameableObjectArgument`、`LiteralArgument`（common の `CommonObjectArgument` などを全 platform が
継承）は `QuotableSuggestions.suggestMatching` で補完する。P5 で次の 2 種類を観測した。

- (a) 健全性:
  - 最小再現: flag `-f`（`--force`）と、key `-f` を持つ object 引数の command `pick`。
  - 実際: `pick ` の補完候補 `-f` を適用した `pick -f` は flag として解析され、executor ではなく help が動く。
    `--force` という key でも同じになる。
  - 期待される動作: Javadoc のとおり候補は元の名前に解析される。option token と衝突する key は quote して `"-f"` と
    補完する必要がある。
- (b) 完全性:
  - 最小再現: key が `"b`（quote を含む）で、入力が `pick "b\"`。
  - 実際: 候補が出ない。key `\` に対する `pick "\\` も同じ。
  - 期待される動作: 入力済みの escape を解いた `"b` を含む key を補完する。
  - 原因: `QuotableSuggestions.java:23-25` は開き quote だけを除き、escape を残した文字列で `containsIgnoreCase` を
    取っている。
- 影響範囲: key が `-` で始まって option token と一致する場合と、key が `"` / `\` を含む場合だけ。

### F6 (Low-Medium、20-F05 関連) Paper の label redirect 下で `getInputs()` が option の綴り方で変わる

- 最小再現（H6）: Paper 1.21+ の登録形をモデル化した。`<plugin>:game` node と、それへ redirect する `game` literal を
  Brigadier 1.0.18 の dispatcher に置いた。この形は CommandLib 自身のコメントにある（`ChainedCommandContext` の
  class Javadoc、`ArgumentCommandNodeCreator.suggestions` のコメント）。command は flag `-a` / `-b` と `<arena>` を持つ。

  | 入力 | `getInputs()` |
  |---|---|
  | `game x` | `[x]` |
  | `game --alpha --beta x` | `[x]`（long 名の順は hub そのもので redirect しない） |
  | `game -b -a x` | `[game, x]` |
  | `plugin:game x` | `[plugin:game, x]` |

- 期待される動作: 同じ command に同じ値を与えた入力では、`getInputs()` と `getInput(0)` が option の綴り方に依存しない。
- 原因: label を input から作り直すのは `OptionGraph.recover` の `prefix`（`OptionGraph.java:655`）だけで、これは最後の
  context の root が option hub のとき（`:463`）にしか通らない。label redirect の後の通常の context には label の
  node が無い。
- 影響範囲: Paper 1.21+ の `getInput(int)` / `getInputs()`。20-F05 は「Paper 1.21+ は root literal を含まない」と
  記録しているが、実際には Paper の中でも一貫しない。実 Paper 上では確かめていない（paper-testing は label redirect を
  再現しない）。

### F7 (Low) 非 ASCII 名の enum 定数は補完されるが入力できない

- 最小再現: `enum Speed { NORMAL, 高速 }` の `EnumArgument`（H7）。`speed ` の補完は `help`、`normal`、`高速` を返す。
  `speed 高速` は `Expected whitespace to end one argument, but found trailing data at position 6` で拒否される。
- 期待される動作: 補完した値は入力できる。それができないなら、登録時に拒否するか quote できる型で読む。
- 原因: `CommonEnumArgument` は `StringArgumentType.word()`（`CommonEnumArgument.java:17`）で読み、word は
  `0-9A-Za-z_-.+` しか読まない。補完は `name().toLowerCase(Locale.ROOT)`（`:23`）をそのまま出す。
- 影響範囲: Java が許す識別子（日本語、`$` など）の定数を持つ enum。全 platform の `EnumArgument` が同じ common 実装を
  使う。

### F8 (Info) Duration が long を超えるが Duration に収まる数を拒否する

- `99999999999999999999t`（約 5×10^18 秒）は Duration で表せるが、`Long.parseLong` が `NumberFormatException` を投げて
  拒否される（`CommonDurationArgument.java:68`）。`:56` のコメントは「malformed or overflows a Duration」のときに null を
  返すと書いている。P4 では 9,000 件中これ以外の食い違いは無かった。実害は無いので、コメントを直すだけでよい。

### F9 (要判断) Paper の offline player 系引数は selector 権限なしで `@a` / `@r` を受け付ける

- 再現（`PbtAuditPaperScratchTest`）: operator でなく、`minecraft.command.selector` を拒否した `FakeSender` の player が、
  `players @a`（`OfflinePlayersArgument`）と `uuids @a`（`UUIDsArgument`）を実行した。両方とも既知の player 全員を受け取った。
- 原因: Paper 版は `ProfileWordArgumentType`（raw word）で読む。`@a` / `@r` は `parseImpl` 自身が
  `Bukkit.getOfflinePlayers()` から作り（`paper/.../OfflinePlayersArgument.java:56`、`UUIDsArgument.java:64`）、権限は
  見ない。
- 未実証の部分:
  - Spigot 版は vanilla の `game_profile` 型で読む。vanilla 1.20.5 以降は解析時に selector 権限を検査するはずだが、
    spigot-testing は mock の parser なので比較できなかった。
  - したがって、platform と version で結果が変わるかは推定にとどまる。
- 文書の矛盾: `docs/arguments.md:98-99` は「selector の権限は vanilla のもの」と書き、`:107-108` はこの 2 つが `@a` / `@r`
  を自前で実装すると書く。非 operator に `@a` を許すかどうかの契約が無い。

## 4. 成立した性質

- P1、P1b、P3 は、生成した入力すべてで値・`hasOption`・`getInputs()` が oracle と一致した（P1 は F4 の shape を oracle から
  除いた後）。
  - 一致した値には、redirect をまたぐ quoted 値（command 名、`-f`、空白、escape を含む）、`run` / `as <name> go` を前置きした
    入力、argument child の option（親 command の option の後でも）、variadic の上限超過の拒否が含まれる。
- P2 では、option token の途中で補完すると、その token が必ず候補に入り、候補を適用した入力はすべて最後まで解析できた。
  - 当初は token 全体を入力した位置でも候補に入ると期待したが、これは oracle の誤りだった。Brigadier の
    `SuggestionsBuilder.suggest` は入力済みの文字列と同じ候補を捨てる。
- P4 の Duration 文法は F8 以外で Javadoc と一致し、補完候補はすべて解析できた。

## 5. 実証できなかった候補

subagent が source から挙げた候補。U2 と U3 は該当コードを読んで確かめたが、実行はしていない。U1 と U4 は platform 側の
挙動が推測を含む。

- U1: 大文字を含む command 名。
  - Velocity は label を小文字にして dispatch するとされる（Velocity 側は未確認）。その場合、`findRegion` が名前と一致せず
    F3 と同じ例外になる。
  - Spigot でも `knownCommands` に大文字のまま入り、検索は小文字なので実行できない可能性がある（推測）。
  - 各 tester は `dispatcher.execute` を直接呼ぶので再現できない。名前を小文字に限る検証を足せば、すべての platform で
    テストできる。
- U2: modded の `UUIDsArgument` の `@a` は profile cache にある online player だけを返し（`modded/.../UUIDsArgument.java:76-88`）、
  Bukkit は offline player 全員を返す。`docs/arguments.md:93` に platform 差の記載が無い。コード上は追えたが、実行していない。
- U3: Spigot の `GameModeArgument` は `EnumArgument` を継承して大文字小文字を無視するが、Paper と modded は vanilla の型で
  完全一致を要求する。`docs/arguments.md:210` は equivalent と書いている。実行していない。
- U4: Paper の `CustomArgumentType.Converted` が `convert(N, S)` を override していると、`VariadicArgument.valueHandle` と
  `OptionGraph.parseValue` の再解析（source を渡さない `parse(reader)`）では override が使われない可能性がある（推測）。

## 6. 既存テストについて

- 期待値そのものの誤りは見つからなかった。`OptionRedirectTest:116` は末尾の空白を拒否するが、vanilla と同じ挙動で、
  コメントで理由も書かれている。
- `TestCommandRunner.executeWithLeadingSlash` は先頭が `/` かを確かめずに 1 文字読み飛ばす。leading slash の扱いを
  確かめるテストとしては弱い。
- 不足しているもの（subagent の棚卸しで確認した範囲）:
  - 拒否テストの多くは例外の型しか見ず、位置もメッセージも見ない。
  - option の quoted 値と、namespaced label（`plugin:cmd`）のテストが無い。
  - 補完候補が解析できることを確かめるテストが無い。
  - P1、P2、P3 はこれらを生成入力で埋める。
