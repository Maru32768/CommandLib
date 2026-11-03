# Review request

- Date: 2026-11-03
- Source: user (Maru32768), pasted into chat, to Claude Code (Claude Opus 5.5)
- Target: `33850895` (master). The working tree had uncommitted documentation changes by the user (`README.md`,
  `mkdocs.yml`, untracked `docs/use-cases/`), which were not reviewed or touched.
- Environment: Windows 11, Gradle 9.5.0, daemon JDK 21; no real server, no integration tests

## Original request (verbatim)

> CommandLib — Property-Based Testing & Bug Hunting
> CommandLibのソースコードと既存テストを徹底的に調査し、未発見の不具合、仕様上の矛盾、境界条件の問題を発見してください。
> 調査方針
>
> 1. コマンド解析、引数処理、補完、バリデーションなどの主要機構を理解する
> 2. 既存のテストが検証している性質と、検証していない性質を整理する
> 3. 実装から成立すべき不変条件を抽出する
> 4. Property-Based Testing、Fuzzing、Metamorphic Testingなど適切な手法を選択する
> 5. 異常系、境界値、複雑な入力の組み合わせを探索する
> 6. Bukkit、Spigot、Paper、Forgeなどのプラットフォーム間の差異を調査する
> 7. 発見した問題について最小再現ケースを作成する
>
> 重視する観点
>
> * パーサーの決定性と例外処理
> * 解析と補完の整合性
> * 引数の組み合わせによる想定外の挙動
> * 入力サイズやネスト深度による問題
> * 状態の持ち越しや共有可変状態
> * プラットフォーム・バージョン間の互換性
> * 既存テストのオラクル自体の誤り
>
> 制約
>
> * 既存のテストフレームワークと実装方針を尊重する
> * 不変条件は仕様またはコード上の根拠を示してから採用する
> * 不正入力による正常な解析失敗をバグとして扱わない
> * 重要な疑惑は再現テストで検証する
> * 本番コードは変更しない
> * 既存テストの大量複製は避ける
> * 実証できなかった問題は明確に区別する
>
> 成果物
> `docs/audits/` に調査結果を作成してください。
> 特に、不具合を発見した場合は、最小再現入力、期待される動作、実際の動作、原因、影響範囲を記載してください。
> 表面的なコードレビューではなく、実際に検証して新たな知見を得ることを目指してください。

## Placement note (agent)

The request names `docs/audits/`, but the repository's `CLAUDE.md` says new review records must not go to
`docs/audits` and belong in `review-records/runs/<YYYY-MM-DD>-<topic>/`. The record follows `CLAUDE.md`; the report is
`reviews/agents/claude-opus-5.5-pbt-1.md`.
