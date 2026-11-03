# Reproduction notes

- `reproduction/PbtAuditScratchTest.java`: the temporary JUnit 5 test placed in
  `common/src/test/java/dev/marulabs/commandlib/` during the audit and removed afterwards. It uses the module's
  package-private test helpers (`TestCommand`, `TestCommandRunner`, `TestCommandSource`, `CommandNodeCreator`), so it must
  live in that package to run again.
  - `p1`, `p1b`, `p2`, `p3`, `p4`, `p5` are property tests. Each generates inputs from `pbt.seed` (default `20261103`,
    offset per property) and, for `p1`, shrinks a failure to a shorter input. A property fails with its minimal
    counterexamples.
  - `h1`-`h4`, `h6`, `h7` are hypothesis probes that assert the expected behavior, so a failure message shows the observed
    one.
  - The P1 oracle leaves out the case F4 describes (`getInputs()` of `scan scan`), so that other failures could surface.
- `reproduction/PbtAuditPaperScratchTest.java`: the temporary probe placed in
  `paper-testing/src/test/java/dev/marulabs/commandlib/argument/` and removed afterwards. It asserts a sentinel
  (`H5-EXPECTED-TO-FAIL`), so "failed" means "observed".
- `reproduction/scratch-test-output.txt` and `reproduction/paper-probe-output.txt`: the outcome of each test, taken from
  the JUnit XML of the last run.
- Commands, at `33850895`, Gradle 9.5.0, daemon JDK 21, Windows 11:
  - `./gradlew :common:test --tests "dev.marulabs.commandlib.PbtAuditScratchTest" -Pcommandlib.typedNms=false --offline`
  - `./gradlew :paper-testing:test --tests "dev.marulabs.commandlib.argument.PbtAuditPaperScratchTest" -Pcommandlib.typedNms=false --offline`
- Last run: 12 common tests, 4 passing properties (`p1`, `p1b`, `p2`, `p3`), 8 failures recording observations; 1 Paper
  probe recording its observation. The run takes about 7 seconds of test time.
- Mapping to the report: h1 → F1 (also H7's `help` suggestion), h2 → F2, h3 → F3, h4 and the first P1 counterexamples →
  F4, p5 → F5, h6 → F6, h7 → F7, p4 → F8, Paper h5 → F9.
- The output files are the JUnit XML failure messages with line endings normalized to LF. SHA-256 at the time of
  recording:
  - `PbtAuditScratchTest.java` `4f9f647311383728cd3711ecb28147073cacd4bf6ec383741a5cb0c5521115ea`
  - `PbtAuditPaperScratchTest.java` `2560a19ed0909cd40bb1f8a6c6bc8f70355f925e736a0c85ee7d207168786299`
  - `scratch-test-output.txt` `997a3823737ce82ff07ec1477529afbc9b2d8d6065997325f8a84c2ee823a4f5`
  - `paper-probe-output.txt` `d85ce7538922d23c5f1f8b6e398fd91e52b9d02174dd95a303fc54efad2c94b2`
- The first P1 run hung because the shrinker accepted equally long inputs and cycled; it was stopped and the shrinker
  now requires a strictly shorter input or fewer non-default fields. The Gradle test worker of that run (PID 69584 at
  the time) kept running after the client stopped and was left for the user to end.
