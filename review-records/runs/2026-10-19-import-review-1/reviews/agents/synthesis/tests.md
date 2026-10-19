# Test suite findings

Two independent Opus auditors: B1 (common, common-testing, common-kotlin, examples, benchmark artefact) and B2
(spigot/paper/velocity/modded testing modules, platform unit tests, integration-test, samples). Both ran the suites and
mutation experiments in disposable copies of HEAD; no tracked file was changed. Text is kept unedited; consolidated
severities are in [`findings.md`](findings.md).

Suite baseline observed by the auditors at HEAD: `:common:test` 377 tests pass; spigot, paper, velocity,
spigot-testing, paper-testing and velocity-testing unit suites pass. Modded suites and Docker integration targets were
not run (shared Loom cache risk / Docker load) and were reviewed statically.


---

## B1 — common/core test-suite audit

Reviewed: `2c8ffd7a..1b8f17dc` (HEAD). Scope: `common/src/test/**`, `common-testing/src/{main,test}`,
`../../../common-kotlin/src/platformTest` (KotlinDslTest), `examples/src/test`, `benchmark/baseline.json`.
Method: reading + mutation runs in a disposable `git archive HEAD` copy (`:common:test`, 377 tests green at
baseline; Error Prone `-Werror` removed in the copy only so trivially-constant mutants compile). The copy has been deleted.

Overall: the new suites are mostly contract-shaped (explicit expected messages, fake clock instead of sleeps, a
hand-driven `FakeScheduler` for async, annotated-vs-fluent differential checks). Cooldown/confirmation, help
filtering and option-permission tests killed every mutant aimed at them. The gaps cluster around **argument-branch
permission/visibility enforced at execution time** and **`optionsAnywhere()` × permissions**, plus a harness
behaviour that silently turns assertion failures inside executors into chat messages.

### Findings

#### B1-01 — P1 — `optionsAnywhere()` × branch permission has no test; with the runtime check also untested, a full permission bypass goes undetected

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

#### B1-02 — P2 — Execution-time re-checks (branch permission, branch visibility, parent visibility) are untested

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

#### B1-03 — P2 — The tester harness turns a failing assertion inside an executor into a chat message; the test passes

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

#### B1-04 — P2 — KotlinDslTest "executeSuspend finishes on the sync thread" cannot detect the wrong thread

- Test: `common-kotlin/src/platformTest/.../KotlinDslTest.kt:152-181`. The sync executor is `Runnable::run`, which runs
  inline on whatever thread calls it, and the assertion only checks the final message text.
- A plausible defect missed: `executeSuspend` resumes on `asyncDispatcher` (it never dispatches back to sync). The
  message is still sent and the test still passes. Compare `AsyncExecutionTest:26-50` and
  `StatsExampleTest:146-189`, which record the thread or the executor per step and so would catch it.
- Confidence: high by reading (not mutated; the Kotlin platform test needs the platform build).
- Suggested correction: use a queuing sync executor, or record `Thread.currentThread()` / a sync-flag inside the
  continuation after `withContext`, as `FakeScheduler.isSyncThread()` does.

#### B1-05 — P3 — Option-token permission requirement: combined tokens with two permissioned options untested

- Production: `OptionGraph.java:143-154` (`permitted`: the sender must hold *all* nodes of a combined token such as
  `-sz`). Mutation M9 (`allMatch` → `anyMatch`) survived, because `OptionPermissionTest` has only one permissioned flag
  (`--silent`; `--reason` is a value option, so it never forms a combined token).
- Impact is limited: execution still re-checks every given option (M1 was killed), so the defect only leaks the
  tokens into the tree and suggestions. Suggested correction: add a second OP flag and assert `-sz` is hidden from a
  sender holding only one of the two nodes.

#### B1-06 — P3 — Rejection tests that do not assert *which* error is sent

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

#### B1-07 — P3 — Structural "not empty" assertions with little contract value

