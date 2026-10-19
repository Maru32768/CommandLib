# ponytail review 2 / C: spigot, paper, kotlin, nms, codegen, buildSrc

Range: `git diff 2c8ffd7a..HEAD` (HEAD = master 02fbf5f9) on spigot/src/main, paper/src/main, spigot-kotlin,
paper-kotlin, nms/**, tools/nms-codegen, buildSrc. I looked hardest at the commits after 16926819 that touch these
paths: 68657e57 (delete `PlatformAdapter.hasPermission(command, ctx, prefix)`), 72f88a97 and 8cbe419e (Paper
`ChatColorArgument` on 26.2+ through `TeamColorArgument`, plus javadoc), and 02fbf5f9 (Paper `RichTexts` moved to
`common/src/adventure`). buildSrc has no change in the range. I skipped the Paper async command-tree
ConcurrentModificationException, as the task said.

What this change does: since pass 1, Paper builds its chat color argument from `TeamColorArgument` on 26.2+ (vanilla
renamed `ColorArgument` there) and still falls back to `ColorArgument` on older servers. The javadoc on both platforms
now says `reset` is rejected from 26.2. The unused 3-argument `hasPermission` is gone from both adapters, and Paper's
`RichTexts` now lives in a source folder that Paper and Velocity share.

Load assumed: one plugin on one server. Commands are parsed on the main thread, a few per tick.

## Findings

None worth fixing.

## New evidence (checked and fine)

- **The generated NMS sources match the generator.** I ran `python -I tools/nms-codegen/generate.py --all` (Python
  3.10, standard library only) in the worktree. All 27 modules were regenerated (52 classes each for spigot-26.1.2,
  paper-1.20.6 and paper-1.21.1), and `git status` was clean afterwards. Pass 1 could not check this.
- **The 26.x Spigot module links on 26.2 and 26.3.** I disassembled the built `nms/spigot-26.1.2` jar (main checkout,
  built 10-16; its sources have not changed for 26.x since then). I listed every `net.minecraft` and
  `org.bukkit.craftbukkit` class, field and method it uses and looked each one up in the local
  `spigot-26.2-R0.1-SNAPSHOT.jar` and `spigot-26.3-R0.1-SNAPSHOT.jar`. All 65 classes are there. The only members
  my script did not find on the class itself are inherited ones, and I confirmed those on the superclass:
  `Vec3i.getX/Y/Z`, `MinecraftServer.resources/vanillaCommandDispatcher/getCommands/getFunctions/isRunning`,
  `Level.getWorld`. So no other class in the module was renamed the way `ColorArgument` was. The only lookup by
  name is the color argument in `NMSArgumentChatFormat`, which already tries both names.
- **Paper's lookups of vanilla classes by name still work on 26.x** (checked against the 26.3 vanilla classes in the
  Spigot jar; Paper runs with the same Mojang names):
  - `VanillaArgumentTypes`: `CompoundTagArgument.compoundTag()` exists. `TeamColorArgument.teamColor()` exists on
    26.3, and `ColorArgument.color()` on 26.1.2.
  - `ChatColorArgument.parseImpl`: the `TeamColor` constant names (`BLACK` .. `WHITE`, no `RESET`) lower-cased are
    exactly the keys of `NamedTextColor.NAMES`.
  - `ServerFunctions`: `MinecraftServer.resources`, the `ReloadableResources` accessors `resourceManager()` and
    `managers()`, `ReloadableServerResources.getFunctionLibrary()`, `ServerFunctionLibrary.reload(SharedState,
    Executor, PreparationBarrier, Executor)`, the `SharedState(ResourceManager)` constructor,
    `MinecraftServer.getFunctions()`, `ServerFunctionManager.postReload` (boolean field) and `replaceLibrary`, and
    `InactiveProfiler.INSTANCE`.
  - `MessageArgument.resolveSelectors`: `MessageArgument$Message.resolveComponent(CommandSourceStack)` (private,
    which the code handles with `getDeclaredMethods` and `setAccessible`).
  - `CommandContext.sendFeedback`: `CommandSourceStack.sendSuccess(Supplier, boolean)`.
  - Spigot `CommandLib.wrappedVanillaCommand`: `VanillaCommandWrapper.vanillaCommand` is still a public field on 26.3.
  - Paper's own internals (`PaperCommands`, `ApiMirrorRootNode`, `PluginCommandNode`) could not be checked offline.
    The paper-26.2 integration target runs them (triage: P1C fixes passed on paper-26.2).
- **Argument parity**: the argument class lists in spigot and paper are the same. Paper also has its internal
  `ProfileWordArgumentType` and `SelectorResults`.
- **68657e57**: no caller of the deleted overload is left in spigot or paper; both compile.
- **02fbf5f9**: `paper/build.gradle.kts` adds `common/src/adventure/java` and the test folder to its source sets, so
  the Paper jar, its sources jar and paper-kotlin still get `RichTexts`.
- Build: `./gradlew --configure-on-demand -Pcommandlib.typedNms=false :paper:test :spigot:compileJava
  :paper-kotlin:test` passes at 02fbf5f9.

Dropped as not worth the churn: Paper `VanillaArgumentTypes.color()` and Spigot `NMSArgumentChatFormat` both probe
`TeamColorArgument` and then `ColorArgument`. They live in modules that do not depend on each other, so sharing them
would cost more than the roughly 5 duplicated lines. spigot-kotlin has a Bukkit-typed DSL test (`BukkitDslTest`)
and paper-kotlin has none. The shared `KotlinDslTest` runs on both, so this gap is small.

Verdict: Ship.

Not checked: no server runs (no Docker). Paper-only internals on 26.3 were not run. The NMS link check used the
10-16 module jar rather than a fresh build (I was told not to build NMS modules).
