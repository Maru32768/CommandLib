# Round 1 / D — Tests and test utilities: common, common-testing, common-kotlin, spigot(-testing), paper(-testing)

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (master `6b898637`).
Method: `code-review` skill (high) over the range, then a test-focused pass per area. Mutation proofs ran in
private detached worktrees, which were removed afterwards. The shared worktree was never edited and nothing was committed.
"Confirmed" means proved by a probe test or a mutation run. "Read" means confirmed by reading the code only. "Unconfirmed" means suspected.

Several high findings are main-code bugs that have no test. They are listed here because the task asked which new
behavior is untested. Reviewers A2 and B may report the same main-code bugs.

## High

### D-1 bug + test-gap, high — `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:93-105` (checkVisible)
- **Problem:** if a `visibleWhen` predicate throws after the input was parsed, the raw exception escapes dispatch. The sender gets the platform's internal error instead of "Unknown command", and the uncaught handlers are skipped. `arguments.isVisible(ctx)` at :101 has the same problem.
- **Why the test misses it:** `VisibilityTest.java:71` ("game broken") never reaches the runner, because the node requirement already hides the command at parse time.
- **Evidence:** confirmed. A probe parsed the input, made the predicate throw, then ran `dispatcher.execute(parsed)`, which threw `IllegalStateException`.
- **Fix:** route the check through the catching helper, the way `CommonCommand.isVisibleTo` does, and share that helper with `ArgumentCommandNodeCreator.isVisibleTo` (there are three copies today). Add a test in the style of `the_runner_rejects_a_command_hidden_after_the_input_was_parsed` that uses a throwing predicate.

### D-2 bug + test-gap, high — `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java:117,122` (`isUsable` / `hasArgumentPermission`)
- **Problem, case 1:** these methods call `isVisible` directly. When one child's predicate throws, the parent's whole help fails with `command.failed` instead of listing the other children.
- **Problem, case 2:** a child whose predicate reads a value the child itself provides appears in the tree, but the parent's help fails the same way. The help context carries the parent's providers, not the child's.
- **Evidence:** confirmed by two probe tests.
- **Fix:** use `child.isVisibleTo(ctx)`, which catches the exception and applies the child's providers, and do the same for branches. Add both cases to `VisibilityTest.help_lists_only_what_the_predicate_allows`.

### D-3 bug + test-gap, high — `common/src/main/java/dev/marulabs/commandlib/Confirmation.java:87-95`
- **Problem:** a confirmed forked execution runs only its first target, and the sender is asked to confirm again. With 4 targets, running `all wipe` twice gives `wiped=1, asked=2`.
- **Why the test misses it:** `CommonCommandConfirmCooldownTest.java:48` (`the_targets_of_one_forked_execution_do_not_confirm_each_other`) confirms with a different input (`wipe`), so it never re-runs the forked input.
- **Evidence:** confirmed by a probe test.
- **Fix:** in main, once a parse confirms, remember it as confirmed so that its sibling targets also pass. In the test, run `all wipe` twice and assert 4 runs and 1 prompt.

## Medium

### D-4 bug + test-gap, medium — `common/src/main/java/dev/marulabs/commandlib/Confirmation.java:89`
- **Problem:** a command that reuses one parse can never be confirmed, because `previous.parse == ctx.parse()` on every run. A datapack function reuses its compiled parse in exactly this way.
- **Evidence:** confirmed. Running `dispatcher.execute(parsed)` 3 times gave `wiped=0`.
- **Fix:** tell executions apart by a per-dispatch token instead of node-list identity. Add a test that executes the same `ParseResults` again.

### D-5 test-gap, medium — `common/src/main/java/dev/marulabs/commandlib/Cooldown.java:142-144`
- **Problem:** no test covers the branch that drops a reservation as old as the cooldown.
- **Evidence:** confirmed. With the expiry changed to 1000 times the duration, every test in `CommonCommandConfirmCooldownTest` still passed.
- **Fix:** add a test whose middleware never completes, advance the clock by the duration, and assert that the next execution runs.