- `OptionRedirectTest.java:24-39` and `OptionArgumentTest.java:58-71` only assert that *some* redirect exists. They
  pin an implementation detail, and they would pass even if the redirects pointed at the wrong hub. The behaviour is
  covered by the transcript tests in the same classes, so these could be dropped. No action is needed beyond that.

#### B1-08 — P3 — Harness fidelity limits (documented, but they hide a class of permission bugs)

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

#### B1-09 — P3 — `../../../benchmark/baseline.json` time metrics are noise; the gate is meaningful only for allocation

- Throughput scores have errors several times the score (e.g. `ExecuteBenchmark cmd5 Alex` thrpt `0.048 ± 0.201`;
  `RegistrationBenchmark commands=10` avgt `19351 ± 42710 µs`). The run was recorded under ~100% CPU from other builds
  and against library code `647ca7c4`, which is older than the option-redirect commits (docs/benchmarks.md:80-90,151-156).
  `jmhCompare` applies tolerances of ±10–20% to these numbers. `gc.alloc.rate.norm` is the only stable metric.
- `ExecuteBenchmark` never checks that each `@Param` input succeeds (result ≠ 0, no messages). A failure path would
  be benchmarked silently, and `FakeSender` accumulates every sent message, which also skews allocation.
- Suggested correction: re-record on an idle machine at HEAD; add a `@Setup` sanity assertion per input.

### Things checked and found sound

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

### Coverage

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

### Mutations run (all in the disposable copy, `:common:test`)

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

---

## B2 — Platform test modules and integration tests (GA audit)

Reviewer: Agent B2. Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (uncommitted comment-only edits ignored).
Scope: spigot-testing, paper-testing, velocity-testing, modded-testing (static), spigot/paper/velocity `src/test`,
integration-test (runner + shared spigot/paper/modded fixtures, static only), samples tests.

Method: read harnesses, fixtures, runner and the production code they stand in for; baseline run of
`:spigot-testing:test :paper-testing:test :velocity-testing:test :spigot:test :paper:test :velocity:test`
(`-Pcommandlib.typedNms=false`) in a disposable `git archive` copy — all green (spigot-testing 213, paper-testing 151,
velocity-testing 28, spigot 130, paper 7, velocity 5 tests); then single-site mutations of production code, one at a
time, each re-running the module's unit tests plus its testing module. Modded and Docker targets were reviewed
statically only (not built, not run).

### Findings

#### B2-01 — P1 — Forge/NeoForge player permission denial is verified nowhere; the integration check passes vacuously

- Runner: `integration-test/src/test/java/dev/marulabs/commandlib/integration/IntegrationTest.java:193-203` sends
  `/commandlibtest deniedProbe` for **every** platform (it is outside the `isBukkitPlatform` guard) and accepts
  `Incorrect argument` / `command.unknown.argument` / `Unknown or incomplete command`.
- Modded fixture: `../../../integration-test/shared/modded/src/main/java/dev/marulabs/testmod` defines **no** `deniedProbe`
  (only the Bukkit fixtures do, `shared/spigot/.../ScenarioTest.java:226-236`). On Forge/NeoForge the bot therefore
  gets Brigadier's "unknown argument" for a sub-command that does not exist, which satisfies the predicate, and
  `noneMatch("deniedProbe executed")` is trivially true. The modded fixture uses only `DefaultPermission.ALL`
  (grep), so no modded integration case exercises a denied node at all.
- Unit harness: `modded-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:249-256` replaces
  `CommandLib.hasPermission(source, ServerPlayer, node)` with a static mock answered by `FakeSender`, and `<1.18`
  mocks `PermissionAPI.hasPermission`. The production Forge/NeoForge resolution in
  `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:313-324` (the `"commandlib." + node` key, the
  `PermissionAPI.getRegisteredNodes()` check, the default-resolver fallback, level 4 for an unknown node) is never
  executed by any test. `FabricPermissionTest` covers only the Fabric branch (`:242-252`).
