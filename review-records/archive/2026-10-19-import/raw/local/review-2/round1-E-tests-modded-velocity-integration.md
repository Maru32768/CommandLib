# Round 1 / E: tests and test infrastructure (modded, velocity, integration-test, benchmark)

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (HEAD = 6b898637). This was a review only: nothing was edited,
no Gradle task and no Docker target was run. Line numbers are at HEAD. Findings marked **(unconfirmed)** come from
reading the code only. The others were checked against the code, not by running it.

How integration results flow, since that decides what can be vacuous:

- Every fixture test that runs a CommandLib command writes a FAILED "Command was not executed." result in `build()`
  before the command runs.
- `IntegrationTest.assertJUnitReportSucceeded` requires `tests > 0`, `failures == 0` and `errors == 0`, on a report
  that is deleted before the run.

So a command that never runs fails the run. The holes found are of three kinds:

- checks that compare nothing;
- expected values that fall back to the same default as a broken actual value;
- records that only bot-side commands can produce, and the bot runs them after the report is written.

The harness itself is sound. Report file names are unique per target. The `paper-26.3-spigot-jar` target with
`expectedEnableFailure` writes no report. A missing Docker daemon fails rather than skips. Bot timeouts end in `fail`.
The warm-up script exits 1 when the server dies or takes more than 600 s. The Mohist bootstrap checks exit codes. The
modded fixture writes a FAILED result for `/reload` before it runs. The benchmarks write to a volatile sink and reject
an input that fails in `@Setup`.

## Findings

### E-1: test-util-bug, high: the velocity-testing tester cannot reproduce Velocity's permission defaults

- Where: `velocity-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java:54, 92-103`,
  `velocity-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:203-213`.
- What's wrong:
  - On a real proxy, `VelocityPermissions.hasPermission` (`velocity/.../VelocityPermissions.java:27-34`) falls back to
    the registered `DefaultPermission` when the subject's value is `Tristate.UNDEFINED`. With no permission plugin,
    only `DefaultPermission.ALL` nodes are granted. The velocity-3.4.0 integration target asserts exactly this
    refusal.
  - The tester never calls `VelocityPermissions.addDefaults`.
  - `FakeSender` never answers `UNDEFINED`. By default it answers TRUE for every node, so a player runs `OP` commands
    that the proxy refuses. With `permissions(...)` it answers FALSE for every other node, so `ALL`-default parent or
    child commands are refused, when a real permission plugin would leave them unset and grant them.
- Result: downstream tests of `DefaultPermission` on Velocity pass where production fails, and fail where production
  passes.
- Evidence: `grep addDefaults|UNDEFINED` over velocity-testing main finds nothing. Only `CommandLibTest` (through the
  real `CommandLib.register`) exercises `UNDEFINED`.
- Suggested fix:
  - In the `CommandTester` constructor, call `VelocityPermissions.addDefaults(c.permissionConfigs(permissionPrefix))`
    for each command. It is in the same package.
  - Make `permissions(...)` / `denyPermissions(...)` answer `UNDEFINED` for nodes they do not list.
  - Add a "no permission plugin" sender mode that answers `UNDEFINED` everywhere, or make it the default (spec
    question).
  - Add a tester test: an `OP` child is refused and an `ALL` child is allowed for such a sender.

### E-2: test-util-bug, medium: the modded console ignores its permission settings