### D-6 test-gap, medium — `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java` (many error paths)
- **Problem:** no test anywhere in the repo checks these errors:
  - :410 `@Execute` together with `@Subcommand`
  - :457 and :473 two methods for the same path
  - :522 an empty `@Subcommand`
  - :429 aliases on a path that ends in a placeholder
  - :753 more than one defaultPermission
  - :837 `@Greedy` on a type other than String
  - :876 and :891 variadic with `@Default`, `@Optional` without an empty value
  - :905 and :915 a bad `@Suggest`
  - :978, :1052, :1057, :1062 `@Flag` misuse
  - :1025 more than one `@Requires` equalTo value
  - :1070 and :1078 List options
  - :1102 `@Range` on an option
  - :1179, :1255

  The duplicate-path errors (:457, :473) matter most, because they stop one method from silently overriding another.
- **Evidence:** read (by grep).
- **Fix:** extend the `assertThatThrownBy` tables at `AnnotationParserTest.java:404-413` and `:816-827`, at least for :410, :457, :473, :522, :905, :978 and :1062.

### D-7 test-util-bug, medium — `spigot-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:333` (`Builder.scheduler`)
- **Problem:** an async scheduler that runs tasks on a real thread loses the follow-up without failing anything. Every sender access goes through `NMSClassRegistry.findClass`, and the Mockito static mock is active only on the thread that created the tester. The Builder javadoc does not warn about this. Downstream users will hit it.
- **Evidence:** confirmed. A scratch test used `CommandScheduler.of(Runnable::run, Executors.newSingleThreadExecutor())` with `supplyAsync(...).thenAccept(ctx.sendSuccess)`. `getSentMessageTexts()` stayed empty, and the only output was a bare NPE on stderr.
- **Fix:** wrap the given executors so that a task on another thread fails at once with a clear `IllegalStateException`, or at least document the limit in the javadoc and in `docs/agents/spigot-testing.md`.

### D-8 test-vacuous / test-gap, medium — `spigot/src/main/java/dev/marulabs/commandlib/argument/TimeArgument.java:46`
- **Problem:** no test reaches the fallback `ticks < minimum` check for servers before 1.19.4, because `MockArgumentTypes.time(minimum)` already rejects such values while parsing. `TimeArgumentTest.unknown_unit_and_too_few_ticks_are_syntax_errors` checks only the mock's own message.
- **Evidence:** confirmed. With the condition changed to `ticks < minimum - 1000000`, `TimeArgumentTest` still passed.
- **Fix:** add a test that uses `mockNmsClass(NMSArgumentTime.class, …)` with a type that ignores the minimum, and assert the "Tick count must not be less than…" failure.

### D-9 test-gap, medium — `spigot-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:190` (`e.rethrowAssertionError()`)
- **Problem:** the `execute` javadoc promises that an assertion failing inside an executor fails the test, but no spigot-testing test covers it. common-testing has such a test.
- **Evidence:** confirmed. With the line deleted, `CommandTesterTest` still passed.
- **Fix:** add `execute(ctx -> { throw new AssertionError(); })` with `assertThatThrownBy(...).isInstanceOf(AssertionError.class)` to `CommandTesterTest.Execute`, and check paper-testing for the same gap.

### D-10 test-util-bug, medium — `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:508-528`, `:90`
- **Problem:** when a registry has no `withFakeRegistryValue` entry, `resourceValue` ignores the key and returns a default (`ZOMBIE`, `POOF`, …). An unconfigured test therefore accepts any key as that value, while a real server rejects unknown keys while parsing.
- **Backcompat:** `withFakeEnchantment`, `withFakeParticle`, `withFakePotionEffectType`, `withFakeBiome`, `withFakeAttribute`, `withFakeEntityType` and `withFakeSound` are pre-GA leftovers replaced by `withFakeRegistryValue`. The only remaining caller is `LightweightArgumentExecutionTest.java:125`, and `docs/testing.md:80` and `docs/arguments.md:359-363` still document them.
- **Evidence:** confirmed. `summon minecraft:pig` and `summon bogus:thing` with no setup both resolved to `ZOMBIE`.
- **Fix:** delete the seven withers, the defaults and the fallback, so that an unknown key fails while parsing. Update both docs.

