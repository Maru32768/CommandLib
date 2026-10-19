# A4 — Production correctness: Bukkit / Spigot / Paper / Folia integration and NMS

Reviewer: Agent A4. Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc. Read-only review; no Gradle run. All file:line
references are at HEAD.

## Findings

### A4-01 — P2 — Spigot `CommandLib.unregister()` is not idempotent; calling it from `onDisable()` throws

- Component: spigot `CommandLib`
- Location: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:715-717`, `:784-802`, `:845-853`
- Description: `unregister(boolean)` has no "already unregistered" guard. Its `finally` block always runs
  `updatePlayerCommandsLater()` (a `BukkitRunnable.runTask(plugin)`) and `compileFunctionsWithCommands()` when called
  through the public `unregister()`. Bukkit fires `PluginDisableEvent` before `JavaPlugin.setEnabled(false)` /
  `onDisable()`, so the `onPluginDisable` handler (`:650-680`) has already unregistered every instance by the time
  `onDisable()` runs, and `isEnabled()` is already false inside `onDisable()`. A plugin that does the natural
  `onDisable() { commandLib.unregister(); saveData(); }` then hits `CraftScheduler.validate` ->
  `IllegalPluginAccessException("Plugin attempted to register task while disabled")`. Bukkit logs it as "Error occurred
  while disabling", and every statement after `unregister()` in `onDisable()` is skipped, which can include saving data.
  A second runtime `unregister()` also triggers a needless tree resend, `recalculatePermissions` for every player, and
  a full function recompilation.
- Contract: the paper artifact makes the same call a no-op (`paper/.../CommandLib.java:528-537` returns early on
  `State.UNREGISTERED`). Spigot and Paper should behave the same way, and an unregister method is expected to be safe
  to call twice.
- Trigger: `commandLib.unregister()` in `onDisable()`, or called twice at runtime.
- Evidence: Verified-by-trace (the event fires before `onDisable()` in `JavaPluginLoader.disablePlugin`; `runTask`
  validates `plugin.isEnabled()`).
- Test coverage: none. spigot has no lifecycle unit test, and the integration `ScenarioTest`/`RuntimePermissionTest`
  call `unregister()` only once, at runtime.
- Confidence: high.
- Suggested correction: keep an `unregistered` flag (or check `INSTANCES.remove(this)`'s return value) and return
  early. Also skip scheduling when `!plugin.isEnabled()`.

### A4-02 — P2 — Paper artifact: a first `register()` after initialization always fails under the documented packaging; the "Direct" fallback is effectively dead code

- Component: paper `CommandLib` / `PluginRegistrationState.Mode.Direct`
- Location: `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:52`, `:327-348`, `:573-633`;
  `PluginRegistrationState.java:196-201`
- Description: when Paper refuses the COMMANDS handler (any call after `onEnable` returns), `doRegister` falls back to
  the static `serverDispatcher`. Only a COMMANDS handler that `register()` installed **in the same copy of
  CommandLib** sets it (`:581`); `bootstrap()` handlers do not. docs/packaging.md requires every plugin to shade and
  relocate CommandLib, so each copy belongs to one plugin. A plugin whose first registration is late therefore always
  gets `IllegalStateException("... no dispatcher is cached yet ...")`. If it registered earlier, its mode is already
  `Handled` and the `Direct` branch is never taken. `reregisterDirectStates` re-adds `Direct` registrations only from
  other plugins' handlers in the same copy, which also do not exist under relocation. The diagnostics text
  ("added to the live dispatcher (registered after the plugin's initialization) ...", docs/diagnostics.md:154) and
  the unit test `fallback_registration_is_restored_after_reload` (`paper-testing/.../CommandLibLifecycleTest.java:258`)
  cover a setup that cannot happen in production: two plugins sharing one unrelocated copy.
- Contract / parity: the spigot artifact on Paper 1.20.6+ supports late registration and keeps it across
  `/minecraft:reload` through `ServerResourcesReloadedEvent` (`spigot/.../CommandLib.java:160-191`). The paper artifact
  already has a reflective lookup of the live dispatcher (`PaperDispatcher.live`, used by `addIfCommandsEventMissed`)
  but does not use it here.
- Trigger: a shaded plugin that registers its first CommandLib commands after `onEnable`, for example
  config-driven commands created by `/myplugin reload` or in a delayed task.
- Evidence: Verified-by-trace.
- Test coverage: only the unrelocated multi-plugin mock test above. No integration target registers late on the paper
  artifact without an earlier init-time registration.
- Confidence: high for reachability. Severity is P2 because it fails fast with a clear message rather than silently.
- Suggested correction: in the `Unhandled` fallback, use `PaperDispatcher.live(serverDispatcher)`. To survive data
  reloads, re-add the commands on `ServerResourcesReloadedEvent` (the listener already receives that event) instead of
  relying on another plugin's handler. Otherwise, document that late first registration is unsupported and remove
  `Mode.Direct`, `reregisterDirectStates` and the misleading diagnostics text.

### A4-03 — P3 — Spigot unregistration removes labels by name, without checking who owns them now, and never restores what registration displaced

- Component: spigot `CommandLib`
- Location: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:381-396` (enable: `knownCommands.remove(label)`
  and `CommandNodeChildren.replace(vanillaRoot, ...)`), `:856-882`, `:958-963` (unregister)
