# Review records

Preserve user and coding-agent reviews, their evidence, and the decisions made from them.
These records also provide source material for Agent Harness evaluation cases.

## Layout

| Path | Purpose |
|---|---|
| `runs/<YYYY-MM-DD>-<topic>/` | Review requests, original responses, triage, evidence, and outcomes |
| `archive/<import-id>/` | Imported originals, provenance, and integrity manifests |
| `cases/` | Evaluation cases with separate agent inputs and grading evidence |
| `templates/run/` | Template for a review run |

Original reviews and supporting evidence belong in version control, including logs and binaries needed to preserve
the record. Directory-local ignore rules override repository-wide exclusions for archived records and review runs.
Use `runs/<id>/scratch/` only for disposable working copies; it is ignored.

## Recording a review

1. Copy `templates/run/` to `runs/<YYYY-MM-DD>-<topic>/`. Add a numeric suffix if the name is already in use.
2. Record the repository, reviewed commit or range, working-tree state, date, and known reviewer information in
   `metadata.json`. Preserve any reviewed uncommitted diff in `inputs/working-tree.patch` and include relevant
   untracked inputs. Leave unknown author, model, or prompt information explicitly unknown.
3. Save the request and constraints in `request.md`. Save user reviews verbatim in `reviews/user.md` and agent
   responses in `reviews/agents/<reviewer>-<round>.md`. Include only the review sources that occurred; ordinary
   feature requests do not need a review record.
4. Keep original text unchanged. Store follow-up responses separately, decisions in `triage.md`, reproduction
   material in `evidence/`, and fixes, verification results, and remaining work in `outcome.md`.
5. Link accepted unresolved work from the roadmap. Record adopted designs in `docs/design` and confirmed defects
   in regression tests. Historical reviews do not replace current specifications or issue status.

Preserve duplicate findings in their original responses and cross-reference them during triage. Record artifact
paths, hashes, execution commands, and environment requirements with the evidence. Exclude credentials from stored
records and document any redaction separately.

## Imported originals

Keep imported originals byte-for-byte unchanged. Use manifests and import notes to map previous paths to archived
paths rather than rewriting historical links. Add corrections as separate notes.

An archive manifest records source paths, archive paths, byte sizes, and SHA-256 hashes. Verify an archive from the
repository root with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File review-records/verify-archive.ps1 -Archive <import-id>
```

The execution policy option applies only to that child process.

## Evaluation cases

Extract cases separately from the originals. Historical acceptance, rejection, and reproduction claims remain
historical judgments until their evidence is checked for evaluation use. A fix commit alone does not establish
correctness. See [evaluation case conventions](cases/README.md).
