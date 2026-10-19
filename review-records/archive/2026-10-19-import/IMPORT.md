# レビュー記録の取り込み・配置変更

取り込み日: 2026-10-19。取り込み時の HEAD は manifest の `import_head` を参照。
整理前の原本一式は commit `91a74d0f` に保存した。

| 記録 | 配置先 | 主な入口 |
|---|---|---|
| `.local/review-1/ga-opus/` | `runs/2026-10-19-import-review-1/` | [対象](../../runs/2026-10-19-import-review-1/inputs/scope.md)、[指摘・判断](../../runs/2026-10-19-import-review-1/triage.md)、[結果](../../runs/2026-10-19-import-review-1/outcome.md) |
| `.local/review-2/` | `runs/2026-10-19-import-review-2/` | [依頼の記録](../../runs/2026-10-19-import-review-2/request.md)、[判断](../../runs/2026-10-19-import-review-2/triage.md)、[結果](../../runs/2026-10-19-import-review-2/outcome.md) |
| `docs/audits/` と `.local/ga-audit-2026-10-18/` | `runs/2026-10-18-ga-audit/` | [監査原文](../../runs/2026-10-18-ga-audit/reviews/agents/architecture-api.md)、[再現資料](../../runs/2026-10-18-ga-audit/evidence/reproduction-notes.md) |
| `.local/work-summary.md`、`parts/`、`commits.txt` | 最初の review run の `inputs/implementation-context/` | レビューに付随する実装過程の資料 |

日付が特定できない review-1 / review-2 は、取り込み日を run 名に使った。
metadata の日時や model 情報は原資料で確認できる範囲だけを記録した。
新たな request / triage / outcome の案内は原文と区別して作成し、ユーザーの依頼原文を復元したとは扱わない。

`manifest.json` schema 2 は保存対象の608ファイルについて次を記録する。

- `source_path`: 整理前の `.local` / `docs/audits` 内のパス。
- `import_path`: 整理前 snapshot の archive directory を基準にしたパス。
- `record_path`: `review-records/` を基準にした現在の run 内のパス。
- `bytes` / `sha256`: 内容を変更していないことを確認する値。

取り込み時の814ファイルから、再生成可能な `.class` 206ファイル（867,741 bytes）を除外した。
削除前のファイルとmanifestは commit `d7b6d5d8` に残る。
再現用ソース、実行ログ、対象commitなどの記録は保持している。

監査の再現手順は配置変更に合わせて `evidence/reproduction-notes.md` を更新した。
監査時の原文は `evidence/reproduction/original-notes.md` に保持し、manifest はその原文を検証する。

原文内の相対リンクや絶対パスは当時のまま。旧パスを manifest で検索して現在の配置を辿る。
再現時は保存原本を直接編集せず作業用コピーを使う。個々の指摘や実行結果は今回再検証していない。

整合性確認:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File review-records/verify-archive.ps1
```
