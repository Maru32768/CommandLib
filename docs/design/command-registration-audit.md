# Command Registration Audit (Spigot / Paper)

This document records how CommandLib registers commands on Spigot and Paper,
compared with CommandAPI 12.1.0 (`commandapi-platforms/commandapi-spigot` and
`commandapi-paper`). It lists what was fixed and what is left open.

Server code was read from the Spigot sources jars that BuildTools installs
(1.16.5 bytecode, 1.20.4 and 1.21.1 sources) and from the Paper 1.20.6 / 1.21.1
dev bundles. Line numbers refer to the commit that adds this document.

## How CraftBukkit holds commands

- `MinecraftServer.vanillaCommandDispatcher` (V) is assigned once in the
  `MinecraftServer` constructor, to the `Commands` of the initial data pack
  resources. It is never replaced.
- `MinecraftServer.getCommands()` (1.16.5: `getCommandDispatcher()`) returns the
  `Commands` of the current resources (R). Before the first
  `CraftServer.syncCommands()`, R is V.
- `CraftServer.enablePlugins(POSTWORLD)` calls `setVanillaCommands(false)`, which
  adds every child of V to the command map as `minecraft:<name>`, then
  `helpMap.initializeCommands()` and `syncCommands()`.
- `syncCommands()` replaces R with an empty `Commands` and adds every
  `knownCommands` entry: a `VanillaCommandWrapper` adds its node, other commands
  are wrapped in `BukkitCommandWrapper`. `/minecraft:reload` calls it as well
  (SPIGOT-5884).
- Chat and command blocks run commands through the command map. A
  `VanillaCommandWrapper` runs its node through the dispatcher it was created
  with. `execute run` redirects to V's root, so a command must be a child of V
  to work under `/execute`.
- Paper 1.20.6+ replaces this: `knownCommands` is `BukkitBrigForwardingMap`
  over the live dispatcher, `syncCommands()` only sends the tree, and
  `/minecraft:reload` builds a new dispatcher, moves only `BukkitCommandNode`s,
  fires the `COMMANDS` lifecycle event, then `ServerResourcesReloadedEvent`.
  The `COMMANDS` event for plugins first fires after `enablePlugins(POSTWORLD)`.

## CommandLib

### spigot module (Spigot 1.16.5-1.21.1, Paper 1.16.5-1.21.1)

`spigot/.../CommandLib.java`:

- `enable()` (line 177): builds the nodes, removes any command with the same
  name from R, V and the command map, adds each node to V (read through the
  typed `NMSDedicatedServer#getVanillaCommandDispatcher`) and to R when R is
  not V, and puts a `VanillaCommandWrapper(V, node)` into `knownCommands`.
  This replaced adding to R and to the root that `execute run` redirects to,
  with a wrapper of R.
- In `onEnable` during startup R is V, so CraftBukkit later adds
  `minecraft:<name>` for the node. At runtime R is the dispatcher built by
  `syncCommands()`.
- `/minecraft:reload` keeps the commands because the wrappers are in
  `knownCommands`.
- Every label also gets `<plugin>:<label>` in the command map, pointing at the
  same wrapper (`addNamespacedCommand`).
- `/help`: the wrapper takes the command permission, the server help text
  (`Command#serverHelp`, else "A command by the &lt;plugin&gt; plugin."), a usage built from the node and the other labels as
  aliases (`describe`). Each label gets a `GenericCommandHelpTopic`, with the
  `serverHelp` page when one is set, which replaces a topic of the same name
  (`putHelpTopic`). CraftBukkit picks a command's index by its class and lists
  every `VanillaCommandWrapper` under "Minecraft", so once the help map has its
  indexes (at registration after startup, else on `ServerLoadEvent`) each topic
  is moved into the plugin's index, made with CraftBukkit's texts when missing
  (`fileUnderPluginIndex`). `IndexHelpTopic` has no accessor for its topics, so
  its protected `allTopics` set is read by reflection.