- Claim vs reality: `../../../docs/compatibility.md` records "2026-10-16 に 4 target とも全 case が通った" for Forge and both
  NeoForge targets, and the roadmap marks permission-aware execution `[x]`. The only player-side permission check on
  those targets is the vacuous one.
- Defect it would miss: `hasPermission` returning `true` (or looking up the node without the `commandlib.` prefix,
  so every lookup falls into `source.hasPermission(4)` and an op bot passes while non-ops are locked out of
  `DefaultPermission.ALL` commands) — every OP/NONE command usable by every player on a real Forge/NeoForge server,
  with all unit and integration tests green. Not mutated (modded builds are off-limits); confidence high from source.
- Fix: add `deniedProbe` (DefaultPermission.NONE) to the modded fixture, and either guard the bot check by platform
  or assert that the sub-command exists (e.g. a control sub-command with the same parent that must reply). Add a
  Forge/NeoForge unit test of `CommandLib.hasPermission` like `FabricPermissionTest` (registered node, unregistered
  node with default resolver, unknown node → level 4).

#### B2-02 — P2 — The testers never run the platform's failure-message path; joining/rendering bugs survive

- `CommandFailure` (`common/.../CommandFailure.java:12-64`) carries two things: `message` (the server's
  `toFailureMessage` component, read via `getRawMessage()` by the platform) and `sends` (replayed by testers).
  Every tester catches it and calls `e.sendMessages()` (spigot-testing `CommandTester.java:187-189`, paper-testing
  `:150-152`, velocity-testing `:118-120`, modded-testing `execute`). No test in the platform/testing modules calls
  `getRawMessage()` / `toFailureMessage`.
- Production paths untested: `spigot/.../CommandContext.java:162-172`, `paper/.../CommandContext.java:201-204`,
  `velocity/.../CommandContext.java:154-158`, `modded/.../CommandContext.java:197`.
- Mutations: M2 (Paper joins only the first message) **survived**; M4 (Velocity, same) **survived**; M3 (Spigot drops
  the newline between messages) **survived**. Integration checks only single-message failures (`captionProbe`), not
  on Velocity (no target) and not on mohist-1.20.1 (B2-03).
- Real defect missed: a multi-line failure (validator message + usage from `ErrorFormatter.SYNTAX_ADVICE`, or two
  `sendFailure` calls in a prerequisite) reaches real players truncated or run together, while every tester shows
  the right lines.
- Fix: a platform unit test per module that builds a failure with ≥2 messages and asserts the `getRawMessage()`
  component (Paper/Velocity: Adventure join with newlines; Spigot: through a mock `NMSCommandListenerWrapper`); or an
  opt-in tester mode that renders `getRawMessage()` instead of replaying `sends`.

#### B2-03 — P2 — mohist-1.20.1 bot assertions accept "Unknown command", turning four checks into no-ops

- `IntegrationTest.java:175-221` (commit ad060861): for `mohist-1.20.1` the incorrect-argument, converted-CSE,
  option-permission and caption checks also pass on `"Unknown command. Type"`.
- What still holds: `optionMessages.noneMatch("secret=true")` still catches a wrongly accepted option, because the
  probe's executor uses `sendFailure` (a plain red message, not a `CommandFailure`), which Mohist delivers.
- What is lost on that target: any verification that CommandLib's own error path (incorrect-argument message,
  `ArgumentParseException` conversion, prerequisite captions and the player-locale caption lookup) works at all. A
  regression that, e.g., throws inside the caption lookup or inside CSE conversion shows up only as "Unknown command"
  and passes. The underlying user-visible defect (no CommandLib failure message ever reaches a Mohist 1.20.1 player) is
  documented (`docs/compatibility.md:37`, roadmap `[ ]` item), yet the same row says "2026-10-16 に両方で全 case が
  通った".
