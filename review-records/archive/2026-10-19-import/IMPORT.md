# 既存レビューの取り込み（2026-10-19）

`.local` と `docs/audits` の既存ファイルを、内容を変更せず `raw/` 以下へ移動した。
`manifest.json` は旧パス、新しい相対パス、サイズ、SHA-256 を全ファイルについて記録する。
取り込み時の HEAD は manifest を参照。これは各レビューの対象 revision とは異なる。

| 元の場所 | 移動先 | 主な入口 |
|---|---|---|
| `.local/review-1/` | `raw/local/review-1/` | [scope](raw/local/review-1/ga-opus/scope.md)、[summary](raw/local/review-1/ga-opus/summary.md)、[findings](raw/local/review-1/ga-opus/findings.md)、[fixes](raw/local/review-1/ga-opus/fixes.md) |
| `.local/review-2/` | `raw/local/review-2/` | [summary](raw/local/review-2/summary.md)、[triage](raw/local/review-2/triage.md)、各 round の生レポート |
| `.local/ga-audit-2026-10-18/` | `raw/local/ga-audit-2026-10-18/` | 再現プログラム、classpath、出力、class ファイル |
| `.local/work-summary.md`、`parts/`、`commits.txt` | `raw/local/` 内の同名パス | 実装過程の周辺資料として保持 |
| `docs/audits/` | `raw/docs-audits/` | [監査レポート](raw/docs-audits/ga-architecture-api-audit-2026-10-18.md)、[再現資料](raw/docs-audits/ga-audit-evidence-2026-10-18.md) |

原本内の旧 `.local/...` 参照は `raw/local/...`、旧 `docs/audits/...` 参照は `raw/docs-audits/...` に読み替える。
それ以外の repository 相対リンクは元の位置を基準に解釈する。原文のリンクを一括置換しない。
再現資料が `.local` へのコード展開を指示する場合は、原本と別の作業用コピーで実行する。

過去レポートの対象・未コミット変更・モデル・実行条件の記述には未確認のものもある。
今回行ったのはファイルの保存・整合性検証であり、指摘や検証結果を再認定したものではない。
未決事項は特に review-2 の triage / summary と GA 監査レポートを参照する。

`raw/` と manifest は Git 管理対象として保存する。

整合性は repository root から
`powershell -NoProfile -ExecutionPolicy Bypass -File review-records/verify-archive.ps1` で再確認できる。
`-ExecutionPolicy` はこの子プロセスだけに適用され、システムの設定は変更しない。