- Paper 1.20.6+ (`usesPaperCommandDispatcher()`): each node is added to Paper's
  dispatcher under its label and `<plugin>:<label>`, with a `PluginCommandNode`
  (no children, the server help text as description) cached as its API node
  through Paper's `wrappedCached` / `unwrappedCached` fields, as Paper's
  registrar leaves it (`addToPaperDispatcher`). Paper's command map then shows a
  `PluginVanillaCommandWrapper`, which names the plugin, so Paper's help map
  lists it under the plugin, and has no permission of its own. Before, the node
  was wrapped in a `ShadowBrigNode` and shown as a `VanillaCommandWrapper`: "A
  Mojang provided command.", the "Minecraft" index, and a
  `minecraft.command.<label>` permission that `SimpleCommandMap#dispatch` (and so
  `Bukkit.dispatchCommand` and `Player#performCommand`) checked. `/minecraft:reload`
  moves only `BukkitCommandNode`s into the new dispatcher, so the nodes are added
  again from a `COMMANDS` lifecycle handler registered reflectively, which Paper
  runs before it sends the new tree. Paper takes the handler only while the
  plugin is enabling; a registration after that adds them again on
  `ServerResourcesReloadedEvent`, as before, after Paper has queued the
  asynchronous tree building for players, which the change can race
  (`ConcurrentModificationException` logged, seen in the paper-1.21.1 run before
  the handler existed; the update on the next tick sends the full tree). Paper builds help
  topics only when the server loads and on `/minecraft:reload`, before that
  event, so CommandLib puts a topic for each label from Paper's wrapper with the
  command permission set on it at registration and after the reload
  (`showPaperHelpTopics`).
- Permissions are checked by the Brigadier `requires` predicate, which asks
  the Bukkit sender for `<prefix>.<command>`. The wrapper carries the same
  permission, so Bukkit's `testPermission` and `/help` agree with it.
- Unregister (line 302, also on `PluginDisableEvent`, line 218) removes the
  nodes from R, from V, and from `knownCommands`; then
  players get `updateCommands()` on the next tick (line 291). The update is
  skipped on plugin disable to avoid racing Paper's asynchronous command tree
  building.
- Aliases are separate literals that share the target's children
  (`CommandNodeCreator.java:299`), not redirects.
