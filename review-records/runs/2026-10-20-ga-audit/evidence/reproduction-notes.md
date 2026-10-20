# Reproduction notes

- `reproduction/GaAuditScratchTest.java`: the temporary JUnit 5 test placed in
  `common/src/test/java/dev/marulabs/commandlib/` during the audit and removed afterwards. It uses the module's
  package-private test helpers (`TestCommand`, `TestCommandRunner`, `TestCommandSource`, `CommandTreeRenderer`,
  `CommandTree`), so it must live in that package to run again.
- `reproduction/scratch-test-output.txt`: the JUnit XML failure messages of the run at `a65a9974`. Each test asserts a
  sentinel (`Hn-EXPECTED-TO-FAIL`) so that the assertion message shows the observed value; "failed" therefore means
  "observed", not "regression". `h1` ended with an `IllegalArgumentException` escaping the dispatcher instead of an
  assertion, which is itself the observation.
- Command: `./gradlew :common:test --tests "dev.marulabs.commandlib.GaAuditScratchTest" -Pcommandlib.typedNms=false --offline`
  (Gradle 9.5.0, daemon JDK 21, Windows 11).
- Mapping to the report: h1/h1b → F01, h2 → F05, h3/h10 → F02, h4 → F11, h5/h9 → F10, h6 → F06, h7 → F03, h8 → F04,
  h11 → no defect (option argument sharing a positional argument's name works).
