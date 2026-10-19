# Round 3 B: platform main code (spigot / paper / modded / velocity / *-kotlin)

Reviewed at master 3db5ca99. Round-2 commits in scope: c5fc6581 (permissions from the CommandTree walk), 312107e0
(cooldown bypass node), b13bf0a0 (Spigot namespaced-label ownership), faca8350 (modded function-compilation source),
2c73a07d (modded ObjectiveArgument error), 3db5ca99 (modded holder wildcard). There are no round-2 changes in
velocity/src/main beyond the c5fc6581 call-site switch, and none in *-kotlin main.

## R3B-1: Spigot/Paper overwrite and later delete a cooldown bypass node the plugin declared itself

- Category: regression
- Severity: medium
- Files:
  - common/src/main/java/dev/marulabs/commandlib/CommandTree.java:50-65 (bypass node is always `DefaultPermission.OP`)
  - spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:821-834 (`registerPermissions`: `removePermissionCompletely` + `addPermission`)
  - spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:836-844 (`unregisterPermissions`)
  - paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:330-332, 579 and CommandPermissions.java:46-66 (`add` replaces, `remove` deletes by name)
- What is wrong: before 312107e0, CommandLib only registered nodes it generated under the permission prefix. Now the
  walk also lists `Cooldown.bypassPermission(...)`, and that node is any name the user picks. On Spigot and Paper,
  registering replaces any existing `Permission` of that name with `new Permission(node, "Skips the cooldown of /x", OP)`,
  and unregistering (or disabling the plugin) removes the node completely. The server adds `plugin.yml` /
  `paper-plugin.yml` permissions before `onEnable`, so a node the plugin declared itself gets clobbered:
  - `myplugin.cooldown.bypass: {default: false}` (operators should not skip cooldowns) silently becomes `OP`. The API
    cannot choose another default.
  - A bypass node that is also a parent permission (for example `bypassPermission("myplugin.admin")` where
    `myplugin.admin` has `children:` in plugin.yml) loses its children, so every holder loses those child permissions.
  - The declared description is replaced.
  Before this change, Bukkit's implicit default for an undeclared node was already OP, so on Bukkit the registration adds
  nothing except clobbering a declaration. A second registration that shares one `Cooldown` also has the node removed
  when the first registration unregisters, because both Spigot and Paper remove by name.
- Evidence: confirmed by reading the code. `registerPermissions` calls `removePermissionCompletely(name)` without
  checking ownership and then calls `addPermission`. That plugin.yml permissions exist before `onEnable` comes from
  memory of `CraftServer#enablePlugin`, not from a test. No Spigot/Paper test covers the bypass node.
- Suggested fix (smallest): on Spigot/Paper, register a `cooldown-bypass` node only when
  `getPluginManager().getPermission(node) == null`, and remove only the nodes this registration added. Command, option
  and argument nodes keep the replace behavior. Another option is `bypassPermission(node, DefaultPermission)`, but that
  still clobbers children and description, so the absent-only rule is needed either way. Update docs/permissions.md
  ("registered, removed ... with the default OP") to say an already declared node is left as is.

## Checked, no finding

- c5fc6581 order / duplicates / defaults: every platform now takes the nodes from `CommandTree.permissionConfigs`
  (Spigot/Paper per top-level command, modded/Velocity/velocity-testing for the whole collection), so registration and
  `permissionsJson()` agree. The walk includes everything the deleted `permissionConfigs` walked: command, option,
  argument-branch, branch-children and child commands. Duplicates: Spigot/Paper replace (last wins), Forge 1.18+
  de-duplicates against `e.getNodes()` before `addNodes`, which would throw on a duplicate (verified in Forge 47.3.22
  `PermissionGatherEvent.Nodes#addNodes`), Forge 1.16.5 `registerNode` overwrites, and modded/Velocity `DEFAULTS` keep
  the first. The first-vs-last split is the open R2C-3, so it is not reported again.
- 312107e0 Forge path: the bypass node becomes `commandlib.<node>`, and `ForgeCommandActor.hasPermission` ->
  `CommandLib.hasPermission` looks up `"commandlib." + node`, so LuckPerms can now grant it once it is registered in
  `PermissionGatherEvent`. `PermissionNode` does not validate names, so arbitrary bypass names are safe. A late
  registration (server already running) still only uses the default resolver, which is documented. On Fabric, Velocity
  and Paper bootstrap the effective result is the same as before (OP -> level 4 / false on proxy / Bukkit OP).
- b13bf0a0: on Paper 1.20.6+, `paperLabelNodes` is refreshed by every `addToPaperDispatcher` (including the
  lifecycle re-add after a reload) and cleared on unregister. Ownership is `root.getChild(label) ==
  paperLabelNodes.get(label)`, which handles a later registration of the same label by the same plugin, for both labels.
  On the vanilla dispatcher, the namespaced label resolves through the knownCommands wrapper that `addNamespacedCommand`
  puts there. No regression found.
- faca8350: vanilla 1.16.5-1.21.4 compiles functions with `CommandSource.NULL` and a null server, and 1.20.3+ macro lines
  are parsed with the executing source (which has a server). The check is narrow (`getServer() == null && raw ==
  CommandSource.NULL`). On Fabric/<1.19, if the reflective field lookup fails, it falls back to the old (strict)
  behavior, which is safe.
- 2c73a07d: the modded custom-scoreboard path now sends `arguments.objective.notFound` through the same
  `ArgumentParseException(Consumer)` form that the Spigot and Paper `ObjectiveArgument` use. The output matches.
- 3db5ca99: the wildcard upper bound is taken after unwrapping the List, so it matches what the parser does for
  `List<? extends Holder<X>>` (Kotlin's emission for an interface element type).
