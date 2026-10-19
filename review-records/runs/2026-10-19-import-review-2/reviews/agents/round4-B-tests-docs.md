# Round 4 B: tests, testing utilities, docs (round-3 fixes 3db5ca99..7a88ae5e)

Scope: modded-testing lower-case player keys (1ced7ecc) and stubFeatures (7a88ae5e), new regression tests (77eda7da, 3cddbf7a, ded4792a, 4df47291 tests, 6142b594 / ef51e32a tests), 33f9e4af, CHANGELOG (d23f04aa), docs/options.md, errors.md, permissions.md.

## Findings

### R4B-1 doc-wrong, medium: docs/suggestions.md:151 still says a throwing provider empties the response
`## Exceptions`: "An exception thrown by a provider is printed to the console and the client gets no suggestions for that request."
ef51e32a (R3A-3) changed this: a provider or vanilla `listSuggestions` that throws an `Exception` synchronously is reported and skipped, and the other providers' suggestions are still sent (`ArgumentCommandNodeCreator` catch around `action.accept`, plus the outer `catch (Exception e) -> Suggestions.empty()` for that node only). docs/errors.md:298 was updated, suggestions.md was not, so the two pages now disagree.
Evidence: read the code and both pages; `CommonArgumentEdgeCaseTest.failing_suggestion_action_keeps_the_other_suggestions` and `CommandStructureTest.a_throwing_suggestion_provider_keeps_the_suggestions_of_the_other_branches` assert the new behaviour.
Fix: replace it with the errors.md wording ("An exception thrown by a provider, or by a stage it awaits, is printed to the console. The client still gets the suggestions of the other providers."), or link to errors.md.

### R4B-2 doc-wrong, low: CHANGELOG.md:86-88, Forge permission bullet is too broad on two points
1. "A command block or an entity that `/execute as` runs as runs only `ALL` commands." Correct for command blocks (level 2). Wrong for `/execute as`: the source keeps the level of whoever ran `/execute`. `ForgeCommandActor.hasPermission` -> `CommandLib.hasDefaultPermission` -> `source.hasPermission(4)`, so an operator or the console running `/execute as @e[type=zombie] run <cmd>` still passes `OP` commands, as it did in 0.17.2 (`hasPermissionLevel(4)`). docs/permissions.md describes this correctly ("their own permission level").
2. "Data pack functions ... pass every CommandLib check". Only functions that run as the server pass (tick/load tags, `/function` from the console), plus compilation. `/function` run by a player keeps the player as the entity, so that player's node is checked (the `entity instanceof ServerPlayer` branch runs first).
Evidence: code read (modded CommandLib.java:191-205, ForgeCommandActor.java:84-104) and `git show 0.17.2:forge/.../PlatformAdapterImpl.java`. Not run on a server.
Fix: e.g. "A command block runs only `ALL` commands, and an entity that `/execute as` runs as is judged by the level of the source that ran `/execute`. Compiling data pack functions, functions that run as the server, the console and RCON pass every CommandLib check; ..."

## Checked, no issue
- 1ced7ecc: every `players` / `onlinePlayers` access goes through `key()` (withPlayer, hasPlayer, Scope, findPlayer). `getPlayerNamesArray` now takes the real names from the values. The refusal test fails on the old exact-name key (`containsKey("Alex")` misses "alex").
- 7a88ae5e: `stubFeatures` is lenient, runs only on mocks that answer null, and leaves a user stub alone. Gated `>=1.19.3`, when `enabledFeatures` was added. I did not run modded (triage says it was mutation-checked).
- 3cddbf7a: mutation confirmed. Dropping `!entities.contains(sender)` in spigot-testing MockEntitySelector:62 makes `:spigot-testing:test --tests *EntitySelectorArgumentTest*` fail (1 of 12). The change is reverted.
- 77eda7da: the four tests use an empty error formatter, so only `CommandFailure.replayed` can make `execute` throw. That matches common-testing.
- ded4792a: the real vanilla EntityArgument settings (single/playersOnly) make the two branches differ, and the assertion checks the "parse differently" message.
- 4df47291 tests: the Spigot test's `declared.getDefault()` check is not vacuous, because `removePermissionCompletely` would set the existing permission to FALSE. The Paper test counts `removePermission` calls (1). Both fail if the skip is removed.
- 6142b594: options.md and the getOption/repeatable javadoc match the `!option.registered` check. No stale "already registered" / "is not an option of this command" text is left in docs, the plugin skill or the CHANGELOG.
- permissions.md cooldown-bypass and Spigot paragraphs match the Spigot/Paper code. The CHANGELOG sibling-name and Folia bullets match CommonCommand.addChildren/addAliases and paper `requireGlobalThread`.
- 33f9e4af: the wording now matches paper-testing (no test pins it, which is fine).
