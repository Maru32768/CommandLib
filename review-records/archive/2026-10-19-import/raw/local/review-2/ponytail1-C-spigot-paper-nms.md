# ponytail review 1 / C: spigot, paper, kotlin, nms, codegen

Range: `git diff 2c8ffd7a..HEAD` (HEAD = master 16926819) on spigot/src/main, paper/src/main, spigot-kotlin,
paper-kotlin, nms/** (looked at the generator plus nms/paper-1.21.1 and nms/spigot-26.1.2), tools/nms-codegen and
buildSrc (buildSrc has no change in the range).

What this change does: Spigot and Paper get the new vanilla-backed arguments (block position, item and block predicate,
int and float range, component, message, NBT, time, registry, vanilla-named display slots and colors) on both platforms.
On Spigot each one goes through a new NMS wrapper that `tools/nms-codegen` generates into all 27 modules. The commit
also adds `CommandDefaults`, diagnostics, `refresh`, Folia scheduling and thread checks on Paper, safer unregister
(a registration only removes labels it still owns), and Kotlin modules for both platforms.

Load assumed: a plugin on one server. Commands are parsed on the main thread and a handful run per tick, so
reflection per parse is fine.

Argument parity: the argument class lists are the same on both platforms (Paper also has its internal
`ProfileWordArgumentType` and `SelectorResults`). Good.

## Must fix

### C1. Paper `ChatColorArgument` cannot be built on Paper 26.2 and 26.3
- category: bug, severity: high
- file: `paper/src/main/java/dev/marulabs/commandlib/util/brigadier/VanillaArgumentTypes.java:26-28`
  (used by `paper/src/main/java/dev/marulabs/commandlib/argument/ChatColorArgument.java:22`)
- What this is: since fix B-2 (2884c3bd), Paper's `ChatColorArgument` builds the vanilla `color` argument by name:
  `Class.forName("net.minecraft.commands.arguments.ColorArgument").getMethod("color")`.
- Problem: Minecraft 26.2 renamed that class to `TeamColorArgument`, with the factory `teamColor()`. Paper runs with
  Mojang names, so on Paper 26.2 and 26.3 the lookup fails and `new ChatColorArgument(..)` throws
  `IllegalStateException("Could not create the vanilla argument type ...")`. Any plugin that uses it fails to build its
  commands. The Spigot side already handles this (`NMSArgumentChatFormat.COLOR_ARGUMENT_CLASSES`), but the Paper fix
  did not copy it.
- Evidence: the Spigot 26.2 server jar (Mojang names) in `~/.m2/.../spigot-26.2-R0.1-SNAPSHOT.jar` has only
  `net/minecraft/commands/arguments/TeamColorArgument.class`, with `public static TeamColorArgument teamColor()`, and
  the same holds for 26.3. The last `paper-26.2` and `paper-26.3` integration results are from 2026-10-17 09:46 and
  09:48. 2884c3bd landed at 16:04 that day. After it, triage lists only paper-1.21.11, which still has `ColorArgument`.
  paper-testing swaps out `VanillaArgumentTypes` for doubles, so no unit test notices. Not run on a server (no Docker
  here).
- Fix: in `color()`, try `TeamColorArgument`/`teamColor` first, then `ColorArgument`/`color`, as
  `NMSArgumentChatFormat.colorArgumentClass()` does. That is about 6 lines. `parseImpl` already works on `TeamColor`
  (it is an enum with the same constant names). Then run `:integration-test:targets:paper-26.2:minecraftIntegrationTest`.
- If we skip it: on the two newest Paper releases, every plugin with a `ChatColorArgument` (or an annotated
  `NamedTextColor` parameter) fails at registration.

## Should fix

### C2. Spigot `ChatColorArgument` accepts `reset` only up to 26.1, and the new fixture test expects it on every version
- category: bug (doc + test), severity: medium
- file: `spigot/src/main/java/dev/marulabs/commandlib/argument/ChatColorArgument.java:9-12` (javadoc),
  `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/ArgumentTest.java:245-259`
- What this is: the Spigot javadoc says the argument takes "a color name ... or `reset`". The `chatColorArgumentReset`
  scenario, added in 2884c3bd, expects `reset` to parse to `ChatColor.RESET`.
- Problem: from 26.2 the vanilla type is `TeamColorArgument`. Its `TeamColor` enum has the 16 colors and no `RESET`
  (javap of the 26.2 jar), so the server rejects `reset` while parsing. On spigot-26.2 and spigot-26.3 the scenario
  stays at "command not executed" and fails. The javadoc is wrong for those versions. The last spigot-26.2 run
  (10-17 10:29) is older than the scenario.
- Fix: say "`reset` up to 26.1" in the javadoc, and in the fixture expect a parse failure from 26.2. This also bears on
  the open triage item B-2 ("should Paper accept `reset` like Spigot?"): vanilla itself dropped `reset` in 26.2. So
  "reject `reset` on both platforms" is the version-stable answer, and it would let the Spigot argument behave the
  same on every version.
- If we skip it: a red integration target on the newest Spigot, and docs that promise input the server refuses.

### C3. No check that the committed NMS sources match the generator
- category: test-gap, severity: low
- file: `tools/nms-codegen/` and `nms/*`
- Problem: this range changes `mojang.py` and `spigot_legacy.py` by about 400 lines and the 27 generated modules by
  about 11k lines. Nothing in the build re-runs the generator and compares the result, so a hand edit to a generated
  class, or a generator change that someone forgot to regenerate, ships without anyone noticing. I could not
  regenerate here: a run deletes the module sources first, and the sandbox refused that command. So drift is
  unconfirmed.
- Fix: one CI or roadmap step: `uv run generate.py --all && git diff --exit-code nms spigot/src/main/java/**/TypedNmsModules.java`.
  It is a single command, with no new code in the repo.

## Nice to have

None worth the churn. Small things I saw and dropped: `NMSClassRegistry.moduleStatus` calls `matchesMappings` twice
(about 1 line); `spigot-kotlin/build.gradle.kts` has two `dependencies {}` blocks (about 2 lines); on 26.1.x,
`NMSArgumentChatFormat.colorArgumentClass()` throws and catches one `ClassNotFoundException` per parse.

## Checked and fine

- Spigot register: `INSTANCES.add` before `enable()`, plus the guard in `unregister(boolean)`, makes the rollback
  work. A failing `registerEvents` (register from `onLoad`) is now rolled back too.
- Owner checks on unregister: Spigot `ownsLabel`/`paperLabelNodes` and the vanilla root identity check; Paper
  `State.Live.owns` through `PaperDispatcher.child`.
- Paper: removing `Mode.Direct` and throwing on a late first registration. `register` rolls back permissions and the
  listener on that throw.
- Function recompile `loadMissed` (both generators): it converges for a player `/minecraft:reload` and for a reload
  run by a plugin task. A data pack without `#load` stays a no-op.
- `SEND_SUCCESS_TAKES_SUPPLIER` (1.20), the 1.20.3-only `BungeeChatJson` path, the `TimeArgument` minimum below
  1.19.4, and the display slot names before and after 1.20.2.
- Folia scheduler: entity, block and global paths, and the fallback for a retired entity.
- Already in triage, not re-reported: B-5 (async `sendSuccess`), B-7 (unused `NMSEntity`), B-8 (Paper reflection
  caching), B-2 (Paper `reset`, see C2), the permission override Pending, D-32 (duplication).

Verdict: fix C1 first, and C2 with it.

Not checked: whether `nms/*` matches the generator (regeneration was refused); no server runs (no Docker). The Paper
reflection paths changed after 10-17 morning (`PaperDispatcher.child`, the C1 path) have been run only on paper-1.21.11,
not on 26.x.