- Description: registration takes the label unconditionally. It replaces another plugin's command-map entry and
  replaces a vanilla node of the same name in the vanilla dispatcher, which destroys that node. Unregistration then
  removes the label from `knownCommands`, the synced root and the vanilla root without checking that the entry is
  still this registration's. Effects:
  1. When two plugins that each shade CommandLib define the same label, disabling or unregistering the first one at
     runtime (PlugMan, a runtime `unregister()`) deletes the second plugin's live command.
  2. A displaced vanilla or other-plugin command (for example a CommandLib `/give` or `/spawn`) is not restored, so
     the label disappears until restart.

  The paper artifact's `Handled` mode avoids (1) for labels Paper did not grant, because it records the labels
  `registrar.register` returned.
- Trigger: label collisions combined with a runtime unregister or plugin disable.
- Evidence: Verified-by-trace. Behavior on a real server is not reproduced.
- Test coverage: none. `MinecraftNamespacedCommandRemovalTest` checks wrapper identity only for the `minecraft:`
  label.
- Confidence: medium (edge case, but realistic with plugin managers).
- Suggested correction: remove a label only while `knownCommands.get(label)` is this registration's wrapper and the
  root child is this registration's node, as `removeMinecraftNamespacedCommand` already does. Optionally remember
  displaced entries and put them back. Document the label-conflict policy and how it differs between the artifacts.

### A4-04 — P3 — `register` / `unregister` do not enforce the thread contract that `refresh` does

- Component: spigot and paper `CommandLib`
- Location: `spigot/.../CommandLib.java:115-147`, `:715`; `paper/.../CommandLib.java:138-168`, `:400`
  (compare `requirePrimaryThread` at spigot `:707-713` and paper `:387-394`)
- Description: registration mutates `SimpleCommandMap.knownCommands` (a `HashMap`), the help map, the plugin manager's
  permission maps and the static `INSTANCES` `LinkedHashSet`, which is commented "Main thread only". It does this
  without a thread check. Permission plugins and config reloads often run asynchronously, so a call from such a
  thread races the main thread silently. `refresh()` fails fast for exactly this reason. docs/execution.md states the
  global-region-thread rule only for Folia.
- Evidence: Verified-by-trace (no check present). The race itself is not reproduced.
- Test coverage: none.
- Confidence: medium.
- Suggested correction: call `requirePrimaryThread()` (on Folia, the global tick thread) at the top of `register` and
  `unregister`, and document it for Spigot and Paper as well.

### A4-05 — P3 — Paper `MessageArgument` silently skips selector resolution when its reflection fails, and no test exercises resolution

- Component: paper `argument/MessageArgument`
- Location: `paper/src/main/java/dev/marulabs/commandlib/argument/MessageArgument.java:55-86`
- Description: selector resolution reads Paper's resolver through `getClass().getMethod("message").invoke(...)` and
  then a declared `resolveComponent` method by its Mojang name. If the first lookup or the invoke fails (for example
  the implementation record is not public, or a future Paper renames it), the code returns `null` and the raw text is
  used. `@p` then reaches the command unresolved, with no log. The spigot artifact always resolves selectors through
  vanilla `MessageArgument.getMessage`. The integration test (`integration-test/shared/paper/.../ArgumentTest.java:590-607`)
  sends only `hello, world!`, so it passes whether or not resolution works.
- Evidence: Plausible. I could not inspect Paper's `SignedMessageResolverImpl` visibility, because only the
  paperclip patch is on disk.
- Confidence: low-medium.
- Suggested correction: add an integration assertion with a selector (for example `@s` sent by the console or a
  player). Make the reflective fallback log once, or fail, when the server is Paper but the members are missing.

### A4-06 — P3 — Registration depends on writing `final` fields reflectively (JEP 500)

- Component: `common/CommandNodeChildren`, used by every Bukkit and Paper registration path
- Location: `common/src/main/java/dev/marulabs/commandlib/CommandNodeChildren.java:78-87`
- Description: the copy-on-write replacement sets Brigadier's `final` `children`/`literals`/`arguments` fields through
  `Field.set`. JDK 26 (JEP 500) warns on reflective mutation of final fields by default, and a later JDK is planned to
  deny it unless `--enable-final-field-mutation` is given. Minecraft 26.x already requires Java 25, so servers that
  move to a newer JDK will print warnings and, eventually, fail registration with `IllegalAccessException`.
