# code-review (round 1)

- Author: subagent (Claude Opus 5.5) running the `code-review` skill, report-only, effort `high`
- Date: 2026-10-21
- Reviewed range: `3279d890..50286c2d` on master (38 commits, 222 files, +3051/-2075)
- Prompt source: coordinator session (Claude Opus 5.5); see `../../request.md`
- Environment: read-only, no build or test run

The text below is the subagent's final report as delivered, unchanged.

---

## (1) Invocation
`Skill(skill="code-review", args="high 3279d890..50286c2d")`. The flags `--fix` and `--comment` were not passed. Before running it I checked the range in C:\Users\maru3\IdeaProjects\CommandLib_2. It resolves to 3279d8901fa4..50286c2dbd07: 38 commits, 222 files changed, +3051/-2075.

## (2) Findings
The skill returned these 9 findings. Its own preface: "found 9 issues. The most serious is that Spigot and Paper can drop a permission node that another live registration still uses. I didn't verify any finding; this was a recall pass, so some are less certain than others (noted per item)." It gave no severity or verdict field for individual findings.

1. **spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:938**
   - Summary: When the newer of two registrations sharing a permission node unregisters first, the node is removed entirely while the older registration's commands stay; the same happens in paper/.../CommandPermissions.java:48-62.
   - Failure scenario: Registration A declares myplugin.command.spawn as ALL, and registration B declares the same node. addPermission/CommandPermissions.add call removePermissionCompletely/remove on A's object, which sets A's default to FALSE and drops it. B.unregister() then removes B's permission because it is still the registered one. A's /spawn is still registered but its node no longer exists, so Bukkit falls back to the OP default and non-op players lose access. Velocity handles this by restoring the oldest live registration's default; Spigot and Paper don't. PermissionReplacementTest.removing_the_newer_registrations_permission_first_removes_it locks this behavior in.

2. **common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java:91**
   - Summary: A literal and an argument with the same name still share one entry in argumentNameToInputArgMap, so a later literal overwrites the argument's input. The fix for the omitted-optional bug (cb396b27) added a parallel givenArguments set that only Arguments.parse reads, instead of fixing collectInputs.
   - Failure scenario: Branch argument(new StringArgument("x")).child(new Command("x")), input `/cmd foo x`. collectInputs stores x->"foo" and then overwrites it with x->"x" from the literal. ctx.getInput("x") returns "x", so additionalParser (CommonArgument:295/300) and ArgumentValidationException.ofIncorrectInput (CommonArgument:234/245) get the literal text instead of "foo". validateNamesAlongRoute does not reject this shape.

3. **common/src/main/java/dev/marulabs/commandlib/ClientSuggestions.java:73**
   - Summary: The testers now filter suggestions with canUse, but on a real server an argument node with server-side (ask_server) suggestions is answered by the unfiltered dispatcher.getCompletionSuggestions, so the server reply still includes hidden literal siblings.
   - Failure scenario: A command has a permission-hidden child literal `admin` and a sibling argument with a custom suggestion provider. On a real server, a client typing `/cmd ` gets `admin` back in the server's suggestion response. CommandTester.suggest now returns only the visible entries, so a test that asserts the hidden subcommand isn't suggested passes while the live server still exposes it. Less certain: this depends on how the vanilla client merges the server reply.

4. **spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:876**
   - Summary: unregister() of the last registration now recompiles data pack functions synchronously inside unregister. Before, this ran in the scheduled task at the start of a tick.
   - Failure scenario: A plugin calls unregister() from its own command executor, or from a command a data pack function runs. server.recompileFunctions then replaces the function library during the current command or function execution, possibly re-running #load. This is the kind of re-entrancy the deferred task and updatePlayerCommandsLater avoid. Also, NMSCraftServer.create(...) runs unguarded in the finally block, so if it throws it hides any exception from unregisterCommands and stopFunctionCompiler is skipped.

5. **paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:209**
   - Summary: register() still applies CommandDefaults to the caller's commands before the new build-time validation, which can now throw. A failed register leaves the commands mutated even though the docs say nothing is registered; the same order is in modded CommandLib:117 and velocity CommandLib:179.
   - Failure scenario: Register a command with an optional argument before a required one. CommandSet.of now throws IllegalStateException after defaults.applyTo has added the defaults' middlewares and providers. The plugin catches the exception, fixes the definition and registers the same instances again, and the default middlewares (cooldown, confirmation, preprocess) are applied twice.

6. **paper/src/main/java/dev/marulabs/commandlib/CommandSet.java:36**
   - Summary: Paper and modded (modded CommandLib:139) build the whole Brigadier node tree at register() only to validate it, throw it away, and build it again on every COMMANDS or RegisterCommands event.
   - Failure scenario: The tree is built at least twice per registration. optionsAnywhere trees grow with options times arguments, so this adds startup cost. CommandTree.of(...) is also recomputed separately by validatePermissions, permissionConfigs and cooldownBypassNodes for the same commands. Cheaper: run the validation steps (validateTopLevel, validatePermissions, validateTrailingArguments, OptionGraph checks) without creating nodes, or keep the first build for the first event.

7. **common/src/main/java/dev/marulabs/commandlib/CommandNodeCreator.java:274**
   - Summary: An alias equal to the command name is filtered out only when nodes are created. aliases() still returns it to every other consumer, and CommandNameValidator still treats it as harmless.
   - Failure scenario: new Command("spawn") with addAliases("spawn"): the command tree snapshot and help show `spawn (aliases: spawn)`, docs/Markdown list the alias, and the Bukkit command map or help topics get a duplicate label. Fixing it at the source (drop or reject the name in addAliases) removes the special case from node creation.

8. **common/src/main/java/dev/marulabs/commandlib/OptionGraph.java:115**
   - Summary: The new 'required options cannot be mutually exclusive' check misses definitions that are just as impossible: a required option whose requiredOptions include an option it excludes, or that requires an option excluded by another required option.
   - Failure scenario: --a is required(), --a requires --b, and mutuallyExclusive(a, b). Registration succeeds, but every input fails: --a is mandatory, --a needs --b, and --b can't follow --a. The rejection this commit adds for the two-required case doesn't catch it.

9. **spigot-testing/src/main/java/dev/marulabs/commandlib/nms/argument/MockNMSArgumentVec3D.java:54**
   - Summary: The vanilla integer-centering coordinate() helper is copied verbatim into paper-testing CommandTester.finePosition/coordinate (around lines 776-794).
   - Failure scenario: Two copies of the same parsing rule (no '.' in the token means add 0.5 for x/z) must stay in sync by hand. The commit set itself removed 11 duplicated Spigot/Paper test files (R1-C-07/R2-D-32), so this adds a new duplicate. It could live in a shared testing source.

## (3) What the review did not cover
- The skill says it did not check the build-script and Gradle version-catalog migration, the moved test files, or the integration-test scenarios.
- It ran no Gradle tasks and verified none of the findings. It treated this as a recall pass; only finding 3 carries an explicit "less certain" caveat.
- It assigned no per-finding severity or verdict. By its own preface, finding 1 is the most serious.
- I made no changes to the repository and did not write a review record under review-records/. I was told to stay read-only, so recording these findings is left to the caller.
