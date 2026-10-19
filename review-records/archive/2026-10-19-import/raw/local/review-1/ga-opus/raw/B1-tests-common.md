# B1 — common/core test-suite audit

Reviewed: `2c8ffd7a..1b8f17dc` (HEAD). Scope: `common/src/test/**`, `common-testing/src/{main,test}`,
`../../../../common-kotlin/src/platformTest` (KotlinDslTest), `examples/src/test`, `benchmark/baseline.json`.
Method: reading + mutation runs in a disposable `git archive HEAD` copy (`:common:test`, 377 tests green at
baseline; Error Prone `-Werror` removed in the copy only so trivially-constant mutants compile). The copy has been deleted.

Overall: the new suites are mostly contract-shaped (explicit expected messages, fake clock instead of sleeps, a
hand-driven `FakeScheduler` for async, annotated-vs-fluent differential checks). Cooldown/confirmation, help
filtering and option-permission tests killed every mutant aimed at them. The gaps cluster around **argument-branch
permission/visibility enforced at execution time** and **`optionsAnywhere()` × permissions**, plus a harness
behaviour that silently turns assertion failures inside executors into chat messages.

## Findings

### B1-01 — P1 — `optionsAnywhere()` × branch permission has no test; with the runtime check also untested, a full permission bypass goes undetected

- Production: `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java:280-285` (`copy()` puts
  `argument.getRequirement()` on each per-state copy of an argument node; for `optionsAnywhere` commands these copies
  are the *only* argument nodes in the tree) and `CommandRunner.java:143-150` (runtime branch-permission check).
- Tests: `OptionsAnywhereTest` (all 10 tests use the all-permission source of `TestCommandRunner(command)`);
  `OptionTreeSizeTest.every_redirect_follows_its_target_in_the_per_player_copy` uses a permissioned *option*, not a
  branch, and only checks redirect order. No test anywhere combines `optionsAnywhere()` with a branch permission or a
  sender lacking it (`grep optionsAnywhere common/src/test` → AnnotationParserTest, OptionsAnywhereTest,
  OptionTreeSizeTest; none permission-restricted).
- Mutations: M3 (`argument.getRequirement()` → `src -> true` in `copy()`) **survived**: the branch becomes visible in
  the tree/suggestions to senders without the node. M4 (runtime branch check `if (!platformAdapter.hasPermission(ctx,
  arguments.permissionName(...)))` → `if (false)`) **survived**. M15 (M3+M4 together) **survived**: a sender without
  `test.command.ban.target` can run `ban Steve` / `ban -s Steve` on an `optionsAnywhere` command and nothing fails.
  A 15-line probe (sender with only `test.command.ban`; assert `visibleChildren("ban")` lacks `target` and both
  inputs throw `CommandSyntaxException`) passes on HEAD and kills M3.
- Claim vs reality: `OptionsAnywhereTest` asserts parsing/limits/suggestions; the permission behaviour of the new
  copied-node tree, which is entirely separate code from the non-anywhere path, is asserted nowhere.
- Confidence: high (demonstrated). Suggested correction: add the probe above to `OptionsAnywhereTest` (plus a case
  where the option comes after the argument, and a child command under the argument), and see B1-02 for the runtime
  check.

### B1-02 — P2 — Execution-time re-checks (branch permission, branch visibility, parent visibility) are untested

- Production: `CommandRunner.java:89-102` (`checkVisible`: parent chain loop and argument-branch loop) and
  `CommandRunner.java:143-150` (branch permission). The comment states they exist "for a state that changed after the
  input was parsed or a platform that runs it anyway".
- Tests: the parse-then-revoke pattern exists only for the command itself (`VisibilityTest.the_runner_rejects_a_command_hidden_after_the_input_was_parsed`)
  and for options (`OptionPermissionTest.the_runner_rejects_an_option_whose_permission_is_gone_at_execution`,
  which killed M1). It is missing for argument branches and for parents.
- Mutations: M2 (branch `isVisible` check → `if (false)`) survived; M4 (branch permission) survived; M8 (parent loop
  checks only the command itself) survived.
- A plausible defect missed: a refactor of `runInContext` drops the branch check; every platform that copies or
  converts nodes without requirements, or a permission revoked between parse and execute, then runs the branch.
