# ponytail-review (round 2)

- Author: subagent (Claude Opus 5.5) running the `ponytail:ponytail-review` skill, report-only
- Date: 2026-10-21
- Reviewed range: `6f116a35..0f40c1a5` on master (internal-package reorganization, a roadmap commit, and the round-1 fixes)
- Prompt source: coordinator session (Claude Opus 5.5)
- Environment: read-only; the subagent checked out `0f40c1a5` detached in an existing agent worktree and switched it back
  afterwards (verified clean by the coordinator); no build or test run

The text below is the subagent's final report as delivered, unchanged.

---

## (1) How I ran the skill

`Skill(skill: "ponytail:ponytail-review", args: "Review commit range 6f116a35..0f40c1a5 on master (git diff 6f116a35 0f40c1a5), report-only, do not apply fixes.")`

I read the range at 0f40c1a5 by checking out a detached HEAD in my own agent worktree, then switched back to `worktree-agent-abc607bd7075456e7`. I changed no files in the repository.

## (2) The review as the skill produced it

What this change does: It moves CommandLib's implementation types into `dev.marulabs.commandlib.internal` (and sub-packages) and marks them `@ApiStatus.Internal`. That covers `PlatformAdapter`, `CurrentCommandContext`, `StringUtil`, `ArgumentBranchDelegate`, Spigot's NMS bridge and typed modules, the Bukkit/reflection/brigadier helpers, modded `CurrentServer` and the spigot-testing NMS mocks. `PlatformAdapterImpl` becomes package-private and is loaded reflectively. It also applies eight fixes from the earlier review: argument input vs. literal of the same name, alias validation, required/exclusive option check, shared permission node restore, deferred function compilation, caption loading, jev-lint paths, and the HelpTopics cache revert.

Load assumed: a Minecraft server plugin/mod library, with registration on the main thread, a handful of registrations per server, and commands run by many players. Nothing in the changed paths is per-player hot code.

**Should fix**

1. **Functions can keep running removed commands after an unregister and a plugin disable in the same tick** (`spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:877-882`, `:734-745`)
   - **What this is:** Data pack functions hold the command nodes they were compiled with. After an unregister, CommandLib must compile them again so they stop running the removed commands.
   - **Problem:** Since 0f40c1a5, `unregister()` of the last registration only marks "changed" and leaves the compiling to next tick's timer task. Example: a plugin calls `registration.unregister()` from its own `PluginDisableEvent` listener, or right before `disablePlugin(this)`. Bukkit then cancels the plugin's tasks, so the timer never runs. `onPluginDisable` compiles only `if (removed)`, and `removed` is false because the instance is already gone from `INSTANCES`. The functions keep calling the disabled plugin's executors until the next `/minecraft:reload`. Before this commit, the last unregister compiled at once.
   - **Fix:** In `onPluginDisable`, keep `FunctionCompilationMarker.changed()` under `if (removed)`. Call `compileFunctionsIfStale(plugin, server)` whenever `server.isRunning()`. The marker makes that call a no-op when nothing is pending, so it is one moved brace.
   - **If we skip it:** In that sequence, a function such as `#load` or `#tick` still runs a command of a plugin that is disabled.

2. **The new "failure not hidden" test does not test the new guard** (`spigot/src/test/java/dev/marulabs/commandlib/CommandLibRegistrationTest.java:150-158`)
   - **What this is:** 0f40c1a5 wraps `compileFunctionsWithCommands()` in unregister's `finally` with a catch-and-log. The goal is that an exception from `NMSCraftServer.create` no longer replaces the exception from `unregisterCommands`.
   - **Problem:** In this test the timer mock from `register("spawn")` is live (`isCancelled()` returns false). So `compileFunctionsWithCommands()` only bumps the marker and never reaches `NMSCraftServer.create`. If you delete the try/catch, the test still passes. The guard only matters when the task was cancelled, for example after an earlier compile failure.
   - **Fix:** Before `registration::unregister`, add `when(timers.get(0).isCancelled()).thenReturn(true);`. Then the finally block calls `NMSCraftServer.create(...).getServer()`, which throws, and the assertion really checks the guard.
   - **If we skip it:** Someone can remove the guard later and no test notices.

**Nice to have**