- Where: `modded-testing/.../FakeSender.java:169-182, 339-361`, `Sender.java` (console with
  `permissions` / `denyPermissions`), and the `CommandTester` class Javadoc ("Permission nodes are answered by the
  executing FakeSender").
- What's wrong: `modded/.../PlatformAdapterImpl.java:28-40` sends only `ServerPlayer` sources to
  `CommandLib.hasPermission`, the only call the tester stubs. A console source returns `true` from `isConsole()`. So
  `FakeSender.console().denyPermissions(..)`, `consoleBuilder().permissions(..)` and
  `@Sender(console = true, denyPermissions = ..)` have no effect.
- `CommandTesterTest.console_builder_denies_listed_permissions_only` (around `:277-288`) checks only
  `FakeSender.hasPermission`. It approves a feature that does nothing at execution.
- Suggested fix: throw `IllegalStateException` from these mutators for the console, and fix the Javadoc. Or answer the
  console from FakeSender too, if consoles with denied nodes are meant to be supported.

### E-3: test-util-bug, medium: the modded fake command source has no level

- Where: `modded-testing/.../FakeSender.java:211-219` (`createSource` passes `null` as the `ServerLevel`).
- What's wrong:
  - `CommandContext.getWorld()` is non-null under NullAway but returns null in tests.
  - `ctx.getLocation()` and `LocationArgument` produce a `Location` with no world, so `getBlockState()` and
    `getChunk()` throw.
  - The tester's `withLevel(..)` is never the source's level.
  - (unconfirmed) `position()` and `level()` are not stubbed on fake players. Selectors that sort by distance or stay
    in one world (`@p` with 2+ players, `@a[distance=..]`) probably throw an NPE. CommandLib turns it into
    "unexpected error", and `execute` returns 0.
- Suggested fix: use the overworld from the tester's levels as the source level. Stub `position()`, `level()` and
  `serverLevel()` on fake players. Add a test that reads `ctx.getWorld()` and resolves `@p` with two players.

### E-4: test-util regression, medium: `ServerLifecycleHooks` is no longer mocked

- Where: the range removed `mockStatic(ServerLifecycleHooks)` from `modded-testing/.../CommandTester.java`. Only
  `CurrentServer` is mocked now (`:245-248`).
- What's wrong: a downstream Forge or NeoForge command that calls `ServerLifecycleHooks.getCurrentServer()` (the
  standard way) now gets null under the tester. `CurrentServer` says it is "Not part of the public API"
  (`modded/.../util/CurrentServer.java:16`), so users have no supported replacement.
- Suggested fix: mock `ServerLifecycleHooks` again on forge and neoforge in `Scope`. Or make `CurrentServer` public
  API and document it as the replacement.

### E-5: test-util-bug, medium (unconfirmed): `CommandLib.refresh` and server-thread work fail in the tester

- Where: `modded/.../CommandLib.java:213` (`player.server` field) and `ServerCommandScheduler.runOnServerThread`.
- What's wrong:
  - On a Mockito `ServerPlayer`, the `server` field is null, so `refresh(player)` throws an NPE. The `visibleWhen`
    Javadoc tells users to call `refresh`.
  - `refreshAll()` and `runOnServerThread` silently drop the task, because `isSameThread()` is false on the mocked
    server.
- Suggested fix:
  - In main code, use `player.getServer()`.
  - In `Scope`, stub `getServer()` on online players, `isSameThread()` to return true, and `getCommands()` with a mock.
  - Add a test.

### E-6: test-vacuous, medium: the Bukkit diagnostics sub-commands are never checked

- Where: `integration-test/shared/paper/.../RuntimeFeatureTest.java:97-101`, and the same code in
  `shared/spigot/.../RuntimeFeatureTest.java`.
- What's wrong:
  - `Bukkit.dispatchCommand(console, "commandlibdiagnostics" + section)` runs inside the `diagnostics` command.
    `ScenarioTest` (`:145`) documents that 1.20.3+ queues a command dispatched from inside another command, so the
    call returns before the sub-command runs.
  - `dispatchCommand` returns false only for an unknown label, and nothing reads the output.
  - The check proves only that the label exists.
- Suggested fix: call the section APIs (`diagnostics().report()`, the docs) directly, or dispatch to a recording
  `CommandSender` from a top-level test command and compare the lines.

### E-7: test-vacuous, medium: the command-block SuccessCount check passes when nothing is stored

- Where: `integration-test/shared/paper/.../VanillaExecutionTest.java:57-62, 178-201`, and the spigot copy.
- What's wrong:
  - The expected value is `#vanilla + ",0,true"`.
  - An unset score reads as 0, and the three `execute store ... data get block` commands run after the reload with
    unchecked results.
  - If all three stores fail, the actual and expected values are both `0,0,true`. The check then passes even when
    CommandLib's SuccessCount is broken. `#failure == 0` is also met by a score that was never written.
- Suggested fix: set `#success`, `#failure` and `#vanilla` to -1 before the stores, as `ScenarioTest` does with
  `#success 7`. Require `#vanilla == 1` except on the known Spigot 1.20.3 target, chosen by version.

### E-8: test-vacuous, low: the execute-store fallback hides regressions

