# Round 1 review B: spigot / paper / spigot-kotlin / paper-kotlin / nms / buildSrc

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (HEAD = `6b898637`), restricted to `spigot/src/main`,
`paper/src/main`, `spigot-kotlin`, `paper-kotlin`, `nms/**`, `buildSrc/**` (no changes in range), and the root
`build.gradle.kts` / `settings.gradle.kts`.

Method: read every changed file under spigot/paper main and the build scripts. For the NMS modules, read
spigot-1.16.5, spigot-1.20.4 and spigot-26.1.2 in full for every new or changed wrapper. Then a script normalized
every module's classes (package suffix replaced, imports dropped) and grouped the variants of each new wrapper across
all 27 modules: NMSArgumentBlockPosition, BlockPredicate, Chat, ChatComponent, ChatFormat, FloatRange, IntRange,
ItemPredicate, NBTTag, ScoreboardSlot, Time, Vec3D, CommandListenerWrapper and DedicatedServer. Every variant boundary
falls where the Minecraft API changed (1.16.5 Spigot names, 1.17-1.18.2 without a build context, 1.19.4 time
minimum, 1.20.2 MinMaxBounds records, 1.21.5 raw components, 26.x reflection), and nothing differs inside a family.
`TypedNmsClasses` lists every class in every module (52/52, or 50/50 up to 1.18.2). The `code-review` skill also ran
on the same range, and its findings are merged in below after I checked them myself. No Gradle build was run.

Argument parity (Spigot vs Paper): both have the same public argument classes, and Paper adds only the package-private
`ProfileWordArgumentType` and `SelectorResults`. This range adds BlockPosition, BlockPredicate, Component,
DoubleRange, Duration, IntegerRange, ItemPredicate, Message, NBTCompound, Registry and Time on both platforms, and
moves ChatColor, EntityType and ScoreboardDisplaySlot to vanilla syntax on both. Behaviour differs only in B-2.

---

## B-1: bug, medium. A failed Spigot registration no longer rolls back

- File: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:792` (guard), `:133-147` (constructor)
- What is wrong: commit `a7582051` made `unregister(boolean)` return at once when `INSTANCES.remove(this)` is false.
  The constructor adds the instance to `INSTANCES` only after `enable()` succeeds (line 147). If `enable()` throws,
  the constructor's rollback `unregister(false)` (line 138) finds the instance absent and does nothing. The comment
  just above it says "The disable listener is not registered yet, so nothing else would remove what was added so
  far", and now nothing removes it at all.
- Failure scenario: `enable()` builds the nodes, then calls `registerPermissions()`, which adds every permission to
  the PluginManager. Then `knownCommands()`, `NMSCraftServer.create` or the vanilla-dispatcher loop throws. A real
  case: the spigot artifact on Paper after 1.21.1 throws `NMSClassNotFoundException` by design (docs/agents/nms-build.md).
  Another case: the loop fails halfway through the commands. In both, the permissions stay registered, and the nodes
  and wrappers added so far stay in the vanilla root, the synced root, `knownCommands` and the HelpMap until the server
  restarts. A retry after fixing the cause finds stale permissions and labels. Paper's `CommandLib` does not have this
  problem, because its guard is a per-instance `state` that starts out not unregistered.
- Evidence: control flow read directly (constructor, then catch, then `unregister(false)`, then
  `INSTANCES.remove == false`, then return). No test covers the constructor's failure path, because spigot-testing's
  `CommandTester` does not go through the constructor. The code-review skill found it independently.
- Fix: add the instance to `INSTANCES` before `enable()`. `onPluginDisable` and the guard then see it, and the
  rollback removes it. Or give the rollback its own path that skips the guard. Add a test for a failing `enable()`,
  for example an NMS mock that throws from `getCommandDispatcher`.

## B-2: bug (parity), medium. Paper `ChatColorArgument` fails with an internal NPE on `reset`, which Spigot accepts

- File: `paper/src/main/java/dev/marulabs/commandlib/argument/ChatColorArgument.java:17`
- What is wrong: Paper's `ArgumentTypes.namedColor()` wraps vanilla `ColorArgument` and converts the parsed value
  with `requireNonNull(result.getColor(), ...)` inside `parse`. Vanilla `ColorArgument` accepts `reset` and the client
  suggests it (`ChatFormatting.getNames(true, false)` includes RESET), but `RESET.getColor()` is null. Paper therefore
  throws a `NullPointerException`, not a `CommandSyntaxException`, while Brigadier parses the input. Brigadier
  catches only `CommandSyntaxException`, so the player gets an unexpected error, or nothing, plus a stack trace in the
  log, instead of a syntax error. The Spigot `ChatColorArgument` documents `reset` and returns `ChatColor.RESET`.
- Evidence: Paper 1.21.1 server source in the local paperweight cache
  (`nms/paper-1.21.1/.gradle/caches/paperweight/taskCache/mappedServerJar.jar`,
  `VanillaArgumentProviderImpl#namedColor` and `NativeWrapperArgumentType#parse`, which calls the converter inside
  `parse`). Not reproduced on a server (unconfirmed at runtime).