3. **`getInputs()` and `getInput(name)` disagree while one value of a variadic argument or an option value is parsed** (`common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java:136-137`, `:714-723`)
   - **What this is:** While a variadic argument parses each value, `replaceHandle` swaps in that single value's input. Before 4eac32cc, the swap went into the one map that both `getInput(name)` and `getInputs()` read.
   - **Problem:** Now only `argumentInputs` is swapped. Example: `/sum <nums...>` with input `1 2 3` and a validator. During the element parse, `ctx.getInput("nums")` returns `"2"`, but `ctx.getInputs()` / `getInput(0)` return `"1 2 3"` (before: `"2"`). For an option value, the value no longer shows up in `getInputs()` at all. The commit message says those two methods are unchanged.
   - **Fix:** Add one line about it to the 20-F05 roadmap item, which already questions these index APIs, or to CHANGELOG. Do not add code, since 20-F05 may delete the APIs.
   - **If we skip it:** A user validator that reads `getInputs()` for a variadic argument quietly sees different text than before.

4. **The design doc promises an annotation on every public internal class, but the generated typed NMS classes and the Kotlin facade have none** (`docs/design/module-structure.md:116-117`; `tools/nms-codegen/codegen.py`; `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/internal/kotlin/CurrentCommandContexts.kt`)
   - **What this is:** The "Packages And Visibility" rule says each internal package has an `@ApiStatus.Internal` `package-info.java` "and each public class there too".
   - **Problem:** About 1,400 generated public classes under `internal.nms.typed.*` rely only on their generated `package-info`. The Kotlin package `internal.kotlin` has no `package-info`, so its public `CurrentCommandContextsKt` class carries no annotation. The package exclusion in the README still hides all of them, so this is only a mismatch between the doc and the code.
   - **Fix:** Change the doc sentence to "each annotated `@ApiStatus.Internal` in its `package-info.java` (hand-written public classes too)". No code change.
   - **If we skip it:** The next reviewer reads the rule and flags 1,400 classes, or someone teaches codegen to emit an annotation nobody needs.

I checked these and found nothing to report:
- `PlatformAdapterHolder` uses `setAccessible` on a package-private class's constructor from another package. This works because both classes are in the same jar, module and class loader.
- No string references to the old `util.nms` / `commandlib.nms` packages remain in code, build files, `fabric.mod.json` or codegen. The only leftovers are jev-lint fixtures.
- Captions are read class-relative and still follow relocation.
- The alias check covers the builder and annotation callers.
- The option check walks `requires` transitively.
- The permission restore on Spigot and Paper: the unregistering instance is removed from the live lists before the restore runs, and the oldest-first order holds.
- The `mockNmsClass` API taking an internal type is already listed in the roadmap as needing a decision.

Verdict: fix 1 first. 2 is cheap and makes 0f40c1a5's own claim testable.

Not checked: I did not build or run any tests, so I don't know whether every `nms/*` module compiles its generated `package-info` (this needs `org.jetbrains:annotations` on each module's classpath). I also did not check that Shadow relocates the `PlatformAdapterImpl` name string in a real shaded plugin jar, or the Forge/NeoForge JPMS behaviour of the new reflective access.

## (3) What the review could not cover

- **No build or test run.** I did not run Gradle, the integration tests or the NMS module compilation; the review is from reading the code.
- **Mechanical renames were not read file by file.** The diff touches 1,792 files, most of them package moves and regenerated typed NMS classes. For these I checked by search:
  - no old package strings remain;
  - which internal directories lack a `package-info` (only `modded/.../internal`, which shares common's package, and `common-kotlin/.../internal/kotlin`);
  - which public internal classes lack the annotation (only the generated typed classes).
- **The two docs commits (446bbec5, 92c3e9d9) were only skimmed.** I looked at the roadmap diff and the README IntelliJ section, not every changed design doc for accuracy.
- **One stray file outside the repo.** An early shell command wrote a throwaway listing to `/tmp/x`, outside the repository and the scratchpad. My working files are in `C:\Users\maru3\AppData\Local\Temp\claude\C--Users-maru3-IdeaProjects-CommandLib-2\46243bc2-5972-4342-9147-4ef21a864713\scratchpad\`.

I attempted no fixes.