- Where: `shared/paper/.../ScenarioTest.java` and `shared/spigot/.../ScenarioTest.java`,
  `executeStoreReadsResultAndFailure` (`storeWorks ? "42,0" : "0,0"`).
- What's wrong: whenever the vanilla control (`#vanilla == 7`) fails, the `42` check is dropped silently. That
  includes a CommandLib regression that breaks `execute store` for vanilla commands too. The paper suite runs only on
  1.21.0+, so the fallback is never legitimate there.
- Suggested fix: allow `0,0` only for the Spigot version known to need it.

### E-9: test-vacuous, low: the Velocity `OP` refusal cannot be told apart from a node that never registered

- Where: `integration-test/src/test/java/dev/marulabs/commandlib/integration/VelocityIntegrationTest.java:94-96,
  123-127`, and `targets/velocity-3.4.0/src/testPlugin/.../TestPlugin.java:52-55`.
- What's wrong: the test asserts that `denied` is missing from the tree and that `/commandlibtest denied` answers
  "Incorrect argument". Both also hold if the `denied` child failed to register. Nothing ever runs it successfully.
- Suggested fix: have the plugin run `commandlibtest denied` as the proxy console after registering (the console gets
  TRUE) and log a marker, then assert the marker is in the proxy log.

### E-10: test-gap, low: the Velocity integration covers little and ignores proxy errors

- Where: `VelocityIntegrationTest.java`, and `integration-test/src/test/.../ArgumentIntegrationCoverageTest.java:21-51`.
- What's wrong:
  - `PlayerArgument` (the real `proxy.getPlayer` lookup and its suggestions) is not exercised.
  - The proxy log is never scanned for exceptions. CommandLib only prints stack traces of unexpected errors, so a
    failing suggestion provider or async continuation goes unnoticed.
  - The coverage test has no Velocity case.
- Suggested fix:
  - Add `commandlibtest find <bot name>` with a `PlayerArgument`, and its suggestions.
  - Assert the proxy logs contain no `Exception`.
  - Add the velocity argument directory to `ArgumentIntegrationCoverageTest`, pointing at the Velocity `TestPlugin`.

### E-11: test-gap, medium: registering after the server started is untested (modded)

- Where: `modded/.../CommandLib.java:242-252` (Fabric), `:283-295` (Forge and NeoForge 1.18+), `:351-362` (older than
  1.18).
- What's untested: adding commands to a live dispatcher, re-sending the tree to online players, the
  `PERMISSIONS.putIfAbsent` path, the default-resolver fallback (`:340-341`), and `refresh` / `refreshAll`.
- Why: the fixture registers at mod construction (`TestMain.java:107`).
- Also: the code-review pass found that on Fabric, registering during `SERVER_STARTING` hits a null `getPlayerList()`
  at `:248` (unconfirmed).
- Suggested fix: register a second `CommandLib` from `runTests` and probe it, including that the bot's tree contains
  it. Or write a unit test with a mocked `CurrentServer` and a real dispatcher.

### E-12: test-gap, medium: Fabric-only and lifecycle paths are untested

- What's untested:
  - The fabric-permissions-api branch (`CommandLib.java:265-268`). `FabricPermissionTest` covers only the branch where
    it is not loaded, and skips the undeclared-node fallback to `hasPermission(4)`.
  - `CurrentServer`'s `SERVER_STOPPED` handling and its `getGameInstance()` fallback.
  - `RawWordArgumentType.addToClassMap` on fabric-1.21.4, which has no target.
  - CommandLib shaded into a mod, instead of nested.
- Suggested fix: unit tests with `mockStatic(FabricLoader)` / `Permissions` that check the node name and the fallback
  passed, and a `CurrentServer` lifecycle test.

### E-13: test-gap and spec question, medium: modded permission defaults for non-player, non-console sources

- Where: `modded/.../CommandLib.java:189-202` (`hasDefaultPermission`) and `ForgeCommandActor.java:80-90`.
- What's wrong:
  - A command block (level 2) needs level 4 for an `OP`-default node. The old adapter returned `true`, so command
    blocks can no longer run any command left at the default.
  - `ForgeCommandActor.hasPermission` still returns false for every non-player source, including the console. It
    disagrees with the adapter, which grants the console everything. For example, a cooldown bypass check denies the
    console.
- The modded fixture tests only `ALL` and `NONE` from command blocks (`PermissionTest`), and the actor path from the
  console not at all.
