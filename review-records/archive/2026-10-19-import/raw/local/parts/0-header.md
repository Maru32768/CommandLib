# CommandLib 作業まとめ（2026-10-15〜16）

`docs/roadmap.md` の項目を、subagent と git worktree を使って並行で進めた作業のまとめ。

- **範囲**: master の `2c8ffd7a..1b8f17dc`（約 190 commit、1,666 ファイル、+65,533 / −3,167 行）。commit 一覧は `git log --reverse 2c8ffd7a..master`。
- **進め方**: 各 agent が自分の worktree の branch で実装し、master に fast-forward で取り込んだ。`docs/roadmap.md` / `docs/roadmap-history.md` は取り込みごとに更新した。
- **検証**: 各変更は対象 module の focused test で確認した（`spotlessCheck`、`:common:test`、`:spigot:test`、`modded-testing` の各 node など）。最後に Docker 上の実サーバー 29 target で integration test を回し、すべて通った（5.8 節）。
- **使用例のコード**: 現在のソースとシグネチャを照合して書いた。ただし compile はしていない。

## 目次

1. [Options と arguments](#1-options-と-arguments)
2. [実行と runtime](#2-実行と-runtime)
3. [Message・help・documentation](#3-messagehelpdocumentation)
4. [Annotation API と Kotlin DSL](#4-annotation-api-と-kotlin-dsl)
5. [Platform・testing・build](#5-platformtestingbuild)
6. [判断が必要な箇所と未対応事項](#6-判断が必要な箇所と未対応事項)

## 破壊的変更の一覧

0.17.x からの移行で影響があるもの。詳細は各節を参照。

| 変更 | commit | 節 |
|---|---|---|
| `CommandOption` の public flag constructor を削除（`Options.flag` を使う）。option のエラー型が `OptionParseException` になった | 4848c3d8 | 1.1 |
| `argument(Consumer<ArgumentBuilder>)` と `ArgumentBuilder` を削除（8 個以上の argument も `argument(...)` で書ける） | 10dd80ea | 1.8 |
| modded の `BlockPosArgument` を `BlockPositionArgument` に rename | dfe862dc | 1.11 |
| Spigot の `EntityTypeArgument` を namespaced key（`minecraft:zombie`）で読むように変更 | e6b1a4a1 | 1.12 |
| `ScoreboardDisplaySlotArgument` / color を vanilla の名前で読むように変更 | 2bbc4dbd | 1.12 |
| `sendSuccess(String, boolean)` / `sendSuccess(RichText, boolean)` が抽象メソッドになった。失敗は `CommandSyntaxException` を投げる。`CommandTester#execute` が `int` を返す | 2f8d4d37 | 2 |
| preprocess / confirm / cooldown を middleware で実装し直した。`disableParentPreprocess()` を `disableParentMiddleware()` に改名 | fecaf526 | 2 |
| 組み込み message を caption（key + locale 別 bundle）にした。一部の英語の文言が変わった | 1458ede5 | 3 |
| Paper の resolve error を vanilla の translatable component で送る。selector の結果が空なら reject する | a3f8ae9b | 3 |

