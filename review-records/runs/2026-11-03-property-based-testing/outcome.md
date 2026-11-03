# Outcome

No production code was changed; the request forbade it. The run is a report with reproduction tests.

## Executed checks

- `./gradlew :common:test --tests "dev.marulabs.commandlib.PbtAuditScratchTest" -Pcommandlib.typedNms=false --offline`
  at `33850895`, Gradle 9.5.0, daemon JDK 21. Properties `p1`, `p1b`, `p2`, `p3` pass; `p4`, `p5` and the probes
  `h1`-`h4`, `h6`, `h7` fail with the observations recorded in `evidence/reproduction/scratch-test-output.txt`.
- `./gradlew :paper-testing:test --tests "dev.marulabs.commandlib.argument.PbtAuditPaperScratchTest" -Pcommandlib.typedNms=false --offline`:
  the probe records `players:1`, `uuids:1` for a sender without `minecraft.command.selector`
  (`evidence/reproduction/paper-probe-output.txt`).
- The existing test suites were not re-run, since no production or existing test code changed.

## Proposed checks (not executed)

- F9 and U1-U3 on real servers (Spigot 1.20.5+ and earlier, Paper 1.21+, Velocity, Forge / Fabric).
- F6 on a real Paper 1.21+ server, since `paper-testing` does not model the label redirect.
- Regression tests for F1-F3, F5, F7 once the decisions in triage are made; the properties `p1`, `p1b`, `p2`, `p3` could
  become permanent tests.

## Remaining work

All findings are open. Accepted and needs-decision items are linked from `docs/roadmap.md` (P0).