- Suggested fix: decide the intended level (vanilla uses 2 for command blocks) and the actor semantics. Add a
  fixture case with an `OP`-default node run from a command block, and an actor `hasPermission` check from the
  console.

### E-14: test-gap, low: `requireConsole` and the UUID error paths are untested (modded)

- What's untested:
  - `Command.requireConsole()` / `consoleRequirer()`: no test anywhere checks that a player gets
    `Caption.REQUIRE_CONSOLE` while the console and RCON pass.
  - The new error paths of `UUIDsArgument` (`INVALID_SELECTOR`, `UNKNOWN_PLAYER_OR_UUID`, `argument.entity.notfound.player`
    for `@a` / `@r` with nobody online) and `UUIDArgument`'s `UNKNOWN_PLAYER_OR_UUID`. Only the `@a` success case is
    covered.
- Suggested fix: add unit tests in `modded-testing`.

### E-15: test-gap, medium: two Spigot-only scenario tests have no Paper copy

- Where: `shared/spigot/.../ScenarioTest.java:214-244` (`unregisteringKeepsALabelTakenSince`, new) and
  `lateRegisteredCommandSurvivesReload` (`spigot/TestMain.java:254`, older than this range).
- What's wrong: Paper registers through a different path, so neither behavior is checked on Paper.
- Suggested fix: port both to `shared/paper/.../ScenarioTest.java`.

### E-16: test-gap, low: the Spigot MessageArgument case no longer exercises selectors

- Where: `shared/spigot/.../ArgumentTest.java:596-600` sends `hello, world!`, while paper (`:604-606`) sends
  `hello, @p!` and expects the player's name.
- Suggested fix: use the paper input on Spigot.

### E-17: test-vacuous, low: weak assertions in the Velocity and modded tester tests

- Where:
  - `velocity-testing/.../CommandTesterTest.java:315` and `CommandLibExtensionTest.java:141-142` (velocity), and
    `modded-testing/.../CommandLibExtensionTest.java:35-39`: a denial is asserted only as
    `isInstanceOf(RuntimeException.class)`, which an NPE also passes. In the modded case, the throw really comes from
    the missing argument-branch node.
  - `velocity-testing/.../CommandTesterTest.java:332-343` ("sends rich text") never checks bold.
  - `CommandTesterTest.java:245-247` checks only `isNotEmpty()`.
  - `modded-testing/.../CommandBuilderTest` compares two outputs that both come from the code under test.
- Suggested fix: assert the cause (`hasCauseInstanceOf(CommandSyntaxException.class)` or the "Command syntax error"
  message), grant the branch node and assert success on the allowed side, and assert the style and the message text.

### E-18: test-util-bug, low: duplicate permission nodes throw

- Where: `velocity-testing/.../FakeSender.java:93, 101` and `modded-testing/.../FakeSender.java:170, 179, 335`.
- What's wrong: `Set.of(varargs)` throws `IllegalArgumentException` on a duplicate node. The nodes come straight from
  user code, including `@Sender(permissions = ...)`.
- Suggested fix: use `Set.copyOf(Arrays.asList(..))` or `new HashSet<>(..)`.

### E-19: test-util-bug, low: default `@Sender` names collide

- Where: `modded-testing/.../CommandLibExtension.java` (around `:70`) and
  `velocity-testing/.../CommandLibExtension.java:71-74`.
- What's wrong: every un-annotated `FakeSender` parameter is named "Player" and registered by name. The second one
  replaces the first in the player lookup (velocity), or loses to it through `putIfAbsent` (modded).
- (unconfirmed) The modded extension's `Store.getOrComputeIfAbsent` also finds a tester in a parent store. One tester
  can then be shared across tests through a `@BeforeAll` parameter, with players piling up.
- Suggested fix: name default senders after the parameter, or fail on a name collision. Refuse resolution outside a
  test-method context.

### E-20: test-util-bug, medium (unconfirmed): the modded tester's scheduler continuations run after the mocks close

- Where: `modded-testing/.../CommandTester.java:424` (`Builder.scheduler`).
- What's wrong: the static mocks are closed when `execute` returns, and `MockedStatic` applies only on the thread that
  created it. So a continuation run from a queued scheduler, which is the use the Javadoc describes, sees no server.
  Its permission checks also bypass `FakeSender`. The Velocity tester keeps its proxy until `close()` for this reason.