- Evidence: Hypothesis about future JDK defaults. The current code works.
- Confidence: medium on direction, low on timing.
- Suggested correction: track it in the roadmap. Possible alternatives: `CommandNode#addChild` on a fresh root swapped
  by reference where the server allows that, `Unsafe`/`VarHandle` with a documented flag, or a pinned fallback
  message that names the JVM flag.

## Checked and found sound (no finding)

- Version detection and NMS selection: `BukkitUtil.getMinecraftVersion` takes `x.y[.z]` from `getBukkitVersion()`, and
  `NMSClassRegistry.resolve` throws `NMSClassNotFoundException` with the reasons and a paper-artifact hint when no
  module covers the version. An unknown or newer version therefore fails at `register()` (the first
  `NMSCraftServer.create`) instead of mis-mapping. `TypedClassLinkage` checks linkage before a module is used, and
  mapping probes split the Paper Spigot-named and Mojang-named packages.
- The `TypedNmsModules` ranges match `generate.py` `MODULES`. settings.gradle.kts requires every `nms/*` directory to be
  in the table, and publishing refuses a jar built without some modules. Typed classes are compiled with
  `typedNmsRelease`: the 26.x module classes are class-file major 60 in the built jar, so Maven Shade 3.6.0 and older
  ASM can read them.
- Generator (`mojang.py`): checked the new argument wrappers per version gate. They cover: Vec3 `centerIntegers`;
  BlockPos without load checks; Item/BlockPredicate build-context and throws gating; MinMaxBounds getters before and
  after 1.20.2 and reflection for 26.x; ComponentArgument `getRawComponent` from 1.21.5; ScoreboardSlot number vs
  `DisplaySlot` at 1.20.2; Color vs TeamColor at 26.2 resolved by name; TimeArgument minimum from 1.19.4 (spigot
  `TimeArgument` re-checks the minimum for older servers); `sendSuccess(Supplier)` from 1.20. Spot-checked the
  generated spigot-1.16.5, spigot-1.19 and paper-1.21.1 classes.
- `FunctionCompilationMarker`: the system-property names survive relocation. The `#load` re-run heuristic depends on
  vanilla dropping a whole tag when a referenced function is missing, which is correct.
- Folia scheduler: work for an entity runs on the entity scheduler and runs inline when the current region owns the
  entity. Command blocks use the region scheduler, and other senders use the global region scheduler. Removed
  entities drop the task, as documented. The spigot artifact refuses Folia. `ServerFunctions` is off on Folia.
- Sender mapping: `getSender()` stays the original sender under `/execute as` on both artifacts, and `getEntity()` is
  the executor. `BukkitCommandActor` classifies remote console, console, player, entity and command block.
- Paper `unregister` is idempotent, removes only the labels it recorded, keeps going after a failed label, and on
  disable updates the trees at once with copy-on-write maps.
- Spigot and Paper argument class sets match. Paper adds only `ProfileWordArgumentType` and `SelectorResults`.

## Coverage

- In depth: spigot `CommandLib` (whole file), paper `CommandLib`, `PaperDispatcher`, `PluginRegistrationState`,
  `FoliaCommandScheduler`, both `BukkitCommandScheduler`s, both `CommandContext`s, `BukkitCommandActor`,
  `CommandPermissions`, `CommandSet`, `ServerFunctions`, `NMSClassRegistry`, `TypedNmsModules`, `NMSClass`,
  `NMSArgument`, `NMSArgumentChatFormat`, `NMSArgumentTime`, `CommandNodeChildren`, `FunctionCompilationMarker`,
  `mojang.py` diff, `generate.py` module table, settings.gradle.kts NMS inclusion, spigot build.gradle.kts packaging.
- Skimmed: `TypedClassLinkage` (header and strategy only), the spigot and paper argument diffs, `MessageArgument`
  (both), `ScoreboardDisplaySlotArgument`, `CommandLibDiagnosticsCommand`, `NMSDedicatedServer.keepPendingLoad`,
  `paper-testing` `CommandLibLifecycleTest` (names and two tests), generated classes of spigot-1.16.5, spigot-1.19
  and paper-1.21.1.
- Not examined: `spigot_legacy.py` diff, buildSrc `SpigotNmsPlugin` and `BuildToolsTask` beyond the release setting,
  paper `HelpTopics` and `RichTexts`, `FunctionLoadErrorFilter`, spigot `annotation/AnnotatedCommands`, the remaining
  ~25 nms modules, Mohist-specific paths, and the paper `MessageArgument` resolver against a real Paper jar (only the
  paperclip patch was available). No Gradle build or test was run.
