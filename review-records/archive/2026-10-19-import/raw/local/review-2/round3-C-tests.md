# Round 3 review C: tests and testing utilities

Reviewer: test-focused (round-2 commits 0ccd8a27..3db5ca99). Base: master 3db5ca99.
Scope: the five FakeSender/CommandLibExtension artifacts, spigot/paper selector resolution, modded fake player type and
overworld feature flags, CommandFailure.replayed in the testers, the new regression tests, integration fixtures.

Checked and found sound (not reported): grant/deny unification in all five FakeSenders (each setter replaces only its own
set, deny wins, duplicates accepted); the @BeforeAll refusal in all five extensions and the EngineTestKit refusal tests
(fixture tag excluded in every build file); paper/spigot case-insensitive lookup and `@p` = executing player tests;
modded TesterServerTest nearest player (distinct distances, can fail); FabricPermissionTest now stubs true; velocity
CommandLibTest's own labels; VisibilityTest positive controls; the namespaced-label and onEnable-unregister scenarios
(the check command runs only after reload, so a post-unregister run would count); modded awaitPending (IT side waits
5 min > 30 s + 120 s); ArgumentIntegrationCoverageTest path message; the docs/testing.md example
(`hasCauseInstanceOf(CommandSyntaxException)` matches every tester's wrapping).

## C3-1
- category: test-util-bug, severity: low
- file: spigot-testing/src/main/java/dev/marulabs/commandlib/nms/argument/MockEntitySelector.java:92
- what: the error for an unknown name reads `"No fake entity entities with name: "`. A find-and-replace slip in
  1ef103f7 (`registered` became `entities`). paper-testing still says "registered".
- evidence: grep; confirmed in source. No test asserts the message, so nothing caught it.
- fix: restore `"No fake entity registered with name: "`.

## C3-2
- category: test-gap, severity: medium
- file: spigot-testing/.../CommandTester.java:208, paper-testing/.../CommandTester.java:139,
  velocity-testing/.../CommandTester.java:129 (also modded-testing/.../CommandTester.java:180)
- what: the `CommandFailure.replayed(...)` wrapper from ea19dc6b is what makes a tester rethrow an AssertionError when
  the error formatter sends nothing. Only common-testing has a test for that case
  (`a_failed_assertion_fails_execute_when_the_error_formatter_sends_nothing`). The other testers' AssertionError tests
  use the default formatter, which sends messages, so they pass without the wrapper.
- evidence: confirmed by mutation. I removed the `replayed` wrapper from the spigot, paper and velocity testers, and
  `:spigot-testing:test :paper-testing:test :velocity-testing:test` all passed.
- fix: copy the common-testing test (silent `errorFormatter`, executor throws AssertionError, `execute` must throw it)
  into each tester's CommandTesterTest, modded included.

## C3-3
- category: test-gap, severity: low
- file: spigot-testing/.../nms/argument/MockEntitySelector.java:62, paper-testing/.../CommandTester.java:497
- what: the guard `!entities.contains(sender)` stops an executing player who is also registered from appearing twice in
  `@a` / `@e`. That is the usual CommandLibExtension case, because an injected sender is registered with
  `withFakePlayer`. No test covers it: every new selector test uses an executing player that is not registered.
- evidence: confirmed by mutation. With the guard removed in both artifacts, every spigot-testing and paper-testing test
  passed (EntitySelectorArgumentTest rerun, 11/11).
- fix: add a test where the sender is registered with `withFakePlayer(sender)` (or injected by the extension) and runs
  `players @a` / `entities @e`. Assert that the sender is listed once.

## C3-4
- category: test-util-bug, severity: low
- file: modded-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:242-248 (`withPlayer`, `hasPlayer`),
  :288 (Scope adds the sender by exact name)
- what: modded keys players by their exact name, while `findPlayer` and the vanilla lookups ignore case. Two problems
  follow. `@Sender("alex")` and `@Sender("Alex")` are both accepted, so two online players share a name. And
  `withPlayer` does not replace a player whose name differs only in case. After 1ef103f7, spigot-testing and
  paper-testing lower-case the key and refuse such a pair; velocity-testing does the same through `key()`. That leaves
  modded as the one artifact that does not match docs/testing.md ("refuses a second injected player of the same name").
- evidence: confirmed by reading the code; not run.
- fix: key `players` (and `onlinePlayers` in Scope) by `name.toLowerCase(Locale.ROOT)`, as spigot-testing and
  paper-testing do.

## C3-5
- category: test-util-bug, severity: low
- file: modded-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:370-386
- what: cb76bb8b stubs `enabledFeatures()` only on the tester's default overworld. When a test adds its own overworld
  mock with `withLevel(level)`, that level answers null. Every source stands in `server.overworld()`, so on 1.19.3-1.20.x
  an operator's selector (for example `@a` in a MessageArgument) hits the same NullPointerException again.
- evidence: unconfirmed (I avoided a modded build). The reasoning follows the commit message of cb76bb8b and
  `createSource`.
- fix: say in the `withLevel` javadoc and in docs/testing.md that a mock overworld must stub `enabledFeatures()`.
  Alternatively, when a supplied level is a Mockito mock whose `enabledFeatures()` returns null, stub it with
  `FeatureFlags.DEFAULT_FLAGS`.

## C3-6
- category: test-gap, severity: low
- file: modded-testing (no test); compare paper-testing/.../argument/EntitySelectorArgumentTest.java:160
- what: the settings comparison for a shared node (89cdb524) exists for vanilla `EntityArgument` (`single`,
  `playersOnly`), but it is tested only against common's `MaxLengthType` and paper-testing's lambda stub. The paper
  test passes because the stub lambda captures `single`. A real vanilla or Paper `EntityArgument` is never compared.
  modded-testing loads the real vanilla classes, so it could cover this.
- evidence: confirmed by reading the code. paper-testing's `selector(...)` returns a lambda, and `parse differently` is
  asserted only in common and paper-testing.
- fix: add the paper test's twin to modded-testing: `PlayerArgument("target")` and `EntitiesArgument("target")` sharing a
  node must throw "parse differently".

## C3-7
- category: spec-question, severity: low
- file: integration-test/src/test/java/dev/marulabs/commandlib/integration/VelocityIntegrationTest.java:161-163
- what: f76cc79a adds `doesNotContainPattern("\\w+Error\\b")` and `"\tat "`, but no real proxy has run with them. The
  compatibility.md row is dated to the round-1 run. A harmless proxy line with a word like `onError` or `ProtocolError`
  would fail the target.
- evidence: unconfirmed (no Docker).
- fix: run velocity-3.4.0 once before relying on it. If a harmless line matches, anchor the pattern to class names
  (`\b[A-Z]\w*Error\b`).
