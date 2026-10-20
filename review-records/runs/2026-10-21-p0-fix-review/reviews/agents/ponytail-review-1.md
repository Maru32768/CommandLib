# ponytail-review (round 1)

- Author: subagent (Claude Opus 5.5) running the `ponytail:ponytail-review` skill, report-only
- Date: 2026-10-21
- Reviewed range: `3279d890..50286c2d` on master
- Prompt source: coordinator session (Claude Opus 5.5); see `../../request.md`
- Environment: read-only from git objects at `50286c2d`, no build or test run

The text below is the subagent's final report as delivered, unchanged.

---

## 1. Skill invocation

`Skill(skill: "ponytail:ponytail-review", args: "Review commit range 3279d890..50286c2d of master in C:\Users\maru3\IdeaProjects\CommandLib_2 (git diff 3279d890 50286c2d). Report-only, do not apply fixes. Finding context: docs/roadmap-history.md \"P0: GA 前の欠陥と契約（完了分）\". Ignore .claude/worktrees/.")`

I read everything from git objects at `50286c2d`. Nothing in the repository was edited, built or run. Line numbers refer to `50286c2d`.

## 2. Review (full skill output)

What this change does: About 40 commits fix the GA review findings. Registration now fails early, with a clear error, on bad command trees: repeated argument names on a route, a child command in two places or under itself, a second executor, two required options in one exclusive group, one permission node with two configs, and registry arguments that clash. Runtime fixes cover parent permission checks, omitted optional arguments that share a literal's name, and permission ownership across registrations on Spigot, Paper and Velocity. The PR also adds client-like completion in the testers, moves the sources shared by Spigot and Paper to one place, adds a Gradle version catalog and cleans up NMS. Assumed load: a library used at plugin start-up and on each command run, with a few registrations per server.

**Must fix**

1. **Unregistering the newer of two live registrations removes a node the older one still uses** (`spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:920-946`, `paper/src/main/java/dev/marulabs/commandlib/CommandPermissions.java:48-70`, `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:655`)
   - **What this is:** Registration A and registration B can both declare one node, for example `permission("myplugin.use")` with default ALL on `/foo` (A) and on `/bar` (B). B's `addPermission` replaces A's `Permission` object. The fix for 18-F05 now removes a node only if the plugin manager still holds this registration's object.
   - **Problem:** That covers only one order: the older registration is unregistered first. If B (the newer one) is unregistered while A is still live, B's object is the current one, so the node is removed completely. A's `/foo` keeps running, but Bukkit treats an undefined permission as OP-only. Non-op players silently lose `/foo`. A's own `Permission` object can't simply be added back either: `removePermissionCompletely` / `CommandPermissions.remove` set its default to FALSE when B replaced it. The new tests only cover "older first, then newer". Velocity fixed this case (the oldest live registration decides), so the platforms now differ.
   - **Fix:** In Spigot's `unregisterPermissions`, and in Paper's unregister at line 655, check each node this registration actually removed. If another live registration (`INSTANCES` on Spigot, `registrationState.instances` on Paper) still declares that node, add a fresh `Permission` for it, rebuilt from that registration's `Command.permissions(commands, prefix)`. Add one test: register A, register B, `B.unregister()`, and assert the node is still there with A's default.
   - **If we skip it:** A plugin that adds a temporary command set and later removes it locks regular players out of its permanent commands until a restart or reload.

**Should fix**

2. **An alias equal to the command name is fixed only where the nodes are built** (`common/src/main/java/dev/marulabs/commandlib/CommandNodeCreator.java:269-276`, root cause in `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:202`)
   - **What this is:** 5061aefd stops `addAliases("foo")` on command `foo` from causing an endless loop by filtering such aliases (and duplicates) in `createAliasCommands`.
   - **Problem:** The command still stores the alias, and every other reader of `aliases()` still sees it. Spigot's `describe` (`spigot/.../CommandLib.java:497-501`) removes the label once from `[foo, foo]`, so `/help foo` shows "Aliases: foo". `CommandTreeRenderer:34` prints `foo (aliases: foo)` into users' golden snapshots. `CommandDocs.labels()` lists `foo` twice. This is the case "fixed in one caller while the shared function stays broken".
   - **Fix:** In `CommonCommand.addAliases`, drop (or reject) an alias equal to `name` or already present. Then remove the `.filter(...)` and `.distinct()` from `createAliasCommands`. About the same number of lines, and the fix is in one place.
   - **If we skip it:** Help pages, snapshots and diagnostics show a command as its own alias.

**Nice to have**

3. **Field cache on a path that is not hot** (`paper/src/main/java/dev/marulabs/commandlib/HelpTopics.java:20,123-127`)
   - **What this is:** ab66cc54 keeps `IndexHelpTopic.allTopics` in a static volatile field.
   - **Problem:** `indexTopics` runs only on register, unregister and the COMMANDS event (a data reload), once per label. A `getDeclaredField` there costs nothing anyone would notice, so the cache only adds state and lines. The `ClassValue` caches in `CommandContext` and `MessageArgument` run per command and are fine.
   - **Fix:** Go back to the plain `getDeclaredField` + `setAccessible` in `indexTopics` and delete the static field.
   - **If we skip it:** About 6 extra lines and one more static to reason about. No bug.

4. **Moved Bukkit sources dropped out of the jev-lint file list** (`.jev-lint.yaml:4-12`)
   - **What this is:** c4bfb121 moved 11 files (`Argument`, `BukkitCommandActor`, `CommandLibDiagnosticsCommand`, `argument/*`) from `spigot/src/main/java` and `paper/src/main/java` to `common/src/bukkit/java`.
   - **Problem:** `.jev-lint.yaml` lists source roots by path and does not list `common/src/bukkit/java`, so the manual comment/name lint no longer sees those files. `common/src/adventure/java` has the same gap from before.
   - **Fix:** Add `- common/src/bukkit/java` (and `- common/src/adventure/java`) under `files:`.
   - **If we skip it:** Those files are no longer checked when someone runs jev-lint.

Verdict: fix 1 first.
Lean: -6 lines possible.
Not checked: I did not build or run any tests (unit, integration, or NMS module compilation). I did not check whether the switch in cbe80b1e from `Caption.class.getResourceAsStream` to `ClassLoader.getResourceAsStream` still finds `lang/commandlib_en.properties` when CommandLib is shaded into a Forge/NeoForge mod that is an explicit JPMS module. If it doesn't, `Caption`'s static setup throws.

## 3. What the review could not cover

- No Gradle build or test run, by design (read-only). Whether the shared-source setup (`common/src/bukkit/java` and `common-testing/src/bukkitTest/java` added as `srcDir` in two projects), the version catalog (`rootLibs`, and `findLibrary` in the applied integration-test script) and the NMS removals compile and pass was not checked.
- Integration tests (testcontainers servers), the modded Stonecutter nodes and the `nms-codegen --check` script were read but not run.
- Docs, CHANGELOG and review-records were only read where they explain the code (`docs/permissions.md`, the CHANGELOG entries, commit messages). Their wording was not reviewed.
- Two possible issues are left unconfirmed rather than reported:
  - The class-loader resource lookup above, which may matter only for explicit JPMS module layers.
  - `getInput(name)` / `hasInput(name)` still mix command literals with argument values. Commit cb396b27 says this stays pending as 20-F05, so it is a known, tracked item.
