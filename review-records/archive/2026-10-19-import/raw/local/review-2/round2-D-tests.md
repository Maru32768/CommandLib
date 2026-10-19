# Round 2 D: tests and test utilities

Scope: every test and test utility module, reviewed at master 0ccd8a27. The focus was the round-1 fix commits
6b898637..HEAD. Items already in triage.md are not repeated unless their fix is incomplete.

## FakeSender permission semantics and CommandLibExtension

### D2-1 test-util-bug, medium: chained permissions()/denyPermissions() on a modded-testing or velocity-testing sender replace each other
- **Where:** `modded-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java:172,184` and `velocity-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java:96,113`.
- **Problem:** in both modules, each instance setter installs a whole new check, so whichever setter runs last wins. For example, `FakeSender.player("a").permissions("x").denyPermissions("y")` grants every node except `y`, including nodes that were never granted. The modded builder and every `@Sender` combine the two lists, and velocity's package-private `permissions(String[], String[])` does too. So the same configuration behaves differently in the same module depending on whether it is written as a chain, with the builder, or with the annotation. common-, spigot- and paper-testing (D-15) keep the granted and denied sets apart, and there `denyPermissions` accumulates. A test written for one artifact and ported to another silently grants more.
- **Status vs round 1:** triage lists the "replace or combine" question for E-21 as Pending. The D-15 row, however, says its policy ("keep grant and deny separately, each setter replaces only its own set") also applies to E-21. Only Spigot, Paper and common were brought in line. This is the unfinished half.
- **Evidence:** read. Each setter assigns `permissionCheck`, or calls `answerPermissions(...)`, with a lambda that ignores the other set.
- **Fix:** store `@Nullable Set<String> granted` and `Set<String> denied` and compute the answer from both: denied wins, and `granted == null` means everything (on velocity it means TRUE, otherwise UNDEFINED). Make `denyPermissions` accumulate. Then delete velocity's package-private two-array overload and have the extension call the two setters. Update the Javadoc ("replacing any earlier call", "Grants every permission node except…") and the `docs/testing.md:195` wording "`denyPermissions(nodes...)` (all but these)". Add a test that chains both setters to modded- and velocity-testing, copying `granted_and_denied_permissions_combine_and_a_new_grant_list_keeps_the_denied_nodes`.

### D2-2 test-util-bug, low: common-testing's CommandLibExtension still shares one tester across a class through @BeforeAll and constructors
- **Where:** `common-testing/src/main/java/dev/marulabs/commandlib/CommandLibExtension.java:50`.
- **Problem:** spigot-, paper-, velocity- and modded-testing now refuse to resolve a parameter when `extensionContext.getTestMethod()` is empty. common-testing does not. A tester resolved for `@BeforeAll` or a constructor goes into the class store. Every test's `getOrComputeIfAbsent` finds it through the parent store, so all tests share one tester and its cooldown and confirmation state. This is the same reason given in 1123131b. The class Javadoc even advertises "test and lifecycle methods".
- **Evidence:** read. The code matches the pre-fix Spigot version; JUnit store lookups fall back to parent stores.
- **Fix:** add the same guard and message, and copy `CommandLibExtensionRefusalTest.refuses_a_before_all_method` to common-testing. The class has no player registry, so the name-collision half does not apply.

### D2-3 test-util-bug, low: spigot- and paper-testing look up player and entity names case-sensitively
- **Where:** `spigot-testing/.../CommandTester.java:63` (`getFakeEntity`, used by `MockEntitySelector`:76), `paper-testing/.../CommandTester.java:208,504`.
- **Problem:** vanilla `PlayerList.getPlayerByName` ignores case, so `tp steve` finds `Steve` on a server. modded-testing (`findPlayer` uses `equalsIgnoreCase`) and velocity-testing (lower-case keys) follow that. In spigot- and paper-testing, `tp steve` throws "No fake entity registered with name: steve", and the extension's duplicate check lets `Steve` and `steve` coexist.
- **Evidence:** read.
- **Fix:** key players case-insensitively, by lower-casing the key on put and on lookup, for both the player and the entity maps. Use the same key in `hasFakePlayer`.

### D2-4 doc-wrong, low: docs/testing.md misstates which artifacts refuse what in the extension
- **Where:** `docs/testing.md:176-184`.
- **Problem:** the text says "In `modded-testing`, two injected players with the same name fail". The next sentence says "In spigot-, paper- and velocity-testing every injected player is also registered (`withPlayer` in the Forge/NeoForge/Fabric artifacts)", which lists modded-testing and excludes it at once. It then says that only spigot, paper and velocity refuse duplicate names and `@BeforeAll`/constructor parameters. In fact modded-testing refuses both, and common-testing refuses neither (see D2-2).
- **Fix:** after D2-2, write a single sentence: "Every artifact refuses parameters of `@BeforeAll` methods and constructors; every artifact except `common-testing` registers injected players and refuses two of one name."