- Assessment: the relaxation reflects a real Mohist behaviour (the commit message cites vanilla `/gamemode
  not_a_mode`), so it is not masking a CommandLib bug by itself; the problem is that it accepts *either* outcome, so
  it can neither detect a CommandLib regression there nor notice when the Mohist limitation goes away.
- Fix: for mohist-1.20.1 require the Unknown-command reply exactly (known-failure expectation) and verify the
  CommandLib side through a channel Mohist does not swallow (the console-driven report, e.g. a probe that records the
  caption text resolved for a player-locale context); state in compatibility.md that these four checks are
  expectation-inverted on that target rather than "全 case が通った".

#### B2-04 — P2 — paper-testing's WorldArgument mock takes the world *name*; real Paper takes the dimension *key*

- `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:291-294` keys fake worlds by
  `world.getName()` and `:324-325` resolves the token against that map.
- `paper-testing/src/test/.../argument/WorldArgumentTest.java:14-27` asserts that `tp world` resolves — input that a
  real Paper server rejects (Paper's `ArgumentTypes.world()` parses a dimension resource location; `world` becomes
  `minecraft:world`). The Paper integration case uses `minecraft:overworld` (`shared/paper/.../ArgumentTest.java:1020`),
  which the mock rejects with "Unknown value". spigot-testing keys by dimension key (`withFakeWorld(key, World)`), and
  `docs/arguments.md:354` says paper-testing is "by key" — the doc and the implementation disagree.
- Probe in the disposable copy: see "Harness probes" below.
- Defect it hides: a plugin's tests (and suggestions/help text written from them) use world names; on the server every
  such input fails.
- Fix: key by `world.getKey().toString()` (or accept both, namespaced first), add a `withFakeWorld(key, world)`
  overload like Spigot's, and change `WorldArgumentTest` to the server's syntax.

#### B2-05 — P2 — paper-testing mocks accept or fail differently from the server (game mode, entities `@a`)