### D-11 test-util-bug, medium — `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:477-498` (selector stubs)
- **Problem:** the selector stubs diverge from real Paper and from spigot-testing's `MockEntitySelector`:
  - A single `EntityArgument` or `PlayerArgument` accepts `@a`.
  - `@s`, `@p`, `@r` and `@e` match nothing.
  - An unregistered name silently sends "not found", where spigot-testing throws "No fake entity registered … call withFakePlayer()".
- **Evidence:** confirmed. With Steve registered, `e @a`, `e @s` and `e Nobody` all executed and sent `argument.entity.notfound.entity`.
- **Fix:** port `MockEntitySelector`'s parse and select rules to the Paper stubs.

### D-12 test-util-bug, medium — `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:315-316`
- **Problem:** the mocked `CommandSourceStack.getLocation()` always returns `Location(null, 0, 0, 0)`, even when the sender is an `Entity` such as a MockBukkit `PlayerMock`. Spigot's `MockNMSCommandListenerWrapper` takes the location from the sender. On Paper, `BlockPositionArgument` and `ctx.getLocation()` therefore always see a null world.
- **Related:** `CommandContextTest` (`entity_is_the_executing_entity_and_location_is_the_source_location`) asserts `getX() == 0.0`, which only tests the mock's own constant.
- **Evidence:** confirmed. For a player at `(world, 5, 6, 7)`, `pos 1 2 3` gave `getWorld() == null`.
- **Fix:** use the sender's location when the sender is an `Entity` and that location is non-null. Make the test assert the sender's location.

### D-13 test-util-bug, medium — `paper-testing/src/test/java/io/papermc/paper/{registry/TestRegistryAccess, TestInternalAPIBridge, plugin/lifecycle/event/types/TestLifecycleEventTypeProvider}.java` and `src/test/resources/META-INF/services/*`
- **Problem:** the ServiceLoader bridges that make `Enchantment`, `Attribute`, `PotionEffectType` and `Biome` mockable are in test scope, so they do not ship. Downstream users cannot build values for `withFakeRegistryValue` for these types without copying the bridges.
- **Evidence:** confirmed. With the `RegistryAccess` service file removed, `EnchantmentArgumentTest` and `AttributeArgumentTest` fail with "Mockito cannot mock this class".
- **Fix:** move the bridges and their service files to paper-testing main, or document the setup in `docs/testing.md`.

### D-14 test-gap (with a suspected main bug), medium — `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:534-544`, `paper-testing/src/test/java/dev/marulabs/commandlib/CommandLibLifecycleTest.java`
- **Problem:** Paper's unregister removes its labels by name. Spigot fixed the same problem with a node-identity check in 743befb1. No test covers two registrations of the same label where the earlier one is unregistered and the later one's `/label` and `plugin:label` must survive.
- **Related:** on Spigot, `CommandLib.java:884` always removes `<plugin>:<label>`, even when a second registration by the same plugin owns it. The integration `ScenarioTest` checks only the plain label.
- **Evidence:** the missing test is read. The main-code bug is unconfirmed (from code review).
- **Fix:** add the two-registration test on Paper and check node identity before removing a label. Also assert the namespaced label in ScenarioTest.