### D2-5 backcompat, low: CommandTester.permissionPrefix() is still public in velocity- and modded-testing
- **Where:** `velocity-testing/.../CommandTester.java:178`, `modded-testing/.../CommandTester.java:269`.
- **Problem:** 313e68b0 removed this method from paper-testing because nothing used it (D-33). Its velocity and modded copies have no caller in the repository either. So the API now differs across the artifacts: present in two, absent in three.
- **Fix:** delete both.

### D2-11 test-util-bug, low: in spigot- and paper-testing, @a and @p leave out the executing player
- **Where:** `spigot-testing/.../nms/argument/MockEntitySelector.java:63-71`, `paper-testing/.../CommandTester.java:491-500`, pinned by `paper-testing/.../argument/EntitySelectorArgumentTest.java:48-77`.
- **Problem:** on a server, a player who runs `@a` is one of the targets, and `@p` from a player is that player (distance 0). Both mocks select only the players registered with `withFakePlayer`. So a player who is not registered gets `@a` = the other players and `@p` = another player. The tests lock this in: `players=[Alex, Steve]` for Admin's `@a`, and `player=Alex` for Admin's `@p`. modded-testing does it the server's way: its `Scope` adds the executing sender to the online players. D-11 moved Paper's rules to match Spigot's, so both now have the same deviation. CommandLibExtension hides it, since injected senders are registered. Manually built senders, the style the docs teach, show it.
- **Evidence:** read, plus the two test expectations above.
- **Fix:** include the executing player (when it is a `Player`) in `@a`, and return it for `@p`. Update the two tests.

## Flaky or order-dependent tests

### D2-6 test-flaky, medium: velocity-testing CommandLibTest passes only because of JUnit's method order
- **Where:** `velocity-testing/src/test/java/dev/marulabs/commandlib/CommandLibTest.java:63` (`registers_each_command_and_alias_and_unregisters_them`) against `:91` (`registration_defaults_apply_to_every_command`) and `:120` (`permission_defaults_apply_when_the_permission_is_unset`).
- **Problem:** `VelocityPermissions.DEFAULTS` is static. Since C-6 the first registration of a node wins (`putIfAbsent`), and unregister never removes it (C-6 Pending). The first test registers `myplugin.command.hub` with the implicit `OP` default. The other two register `hub` with `ALL` and expect an unset player to run it. They pass only when JUnit's default (hash-based) method order happens to run them first. Adding, renaming or reordering a test can break the class.
- **Evidence:** confirmed. With `@TestMethodOrder(OrderAnnotation)` and `@Order(1)` on the first test, both later tests fail with `CommandSyntaxException` (:114, :137). The probe was reverted.
- **Fix:** give each test a distinct label, or plugin id, so the nodes differ. Or reset `DEFAULTS` in `@BeforeEach` through a package-private test hook. The C-6 decision on removing defaults at unregister would also settle this.

## Gaps left by the round-1 fixes

### D2-7 test-gap, likely bug, medium: Spigot still removes the namespaced label of a label that a later registration took (D-14 only half done)
- **Where:** `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:883-888`, `integration-test/shared/{spigot,paper}/.../ScenarioTest.java` (`unregisteringKeepsALabelTakenSince`), `paper-testing/.../CommandLibLifecycleTest.java` (`unregister_leaves_a_label_that_a_later_registration_took`).
- **Problem:** D-14 asked for three things: an ownership check on Paper (done in 59930f89), the namespaced label covered on Spigot, and an assertion on the namespaced label in ScenarioTest. On Spigot, `removeRegisteredCommand(root, knownCommands, namespace(plugin) + ":" + node.getName())` still runs with no ownership check. Its comment, "The namespaced label names this plugin, so it is this registration's", is false when one plugin registers a label twice, which is exactly the scenario. Neither the ported ScenarioTest nor the Paper lifecycle test checks `testplugin:<label>`, so the integration suite would pass anyway.
- **Evidence:** read (unconditional removal at :888). Not run on a server.
- **Fix:** apply `ownsLabel` to the namespaced name as well. Dispatch `testplugin:commandlibowned` in both ScenarioTest fixtures, and assert `getChild("testplugin:hello")` in the Paper lifecycle test.

