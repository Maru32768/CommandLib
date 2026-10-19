# Implementation follow-up

IT-001 is addressed by the shared runner's `JUnitReportAssertions` and the separately maintained inventories under
`integration-test/expected-cases`. Target assignment is explicit for all 76 positive shared-runner targets. Version
exclusions are documented separately; unknown targets fail. The expected-enable-failure branch still returns before
inventory validation, and Velocity retains its separate runner.

The checker compares exact case IDs without depending on order, rejects duplicates, and checks the summary test count
against the expected inventory. Both summary counters and individual failure/error/skipped elements must be zero.
This catches omissions of a suite or builder, a replacement case at the same count, and incorrect summary counters.

Eight regression tests exercise the report checker and target/exclusion configuration. The initial test run caught a
test diagnostic-string mismatch (`duplicates` versus AssertJ's `duplicate(s)`), which was corrected without changing
the checker. Earlier unquoted PowerShell `-Pcommandlib.typedNms=false` attempts were split into invalid task arguments;
the verification commands quote that argument. Sandbox attempts were canceled during unrelated Loom cache preparation;
the Docker run uses the normal user environment, where Docker is available.

Final verification results are recorded separately in evidence/verification.md after completion. Original review text
and the original source-review outcome are preserved unchanged.