- Data pack functions: a task started at the first registration checks every
  tick whether the data pack resources were replaced (startup, `/minecraft:reload`,
  `/datapack`, `Bukkit#reloadData`) or the commands changed (a registration or a
  runtime unregister), and then points the function library at the dispatcher
  holding the commands (V, or Paper's live dispatcher on 1.20.6+) and compiles
  the functions again (`compileFunctionsWithCommands`,
  `NMSDedicatedServer#recompileFunctions`). Bukkit tasks run before the server
  ticks functions, so the `#load` functions run once, with the commands.

### paper module (Paper 1.21+)

`paper/.../CommandLib.java`:

- During plugin initialization it registers a `COMMANDS` handler (line 275)
  and registers each node with `Commands.register(node, description)`, the
  description being the server help text, which also adds `<plugin>:<name>`
  and leaves labels owned by other plugins alone.
- The handler runs again with the new dispatcher after `/minecraft:reload`.
- After initialization it adds the nodes to the cached dispatcher directly,
  under their labels and `<plugin>:<label>` (`addNodes`), so they work at once,
  and re-adds them from another plugin's handler after a reload (line 310).
  Without any cached dispatcher it throws (line 155). Paper shows such plain
  nodes as Mojang commands, so a data reload follows on the next tick
  (`reloadDataLater`), as in CommandAPI: the `COMMANDS` handlers register the
  commands again through the registrar into a fresh dispatcher, the help map is
  rebuilt and players get the tree. The changes of one tick share the reload,
  and it is skipped when a reload has happened in between.
- Unregister removes the recorded labels with Paper's
  `RootCommandNode.removeCommand`; at runtime the same data reload follows.
- A plugin enabled after startup (e.g. by PlugMan) installs its handler but has
  missed the event; on the next tick it calls `Bukkit.reloadData()` if the
  handler has not run (`reloadIfCommandsEventMissed`).
- `bootstrap(BootstrapContext, ...)` registers from a `PluginBootstrap`. Paper
  fires the bootstrap `COMMANDS` handlers in
  `ReloadableServerResources.loadResources`, before the function library is
  loaded, so data pack functions can use the commands. The server does not exist
  then, so the permissions are not added to the plugin manager; CommandLib keeps
  their defaults and applies them when a permission is neither set on the sender
  nor registered (`CommandLib.hasPermission`). Paper's `NullCommandSender`, used
  to compile functions, keeps its "every permission" answer. Bootstrap commands
  cannot be unregistered, and Paper refuses Bukkit `/reload` while a bootstrapper
  handles `COMMANDS`.
- Labels and `/help`: Paper's `Commands.register` adds `<plugin>:<name>` itself
  and wraps each label in a `PluginCommandNode`, which the command map shows as
  a `PluginVanillaCommandWrapper`, so the help map lists the commands under the
  plugin instead of "Minecraft". The wrapper has no permission, so `/help`
  listed the commands for everyone; CommandLib now calls `amendCanSee` with the
  command permission on each label's topic after `ServerLoadEvent` and
  `ServerResourcesReloadedEvent`, when the help map has been rebuilt, and
  applies the `serverHelp` page (`amendHelpTopics`). At startup and on Bukkit
  `/reload` Paper builds the help map once before the `COMMANDS` event and once
  after it, keeping the plugin indexes of the first pass, so a plugin index made
  for the plugin's `plugin.yml` commands lacked CommandLib's; `amendHelpTopics`
  adds them. The description, empty before, is the server help text. Bootstrap
  commands keep the unrestricted topic, since there is no plugin to listen with.

### Forge / NeoForge

Commands are added to the dispatcher's root under their names only. Vanilla
commands are not namespaced in the dispatcher, and neither Forge nor NeoForge
adds a `<modid>:` label for mod commands, so CommandLib adds none. There is no
`/help` topic system, so `serverHelp` is ignored, and no unregister API.

## CommandAPI

- Spigot: nodes always go into V (`SpigotCommandRegistration.java:262-290`),
  wrapped with V (`SpigotNMS_1_20_R1.java:99-104`). At startup CraftBukkit moves
  them into the command map; CommandAPI then removes the `minecraft:` labels it
  does not want and adds the namespaced labels (`fixNamespaces`, lines 124-149)
  and sets each wrapper's Bukkit permission (`fixPermissions`, lines 154-184).
  At runtime it does the same work itself: `knownCommands.putIfAbsent`, R, and
  namespace labels (lines 187-259). Unregister optionally removes namespaces and
  only removes wrappers of the matching kind (lines 313-349).
- Data packs: `reloadDataPacks` points the function library at V so functions
  can use the commands (`SpigotNMS_1_20_R1.java:110-123`).
- Help: topics are written by CommandAPI (`CommandAPIBukkit.java:204`).
- Paper 1.20.6+: commands are collected in private dispatchers and registered
  from `COMMANDS` handlers for bootstrap and plugin
  (`PaperCommandRegistration.java:128-183`), with a description. A runtime
  register or unregister schedules `Bukkit.reloadData()` (lines 185-200); on
  Folia that throws.

## Findings

| Area | CommandLib | CommandAPI | Verdict | Status |
|---|---|---|---|---|
| Spigot dispatcher choice | was R (V during startup) plus V through the `execute run` redirect | always V, R at runtime | The redirect lookup was indirect and missed V without an `execute` node. | Fixed: always V through the typed accessor, plus R when it is not V |
| Startup `minecraft:<name>` label | Created by CraftBukkit; unregister left it in the command map and client tree, pointing at a removed command | removed or kept deliberately | Bug | Fixed: unregister removes the label when it wraps the node (`removeMinecraftNamespacedCommand`, line 335) |
| Paper module direct add | `addChild` merged into a same-named command, keeping its requirement and children | goes through the registrar | Bug | Fixed: `replaceChild` (line 387) removes the command first |
| `plugin:<name>` label (spigot module) | not added | added (`fixNamespaces`, runtime path) | Missing feature | Fixed on Spigot and Paper before 1.20.6: every label, aliases included, also gets `<plugin>:<label>`, the same wrapper under a second command-map key like Bukkit's fallback labels; a runtime registration also adds a copy of the node under that name to R, as `syncCommands` does at startup (`addNamespacedCommand`). Unregister removes both. `execute run <plugin>:<label>` does not work, as for Bukkit's own namespaced labels, because the label is not a node of V. Fixed on Paper 1.20.6+: a copy of the node under `<plugin>:<label>` is added to Paper's dispatcher, as Paper's registrar does, and unregister removes it; there `execute run <plugin>:<label>` works. Checked by the integration fixture (`ScenarioTest`, `HelpTopicTest`) |
| `plugin:<name>` label (paper module) | registrar labels; the runtime direct add had none | registrar | Missing edge case | Fixed: the direct add also adds `<plugin>:<label>`, and the data reload that follows registers it through the registrar |
| Mod id label (Forge / NeoForge) | none | - | Platform convention: vanilla and Forge / NeoForge mod commands are not namespaced | Intentional, nothing added |
| Overriding an existing command | removes the existing label and vanilla node | `putIfAbsent` at runtime | Intentional difference | Documented |
| Wrapper permission / `/help` | wrapper permission was null and its description "A Mojang provided command." | sets the permission and writes help topics | Missing edge case | Fixed on Spigot and Paper before 1.20.6: the wrapper takes the command permission, the server help text, a usage built from the node's children and the other labels as aliases, and each label gets a `GenericCommandHelpTopic` that replaces an existing topic of that name; unregister removes the topics (`describe`, `putHelpTopic`). Fixed on Paper 1.20.6+ (`PluginCommandNode`, `showPaperHelpTopics`) |
| `/help` text | the description when it was a string, else "A command by the &lt;plugin&gt; plugin."; the paper module passed an empty description | `withShortDescription` / `withFullDescription` / `withHelp` | Missing feature | Added `Command#serverHelp(shortText)` and `serverHelp(shortText, fullText)`: the short text for the `/help` lists and the command-map description, the full text replacing the `/help <command>` page (through `HelpTopic#amendTopic`, so `<text>` in it stands for the generated page, as in `help.yml`). Without it the plugin-named text, on every Bukkit-family path; `description(...)` is not used for the server help |
| `/help` plugin index | CraftBukkit listed the spigot module's commands under "Minecraft", since it picks the index by the wrapper class | CommandAPI's Spigot topics stay under "Minecraft" too | Missing feature | Fixed: topics are moved into the plugin's index (`fileUnderPluginIndex`); on Paper 1.20.6+ the plugin-attributed wrappers are listed under the plugin by Paper itself |
| `/minecraft:reload` | Spigot: kept through `knownCommands`; Paper 1.20.6+: added again on `ServerResourcesReloadedEvent`; paper module: `COMMANDS` handler | same | Correct | - |
| Data pack functions | functions are compiled when data packs load, before plugins add commands, and a function with an unknown command is dropped | function library points at V and all data packs are reloaded after startup; nothing for `/minecraft:reload` on Spigot; Paper bootstrap registration | Missing feature | Fixed in the spigot module (Spigot, Paper before and from 1.20.6): the function library's dispatcher is set to the one holding the commands and only the functions are compiled again (`NMSDedicatedServer#recompileFunctions`), instead of CommandAPI's full data pack reload, which also needs recipes registered again. Spigot fires no event for `/minecraft:reload`, so a task checks each tick whether the data pack resources object was replaced or the commands changed, which also covers a command registered or unregistered after startup (`compileFunctionsWithCommands`). The check costs a few reflective calls per tick. A permission check from the compiling source, which has no Bukkit sender, passes, as in CommandAPI. Checked by the integration fixture after `/minecraft:reload` (`DataPackFunctionTest`). Paper module: `CommandLib.bootstrap(BootstrapContext, ...)` registers from the bootstrapper's `COMMANDS` handler, which fires before functions are compiled (see "paper module" above); commands registered from a plugin are not seen by functions, by design of Paper's reload order |
| Bukkit `/reload` (Spigot) | after reload `execute run <command>` failed: no `execute` node in R during `onEnable`, so the node never reached V | uses V directly | Missing edge case (unsupported server feature) | Fixed: nodes go to V through the typed accessor |
| Runtime wrapper dispatcher | wrapped with R, which goes stale after a reload but still holds the node | wraps with V | Worked, held the old dispatcher | Fixed: wraps with V |
| Paper module, plugin enabled at runtime | the `COMMANDS` handler registers but does not fire until the next reload, so the commands are missing | `Bukkit.reloadData()` after runtime changes | Missing edge case | Fixed: once the plugin is enabled, a task on the next tick calls `Bukkit.reloadData()` if the handler has not run yet (`reloadIfCommandsEventMissed`). Startup fires the event before the first tick, so the check is a no-op there. A reload was chosen over a direct add because Paper has no public way to reach the live dispatcher outside the event (a fresh plugin has nothing cached), and the reload path also adds `<plugin>:<name>`, rebuilds help topics and sends the tree to players. Registering only after `onEnable()` with nothing cached still throws, since Paper then rejects the handler |
| Paper module `plugin:<name>` label and `/help` | Paper's registrar adds the namespaced label and plugin-attributed help topics, but the topic had no permission | `/help` topics with permissions | Missing edge case | Fixed: topics are amended with the command permission (`amendHelpTopics`); namespaced labels need nothing |
| Paper module runtime direct add / unregister | mutated the live root on the main thread and called `updateCommands()` at once; Paper builds the command tree on its async pool, which threw `ConcurrentModificationException` when a later change in the same tick mutated the root it read (seen in the paper-1.21.0 integration run when the fixture registers and unregisters in the same tick) | `Bukkit.reloadData()` on the next tick instead of mutating | Missing edge case; only logged, the next update sends a good tree | Fixed: the change still applies at once, and nothing is sent in that tick; one data reload on the next tick, shared by the tick's changes, registers the commands through the registrar into a fresh dispatcher and sends the tree (`reloadDataLater`). A change can still race a tree being built for another reason, such as a player joining, as any live change of Paper's dispatcher can |
| Paper < 1.20.6 async command tree | mutates the root on the main thread; skips the update on disable | same | Acceptable | - |
| Folia | no handling | runtime changes throw | Not supported (`compatibility.md`) | - |
| Mohist | no special handling; integration fixtures 1.16.5 / 1.20.1 | none | Acceptable | - |
