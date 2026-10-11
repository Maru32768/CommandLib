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
  name from R and from the command map, adds each node to R, puts a
  `VanillaCommandWrapper(R, node)` with a null permission into `knownCommands`,
  and adds the node to the root that `execute run` redirects to (V), lines
  195-206.
- In `onEnable` during startup R is V, so the node lands in V and CraftBukkit
  later adds `minecraft:<name>` for it. At runtime R is the dispatcher built by
  `syncCommands()`, and the node reaches V through the `execute run` redirect.
- `/minecraft:reload` keeps the commands because the wrappers are in
  `knownCommands`.
- Paper 1.20.6+ (`usesPaperCommandDispatcher()`, line 135): nodes are added to
  Paper's dispatcher wrapped in `ShadowBrigNode` (line 165) and added again on
  `ServerResourcesReloadedEvent` (line 103).
- Permissions are checked only by the Brigadier `requires` predicate, which
  asks the Bukkit sender for `<prefix>.<command>`.
- Unregister (line 302, also on `PluginDisableEvent`, line 218) removes the
  nodes from R, from V (through the redirect), and from `knownCommands`; then
  players get `updateCommands()` on the next tick (line 291). The update is
  skipped on plugin disable to avoid racing Paper's asynchronous command tree
  building.
- Aliases are separate literals that share the target's children
  (`CommandNodeCreator.java:299`), not redirects.

### paper module (Paper 1.21+)

`paper/.../CommandLib.java`:

- During plugin initialization it registers a `COMMANDS` handler (line 275)
  and registers each node with `Commands.register(node, "")` (line 357), which
  also adds `<plugin>:<name>` and leaves labels owned by other plugins alone.
- The handler runs again with the new dispatcher after `/minecraft:reload`.
- After initialization it adds the nodes to the cached dispatcher directly
  (line 137) and re-adds them from another plugin's handler after a reload
  (line 310). Without any cached dispatcher it throws (line 155).
- Unregister removes the recorded labels with Paper's
  `RootCommandNode.removeCommand`.

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
| Spigot dispatcher choice | R (V during startup) plus V through the `execute run` redirect | always V, R at runtime | Works on every version; the redirect lookup is indirect. | Acceptable |
| Startup `minecraft:<name>` label | Created by CraftBukkit; unregister left it in the command map and client tree, pointing at a removed command | removed or kept deliberately | Bug | Fixed: unregister removes the label when it wraps the node (`removeMinecraftNamespacedCommand`, line 335) |
| Paper module direct add | `addChild` merged into a same-named command, keeping its requirement and children | goes through the registrar | Bug | Fixed: `replaceChild` (line 387) removes the command first |
| `plugin:<name>` label (spigot module) | not added | added (`fixNamespaces`, runtime path) | Missing feature | Open: add the label when the plugin namespace is wanted |
| Overriding an existing command | removes the existing label and vanilla node | `putIfAbsent` at runtime | Intentional difference | Documented |
| Wrapper permission / `/help` | wrapper permission is null, so `/help` lists the command for everyone, under "Minecraft" with "A Mojang provided command." | sets the permission and writes help topics | Missing edge case | Open: set the wrapper permission to the command permission and write a help topic |
| `/minecraft:reload` | Spigot: kept through `knownCommands`; Paper 1.20.6+: added again on `ServerResourcesReloadedEvent`; paper module: `COMMANDS` handler | same | Correct | - |
| Data pack functions | functions are compiled against the new resources before CommandLib's commands are there, so they cannot use them | function library points at V; Paper bootstrap registration | Missing feature | Open: point the function library at V on Spigot, support bootstrap registration on Paper |
| Bukkit `/reload` (Spigot) | after reload `execute run <command>` fails: no `execute` node in R during `onEnable`, so the node never reaches V | uses V directly | Missing edge case (unsupported server feature) | Open: add a typed NMS accessor for `vanillaCommandDispatcher` and add to and wrap with V |
| Runtime wrapper dispatcher | wraps with R, which goes stale after a reload but still holds the node | wraps with V | Works, holds the old dispatcher | Open: same V accessor |
| Paper module, plugin enabled at runtime | the `COMMANDS` handler registers but does not fire until the next reload, so the commands are missing | `Bukkit.reloadData()` after runtime changes | Missing edge case | Open: detect a server that finished loading (e.g. `ServerLoadEvent` seen) and add directly or reload data |
| Paper < 1.20.6 async command tree | mutates the root on the main thread; skips the update on disable | same | Acceptable | - |
| Folia | no handling | runtime changes throw | Not supported (`compatibility.md`) | - |
| Mohist | no special handling; integration fixtures 1.16.5 / 1.20.1 | none | Acceptable | - |
