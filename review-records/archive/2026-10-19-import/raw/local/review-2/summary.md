# レビューまとめ（2c8ffd7a より後のコミット）

- 対象: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..6b898637`（244 commit）。レビュー開始時の master は 6b898637。
- 結果: master に 183 commit を追加（fix 86 / docs 45 / test 31 / refactor 19 / build 2）。最終 HEAD は 3cb2008d。
- 判断の全記録は `triage.md`、各回の生レポートは `round*-*.md` / `ponytail*-*.md`、jev-review の出力は `jev-review-raw.txt`。
- 作業ツリーの未コミット変更（ArgRef.java / OptionGraph.java の Javadoc 文言）は触っていない。各取り込み時に stash → apply で保持した（念のため `user-uncommitted-backup.patch`）。

## 進め方

1. `/code-review` + 後方互換チェック（code）と `/code-review` のテスト重点（tests）を、領域ごとに分けた subagent で回した（各 subagent は専用 worktree）。
   - Round 1: 7 体（common 解析 / common 実行 / spigot・paper・nms / modded・velocity・kotlin / テスト ×2 / docs）
   - Round 2: 5 体、Round 3: 4 体、Round 4: 2 体（前ラウンドの修正分に絞る）
   - Round 4 で low のみになったため収束と判断。
2. ponytail-review を 3 回（5 体 → 5 体 → 2 体）。3 回目で指摘なし。
3. `mise run jev-review 2c8ffd7aa17181325ba6f4759586a2bfb94e86ed` を実行。8 件の flag（commandlib-comment-describes-declaration）はすべて確認し誤検知（tester 4 件の `Builder.defaults` Javadoc、CommandOption の `permission(String)`、AnnotationParser の `registerContextValue` / `checkRange` / `errorName` のコメントは実装と一致）。

## 主な修正（抜粋）

- 第1ラウンドで見つかった high: 共有 argument node で executor が上書きされる、velocity-testing が proxy の default 権限を再現しない など。
- 修正によるリグレッションもレビューで捕捉して修正: modded の data pack function が読み込めない（R2C-1）、Paper 26.2+ で ChatColorArgument が構築時に失敗（P1C-1）、common の bare runner / bridge method / suggestion merge など。
- 実サーバーでのみ出た問題: Spigot 1.20.3 の selector 付き MessageArgument の例外、Paper の unregister が他の登録の label を消す、Paper 1.21.4+ の非同期 command tree 送信での ConcurrentModificationException（root map の in-place 変更が原因）。
- 不要 API の削除: `addPreprocess(Consumer)`、`sendComponent`、modded の `allowLogging` overload、`CommandOption` の public constructor、paper-testing の `withFake*` registry、`PlatformAdapter` の不要 method / 型引数、`UnknownCommandActor` など。
- 重複の削減: permission の walk を 1 本化、Adventure RichTexts の共有、integration fixture の Spigot/Paper 共通化（約 1,850 行削減）。

## 検証

- 各修正ごとに focused test。最終付近で modded 全 9 node の compile + modded-testing 全 9 node のテストを複数回通過。
- 実サーバー（Docker）: spigot-1.20.3 / 1.21.11 / 26.2、paper-1.21.1（spigot artifact）/ 1.21.11 / 26.2、velocity-3.4.0 で通過。
- 未実行: folia、mohist、古い paper（1.21.4〜1.21.10、26.1.x、26.3 は CME の原因が同じだが未実行）、modded の実サーバー（R2C-1 の function、C-2 の permission 判定は unit test のみ）。

## Pending（仕様判断が必要）

詳細は `triage.md` の各表。

| id | 内容 |
|---|---|
| A1-9 | `requires(option, value)` が未指定時の default 値を受け入れる一方、`requires(option)` は指定を要求する。どちらに揃えるか |
| A2-3 / D-4 / A-1 | function など再利用される parse の confirm。dispatch token がない。tick を token にするか、function では confirm 不可と文書化するか（現状は confirm 後 timeout まで確認なしで再実行できる） |
| A2-7 | `getInput(int)` / `getInputs()` が literal を含み `getArgument(int)` と index がずれる。削除か argument のみに限定か |
| A2-8 | argument branch は親の uncaught exception handler を継承し、子 command は継承しない |
| A-17 | option は位置引数より先に parse されるため、option の validator から getArgument を読めない |
| A-18 | `/config -f <key> set` で親の `-f` が受理された後に捨てられる（docs/options.md と矛盾） |
| A-20 | Options の型別 factory と Options.of の重複・default の意味の違い（@Range option は int のみ） |
| A-21 | `addPreprocess(Predicate)` と addMiddleware / addPrerequisite の重複 |
| B-5 | `sendSuccess`（broadcastToOps）を async から呼んだ場合。sync に移すか sync 専用と文書化か |
| B-7 | 未使用の NMSEntity を全 27 NMS module と codegen から削除するか |
| E-13 | modded の command block に OP default node は level 4 が必要（vanilla は level 2） |
| R2C-3 / C-6 | 同じ node の default が複数登録で異なる場合の方針（Velocity/modded は最初、Spigot/Paper は最後）。Velocity は unregister 後も default が残る |
| R2C-4 | console の扱いの platform 差（NONE node、RCON と requireConsole） |
| D-27 | cooldown 予約中に弾かれた実行に「残り時間 = 全期間」と伝える現挙動 |
| F-35 | samples / README の `latest.release` が 0.18.0 の tag 前は旧 API を解決する |
| R3B-1 の続き | plugin.yml で宣言済みの command/option/argument permission node を上書きする現挙動（bypass node は修正済み） |
| CME の続き | Paper 1.21.4+ で実行時登録の label に plugin metadata を付けるか |

解消した Pending: B-2（vanilla 自体が 26.2 で reset を廃止したため Paper は reset を拒否のまま）、E-21（全 testing artifact の FakeSender を grant/deny 別保持・deny 優先に統一）。

## 見送り（ROI 低）

A1-18、B-8、D-30、D-32、D-34、E-12 の一部、P1B-3、P2C の小項目。理由は `triage.md` を参照。