- `CommandTester.java:316-317`: `GameMode.valueOf(token.toUpperCase(Locale.ROOT))` accepts `SURVIVAL` / `Creative`
  (vanilla's `gamemode` argument accepts only the lower-case names). An unknown token does end as a
  `CommandSyntaxException` (probe), so only the over-acceptance is wrong. Spigot-testing runs the real parser.
- `:466-475`: `entities` with `@a` returns every fake entity (zombies included); vanilla `@a` selects players only.
  Probe: with only a zombie registered, `ents @a` executes with 1 entity; a server would select no one.
- Severity is P2 only because these mocks are the documented way to test Paper plugins; each is a case where a
  user's test passes for input the server treats differently.
- Fix: accept only the lower-case names; filter `@a` to players for `entities`.

#### B2-06 — P2 — Paper parity: ten Paper arguments are never executed in paper-testing, and their failure paths are untested anywhere

- Arguments with no execution test in paper-testing (only constructed by `ArgumentCoverageTest.java:146-161`, which
  asserts "does not throw" while building a tester): `AdvancementArgument`, `AttributeArgument`, `BiomeArgument`,
  `EnchantmentArgument`, `EntityTypeArgument`, `LootTableArgument`, `ObjectiveArgument`, `PotionEffectArgument`,
  `RecipeArgument`, `SoundArgument`. Spigot-testing has a `*Test` per argument and enforces it
  (`spigot-testing/.../ArgumentCoverageTest.java:46-70`); paper-testing has no such rule.
- These are not thin wrappers: e.g. `paper/.../argument/ObjectiveArgument.java:25-60` has its own suggestion filter
  (`containsIgnoreCase`) and not-found failure; `AdvancementArgument.java:15-35` has Bukkit lookup + translatable
  failure. Integration (`shared/paper/.../ArgumentTest.java`) covers only the happy path.
- Mutations: M7 (Paper advancement not-found key) **survived**; M1b (Paper objective not-found key) **survived**.
  The same mutations on the Spigot classes (M9/M10) are killed by `KeyedArgumentFailureTest`.
- Fix: add a paper-testing `*Test` per argument (happy path, not-found failure message, suggestions), mirroring the
  Spigot tests, and give paper-testing the same "every argument has a test" rule.

#### B2-07 — P2 — Spigot lifecycle (register / unregister / plugin disable) has no unit tests; plugin-disable is untested anywhere

- `../../../spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java` (~800 lines) — `register`, `unregister(boolean)`
  (`:715`), `onPluginDisable` (`:650-680`, including the function recompilation) — has no test in `spigot/src/test`
  or `spigot-testing`. Paper has 20 lifecycle unit tests (`paper-testing/.../CommandLibLifecycleTest.java`).
- Integration covers register → run → unregister on Spigot/Paper from the console
  (`shared/spigot/.../ScenarioTest.java:137-200`) and late registration surviving reload (Spigot fixture only;
  the Paper fixture lacks `lateRegisteredCommandSurvivesReload`), but no fixture disables a plugin, so the
  `onPluginDisable` path (removal of every instance, function recompilation, filter uninstall) runs nowhere.
- Defect it would miss: commands of a disabled/reloaded plugin left in the dispatcher (stale classloader,
  `NoClassDefFoundError` on use) or a recompile on a stopping server.
- Fix: a fixture case that registers through a second registration and fires/forces the disable path, or a unit test
  with the NMS wrappers mocked as `CommandTreeRefreshTest` does; port `lateRegisteredCommandSurvivesReload` to the
  Paper fixture.

#### B2-08 — P3 — Folia: detection and the real scheduler are untested; the tests check routing only

- `FoliaCommandSchedulerTest.java:60-138` verifies which mocked scheduler receives a task. The detection branch
  (`FoliaCommandScheduler.java:30-36,54-66,115-127`) is only checked negatively (`the_test_server_is_not_folia`).
  M6b (wrong `RegionizedServer` class name → Folia never detected → `BukkitCommandScheduler`, which throws on Folia)
  **survived**. There is no Folia integration target (documented as best effort, roadmap `[ ]`).
- Fix: a test that loads a stub `io.papermc.paper.threadedregions.RegionizedServer` in an isolated class loader and
  asserts `forServer` returns the Folia scheduler; the Folia target already on the roadmap.

#### B2-09 — P3 — Harness permission semantics differ from every server (documented, but no realistic mode)

- All `FakeSender`s grant every node by default and ignore `PermissionDefault` / `DefaultPermission`
  (spigot-testing `FakeSender.java:86-89`, paper-testing `:80-83`, velocity-testing `:54`, modded `:48`). A command
  left at the default OP permission, or `PermissionDefault.FALSE`, runs for a "non-operator player" in tests. On
  Velocity a real player gets `Tristate.UNDEFINED` → only `DefaultPermission.ALL` (`VelocityPermissions.java:28-34`).
  Documented in docs/testing.md:153-166, so not a bug, but no test helper reproduces the server's defaults.
- Integration does not fill the gap: the bot is op level 4 (`IntegrationTest.java:384-387`), and the only denial
  probe uses `FALSE`; "OP default denies a non-op player" is not tested on any real server.
- Fix: a `FakeSender` mode that answers from the registration's permission defaults and `isOp`; a non-op bot case.

#### B2-10 — P3 — velocity-testing's proxy is a static swapped only during `execute`

- `velocity-testing/.../CommandTester.java:113-126,138-151` sets the static `CommandContext.proxy`
  (`velocity/.../CommandContext.java:22-23`) only for the call. A queued async follow-up (the pattern docs/testing.md
  recommends) that calls `ctx.getProxy()` after `execute` returns throws `IllegalStateException` in tests although it
  works on a proxy; two testers on parallel threads overwrite each other. No test covers it.
- Fix: keep the proxy set for the tester's lifetime (restore in `close`) or bind it to the context.

#### B2-11 — P3 — Integration coverage guard checks only that `new XArgument(` appears in the fixture source

- `ArgumentIntegrationCoverageTest.java:60-106` passes if the class name is instantiated anywhere in `ArgumentTest.java`
  (a comment-free regex), not that the case is dispatched, has `putCommandNotExecutedResult`, or asserts a value. The
  runner (`IntegrationTest.java:331-342`) only requires `tests > 0` and zero failures, so a fixture case that silently
  stops being built drops out of the report without failing. The modded fixture passes this guard although
  `deniedProbe` is absent (B2-01).
- Most of the fixture cases run as the console (`shared/spigot/.../TestMain.java:125-131`), while
  integration-test/README.md says "A real player connection exercises the server command path"; only the seven bot
  checks run as a player.
- Fix: assert an expected minimum/explicit list of report keys per platform; note the console/player split in the
  README.

#### B2-12 — P3 — Spigot source location/rotation mapping is integration-only

- spigot-testing's `MockNMSCommandListenerWrapper.getBukkitLocation` (`:69-74`) overrides the production
  `NMSCommandListenerWrapper.getBukkitLocation` (`spigot/.../NMSCommandListenerWrapper.java:62-73`), so the
  yaw/pitch mapping has no unit test. M8 (yaw ← `rotation.x()`) **survived** the unit suites; the integration case
  `execute positioned 10.25 70.5 -20.75 rotated 90 45` (`shared/spigot/.../VanillaExecutionTest.java:99,204`) would
  catch it. Acceptable, recorded for completeness.

### Things checked and found sound

- Spigot-testing selector mock rejects `@a`/`@e` for single arguments and `@e` for player arguments at parse time,
  and fails loudly for an unregistered name (documented); `MockArgumentTypes` range/time/resource parsing mirrors
  vanilla closely enough for its documented scope.
- Fixture result integrity: `putCommandNotExecutedResult` pre-fails each case; rejected-input cases use a control
  input plus an `AtomicBoolean`, so "never executed" cannot pass (`shared/spigot/.../OptionTest.java:152-200`).
- Report parsing waits for a parseable file, fails on Docker absence, and checks the log for CME and for function
  load errors after the reload marker (`IntegrationTest.java:113-167`).
- Bukkit/Paper/Velocity/modded scheduler unit tests verify the thread hand-off with mocks
  (`BukkitCommandSchedulerTest`, `ServerCommandSchedulerTest`, `VelocityCommandSchedulerTest`), and the real Bukkit
  thread hand-off is asserted in the fixture (`RuntimeFeatureTest.executeAsyncAndMiddleware`).
- Paper bootstrap permission defaults are unit tested (`CommandLibBootstrapTest.java:74-110`).
- No `@Disabled`, sleeps, timeouts or swallowed results in the unit suites; only the two coverage tests rely on
  "does not throw".
- The 29-target count in the roadmap matches the list in `../../../docs/compatibility.md` (10+10+1+2+4+2). The run
  reports themselves are not stored in the repository, so the claim is not reproducible from it.
- Spigot and Paper snapshot files are identical except `help-error-formatter.snap`, where Paper's flattened
  translatable keys (`command.unknown.argument`, `...foocommand.context.here`) are an artefact of the plain-text
  serializer, not wrong output.

### Harness probes (disposable copy)

A throw-away JUnit test added to the copy's paper-testing (one tester with `GameModeArgument`, `WorldArgument`
with a fake world named `world`, `EntitiesArgument` with one fake zombie `z`, sender a FakeSender player) printed:

