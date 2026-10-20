# code-review (round 2)

- Author: subagent (Claude Opus 5.5) running the `code-review` skill, report-only, effort `high`
- Date: 2026-10-21
- Reviewed range: `6f116a35..0f40c1a5` on master (internal-package reorganization, a roadmap commit, and the round-1 fixes)
- Prompt source: coordinator session (Claude Opus 5.5)
- Environment: read-only, no build or test run; line numbers from `git show 0f40c1a5:<path>`

The text below is the subagent's final report as delivered, unchanged.

---

## (1) Skill invocation
`Skill(skill="code-review", args="high 6f116a35..0f40c1a5")`. The run was report-only: I passed neither `--fix` nor `--comment`. It ran as a forked execution and did not ask for the target.

## (2) Findings, as the skill produced them
The skill's own words: "Eight findings in `6f116a35..0f40c1a5`, deduplicated but not run through a verifier. The worktree is checked out at an older commit (`bc7a9554`), so every line number comes from `git show 0f40c1a5:<path>`." The skill gave no severity or verdict fields.

```json
[
  {
    "file": "common/src/main/java/dev/marulabs/commandlib/OptionGraph.java",
    "line": 124,
    "summary": "The new 'always given' closure follows every requiredOptions() edge. That includes edges added by requires(option, value) and requires(option, predicate, ...), whose check reads getOption() and falls back to the default value without the option being typed. So valid definitions are now rejected at build time.",
    "failure_scenario": "--force is required and declares requires(dryRun, false), where --dry-run is a flag that defaults to false, and the command has mutuallyExclusive(force, dryRun). The input '--force' is valid: dry-run is absent, its default false passes the check, and the exclusion is not hit. Since ef234ebf, building the command throws IllegalStateException ('every input must give both'). Only plain requires(option) edges, which check hasOption, should be followed."
  },
  {
    "file": "spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java",
    "line": 880,
    "summary": "unregister() of the last registration no longer compiles the functions itself. It relies on the timer task's next tick, and nothing else compiles them if the plugin is disabled before that tick.",
    "failure_scenario": "A plugin calls registration.unregister() at runtime and is then disabled in the same tick (for example a PlugMan unload, or the plugin disabling itself). Bukkit cancels the plugin's tasks, so the timer task never runs. onPluginDisable finds no registrations left (removed == false), so it skips compileFunctionsIfStale. Compiled data pack functions keep the removed command nodes and run the disabled plugin's code until the next /reload. Before 0f40c1a5 they were compiled synchronously in unregister()."
  },
  {
    "file": "common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java",
    "line": 714,
    "summary": "replaceHandle now changes only argumentInputs, not argumentNameToInputArgMap. While a variadic element or an option value is parsed, getInputs() and getInput(int) still show the whole input, although the commit message says they are unchanged.",
    "failure_scenario": "A variadic <names> argument gets the input 'a b c'. An element validator or additionalParser that calls ctx.getInputs() or ctx.getInput(index) during the element parse used to see the single token ('a'). It now sees 'a b c'. Before 4eac32cc the map was swapped together with the handle."
  },
  {
    "file": "spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java",
    "line": 868,
    "summary": "Only compileFunctionsWithCommands() inside unregister's finally block is guarded. unregisterPermissions() (which now also restores nodes), removeHelpTopic and updatePlayerCommandsLater() can still throw from the finally block, hide the unregisterCommands failure, and skip the cleanup after them.",
    "failure_scenario": "unregisterCommands throws, and then updatePlayerCommandsLater's runTask(plugin) throws IllegalPluginAccessException because the plugin is disabling, or restorePermissions throws. The caller sees the cleanup exception instead of the root cause, and the function recompile and filter teardown never run."
  },
  {
    "file": "spigot/src/test/java/dev/marulabs/commandlib/CommandLibRegistrationTest.java",
    "line": 150,
    "summary": "a_failure_to_remove_the_commands_is_not_hidden_by_the_cleanup never reaches the new try/catch. functionCompiler is a mock whose isCancelled() returns false, so compileFunctionsWithCommands() returns before it calls NMSCraftServer.create.",
    "failure_scenario": "Remove the new try/catch around compileFunctionsWithCommands() and the test still passes. The guard the commit adds is untested, and a regression would go unnoticed."
  },
  {
    "file": "common/src/main/java/dev/marulabs/commandlib/VariadicArgument.java",
    "line": 75,
    "summary": "parseValues saves sameCtx.getInput(name()), which now falls back to a literal's text or \"\", and always puts it back with a non-null value. CommonArgument was changed to pass null when isArgumentGiven is false, but this path was not.",
    "failure_scenario": "Since replaceHandle now writes into argumentInputs, which isArgumentGiven reads, the restore step can mark the argument as given with the literal's text or \"\" when it had no entry there. This only happens if the handle's nodes and the collected inputs disagree, for example names skipped by collectInputs (\"-\" or the internal prefix). Then Arguments' optional-default check and getInput(name) return the wrong value."
  },
  {
    "file": "paper/src/main/java/dev/marulabs/commandlib/CommandLib.java",
    "line": 679,
    "summary": "restorePermissions is implemented twice, almost line for line, in spigot/CommandLib and paper/CommandLib. Only the source of the oldest-first list differs.",
    "failure_scenario": "A fix to the restore rule has to be made in both copies. The rest of this diff already shows the copies drifting: Paper returns early on an empty set and Spigot does not. A shared helper in common/src/bukkit/java that takes the ordered live registrations would remove the copy."
  },
  {
    "file": "spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java",
    "line": 961,
    "summary": "Who owns a node is still worked out after the fact: identity comparison in removePermissions, then a rescan of every live registration's generated permissions to re-add it. It is not recorded when the node is registered.",
    "failure_scenario": "Every unregister that removes a node rebuilds Command.permissions for the live registrations and moves Permission objects between their registeredPermissions lists. Each new rule (bypass nodes, plugin.yml nodes) needs another special case on both platforms. A per-node-name list of the registrations that declare it, kept on register and unregister, would replace both steps."
  }
]
```

The skill's closing note: "I didn't build or run any tests. For the moved packages I only searched for package names written as strings, not the full mechanical move."

## (3) What the skill could not cover
- **No verifier pass:** none of the findings were checked by a verifier, and nothing was built or tested. Two of the eight are design and cleanup remarks rather than bugs: the duplicated `restorePermissions` (Paper, line 679) and the after-the-fact node ownership (Spigot, line 961).
- **Older checkout:** the worktree was at `bc7a9554`, not the reviewed tip. Line numbers come from `git show 0f40c1a5:<path>`.
- **Package reorganization only partly checked:**
  - For the moved packages, the skill searched only for package names written as strings. It did not go through the whole mechanical move.
  - There are no findings about reflection by class name, the Fabric entrypoint string, typed NMS lookup by package prefix, or shading and relocation. That means "not flagged", not "verified correct".
  - Whether the public API grew was not assessed explicitly. That covers `@ApiStatus.Internal` coverage, the package-private `PlatformAdapterImpl`s, and the `org.jetbrains:annotations` 16 → 26 bump.
- **Fixes covered:** of the eight review fixes, the findings touch CR-8 (OptionGraph), CR-4 (Spigot function recompilation), CR-1 (permission restore) and CR-2 (input kept apart from literals: `CommonCommandContext.replaceHandle` and `VariadicArgument`). There are no findings for CR-7, PT-3, PT-4 or PT-NC-1.
- **Not done:** I did not reproduce, verify or fix any of these findings, and I did not write a review record under `review-records/runs/`.