### D-15 test-util-bug, medium — `spigot-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java:148-160`, same in `paper-testing/.../FakeSender.java:141-153`
- **Problem:** the instance methods `permissions(...)` and `denyPermissions(...)` each re-stub `hasPermission(anyString())`, and only the last stub wins. `player("a").permissions("x").denyPermissions("y")` therefore grants everything except `y`. common-testing and `Builder.build()` combine both lists correctly. The bug predates the range, but the code is still shipped.
- **Evidence:** read.
- **Fix:** keep the granted and denied sets as fields and install a single answer, as `Builder.build()` does.

### D-16 test-util-bug / spec-question, medium — `common-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java:117-119`
- **Problem:** in common-testing an operator bypasses an explicit grant list (`operator || allowAll || permissions.contains`). In spigot-testing and paper-testing the list wins. So `@Sender(op = true, permissions = "x")` grants everything in common-testing and only `x` on Spigot and Paper. The shared `Sender` javadoc says "Grants only these permission nodes".
- **Evidence:** read.
- **Fix:** pick one rule (the explicit list wins) and apply it in all three modules.

### D-17 test-util-bug, medium — `spigot-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:168-170`, `nms/argument/MockNMSArgumentDimension.java:15-33`
- **Problem:** `withFakeWorld` stores the raw key, and the mock reads a raw token. `withFakeWorld("minecraft:overworld", w)` followed by input `overworld` fails, although a server accepts it. Commit bb543cff fixed this in paper-testing only.
- **Evidence:** read.
- **Fix:** normalize the key at registration and at lookup, as paper-testing does, and read a resource location.

### D-18 test-gap, medium — `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/Coroutines.kt:83-91` (`startCommand`), `:136` (`addMiddlewareSuspend`)
- **Problem:** the javadoc promises that cancelling reports nothing and that an exception is reported like a synchronous one. Neither is tested. `KotlinDslTest` runs under `:spigot-kotlin:test`, `:paper-kotlin:test` and `:velocity-kotlin:test`, and covers only `CommandPrerequisiteException` and `withTimeout`.
- **Evidence:** read (grep finds no cancellation test).
- **Fix:** add three tests:
  - a cancelled `executeSuspend` sends no messages;
  - `executeSuspend { error("x") }` sends the internal-error message;
  - a suspending middleware around a cancelled `executeSuspend` reports nothing.

## Low

### D-19 test-vacuous (weak), low — `spigot-testing/src/test/java/dev/marulabs/commandlib/CommandLibExtensionTest.java:29-32` (same in paper-testing), `DocumentedErrorBehaviorTest.java:60`, `OptionalVariadicArgumentTest` (`denied_branch…`)
- **Problem:** these permission-denial tests assert only `isInstanceOf(RuntimeException.class)`, and there is no positive control. Any NPE or mocking failure also passes.
- **Evidence:** read. The common-testing version uses `hasCauseInstanceOf(CommandSyntaxException.class)` plus a positive control.
- **Fix:** assert the syntax-error cause or message, and assert that the same input succeeds for a sender who has the permission.

### D-20 test-util-bug, low — `spigot-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:225` (`suggestions()`)
- **Problem:** `suggestions()` still parses with `new Object()` as the source, while `execute()` now passes the sender. A suggestion provider that completes later and calls `ctx.getSender()` falls back to the thread-local and throws.
- **Evidence:** unconfirmed.
- **Fix:** `dispatcher.parse(input, sender)`.

### D-21 test-util-bug, low — `spigot-testing/src/main/java/dev/marulabs/commandlib/nms/argument/MockArgumentTypes.java:100` (`bounds`)
- **Problem:** the bounds use `Double::valueOf` and `Integer::valueOf`, so the mock accepts `1d`, `1e3`, `NaN`, `Infinity` and `+5`. Vanilla `MinMaxBounds` rejects all of them, so such a test passes in the tester and fails on a server.
- **Evidence:** unconfirmed.
- **Fix:** validate each bound against `-?\d+` (int) or `-?(\d+\.?\d*|\.\d+)` (float) before converting.

