# ponytail2-E: tests and test utilities (second pass, 2c8ffd7a..02fbf5f9, focus 16926819..HEAD)

Scope: common/src/test, common/src/adventureTest, common-testing, spigot/src/test, paper/src/test, velocity/src/test,
spigot-testing, paper-testing, velocity-testing, modded-testing, integration-test (including shared/bukkit), benchmark.
Load assumed: developer and CI test runs only, no production traffic.

What this change does: since 16926819, 16 commits fixed the first ponytail round. In my paths, that means: the
common-testing help-context fix (P1E-1) with a regression test, the store/readDouble cleanups (P1E-3, P1E-4), 13
Bukkit fixtures moved to `integration-test/shared/bukkit` (P1E-2), the shared Adventure `RichTextsTest` in
`common/src/adventureTest`, the 26.2 ChatColor reset fixture branches, and new tests for the short-flag budget,
`@Range` on required/repeatable options, option-like names and the modded bypass node.

## How I checked

- Read every diff hunk in my paths after 16926819 and the main code each new test covers (OptionGraph budget,
  AnnotationParser range check, CommandNameValidator, modded `Command.permissionEntries` and `CommandLib.register`,
  Paper `VanillaArgumentTypes.color`, common-testing `TestCommandContext`/`CommandTester`).
- For each new test I checked by reading that it fails on the pre-fix code:
  - `OptionTreeSizeTest` flags(12): before the fix, the orders were all generated before the budget check.
  - `AnnotationParserTest.required_and_repeatable_options_take_a_range_without_zero`: `parse()` threw before.
  - `CommandStructureTest.names_that_look_like_options_are_rejected`: no check existed before.
  - `CommandTesterTest.execute_and_get_context_returns_help_context_with_visibility_checks`: `isNotEmpty()` fails on
    the old empty source context.
  - `CommandLibRegistrationTest.a_cooldown_bypass_node_another_command_uses_keeps_that_commands_default`: modded
    `PERMISSIONS.putIfAbsent` is first-wins. With `kit` first, the old per-command walk put the bypass entry (OP)
    first, so the guest was denied.
- Wiring: `common/src/adventure` and `common/src/adventureTest` are added as srcDirs in `paper/build.gradle.kts` and
  `velocity/build.gradle.kts`, so the shared `RichTextsTest` runs in both. The integration script adds
  `shared/bukkit` for the `spigot` and `paper` suites (the only other suite is `folia`). It adds
  `common/src/adventure` whenever `commandlibModule == "paper"`, which covers Folia too. Both shared fixture
  projects add `../bukkit/src/main/java`. `ArgumentIntegrationCoverageTest` still reads the per-suite
  `ArgumentTest.java`, which did not move.
- Sibling check for P1E-1: no other tester (Spigot, Paper, Velocity, modded) has `executeAndGetContext` or a
  "latest context" field, so the fix did not need to go anywhere else.
- Re-ran a "test that cannot fail" scan on all 226 changed test files, with a smarter parser than the first pass.
  It flags @Test methods whose only assertions sit inside callbacks (lambda bodies, `-> assert...`,
  anonymous-class or double-brace blocks), so they pass if the callback never runs. Hits:
  - the `CommandLibExtensionRefusalTest` inner fixtures (run through the engine on purpose);
  - `CommandStructureTest.arguments_sharing_a_node_with_types_holding_per_instance_records_share_it` and
    `OptionTreeSizeTest.every_redirect_follows_its_target_in_the_per_player_copy`, which are "must not throw" tests
    by design;
  - `paper-testing ArgumentCoverageTest.allArgumentsCanBeWiredIntoCommandTester`, whose assertion sits inside
    `forEach`. A sibling test checks that the factory map covers every class, so it is not empty.
  No real finding.
- Unused-helper scan over the testing artifacts, common/src/test helpers, integration fixtures and benchmark sources:
  every declared method has at least one other reference, except Folia's `onSendSuggestions`, which is an event
  handler.
- Compared the files left in `shared/spigot` and `shared/paper` after P1E-2 (see finding 1).

## Must fix

None.

## Should fix

None.

## Nice to have

### P2E-1 (simplify, low) Two more Bukkit fixtures differ only by a guard that never fires on Paper
- Where: `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/ScenarioTest.java` (332 lines) and
  `VanillaExecutionTest.java` (240 lines), plus their copies under `integration-test/shared/paper/...`.
- What this is: P1E-2 moved the byte-identical Spigot/Paper fixtures to `shared/bukkit` and left the files that
  differ.
- Problem: these two differ only in comments plus one Spigot 1.20.3 guard each:
  `"1.20.3".equals(BukkitUtil.getMinecraftVersion()) ? "0,0,0" : "42,0,7"` (ScenarioTest:137-138) and
  `int runs = "1.20.3".equals(...) ? 0 : 1` (VanillaExecutionTest:207-209). No Paper target runs 1.20.3, and the
  comment already says "Paper has no 1.20.3", so the Spigot copy acts exactly like the Paper copy on every Paper
  target. `BukkitUtil.getMinecraftVersion()` exists in both the spigot and paper modules, and the Paper suite
  already uses it (`shared/paper/.../ArgumentTest.java:5`). So each fix to these scenarios is still made twice.
- Evidence (confirmed by `diff`): ScenarioTest has 7 differing lines (one import, the `LATE_COMMAND` javadoc, the
  guard). VanillaExecutionTest has 8 (one import, one javadoc sentence, the guard). The other remaining pairs differ
  for real: DataPackFunctionTest has the 26.1 world-folder handling and the Mohist load reset, HelpTopicTest has the
  `minecraft:` label check, ArgumentTest and TestMain differ too. They should stay split.
- Fix: move the Spigot versions of both files to `shared/bukkit` and delete the Paper copies. Merge the
  `LATE_COMMAND` javadoc into one sentence that covers both modules: the spigot module on Paper 1.20.6+ adds the
  command again after the reload, and the paper module adds it through the COMMANDS lifecycle handler. Update
  `docs/vanilla-integration.md:9`, which names `VanillaExecutionTest` under `shared/spigot` and `shared/paper`.
- Saving: about -570 lines.
- If we skip it: the vanilla-execution and scenario checks can drift between the suites, which is the problem
  P1E-2 fixed for the other 13 files.

## Looked at, no finding

- 26.2 `chatColorArgumentReset` fixtures: on 26.2+ the result starts as SUCCEEDED. A wrong parse still fails:
  Paper's executor and error formatter, and Spigot's executor and uncaught handler, each record FAILED. "Command not
  registered at all" is the only case this check misses, and the sibling `dark_red` check catches it. paper-testing
  still models the 26.1 `color` argument (it accepts `reset`). That is a choice about which version the mock copies.
  It is not a bug.
- `modded CommandLib.register` now rejects an empty prefix and copies the list (da8f4660), with no test. No other
  platform tests its matching check either, and the code is two plain guard lines. Not worth a test.
- common-testing `CommandLibExtension` now stores the tester directly. `CommandTester.close()` is empty, so if JUnit
  auto-closes `AutoCloseable` store values, nothing happens.
- `executeAndGetContext` still always finds a context after a successful run: every real Brigadier context has
  non-empty input (at least the root literal).

Verdict: Ship.
Lean: -570 lines possible (P2E-1).
Not checked: I built nothing and ran no Gradle or integration targets. The "fails before the fix" claims come from
reading the code, not from mutation runs.
