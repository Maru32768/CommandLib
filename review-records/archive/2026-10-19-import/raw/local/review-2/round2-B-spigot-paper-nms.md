# Round 2 review B: spigot / paper / spigot-kotlin / paper-kotlin / nms / buildSrc / tools/nms-codegen

HEAD = `0ccd8a27` (master). Review range: `2c8ffd7a..HEAD`, with most attention on the round-1 fix commits in this area:
`5398205f` (B-1), `cd9eec62` (B-6), `06c4780a` (B-10), `7009ee57` (B-9), `7a35eeea` (B-3), `a6e97500` (B-4),
`2884c3bd` (B-2), `2b3561dd` (sendComponent), `a3a3b327` (BungeeChatJson for Spigot 1.20.3), `59930f89` (Paper
unregister label ownership). spigot-kotlin, paper-kotlin and buildSrc have no changes since round 1. The only NMS change
is spigot-1.20.3's two Bungee chat wrappers.

How I checked:
- I read every diff since `6b898637` in the area, plus the Spigot and Paper `CommandLib` registration and unregister
  paths, `PaperDispatcher`, `HelpTopics` and `FoliaCommandScheduler` in full.
- I ran `python3 tools/nms-codegen/generate.py --all` in the worktree. `git status` showed no change, so the
  committed NMS sources match the generator, including the new 1.20.3 `TO_JSON` branch.
- I patched the local Folia 26.3 paperclip jar (`-Dpaperclip.patchonly=true`, in the scratchpad) and read the bytecode
  of `TickRegionScheduler`, `RegionScheduleHandle` and `RegionShutdownThread` with javap, to check the B-4 fix.
- I checked which bungeecord-chat each spigot-api POM in `~/.m2` declares. 1.20.1 to 1.20.3 use `1.20-R0.1`, and
  1.20.4 to 1.21.4 use `1.20-R0.2`. This fits the `V == (1, 20, 3)` flag in `mojang.py`, but see "Checked" below.
- No Gradle build, no NMS build.

Parity: Spigot and Paper still have the same public argument classes. Paper adds only the package-private
`ProfileWordArgumentType` and `SelectorResults`. The behaviour differences that remain are the triaged Pending items
(Paper `reset`, B-5).

---

## R2B-1: bug (parity, half-done fix), low. Spigot `unregister()` still removes the namespaced label that a later registration took