### D-22 test-util-bug, low — `spigot-testing/src/main/java/dev/marulabs/commandlib/nms/argument/MockNMSArgumentChatFormat.java:24`
- **Problem:** the mock accepts only exact lower-case names. Vanilla `ChatFormatting.getByName` normalizes the input, so `DARK_RED` and `darkred` work on a server but fail in the tester.
- **Evidence:** unconfirmed.
- **Fix:** normalize the input like vanilla does, or document the limit.

### D-23 test-gap, low — `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:684-1000`
- **Problem:** these branches have no unit test:
  - `ownsLabel` and the vanilla-root identity check in `unregisterCommands`;
  - the idempotent `if (!INSTANCES.remove(this)) return;`;
  - the Folia rejection and `requirePrimaryThread` in register and unregister (only the refresh checks are tested).
- **Evidence:** read.
- **Fix:** add tests with mocked Bukkit that an off-thread register or unregister throws and that a second `unregister()` does nothing.

### D-24 test-gap, low — `spigot/src/main/java/dev/marulabs/commandlib/argument/ScoreboardDisplaySlotArgument.java:52`
- **Problem:** no test covers the `belowName` alias used before 1.20.2, because the mock yields only 1.20.2+ names.
- **Evidence:** read.
- **Fix:** add a test-only mock that yields `belowName`.

### D-25 test-gap, low — `common/src/main/java/dev/marulabs/commandlib/Json.java:72-101`
- **Problem:** no test or snapshot reaches the escaping of `"`, `\`, control characters or newlines.
- **Evidence:** read.
- **Fix:** add a description containing `"a\b\n` to `CommandDocsTest` and assert the escaped JSON.

### D-26 test-gap, low — `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:787-790`
- **Problem:** the cooldown reservation release is tested only for a skipping middleware. A middleware or confirmation that completes exceptionally is not tested.
- **Evidence:** unconfirmed.
- **Fix:** add a test whose middleware returns a failed future, then assert that the next run is allowed.

### D-27 spec-question, low — `common/src/main/java/dev/marulabs/commandlib/Cooldown.java:146`, pinned by `CommonCommandConfirmCooldownTest.java:261`
- **Problem:** an execution rejected because another execution holds the reservation is told that the full duration remains, although the cooldown has not started and may never start. The test locks in this message.
- **Fix:** decide on an "already running" message, then update the assertion.

### D-28 test-gap, low — Kotlin DSL: `KotlinDslTest.kt:291-297`, `Dsl.kt:207`, `CurrentCommandContexts.kt:11-15`
- **Problem:**
  - In the require test, `execute { name, ctx -> }` is replaced at once by `executeSuspend`, so the non-suspend value passing is never asserted.
  - `ArgumentBranch.require(...)` is never used.
  - The `IllegalStateException` thrown when an option delegate is read outside a callback is untested.
- **Evidence:** read.
- **Fix:** split the require test into separate branches, and add one `argument { require { } }` case and one `assertThatThrownBy` case.

### D-29 test-gap, low — paper-testing parity with spigot-testing; `paper/src/main/java/dev/marulabs/commandlib/argument/MessageArgument.java` (`resolveSelectors`)
- **Problem:** Paper lacks several Spigot tests:
  - `CommandOptionTest`: a permission-gated option hidden from a combined flag, and repeatable, required and exclusive options;
  - `OptionalVariadicArgumentTest`: an optional player defaulting to the sender, and the optional default shown in help;
  - `CommandLibDiagnosticsCommandTest`: "nothing registered" and the permission prefix.

  Also, nothing tests the case where `MessageArgument.resolveSelectors` gets a `CommandSyntaxException` from `resolveComponent`.
- **Evidence:** read.
- **Fix:** port the Spigot tests, and add a `PaperResolvers` variant whose `resolveComponent` throws.