| Input | paper-testing result | Real Paper server |
|---|---|---|
| `gm SURVIVAL` | executes, `SURVIVAL` | rejected (lower-case names only) |
| `gm Creative` | executes, `CREATIVE` | rejected |
| `gm foo` | `RuntimeException` caused by CSE | rejected (CSE) — consistent |
| `tp world` | executes, world `world` | rejected (`minecraft:world` is not a dimension) |
| `tp minecraft:overworld` | `RuntimeException` caused by CSE | executes (the integration fixture's input) |
| `ents @a` | executes with 1 entity (the zombie) | `@a` selects players only |

### Mutation table

Each mutation was applied alone in the disposable copy and the listed tasks were run (`-Pcommandlib.typedNms=false`).

| ID | Site | Change | Tasks | Result |
|---|---|---|---|---|
| M1 | `paper/.../argument/ObjectiveArgument.java:55` | `if (objective == null)` → `if (false)` | paper, paper-testing | killed at compile (NullAway) — replaced by M1b |
| M1b | same file `:56` | translation key `arguments.objective.notFound` → `arguments.objective.missing` | paper, paper-testing | **survived** |
| M2 | `paper/.../CommandContext.java:203` | `Component.join(newlines(), messages)` → `messages.get(0)` | paper, paper-testing | **survived** |
| M3 | `spigot/.../CommandContext.java:167` | drop `joined.addExtra("\n")` | spigot, spigot-testing | **survived** |
| M4 | `velocity/.../CommandContext.java:157` | join → `messages.get(0)` | velocity, velocity-testing | **survived** |
| M5 | `paper/.../CommandPermissions.java:38-39` | remove `|| Bukkit.getPluginManager().getPermission(name) != null` (a permission registered with the plugin manager no longer overrides the bootstrap default) | paper, paper-testing | **survived** (first attempt aborted by an external `gradle --stop`; re-run clean) |
| M6 | `paper/.../FoliaCommandScheduler.java:56` | `if (isGlobalTickThread == null)` → `if (true)` | paper, paper-testing | killed at compile (NullAway) — replaced by M6b |
| M6b | same file `:119` | class name `...RegionizedServer` → `...RegionizedServerX` (Folia never detected) | paper, paper-testing | **survived** |
| M7 | `paper/.../argument/AdvancementArgument.java:31` | key `advancement.advancementNotFound` → `advancement.notFound` | paper, paper-testing | **survived** |
| M8 | `spigot/.../util/nms/command/NMSCommandListenerWrapper.java:70` | yaw from `rotation.x()` | spigot, spigot-testing | **survived** (integration `VanillaExecutionTest` would catch) |
| M9 | `spigot/.../argument/ObjectiveArgument.java:56` | same key change as M1b | spigot, spigot-testing | killed (`KeyedArgumentFailureTest`) |
| M10 | `spigot/.../argument/AdvancementArgument.java:33` | same key change as M7 | spigot, spigot-testing | killed (`KeyedArgumentFailureTest`) |

M9/M10 against M1b/M7 is the parity gap of B2-06 in one line: the same mutation is caught on Spigot and survives on
Paper. M5 adds a Paper bootstrap-permission branch to the list of untested paths (`CommandLibBootstrapTest` never
registers a `Permission` with the mocked plugin manager).

### Coverage summary

| Area | Unit (harness) | Real server | Gap |
|---|---|---|---|
| Spigot arguments | per-argument tests, enforced | 10 Spigot + 5 spigot-module Paper + Mohist targets | none found (M9/M10 killed) |
| Paper arguments | 17 with tests; 10 only constructed | 5 paper-module targets, happy path only | B2-06 |
| Failure message rendering | replayed, never rendered | single-message only, not Mohist 1.20.1 | B2-02, B2-03 |
| Permissions | FakeSender grants all; modded mocks production check | op bot; FALSE probe on Bukkit only | B2-01, B2-09 |
| Lifecycle | Paper thorough; Spigot none | register/unregister/reload from console | plugin disable (B2-07) |
| Folia | mocked routing | none | B2-08 |
| Velocity | tester + scheduler | none (manual 2026-10-15) | B2-02, B2-10 |
| Fabric | modded-testing + Fabric permission unit test | none | — |