- Suggested correction: copy the `OptionPermissionTest`/`VisibilityTest` parse-then-revoke tests for (a) a branch
  permission, (b) a branch `visibleWhen`, (c) a hidden parent of an executed child.

### B1-03 — P2 — The tester harness turns a failing assertion inside an executor into a chat message; the test passes

- Harness: `common-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:64-74` catches `CommandFailure`
  and replays it; `CommandRunner.toFailure` (`CommandRunner.java:281-312`) maps any non-CommandLib `Throwable`,
  including `AssertionError`, to UNEXPECTED → `command.failed` + `Check the console for details.`. Same pattern in
  `common/src/test/.../TestCommandRunner.java:80-87` (`executeAndGetResult`/`execute`).
- Demonstrated: a probe in `common-testing` with `execute(ctx -> assertThat(1).isEqualTo(2))` then
  `tester.execute("probe", FakeSender.console())` **passed**; it printed `result=0 messages=[command.failed, Check the
  console for details.]`. It is only "visible" as a stack trace on stderr.
- docs/testing.md:68-70 says unexpected errors are sent to the sender, so this is documented. It is still a trap for
  downstream users, who often assert inside executors, and for this repo: today only `ContextValueTest:177-199`
  asserts inside executors, and it is safe because it uses `dispatch()`, which rethrows.
- Suggested correction (harness, not a test): rethrow `Error`s (at least `AssertionError`) from
  `CommandTester.execute`, or have a strict mode that fails on `CommandError.Kind.UNEXPECTED`; add a
  `CommandTesterTest` for it. Until then, document "do not assert inside executors; capture and assert after
  `execute`".

### B1-04 — P2 — KotlinDslTest "executeSuspend finishes on the sync thread" cannot detect the wrong thread

- Test: `common-kotlin/src/platformTest/.../KotlinDslTest.kt:152-181`. The sync executor is `Runnable::run`, which runs
  inline on whatever thread calls it, and the assertion only checks the final message text.
- A plausible defect missed: `executeSuspend` resumes on `asyncDispatcher` (it never dispatches back to sync). The
  message is still sent and the test still passes. Compare `AsyncExecutionTest:26-50` and
  `StatsExampleTest:146-189`, which record the thread or the executor per step and so would catch it.
- Confidence: high by reading (not mutated; the Kotlin platform test needs the platform build).
- Suggested correction: use a queuing sync executor, or record `Thread.currentThread()` / a sync-flag inside the
  continuation after `withContext`, as `FakeScheduler.isSyncThread()` does.

### B1-05 — P3 — Option-token permission requirement: combined tokens with two permissioned options untested

- Production: `OptionGraph.java:143-154` (`permitted`: the sender must hold *all* nodes of a combined token such as
  `-sz`). Mutation M9 (`allMatch` → `anyMatch`) survived, because `OptionPermissionTest` has only one permissioned flag
  (`--silent`; `--reason` is a value option, so it never forms a combined token).
- Impact is limited: execution still re-checks every given option (M1 was killed), so the defect only leaks the
  tokens into the tree and suggestions. Suggested correction: add a second OP flag and assert `-sz` is hidden from a
  sender holding only one of the two nodes.

### B1-06 — P3 — Rejection tests that do not assert *which* error is sent