- File: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:881-887` (`unregisterCommands`)
- What is wrong: `743befb1` gated the removal of the plain label with `ownsLabel(...)`. The `<plugin>:<label>` line
  just below it still runs unconditionally, with the comment "The namespaced label names this plugin, so it is this
  registration's". That is false when the same plugin registers the same label twice, which is exactly what
  `unregisteringKeepsALabelTakenSince` does.
  - The second registration replaces both labels. On the vanilla path, `addNamespacedCommand` puts the second
    wrapper and a copied node. On Paper 1.20.6+, `addToPaperDispatcher` replaces the root children.
  - `first.unregister()` then deletes `testplugin:commandlibowned`, which belongs to the second registration.
  - Paper's fix (`59930f89`) checks `live.owns(label)` for every recorded label, including the namespaced one. So
    Paper keeps it and Spigot deletes it.
- Evidence: control flow read. Unconfirmed at runtime, because the Spigot and Paper scenarios only dispatch the plain
  label.
- Fix: gate the namespaced removal the same way as the plain label.
  - Vanilla path: remove it only when `knownCommands.get(ns)` is the wrapper this registration created. Use
    `wrappedVanillaCommand(...) == node`, since the namespaced label holds the same wrapper.
  - Paper 1.20.6+ path: remember the copy node made in `addToPaperDispatcher`, and compare it with
    `root.getChild(ns)`.
  - In both scenario fixtures, also dispatch `<plugin>:commandlibowned` and expect the later registration to run.

## R2B-2: doc-wrong, low. The CHANGELOG still says Folia allows register/unregister on any region thread

- File: `CHANGELOG.md:89-90`
- What is wrong: it says register and unregister throw "off the main thread (on Folia, off a region or the global
  region thread)". Since `a6e97500`, Paper's `requireGlobalThread` refuses every region thread on Folia and accepts
  only the global region thread or the server thread outside the tick loop. The class Javadoc, the exception message
  and `docs/execution.md:173-174` already say "global region thread". The fix commit did not update the CHANGELOG.
- Fix: change it to "(on Folia, off the global region thread)". Leave `refresh`/`refreshAll`, which do accept any
  region thread, as they are.

## R2B-3: test-gap, low. No real-server case unregisters, at runtime, a Paper registration made through the COMMANDS handler

- Files: `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:79-91,555-561` (`Live.of`/`owns`, used by
  `unregister`), and `integration-test/shared/paper/.../ScenarioTest.java`
- What is wrong: since `59930f89`, `unregister()` removes a label only if the vanilla node under it is still the node
  captured right after `registrar.register(...)` in the COMMANDS handler.
  - The real-server cases that check removal cover only runtime registrations, which go through `registerDirectly`
    and `addNodes`: `runtimeRegisteredCommandIsExecutableUntilUnregistered` and `unregisteringKeepsALabelTakenSince`.
  - For registrations made during initialization, which is the main path, unregister runs only in the disable
    scenario, and nothing checks that the commands are gone.
  - If Paper replaced or re-wrapped a registrar-added node after the event, `owns()` would return false. The commands
    would then silently stay after `unregister()`. Examples of where that could happen: command-map forwarding,
    server aliases, or a future Paper change.
  - paper-testing's `CommandLibLifecycleTest` uses a plain or fake root, so it cannot catch this.
- Evidence: unconfirmed. I found no Paper code path that replaces those nodes, so this is a coverage gap, not a known
  bug.
- Fix: add a Paper scenario that registers a command during `onEnable` and runs it. Then call `unregister()` on that
  registration at runtime and dispatch both labels again, expecting "Unknown command".

---

## Checked, no finding

- **B-1 (Spigot rollback):** `INSTANCES.add` now comes before `enable()`, and `registerEvents` and the reload listener
  are inside the rolled-back block. When `unregisterCommands()` throws again, for example when NMS is missing, its
  `finally` still removes the permissions, listeners and help topics, and the second exception is added as suppressed.
  A Paper lifecycle handler that cannot be removed becomes a no-op, because `registeredCommands` is cleared. Bonus:
  `register` from `onLoad` (where `registerEvents` throws) is now rolled back too.
- **B-4 (Folia region threads):** checked on the Folia 26.3 bytecode.
  - `getCurrentRegion()` returns `TickThreadRunner.currentTickingRegion`. It is set only when the schedule handle has a
    region, so on the global tick it is null.
  - On other threads it returns `RegionShutdownThread.getRegion()`. `RegionShutdownThread.run()` calls `stopServer()`,
    which disables the plugins, before its region loop, and resets `shuttingDown` in `finally` blocks.
  - So `unregister()` from `onDisable` at shutdown, and `register()` from `onEnable` at startup, both pass. A player
    command (on a region thread) is refused, as intended.
  - Not run on a live Folia server; the roadmap already lists it as unverified.
- **B-3 (Folia retired):** the `retired` callback and the `execute(...) == false` fallback both send the task to the
  global region scheduler. Docs and tests (`FoliaCommandSchedulerTest`) are updated.
- **B-2 (Paper ChatColorArgument):**
  - Vanilla `ColorArgument` goes through `VanillaArgumentTypes`. `parseImpl` maps `ChatFormatting` to `NamedTextColor`
    by name and turns `reset` into `NOT_A_COLOR`, with en and ja captions.
  - `cast()` still receives the converted value, because `parsedArgMap` holds `parseImpl`'s result.
  - Both fixtures have a `reset` case, and docs/arguments.md documents the difference. Spigot vs Paper `reset` stays
    Pending in triage.
- **BungeeChatJson:**
  - It recurses through every object and array, converts only a four-int `id` under a `show_entity` `contents`, and
    leaves string ids alone.
  - The deprecated `new JsonParser()` still exists in Spigot 1.20.3's Gson. Unit test: `BungeeChatJsonTest`.
  - The 1.20.3-only flag matches the bungeecord-chat versions in the POMs. 1.20.4 to 1.21.4 ship `1.20-R0.2`, which
    reads int-array UUIDs as far as I know. The new selector `MessageArgument` case only ran on spigot-1.20.3 and
    spigot-1.21.11 after the fix, so 1.20.4 to 1.21.4 are unverified at runtime for this input.
- **B-6, B-9, B-10, sendComponent:**
  - B-6, B-9 and B-10 are correct. The B-10 test is updated.
  - `sendComponent` has no remaining references in code. `docs/design/rich-text-messages.md` mentions it only as the
    state before Phase 1, which is fine.
- **Paper `Live.owns`:**
  - With a `null` node or the plain-root test path, the capture compares equal.
  - `PAPER == null` with a real mirror root still works, because `ApiMirrorRootNode#getChild` returns the cached
    wrapper.
  - A reload rebuilds `Live` from the new registrar, so ownership resets correctly.
- **No backward-compat leftovers** in the round-1 fix commits.