- Suggested fix: keep the scope open until `close()`, as the Velocity tester does. Add a queued-scheduler test like
  `velocity-testing/.../AsyncCommandTest.a_follow_up_after_execute_returned_still_sees_the_proxy`.

### E-21: backcompat, low: dead code in the modded test utilities before GA

- `modded-testing` `CommandTester.close()` does nothing, but `CommandLibExtension` wraps the tester in a
  `TesterResource` that implements `Store.CloseableResource`, which JUnit 5.13 deprecates. Store the tester directly
  and drop the wrapper.
- `FakeSender.permissions()` and `denyPermissions()` replace each other, while the Builder combines them so that
  denials win. Pick one semantics and delete the other setter (the same applies to spigot-testing).
- `FakeSender.locale()` is silently ignored on fabric-1.20.1. Throw or document.
- Every fake source has `shouldInformAdmins = false`, so broadcasting to ops cannot be tested.

### E-22: test-flaky, low: fixed tick waits instead of completion

- Where: `shared/folia/.../TestMain.java:52, 166-168` (report written 40 ticks after the console runs `asyncThread`)
  and `shared/paper/.../TestMain.java:34, 212-219` (checks run 5 ticks after the reload is done).
- What's wrong: on a slow, CPU-limited container these produce false failures. They never produce a false pass.
- Suggested fix: write the result when the future completes or `loadRuns > 0` becomes true, and keep a timeout as a
  backstop.

### E-23: improvement, low: dead failure records and lost dispatch details in the Bukkit fixtures

- These FAILED records can never reach the report, because only the bot runs their commands, after the report is
  written:
  - `shared/{paper,spigot}/.../OptionTest.java` `optionPermissionProbe`: "Option without permission was given".
  - `shared/folia/.../TestMain.java:133-139`: `fail(DENIED_PROBE, ..)`.
  The bot checks at `IntegrationTest.java:205, 213` are what cover these. Delete the dead records.
- `TestBase.extractSubCommand` (`shared/paper/.../TestBase.java:109-116`) takes the second word, so
  `execute as ... run commandlibtest X` hooks under `as`. Failing execute-wrapped cases show only "Command was not
  executed." Take the word after `MainCommand.NAME` instead.

### E-24: harness-bug, low: `jmhCompare` can pass while comparing nothing

- Where: `benchmark/build.gradle.kts:101-102, 114-129`.
- What's wrong:
  - A baseline entry with no result (a renamed or removed benchmark or param) is ignored silently.
  - When `--args` replaces the defaults without `-prof gc`, there is no `alloc`. `check` then returns early, so the
    only gated metric is skipped and the task passes.
- Suggested fix: fail, or at least print, when a baseline key has no result or the results lack `gc.alloc.rate.norm`
  while the baseline has it.

### E-25: docs, low: stale statements in the docs

- `integration-test/README.md:198-200` says the test mod executes the commands through Brigadier directly on 1.20.3+.
  `TestMain` uses `performPrefixedCommand` and a queued `reportTests`, which README `:179-181` also describes.
- `integration-test/README.md:187` and `docs/agents/modded.md:131-132` say no Fabric target runs a command block. The
  shared modded fixture places command blocks (`ActorTest.commandBlockActorType`, `PermissionTest`), and the Fabric
  targets run it.
- Suggested fix: delete or correct both statements.

### E-26: spec question, low: Velocity permission defaults are last-wins and never removed

- Where: `velocity/.../VelocityPermissions.java:20-22`, and `CommandLib.unregister` (`velocity/.../CommandLib.java:181-185`).
- What's wrong: `put` means a later registration can open an earlier `OP` node to everyone. Modded uses
  `putIfAbsent`. `unregister` leaves the defaults in place. No test covers either.
- Suggested fix: use `putIfAbsent` (or remove the node on unregister), and add a `CommandLibTest` case.

## Not checked

- Nothing was run: no Docker targets, and no modded or velocity Gradle tasks.
- The null-level `@p` NPE (E-3), the `refresh` NPE (E-5) and the Fabric `SERVER_STARTING` NPE (E-11) are from reading
  the code.
- Statements about vanilla behavior (nested-dispatch queueing, unset scores reading as 0) come from code comments and
  knowledge of Minecraft.