- `OptionArgumentTest.java:82-90`: `noneMatch(startsWith("Alex")).isNotEmpty()` for an invalid option value. M10
  (option-argument parse/validation failure sends `Caption.UNEXPECTED_ERROR` instead of the argument's message)
  **survived**; no other test asserts the message of an option-argument failure.
- `OptionalVariadicArgumentTest.java:99-100` (`doesNotContain("unreachable")`) and `:143-144`
  (`doesNotContain("[NORTH]")`) only show that the executor did not run; a silent failure or a generic error passes.
- `AnnotationParserTest.java:228-236` (`calc fail` → `isZero()`) does not check that the IOException reached the
  uncaught handlers or produced the UNEXPECTED messages.
- `CommandLibExtensionTest.java:43`: `isInstanceOf(RuntimeException.class)` accepts an NPE or any bug, and there is no
  positive control showing that the same console *without* the deny succeeds.
- Suggested correction: assert the exact messages (or `CommandError.Kind` through an `errorFormatter`), and for the
  extension test use `hasCauseInstanceOf(CommandSyntaxException.class)` and add the positive control.

### B1-07 — P3 — Structural "not empty" assertions with little contract value

- `OptionRedirectTest.java:24-39` and `OptionArgumentTest.java:58-71` only assert that *some* redirect exists. They
  pin an implementation detail, and they would pass even if the redirects pointed at the wrong hub. The behaviour is
  covered by the transcript tests in the same classes, so these could be dropped. No action is needed beyond that.

### B1-08 — P3 — Harness fidelity limits (documented, but they hide a class of permission bugs)

- `FakeSender.player(...)` and the default `@Sender` grant **every** permission (`FakeSender.java:21-27`,
  `CommandLibExtension.createSender`), and `hasPermission` ignores `DefaultPermission` entirely (an operator gets even
  `NONE`/`NOT_OP` nodes). docs/testing.md:153-187 documents this. Consequence: a downstream test written with the
  defaults can never fail on a permission bug, and `DefaultPermission` semantics are only checked as exported data
  (`permissionConfigs`, docs), never as enforcement. Suggested correction: consider making the defaults
  permission-less, or add a `FakeSender` mode that applies `PermissionConfig` defaults (OP→operator, NOT_OP, NONE).
- `CaptionsTest.the_command_sends_captions_in_the_senders_locale` (`CaptionsTest.java:92-102`) actually changes
  `Locale.setDefault` (the JVM-default fallback). It is restored in `finally`, so there is no leak. The sender-locale
  path is covered by `CommandTesterTest.sender_locale_is_used_as_language`, so only the name is misleading. The
  English assertions throughout `common` depend on `user.language=en`, which is pinned only in Gradle
  (`build.gradle.kts:137-141`), so IDE runs on a ja_JP machine fail. That is a minor note.

### B1-09 — P3 — `../../../../benchmark/baseline.json` time metrics are noise; the gate is meaningful only for allocation

- Throughput scores have errors several times the score (e.g. `ExecuteBenchmark cmd5 Alex` thrpt `0.048 ± 0.201`;
  `RegistrationBenchmark commands=10` avgt `19351 ± 42710 µs`). The run was recorded under ~100% CPU from other builds
  and against library code `647ca7c4`, which is older than the option-redirect commits (docs/benchmarks.md:80-90,151-156).
  `jmhCompare` applies tolerances of ±10–20% to these numbers. `gc.alloc.rate.norm` is the only stable metric.
- `ExecuteBenchmark` never checks that each `@Param` input succeeds (result ≠ 0, no messages). A failure path would
  be benchmarked silently, and `FakeSender` accumulates every sent message, which also skews allocation.
- Suggested correction: re-record on an idle machine at HEAD; add a `@Setup` sanity assertion per input.

## Things checked and found sound

- Cooldown/confirmation: a deterministic `MutableClock` with no sleeps; the boundary at exactly the timeout is
  checked; per-sender keying (uuid vs `type:name`), bypass, reset/resetAll, the cooldown-before-confirmation order and
  inheritance/`disableParentMiddleware` are all exercised. M5, M6, M7 and M14 were killed.
- Async: `FakeScheduler` queues off-thread sync work and is driven explicitly. The tests assert both "not yet" and
  "after" states, and the timeout test joins its thread before draining. No un-awaited futures were found.
- Help filtering by option permission, branch permission and child visibility: M11, M12 and M13 were killed.
- Shared static state: `TestCommandContext.latest` is reset per dispatch. `FunctionCompilationMarkerTest` and
  `SnapshotsTest` clear their system properties in `@AfterEach`. JUnit parallelism is not enabled. No order
  dependence was found. `SnapshotsTest` writes into `src/test/resources/snapshots` and would itself fail if
  `COMMANDLIB_UPDATE_SNAPSHOTS=true` is exported; that is minor.
- Snapshots (`command-docs*.snap`, help snaps): I read them for plausibility. The one removed help line
  (`/a <n> <p>: Run root arguments`) matches the intentional change in commit `59baaa24`. The docs and annotation
  tests also cross-check annotated vs fluent output (tree, json, markdown, transcripts), which guards against
  snapshots that only reproduce the current output.

## Coverage

- Examined in depth: CommonCommandConfirmCooldownTest, AsyncExecutionTest, CommandMiddlewareTest, VisibilityTest,
  ContextValueTest, CommandResultTest, OptionsAnywhereTest, OptionRedirectTest, OptionPermissionTest,
  OptionTreeSizeTest, OptionalVariadicArgumentTest, OptionArgumentTest, CaptionsTest, HelpErrorFormatterTest,
  CommandDocsTest (+ md/json/tree snapshots), SnapshotsTest, FakeScheduler, TestCommandRunner,
  TestCommandContext/PlatformAdapterImpl (both), common-testing CommandTester/FakeSender/CommandLibExtension/
  TestCommandContext, CommandLibExtensionTest, StyleParity, StatsExampleTest, benchmark baseline + ExecuteBenchmark.
- Skimmed: AnnotationParserTest (structure, differential helpers, the async/exception and invalid-usage tests),
  CommandStructureTest/CommonArgumentTest/CommonCommandExecutionTest/CommonCommandHelpSnapshotTest/
  CommonCommandContextTest/CommonCommandRequireTest diffs, CommandOptionTest (names), CommonCommandPermissionTest
  (names), FunctionCompilationMarkerTest, FunctionLoadErrorFilterTest diff, CommandLibDiagnosticsTest,
  CommandTesterTest, KotlinDslTest, BanExampleTest + ban.snap, Trees.
- Not examined: ArgumentErrorMessageTest, CommonArgumentEdgeCaseTest, CommandNodeChildrenTest,
  CommonCommandActorTest, FakeSenderTest, the remaining example tests/snapshots (balance, home, kit, score, team),
  the other benchmark classes, and `common/src/test/.../util`.

## Mutations run (all in the disposable copy, `:common:test`)

| ID | File:line | Change | Result |
|---|---|---|---|
| M1 | CommandRunner.java:118 | skip option-permission check at execution | KILLED — OptionPermissionTest#the_runner_rejects_an_option_whose_permission_is_gone_at_execution |
| M2 | CommandRunner.java:97 | skip branch `isVisible` check at execution | **SURVIVED** |
| M3 | OptionGraph.java:285 | anywhere copies drop the branch requirement (`src -> true`) | **SURVIVED** (killed by the B1-01 probe) |
| M4 | CommandRunner.java:144 | skip branch-permission check at execution | **SURVIVED** |
| M5 | Cooldown.java:144 | ignore bypass permission | KILLED — cooldown_skips_senders_without_unique_id_and_bypassing_senders |
| M6 | Confirmation.java:257 | any pending input confirms | KILLED — confirm_runs_the_second_execution_of_the_same_input |
| M7 | Confirmation.java:254 | never expire pending confirmations | KILLED — confirm_expires_after_the_timeout |
| M8 | CommandRunner.java:90 | visibility check skips parents | **SURVIVED** |
| M9 | OptionGraph.java:153 | combined-token permission `allMatch` → `anyMatch` | **SURVIVED** |
| M10 | CommandRunner.java:137 | option-argument failure sends UNEXPECTED_ERROR instead of its message | **SURVIVED** |
| M11 | HelpMessageAction.java:77 | help lists permissioned options to everyone | KILLED — OptionPermissionTest#help_lists_only_the_options_the_sender_may_give |
| M12 | HelpMessageAction.java:122 | help ignores branch permission | KILLED — CommonCommandPermissionTest#help_hides_argument_branch_without_permission |
| M13 | HelpMessageAction.java:128 | help ignores child visibility | KILLED — VisibilityTest#help_lists_only_what_the_predicate_allows |
| M14 | Cooldown.java:138 | stale-entry sweep removes every sender | KILLED — cooldown_can_be_queried_and_reset |
| M15 | M3 + M4 | branch permission removed from tree and runner (optionsAnywhere bypass) | **SURVIVED** |
| P1 | probe (common-testing) | `assertThat(1).isEqualTo(2)` inside executor via `CommandTester.execute` | test PASSED; result 0, messages `[command.failed, Check the console for details.]` |