### D2-8 test-flaky, low: the modded integration fixture still waits a fixed delay (E-22 left it out)
- **Where:** `integration-test/shared/modded/src/main/java/dev/marulabs/testmod/TestMain.java:35,100-101`.
- **Problem:** the fixture reports after a fixed `REPORT_DELAY_SECONDS = 2` and assumes that the `executeAsync` work and the `auto:1b` command block have run by then. E-22 removed this pattern from the Bukkit and Folia fixtures only. On a slow container it can only fail falsely, never pass falsely.
- **Evidence:** read.
- **Fix:** have the async test and the command-block test signal completion, and poll for it with a timeout, as `readyAfterReload` does.

### D2-9 test-vacuous (weak), low: the Velocity integration log check misses Errors
- **Where:** `integration-test/src/test/java/dev/marulabs/commandlib/integration/VelocityIntegrationTest.java:159-160`.
- **Problem:** `proxy.getLogs()` is checked only with `doesNotContain("Exception")`. A `NoSuchMethodError`, `LinkageError` or `AssertionError` without an exception cause passes. These are the likely shading and API-drift failures on a proxy.
- **Fix:** also reject `Error` and `\n\tat ` stack frames, or match CommandLib's unexpected-error log prefix.

## Tests that cannot fail

### D2-12 test-vacuous, medium: VisibilityTest does not cover the branch half of the D-2 fix
- **Where:** `common/src/test/java/dev/marulabs/commandlib/VisibilityTest.java:177-205` (`help_skips_a_child_whose_predicate_throws_and_gives_each_child_its_provided_values`).
- **Problem:** the `kick` assertion runs `game kick`, which executes `kick` itself, so the context already holds kick's providers. `/game` help lists only child names, so it never evaluates a child's branch predicate. The `isUsable` (child) half of the fix is covered. The `hasArgumentPermission` (branch) half is not.
- **Evidence:** confirmed. With `HelpMessageAction.hasArgumentPermission` put back to the old `&& arguments.isVisible(ctx)`, VisibilityTest still passed. The probe was reverted.
- **Fix:** add a child command under an argument whose branch `visibleWhen` reads a value that only that child provides, then assert that the parent's help lists `set <value>`.

### D2-13 test-vacuous (weak), low: VisibilityTest checks the hidden branch with no positive control
- **Where:** `VisibilityTest.java:108-109`.
- **Problem:** the test checks only that no line contains `player`. Empty or failed help output passes too.
- **Evidence:** read.
- **Fix:** also run `game kick help` with the predicate true, and assert that `player` appears.

### D2-14 test-vacuous (weak), low: FabricPermissionTest's "open" assertion cannot tell the fallbacks apart
- **Where:** `modded-testing/src/test/java/dev/marulabs/commandlib/FabricPermissionTest.java` (`fabric_permissions_api_decides_with_the_default_as_its_fallback`).
- **Problem:** it stubs `Permissions.check(player, "fabricapitest.open", true)` to return `false` and then asserts `false`. The unstubbed static mock also returns `false`, so passing the wrong fallback (false for `ALL`) still passes.
- **Evidence:** read. The Fabric node was not built.
- **Fix:** stub the call to return `true` and assert `true`, or `verify` the exact fallback argument.

### D2-15 test-gap / implementation-detail, low: modded CommandLibExtension refusals are tested through hand-made JUnit mocks
- **Where:** `modded-testing/src/test/java/dev/marulabs/commandlib/CommandLibExtensionTest.java` (`two_injected_players_with_the_same_name_are_refused`).
- **Problem:** the test mocks `ExtensionContext`, `Store` and `ParameterContext`, and stubs `getOrComputeIfAbsent(.., eq(CommandTester.class))`, which ties it to the store's value type. Nothing covers the refusal of `@BeforeAll` or constructor injection.
- **Fix:** port `CommandLibExtensionRefusalTest` (EngineTestKit fixtures tagged `fixture`, excluded in `build.gradle.kts`) and drop the hand-made mocks.

### D2-16 test-vacuous (weak), low: the modded nearest-player test does not check nearness
- **Where:** `modded-testing/src/test/java/dev/marulabs/commandlib/TesterServerTest.java` (`the_nearest_player_resolves_among_several`).
- **Problem:** `isIn("Alex", "Bob", "Steve")` accepts any player, and every fake player stands at `Vec3.ZERO`.
- **Fix:** rename it as a smoke test, or give the players distinct positions and assert the nearest one.

## Docs

### D2-10 improvement, low: the docs example for permission denials keeps the weak assertion that D-19 removed from the tests
- **Where:** `docs/testing.md:247`.
- **Problem:** `assertThatThrownBy(...).isInstanceOf(RuntimeException.class)` also passes on an NPE or a mocking failure. Downstream users copy this example.
- **Fix:** use `.hasCauseInstanceOf(CommandSyntaxException.class)` and add the positive control, as the fixed tests do.
