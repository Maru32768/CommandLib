# ponytail1-E: tests and test utilities (2c8ffd7a..16926819)

Scope: common/src/test, common-testing, spigot/src/test, paper/src/test, velocity/src/test, spigot-testing,
paper-testing, velocity-testing, modded-testing, integration-test, benchmark. 420 files, about 27k added lines.
Load assumed: developer and CI test runs only, no production traffic.

What this change does: it adds a large set of tests in every module and grows the shipped testing artifacts
(CommandTester, FakeSender, CommandLibExtension, @Sender for common, Spigot, Paper, Velocity and modded). It also adds
real-server checks: Folia, Velocity, modded and new Bukkit fixture suites, plus JMH benchmarks with a regression gate.

How I checked:
- A script listed every @Test with no assertion among the 1,014 tests in the changed files. Every hit was a false
  positive: a constructor-throws test, or an assert after the try block.
- I read the shipped testers and extensions in full (common, Spigot, Paper, Velocity, modded), plus the integration
  harness, the Folia fixture and jmhCompare.
- I grepped every public tester method for users. All of them have a test or a docs user.
- I confirmed finding 1 with a throwaway test (deleted afterwards).

## Must fix

### P1E-1 (bug, medium) `executeAndGetContext` returns an empty help context
- Where: `common-testing/src/main/java/dev/marulabs/commandlib/TestCommandContext.java:25-29`, used by
  `common-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:78-87`.
- What this is: `executeAndGetContext(input, sender)` runs a command and returns "the `TestCommandContext` of the run"
  (docs/testing.md:73). It does this by remembering the last context created on the thread.
- Problem: when a command shows its help and a child has `visibleWhen`, the help builds one extra "source context"
  (empty input, see `HelpMessageAction.sourceContext` and `PlatformAdapter.createSourceContext`) per visibility check.
  This happens after the real context, so it becomes "the latest". The method then returns a context with no messages.
  `TestCommandContext.latest()` has the same problem.
- Evidence (confirmed): a probe test with `/game` and a child `start` that has `visibleWhen(ctx -> true)` gave
  `SENT=[-----, Usage:, /game start, -----]` but `ctx.messages()=[]` and `ctx.getHandle().getInput()=""`.
- Fix: record only contexts with input, as the copy in `common/src/test/.../TestCommandContext.java:16-21` already
  does: in the constructor, `if (!ctx.getInput().isEmpty()) { LATEST_ON_THREAD.set(this); latest = this; }`. Add the
  probe as a regression test in `common-testing/.../CommandTesterTest.java`, and assert
  `ctx.messages()` equals `sender.getSentMessageTexts()`.
- If we skip it: a user who checks help output, or any context value of a help run, gets an empty context and a
  confusing failure, or a negative assertion that passes on nothing.

## Should fix

### P1E-2 (simplify/delete, low) 13 Bukkit fixture classes are byte-identical in two suites
- Where: `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/` and
  `integration-test/shared/paper/src/main/java/dev/marulabs/testplugin/`.
- What this is: the Bukkit test plugin is compiled from `shared/core` plus one suite dir
  (`integration-test/shared/integration-test.gradle.kts:187-192`, `shared/$testPluginSuite/src/main/java`).
- Problem: these files are identical in both suites (diff = 0 lines): OptionTest (376), RuntimePermissionTest (133),
  CommandSyntaxExceptionTest (132), OptionalVariadicArgumentTest (128), TestBase (128), RuntimeFeatureTest (120),
  CommandDispatchProbe (76), SuggestionTest (53), CommandDispatchResult (47), ReloadWatcher (33), HelpMessageTest (30),
  MainCommand (20), CommandDispatchErrorHook (7). Several of them are new in this range. Every fix has to be made twice.
  Round 1 already caught them drifting apart: E-15 had to port Spigot-only scenarios to Paper by hand.
- Evidence (confirmed): `diff` of each pair prints nothing. This is not D-32, which covers spigot-testing and
  paper-testing.
- Fix: move the 13 files to `integration-test/shared/bukkit/src/main/java/...`. Add
  `"../../../shared/bukkit/src/main/java"` to `srcDirs` when `testPluginSuite` is `spigot` or `paper` (not `folia`).
  One build line changes, and nothing else does. Leave the files that differ (ArgumentTest, ScenarioTest,
  VanillaExecutionTest, DataPackFunctionTest, HelpTopicTest, TestMain) where they are.
- Saving: about -1,280 lines.
- If we skip it: the two suites keep drifting, so a check runs on Spigot but not on Paper, or the other way round.
- Not checked: I did not build a Bukkit target (that needs Docker and server jars). The files compile in both suites
  today, so one shared copy compiles too.

## Nice to have

### P1E-3 (delete, low) common-testing wraps a no-op `close()` in a store resource
- Where: `common-testing/src/main/java/dev/marulabs/commandlib/CommandLibExtension.java:60-63, 86-97`.
- What this is: the extension keeps the tester in the JUnit store, wrapped so that JUnit calls `tester.close()`.
- Problem: `common-testing` `CommandTester.close()` is empty (`CommandTester.java:125-127`). The modded extension
  already stores the tester directly for the same reason (`// close() does nothing, so the store need not close the
  tester`).
- Fix: store the tester directly, as modded does: `getOrComputeIfAbsent(this, key -> testerFactory.get(),
  CommandTester.class)`. Then delete `TesterResource`. Keep the wrappers in Spigot, Paper and Velocity, whose
  `close()` does real work.
- Saving: about -13 lines.

### P1E-4 (delete, low) Paper tester has a one-line `readDouble` wrapper
- Where: `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:774-785`.
- Problem: `readDouble(reader)` only returns `reader.readDouble()`.
- Fix: call `reader.readDouble()` at the three call sites and delete the method.
- Saving: about -4 lines.

## Looked at, no finding
- No-assertion scan: clean (see above).
- Negative-only assertions (`doesNotContain` and `noneMatch`, 49 new uses): in the ones I sampled, a positive
  assertion or a matching `assertBotReceives` wait comes first. The one weak case,
  `spigot-testing CommandTesterTest.require_player_blocks_console_sender`, predates this range.
- Paper/Spigot/Velocity/modded tester logic (selectors, ranges, time units, NBT/JSON reader, scope reopening on
  scheduler threads, registry bootstrap): each behavior I grepped has a test (`Min cannot`, `Invalid unit`,
  `Selector arguments are not supported`, the queued-async-task tests, and others).
- Folia fixture: it pre-seeds FAILED for both async checks, so a check that never runs still fails.
- `jmhCompare`: it gates only on allocation, fails when nothing is compared, and reports missing results. That matches
  docs/benchmarks.md.
- Spigot NMS mocks: every `Mock*` class has a reference.
- Test helpers in common/src/test (TestCommandRunner, FakeScheduler): every method has a user.
- `OptionTreeSizeTest.benchmarkCommand` copies benchmark `Trees.command`. The comment says so, and they sit in modules
  that cannot share code cheaply. No finding.

Verdict: fix 1 first.
Lean: -1,300 lines possible (mostly P1E-2).
Not checked: I did not build or run the integration targets or the modded nodes. I sampled the 228 changed test files
rather than reading them all.
