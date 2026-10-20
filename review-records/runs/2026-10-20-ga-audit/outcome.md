# Outcome

No fixes were made in this run; the audit is a report only.

## Executed checks

- `./gradlew :common:test --tests "dev.marulabs.commandlib.GaAuditScratchTest" -Pcommandlib.typedNms=false --offline`
  at `a65a9974`, Gradle 9.5.0, daemon JDK 21. The test file is in `evidence/reproduction/GaAuditScratchTest.java`; every
  scenario asserts a sentinel so the failure message records the observed behavior
  (`evidence/reproduction/scratch-test-output.txt`). All 12 scenarios ran; `h11` showed no defect.
- The existing test suites were not re-run.

## Proposed checks (not executed)

- Real-server confirmation of F02 on Paper (`/minecraft:reload` after a post-registration mutation) and Forge (`/reload`).
- Regression tests for F01, F03, F04, F05, F06, F10 once the decisions in triage are made.

## Remaining work

All findings are untriaged or need a decision; see `triage.md`. Unresolved accepted work should be linked from
`docs/roadmap.md`.