### D-30 improvement, low — `paper-testing/src/test/resources/snapshots/help-error-formatter.snap:10-11`
- **Problem:** Paper's `FakeSender.getSentMessageTexts()` prints translatable components as raw keys, so the snapshot records `command.unknown.argument` and `…foocommand.context.here`. The Spigot snapshot shows the real text, and downstream snapshots get the same unreadable output.
- **Evidence:** confirmed by comparing the two snapshots.
- **Fix:** render the few vanilla command keys CommandLib emits through a small English table in Paper's `FakeSender`.

### D-31 improvement, low — `spigot-testing/src/test/java/dev/marulabs/commandlib/CommandLibDiagnosticsCommandTest.java:28`
- **Problem:** the asserted line "Typed NMS module: none supports Minecraft 1.13.2" exists only because the static `NMSClassRegistry` mock returns an empty list from `moduleStatus()`. The test asserts a Mockito default.
- **Fix:** stub `moduleStatus()` explicitly in the test, or drop the line from the assertion.

### D-32 improvement, low — duplicated test boilerplate
- **Problem 1:** about 850 lines of tests and 4 snapshots are byte-identical in spigot-testing and paper-testing: the Annotated and Fluent commands, AnnotationStyleTest, AsyncCommandTest, CaptionLocaleTest, CommandBuilderTest, HelpErrorFormatterSnapshotTest, and nearly all of SenderMappingTest and CommandLibExtensionTest. The shipped `CommandLibExtension.java` and `Sender.java` are also identical.
- **Problem 2:** several spigot-testing tests (Time, ScoreboardDisplaySlot, ChatColor, Duration, Registry, EntityType, CommandOption) still use `suggestions(..).get().getList()` + `extracting(Suggestion::getText)`, although the new `tester.suggest(..)` helper exists.
- **Evidence:** confirmed with `diff` and `cmp`.
- **Fix:** keep one shared copy and add it to both modules' source sets, and switch the tests to `tester.suggest`.

### D-33 backcompat / API inconsistency, low — testing modules
- **Problem:** `CommandTester.permissionPrefix()` is public in paper-testing (:219), velocity-testing and modded-testing, but missing in spigot-testing and common-testing. Only common-testing has `executeAndGetContext` and the `CommandTester(TestCommand)` constructor.
- **Evidence:** read (grep).
- **Fix:** delete `permissionPrefix()` where nothing uses it, or add it everywhere. Decide whether `executeAndGetContext` belongs in every tester.

### D-34 test-flaky (latent), low — `common-testing/src/main/java/dev/marulabs/commandlib/TestCommandContext.java:24,33-46`
- **Problem:** the public `latest()` reads one global volatile that every `execute` clears. Under JUnit parallel execution it would return another test's context. Parallel execution is not enabled today.
- **Evidence:** unconfirmed (not reproduced).
- **Fix:** scope it to the tester, or document that it is not safe for parallel tests.

## Checked and fine
- `OptionTreeSizeTest.every_redirect_follows_its_target_in_the_per_player_copy` asserts in a helper that ran 262 times, so it is not vacuous.
- Every snapshot file is referenced by a test.
- `FakeScheduler`, `TestCommandRunner`, `BukkitCommandSchedulerTest`, `CommandTreeRefreshTest`, `NMSArgumentTest`, `NMSClassRegistryTest.moduleStatus` and `NMSDedicatedServerTest` hold up.
- The other new spigot-testing mocks match vanilla syntax: time, ranges, scoreboard slots, NBT and resource keys.
- `CommandLibExtension`'s store and lifecycle hold up.

## Not checked
- The Folia path of `FoliaCommandScheduler.forServer`.
- The `MessageComponentSerializer` deserialize path, which needs a server.
- `CommandDocs`, `CommandLibDiagnostics`, `ContextKey` and `ChainedCommandContext` were not read closely.
- `CommandLibExtension` on JUnit 5.8.1 at runtime, and its use in `@Nested` classes.
- `OptionGraph` MAX_NODES not counting `optionsAnywhere` copies (`OptionGraph.java:216`, from code review) has no test. It is unconfirmed and left to reviewer A.
