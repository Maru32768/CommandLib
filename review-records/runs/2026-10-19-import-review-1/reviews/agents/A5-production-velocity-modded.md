# A5 — Production correctness: Velocity and modded (Forge / NeoForge / Fabric)

Reviewed HEAD `1b8f17dc` against baseline `origin/master` `2c8ffd7a`. Static review. I checked vanilla and loader behavior
against the Loom-cached Minecraft/Forge jars and sources (javap on `MinecraftServer` 1.16.5, Forge 1.20.1
`PermissionAPI` / `ConsoleCommandCompleter`, Fabric API `CommandManagerMixin`, Velocity's Brigadier fork). I ran no
Gradle builds.

---

## A5-01 — P1 — Forge 1.16.5: CommandLib commands disappear after `/reload` and are missing from data pack functions

- **Component:** modded, `forge-1.16.5` branch of `CommandLib`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:325-356` (the `//?} else {` branch)
- **Description.** On 1.16.5 the commands are added once to `server.getCommands().getDispatcher()` from
  `FMLServerStartedEvent`. In 1.16.5, `MinecraftServer.getCommands()` reads `this.resources` (a `ServerResources`), and
  `reloadResources` replaces that field with a freshly loaded `ServerResources`. Its constructor runs
  `new Commands(selection)`. Bytecode confirms this: `lambda$reloadResources$11` does `resources.close(); putfield
  resources`, and `ServerResources.<init>` runs `new net/minecraft/commands/Commands`. After `/reload`, the new dispatcher
  has none of the CommandLib commands, and `PlayerList.reloadResources()` resends the tree without them.
  Forge 36 already fires `net.minecraftforge.event.RegisterCommandsEvent` from every `Commands` constructor
  (`ForgeEventFactory.onCommandRegister`; its Javadoc says "whenever the Commands class is constructed"). The 1.18+
  branches already use that event.
  - Second effect: the startup function load runs before `FMLServerStartedEvent`, so data pack functions that call
    CommandLib commands fail to parse on 1.16.5.
  - Third effect: when `register` runs while a server exists (`server != null`), the branch never subscribes. A
    singleplayer world started later in the same JVM has no commands.
- **Violated contract.** Commands survive `/reload` on every other platform. `../../../../docs/permissions.md` and
  `docs/compatibility.md` do not mention this limitation. The diagnostics line on this node ("the server's dispatcher
  when the server has started") hides it.
- **Trigger.** `/reload` on a Forge 1.16.5 server or singleplayer world, or a function tag / `#load` function that calls
  a CommandLib command.
- **Evidence.** Verified-by-trace (bytecode of the cached `forge-1.16.5-36.2.42` merged jar, and the Forge 1.16.5 sources
  of `RegisterCommandsEvent` and `ForgeEventFactory`).
- **Tests.** None. No modded integration case runs `/reload` or functions; `modded-testing` builds its own dispatcher.
- **Status / confidence.** Verified-by-trace, high. The code predates this range but is unchanged final code.
- **Suggested correction.** Register on `RegisterCommandsEvent` on 1.16.5 as on 1.18+, and always subscribe, whether or
  not a server is running. Keep `PermissionAPI.registerNode` at server start, or move it to the constructor. Add a
  `/reload` case to the modded integration test.

## A5-02 — P2 — Fabric production: command blocks count as the console; RCON is never reported as `REMOTE_CONSOLE` on 1.17+ runtimes

- **Component:** modded, `ForgeCommandActor`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:41-66` (`getType`), `:68-70`
  (`isConsole`). Consumer: `Command.consoleRequirer`, `modded/src/main/java/dev/marulabs/commandlib/Command.java:57-65`.
- **Description.** The source type is detected from the raw `CommandSource`'s class name:
  `contains("CommandBlock")`, `contains("RCon")`, `contains("MinecraftServer")`, with `CONSOLE` as the fallback.
  - On Fabric production, vanilla classes carry intermediary names. A command block's source is
    `net.minecraft.class_2593$1` (Mojang name `CommandBlockEntity$1`) and a command block minecart's is
    `class_1697$class_1698`. Neither contains `CommandBlock`, so both fall through to `CONSOLE`. `isConsole()` then
    returns true, and `requireConsole()` / `consoleRequirer()` accept command blocks.
  - On every Mojang-named runtime (Forge 1.17+, NeoForge, Fabric dev, and all tests) the RCON class is
    `net.minecraft.server.rcon.RconConsoleSource`. That is lowercase "con", so the case-sensitive `contains("RCon")`
    never matches, and RCON reports as `CONSOLE`. The cached 1.20.1 and 1.16.5 jars confirm the class name.
- **Violated contract.** `CommandActorType.COMMAND_BLOCK` and `REMOTE_CONSOLE` exist, and `Command.consoleRequirer`'s
  Javadoc says it accepts only "the console or a remote console". On Fabric production a command block passes a
  console-only prerequisite.
- **Trigger.** A Fabric server (intermediary names) where a command block runs a command with `requireConsole()`, or any
  code that branches on `getActor().getType()`.
- **Evidence.** Verified-by-trace. Fabric runtime names are intermediary, and `../../../../docs/agents/modded.md` says Fabric
  production names are not checked on a real server.
- **Tests.** None for `COMMAND_BLOCK` or `REMOTE_CONSOLE` in `modded-testing` (no grep hit). Dev-name tests could not
  catch the Fabric case anyway.
- **Status / confidence.** Verified-by-trace, high for Fabric command blocks and for RCON.
- **Suggested correction.** Use type checks instead of names:
  - `rawSource instanceof BaseCommandBlock` for command blocks.
  - `instanceof RconConsoleSource` for RCON.
  - `instanceof MinecraftServer` for the console.
  - Anything else should be `UNKNOWN`, not `CONSOLE`.

## A5-03 — P2 — Fabric with a shaded (recommended) copy: registering after `SERVER_STARTING` adds nothing until `/reload`

- **Component:** modded, Fabric `CurrentServer` and `CommandLib`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/util/CurrentServer.java:18-29`;
  `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:214-235` (Fabric constructor)
- **Description.** On Fabric, `CurrentServer` learns of the server only through `ServerLifecycleEvents.SERVER_STARTING`.
  It registers that listener in its static initializer. With the relocated shaded copy, which `../../../../docs/packaging.md`
  recommends and which leaves out `fabric.mod.json`, nothing runs that initializer until the first CommandLib call.
  - If a mod's first `CommandLib.register` happens in `SERVER_STARTING` (before CommandLib's listener) or in
    `SERVER_STARTED` or later, `CurrentServer.get()` returns null for the rest of that server's life.
  - The constructor then only adds a `CommandRegistrationCallback` listener. The commands are not in the running
    dispatcher, and players do not see them until `/reload` or the next world start.
  - Side effects: `refreshAll()` does nothing, diagnostics report "not running", and `BuiltInCommandBuildContext` uses
    the built-in registries with `DEFAULT_FLAGS`. Arguments over data-driven registries, such as enchantments on 1.21+,
    then fail.
  - Forge and NeoForge read `ServerLifecycleHooks.getCurrentServer()` and do not have this problem.
- **Violated contract.** The Fabric diagnostics line says registration happens "or the running server's dispatcher
  when registered later". Forge and NeoForge keep that promise; the shaded Fabric copy breaks it.
- **Trigger.** A shaded Fabric copy plus a first `CommandLib.register` from `SERVER_STARTED` or a later runtime hook.
- **Evidence.** Verified-by-trace.
- **Tests.** None (`modded-testing` mocks `CurrentServer`).
- **Status / confidence.** Verified-by-trace for the mechanism. How often it happens depends on where users register,
  and `onInitialize` is the norm.
- **Suggested correction.** Fall back when `server == null`. On a dedicated server, use
  `FabricLoader.getInstance().getGameInstance()`. On the client, `Minecraft.getInstance().getSingleplayerServer()` would
  need a client-only guard. Otherwise, document that the first CommandLib call must come before the server starts
  (from `onInitialize`).

## A5-04 — P2 (design, documented) — Forge / NeoForge / Fabric: every non-player source passes CommandLib permission checks, including `/execute as <non-player entity>`

- **Component:** modded, `PlatformAdapterImpl.hasPermission`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/PlatformAdapterImpl.java:27-38`
- **Description.** When the source entity is not a `ServerPlayer`, the method returns `true`. A permission-level-2
  operator who can use `/execute` can run `execute as @e[type=pig,limit=1] run <cmd>`.
  `CommandSourceStack.withEntity` keeps their level 2, and the CommandLib command runs even when its default is
  `DefaultPermission.OP`, which is level 4 for players.
  - Vanilla level-3/4 commands (`/op`, `/stop`) check `source.hasPermission(n)` and still refuse in that case.
  - Spigot / Paper ask the Bukkit entity sender, which is not op, so they also refuse.
  - Command blocks (level 2) can likewise run level-4-default CommandLib commands.
  - `../../../../docs/errors.md` ("Allowed unless the source entity is a player") and `docs/permissions.md` document this
    behavior, so this is a design concern, not a contract breach.
- **Trigger.** A level-2 operator (or a command block) runs `execute as <non-player> run <OP-default CommandLib command>`.
- **Evidence.** Verified-by-trace.
- **Tests.** Behavior is documented. I found no test that pins it.
- **Status / confidence.** Subjective on whether it should change. The trace is certain.
- **Suggested correction.** For non-player sources, fall back to `source.hasPermission(level)`, using the node's default
  (4 for `OP`, true for `ALL`, false for `NONE`), instead of returning `true`. The console (level 4) and functions keep
  working, because function permission level is configurable.

## A5-05 — P3 — Velocity `ServerArgument`: suggestions for names that need quotes are inserted unquoted and do not filter while quoting

- **Component:** velocity, `ServerArgument`
- **Location:** `velocity/src/main/java/dev/marulabs/commandlib/argument/ServerArgument.java:16-30`
- **Description.** The type is `StringArgumentType.string()`, and the Javadoc promises that any server name can be
  given as a `"quoted name"`. The suggestion provider calls `sb.suggest(name)` with the raw name. For a name with a
  space or another character not allowed unquoted (TOML keys can be quoted, so such names are possible), accepting the
  suggestion inserts unquoted text that then fails to parse. While the user types `"lob`, the latest input includes the
  quote, so `containsIgnoreCase` filters every name out. Common's suggestion code does no quoting.
- **Trigger.** A proxy with a server named, for example, `my lobby`.
- **Evidence.** Verified-by-trace.
- **Tests.** None for quoted names.
- **Status / confidence.** Verified-by-trace, low impact.
- **Suggested correction.** Suggest `"..."`-escaped text when `StringReader.isAllowedInUnquotedString` fails for any
  character, and strip a leading quote from the input before filtering.

---

## Checked, no finding

- **Velocity registration.**
  - Each label and alias is its own `BrigadierCommand` and `CommandMeta` under the plugin.
  - `unregister(CommandMeta)` removes only this registration's metas.
  - Velocity lowercases labels itself.
  - Commands run on Velocity's async executor, and `sync` runs inline, which is fine because the proxy has no main
    thread.
  - `Cooldown` and `Confirmation` use `ConcurrentHashMap`.
  - `CommandContext.proxy` is volatile.
  - `getPlayer` and `getServer` are case-insensitive in Velocity.
- **Velocity permissions.**
  - A non-`UNDEFINED` Tristate wins; otherwise only `ALL` grants. This is documented.
  - The console is `ALWAYS_TRUE`.
  - The `DEFAULTS` map is never cleared on unregister. That is harmless.
- **Velocity Brigadier fork.** Its public `CommandContext` constructor matches the Mojang 1.0.18 signature that
  `PlatformAdapter.createSourceContext` calls.
- **Velocity `RichTexts`.** All 4 `ClickAction` types are mapped. The factories work on Adventure 4 and 5.
- **Forge / NeoForge 1.18+ registration.**
  - `RegisterCommandsEvent` re-adds the commands on every `Commands` construction (`/reload`, new world).
  - `PermissionGatherEvent.Nodes` refreshes `PERMISSIONS` on every server start, so singleplayer restarts get fresh
    nodes.
  - Unregistered nodes fall back to the default resolver, which is documented.
- **Fabric registration.** Fabric API's `CommandManagerMixin` fires `CommandRegistrationCallback` from every `Commands`
  constructor, and `SERVER_STOPPED` clears `CurrentServer`.
- **`BuiltInCommandBuildContext`.**
  - 1.20.5+: the lookup is re-resolved when the server's registry identity or feature flags change, so there are no
    stale registries across singleplayer worlds.
  - 1.19.3–1.20.4: it uses the static registries, whose tags the server rebinds.
  - 1.19–1.19.2: `missingTagAccessPolicy` FAIL is evaluated at parse time, after tags load.
- **`RawWordArgumentType` on Fabric.** It finds the class map by content. Fabric API 0.92 / 0.116 / 0.119
  command-api-v2 do not filter argument types by registry id, so the unregistered `Info` serializes as vanilla
  `brigadier:string`.
- **Argument parity with Spigot.** The modded set matches, with vanilla-typed equivalents: `BlockState` for
  `BlockData`, `ResourceLocation` for `NamespacedKey`, `GameProfile` for `OfflinePlayer(s)`, `Effect` for
  `PotionEffect`.
- **Recipe / Advancement / LootTable / Objective version branches.** They match each version's vanilla API, as compile
  checks per node also confirm.
- **Async pool.** `ServerCommandScheduler` keeps a static daemon pool that survives world restarts, and the sync
  executor re-reads the source's server.

## Coverage

- **In depth:**
  - velocity: `CommandLib`, `CommandContext`, `Command`, `PlatformAdapterImpl`, `VelocityCommandActor`,
    `VelocityCommandScheduler`, `VelocityPermissions`, `PlayerArgument`, `ServerArgument`, `RichTexts`.
  - modded: `CommandLib` (every branch), `PlatformAdapterImpl`, `Command`, `ForgeCommandActor`, `CommandContext`,
    `CurrentServer`, `ServerCommandScheduler`, `BuiltInCommandBuildContext`, `RegistryEntries`, `RegistryArgument`,
    `BiomeArgument`, `RecipeArgument`, `AdvancementArgument`, `LootTableArgument`, `ObjectiveArgument`,
    `RawWordArgumentType`, `fabric.mod.json`, `../../../../modded/build.gradle.kts`, `velocity` and `velocity-kotlin` build scripts.
- **Skimmed:**
  - `CommandLibDiagnosticsCommand` (both).
  - `TimeArgument`, `GameModeArgument`, `AttributeArgument`, `EntityTypeArgument`.
  - The common `CommandNodeCreator` alias and requirement paths.
- **Not examined:**
  - velocity and modded `annotation/AnnotatedCommands`.
  - Modded `UUIDArgument` / `UUIDsArgument`, `Location`, `LocationArgument`, `GameProfileArgument`,
    `Enchantment` / `Effect` / `Component` / `Message` / `Particle` / `Predicate` arguments, `util/text/RichTexts`
    (modded).
  - `velocity-testing` / `modded-testing` internals.
  - Velocity proxy internals beyond the API (`CommandGraphInjector` handling of shared alias children was not checked
    against source).
  - No builds or tests were run.