- Fix: on Paper, use the vanilla `color` type through `VanillaArgumentTypes`, like `compoundTag()`, wrapped in a
  `CustomArgumentType` that maps RESET either to an explicit syntax error or to a value the return type can hold. Or
  return `TextColor`/`Optional` and handle reset. At minimum, document that `reset` is rejected and turn it into a
  proper `CommandSyntaxException`. Add a `reset` case to both integration fixtures.

## B-3: bug, medium. Folia: the follow-up of an async command is dropped silently when its entity is removed, so middleware completion hooks never run

- File: `paper/src/main/java/dev/marulabs/commandlib/FoliaCommandScheduler.java:72-80`
- What is wrong: the sync executor for an entity source calls `entity.getScheduler().execute(plugin(), task, null, 1L)`
  with no `retired` callback, and ignores the `false` it returns. `CommandRunner.step` and `whenComplete` (common,
  lines 236-249) bring every async completion back through `scheduler.sync(ctx)`. A dropped task leaves that
  `whenCompleteAsync` future incomplete forever. The class Javadoc does mention the drop, but the consequence goes
  beyond "the follow-up is dropped".
- Failure scenario: a player runs an `executeAsync` command wrapped by a middleware that takes a per-player lock or
  cooldown and releases it in its completion hook. The player logs out while the async work runs. Their entity
  scheduler is retired, so the completion task is dropped. The middleware's after-hook never runs, which leaves the
  player locked out after rejoining, and `reportLateFailure` never reports an error. On Bukkit the same drop happens
  only when the plugin is disabled. On Folia it happens on every logout or entity death.
- Evidence: code read (Folia `EntityScheduler#execute` returns false and runs `retired` when the entity is removed).
  The code-review skill reported it, and I checked it against `CommandRunner`.
- Fix: pass a `retired` callback that runs the task on the global region scheduler. Middleware hooks and failure
  reporting do not need the entity's thread. Or, when `execute` returns false, run the task through the global region
  scheduler. Document what thread a follow-up runs on for a removed entity.

## B-4: bug, low. Folia: `requirePrimaryThread` accepts any region thread, but `unregister()` is documented as global-region only

- File: `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:381-386`, `:390-394`
- What is wrong: on Folia `Bukkit.isPrimaryThread()` is true on every tick thread, so `register()`/`unregister()`
  pass the check on any region thread. They still change shared state: the dispatcher root through
  `PaperDispatcher.replaceChildren`, `HelpTopics`, and `CommandPermissions`. The Javadoc of `unregister()` says
  "on Folia the global region thread", and the error message says "a region or global region thread".
- Failure scenario: a plugin calls `unregister()` from an entity or region task on Folia. The check passes, and the
  plugin changes the help and permission maps concurrently with another region thread, which can lose updates or
  throw CME.
- Fix: on Folia (`FoliaCommandScheduler.ON_FOLIA`), make register/unregister require the global tick thread
  (`IS_GLOBAL_TICK_THREAD`). Keep the looser check for `refresh`/`refreshAll`, where it is documented. Align the
  message with the Javadoc.

## B-5: spec-question, low. `sendSuccess` now calls vanilla `CommandSourceStack.sendSuccess` on the caller's thread

- File: `spigot/src/main/java/dev/marulabs/commandlib/CommandContext.java:126-135`,
  `paper/src/main/java/dev/marulabs/commandlib/CommandContext.java:125-163`
- What is wrong: `sendSuccess` used to be `sender.sendMessage`, which is tolerated off the main thread. It now runs
  vanilla feedback. With `broadcastToOps`, that iterates `server.getPlayerList().getPlayers()` and reads gamerules.
  docs/execution.md tells users to come back to the sync thread, but nothing enforces it. A
  `ctx.supplyAsync(() -> { ...; ctx.sendSuccess("x", true); })` now touches server internals from the async pool (CME
  risk, unconfirmed). The new behaviour also honours `sendCommandFeedback=false` for non-broadcast success messages,
  which is intended and documented.
- Fix: either hop to `syncExecutor()` inside `sendSuccess` when off-thread, or document that `sendSuccess`/`sendFailure`
  must run on the sync executor (`ctx.syncExecutor()`), unlike `sendMessage`.

## B-6: improvement, low. `ScoreboardDisplaySlotArgument.vanillaName` can break class initialization on an unknown `DisplaySlot`

- File: `spigot/src/main/java/dev/marulabs/commandlib/argument/ScoreboardDisplaySlotArgument.java:816-828` (via the
  static `SLOTS` at :780)
- What is wrong: every constant other than PLAYER_LIST, BELOW_NAME and SIDEBAR is assumed to start with `sidebar_`, and
  `substring(8)` runs in a static initializer. A future Bukkit, Paper or fork constant outside that pattern throws
  `StringIndexOutOfBoundsException` or maps wrongly, and every later use fails with `NoClassDefFoundError`. All the
  constants current Spigot and Paper have work.
- Fix: skip constants that do not match `sidebar_(team_)?<color>` instead of computing a name for them.

## B-7: improvement, low. Dead NMS wrapper `NMSEntity` (27 implementations) and unused `NMSClass` accessors

