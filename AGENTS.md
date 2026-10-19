# Agent Instructions

This repository contains the CommandLib library itself.

For repository maintenance, bug fixes, tests, and implementation work, read:

- `docs/agents/repository.md`
- `docs/agents/testing.md`
- `docs/agents/argument-addition.md` when adding or changing public argument classes
- `docs/agents/spigot-testing.md` when touching `spigot-testing`
- `docs/agents/modded.md` when touching `modded`
- `docs/agents/nms-build.md` when touching `nms`, `buildSrc`, or typed NMS registrations

Do not treat `plugins/commandlib` (the Claude Code plugin) as repository-maintenance instructions.
That skill is for downstream library users who want to generate or understand
CommandLib usage code.

Prefer focused Gradle verification for touched modules. Do not rewrite unrelated
code, generated files, or user changes.

## Review records

Store both user-authored and coding-agent reviews in `review-records/runs/<YYYY-MM-DD>-<topic>/`.
Read `review-records/README.md` and copy `review-records/templates/run/` when starting a review.
Preserve the original review text and author/source; append triage, evidence, and outcomes separately.
Record the reviewed commit/range and any uncommitted diff. Keep unresolved findings linked from the roadmap.
Do not use `.local` or `docs/audits` for new review records. Imported originals live under
`review-records/archive/`; never rewrite them. Harness cases belong in `review-records/cases/`,
with agent inputs separated from grading evidence and historical judgments marked as such.