- Files: `spigot/src/main/java/dev/marulabs/commandlib/util/nms/world/NMSEntity.java`, `nms/*/.../NMSEntity_*.java`
  (all 27 modules, each touched in this range for `requireHandle`), the `NMSEntity` entries in
  `tools/nms-codegen/mojang.py:674` and `spigot_legacy.py:460`, and `NMSClass#getServerClass` (unused) and
  `NMSClass#getHandle` (used only by `spigot-testing`'s `MockNMSCommandListenerWrapper`).
- What is wrong: nothing calls `NMSEntity.create` in spigot, spigot-testing or the integration fixtures, at base or at
  HEAD. Even so, every module generates, lists and link-checks the class (`TypedClassLinkage`, `TypedNmsTest`), and
  each regeneration has to keep it compiling.
- Fix: delete `NMSEntity`, its 27 implementations and its codegen entries, and regenerate `TypedNmsClasses`. Delete
  `getServerClass()`. Move the mock to `requireHandle()` or a protected accessor and drop the nullable public
  `getHandle()`.

## B-8: improvement, low. Paper hot-path reflection, and fallbacks for test doubles in production code

- Files: `paper/src/main/java/dev/marulabs/commandlib/argument/BlockPredicateArgument.java:54-65`, which runs
  `Class.forName` and two `getMethod` calls on every parse; `paper/.../CommandContext.java:139-163`, which runs
  `getMethod("sendSuccess", …)` on every message and falls back to `sender.sendMessage` "for a test mock";
  `paper/.../argument/MessageArgument.java:532-536`, where a class-name check exists for test doubles; and the
  invoke-and-unwrap logic copied in `BlockPredicateArgument.invoke`, `MessageArgument` and `CommandContext`.
- What is wrong: the lookups never change. Branches whose only job is to let paper-testing's doubles pass make
  production behaviour depend on the class name or shape of the source. `VanillaArgumentTypes` already shows the
  cleaner pattern: paper-testing replaces the class.
- Fix: resolve the `Method`s once, lazily, into static holders. Move the reflective vanilla access, including
  feedback and message resolution, into one helper next to `VanillaArgumentTypes` that paper-testing substitutes, and
  drop the test-only fallbacks from the production classes.

## B-9: improvement (API parity), low. Spigot `register(Plugin, Collection<Command>)` is narrower than every other overload and than Paper

- File: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:69`
- What is wrong: `Collection<Command>` rejects a `List<MyCommand>`. Paper's overload and the Spigot overloads with
  `permissionPrefix`/`defaults` all take `Collection<? extends Command>`.
- Fix: change the parameter to `Collection<? extends Command>`. This is a breaking change, which is allowed.

## B-10: improvement, low. Spigot `diagnostics()` is documented as main-thread only but does not check it

- File: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:739-766`
- What is wrong: it iterates the main-thread-only `LinkedHashSet INSTANCES`. A call from an async web or admin task
  while a plugin disables throws CME. register, unregister and refresh all call `requirePrimaryThread`. Paper's
  `diagnostics()` copies under locks instead.
- Fix: add `requirePrimaryThread("Diagnostics must be read")`.

---

## Backward-compatibility leftovers

I searched the area for `@Deprecated`, "compat", "legacy", "kept for", "older" and "fallback". The area has no
deprecated members and no compatibility aliases. The range itself deleted old paths: Paper's `Mode.Direct` /
`reregisterDirectStates`, the `PlatformAdapter#hasPermission(Command, source, …)` overloads, and the old text-only
error messages. The reflection that remains serves supported versions and is legitimate:
`usesPaperCommandDispatcher` for Paper before and after 1.20.6, `NMSArgumentChatFormat` / `NMSArgument.bound` for the
26.1-to-26.2 renames, `rangeArgument` and the 1.16.5 barrier proxy for Mohist 1.16.5, and `BlockPredicateArgument`
reflection for Paper 1.21-1.21.7, marked with a `shortcut:` comment. The only removable leftover I found is
pre-existing duplicate API: `sendComponent(C)` on both platform `CommandContext`s, from the abstract
`CommonCommandContext#sendComponent` in common, has no caller anywhere and duplicates `sendMessage(C)`. The root
belongs to common, so it is outside this report's area, and it should be deleted there together with these two
overrides.

## Checked and not reported

- Paper `convertCommandSyntaxException` now sends only the raw message, without the `<--[HERE]` context. Spigot has
  always done the same, so the platforms now agree.
- Paper `register()` now throws when the first call comes after initialization (`Mode.Direct` removed). This breaking
  change is intended and documented.
- `NMSDedicatedServer#recompileFunctions(…, reloaded)` with `keepPendingLoad(…, loadMissed)` is consistent in all 10
  variants. The #load re-run only happens when the server's own compilation dropped the tag and CommandLib's has it.
- Root `build.gradle.kts`: Error Prone on `:nms:*` with `max(toolchain, 21)` and no `--release` override, `LICENSE`
  in every Jar with `EXCLUDE` for duplicates, and Kotlin modules inheriting toolchain 11 (spigot-kotlin) or 21
  (paper-kotlin). I found no problem.
