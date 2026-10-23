# Permissions

CommandLib gives every command, subcommand, and argument branch its own permission node, registers the nodes with the
platform, and checks them for visibility, tab completion, help, and execution.

## Node names

A node is the registration **prefix**, a dot, and the **path** of the route.

| Registration | Prefix |
|---|---|
| `CommandLib.register(plugin, commands...)` (Spigot, Paper) | `<plugin name in lower case>.command` |
| `CommandLib.register(plugin, "myplugin.cmd", commands...)` (Spigot, Paper) | `myplugin.cmd` |
| `CommandLib.bootstrap(context, commands...)` (Paper) | `<plugin name in lower case>.command` |
| `CommandLib.register("mymod.command", commands...)` (Forge / NeoForge / Fabric) | `mymod.command` (required) |
| `CommandLib.register(plugin, proxy, commands...)` (Velocity) | `<plugin id>.command` |
| `CommandLib.register(plugin, proxy, "myproxy.cmd", commands...)` (Velocity) | `myproxy.cmd` |

The path is built from names, not from aliases:

| Route | Path | Node with prefix `myplugin.command` |
|---|---|---|
| `/spawn` | `spawn` | `myplugin.command.spawn` |
| `/game start` (child command) | `game.start` | `myplugin.command.game.start` |
| `/config <key>` (argument branch) | `config.key` | `myplugin.command.config.key` |
| `/msg <target> <text>` (one branch with two arguments) | `msg.target.text` | `myplugin.command.msg.target.text` |
| `/config <key> get` (child of an argument branch) | `config.get` | `myplugin.command.config.get` |
| `/ban --silent` (option with `permission(DefaultPermission)`) | `ban.option.silent` | `myplugin.command.ban.option.silent` |

Running a route needs the node of every step on it. `/config <key> get` needs `myplugin.command.config`,
`myplugin.command.config.key`, and `myplugin.command.config.get`. An alias such as `/sp` for `/spawn` uses the node of
the command it points to.

`command.permissionName(prefix)` returns the node of a command, which helps when writing permission docs or tests.
`CommandDocs.of(prefix, commands).permissionsMarkdown()` (or `permissionsJson()`) lists every node of the commands with
its default; see [permission export](diagnostics.md#permission-export).

### Overriding the node

```java
class SpawnCommand extends Command {
    SpawnCommand() {
        super("spawn");

        permission("myplugin.spawn"); // this exact node, without the prefix

        argument(new PlayerArgument("target")).permission("myplugin.spawn.others")
                                              .execute((target, ctx) -> target.teleport(target.getWorld()
                                                                                              .getSpawnLocation()));
    }
}
```

An overridden node replaces only that step; children keep building their own path from names
(`/spawn` children still get `<prefix>.spawn.<child>`).

Routes of one registration can share a node, such as two commands that set the same `permission("myplugin.admin")`, or
`/config get` and `/config <key> get`, whose paths are both `config.get`. They must then give it the same default and
description; registration fails with an `IllegalStateException` that names both routes otherwise, since the node is
registered once. A cooldown bypass node is not checked (see [Cooldown bypass](#cooldown-bypass)).

### Options

Options need no node of their own unless they ask for one. `permission(...)` on an option makes a sender without the
node unable to see or give it:

```java
class BanCommand extends Command {
    BanCommand() {
        super("ban");
        permission(DefaultPermission.ALL);

        // myplugin.command.ban.option.silent, operators only
        option(Options.flag("silent", 's').permission(DefaultPermission.OP));
        // this exact node, with the command's default (ALL)
        option(Options.string("reason", 'r', "").permission("myplugin.ban.reason"));
        // this exact node and default
        option(Options.flag("broadcast", 'b').permission("myplugin.ban.broadcast", DefaultPermission.NONE));

        argument(new PlayerArgument("target")).execute((target, ctx) -> target.kickPlayer("Banned"));
    }
}
```

The generated node is the command's path (from names, as for children) followed by `.option.<name>`. Option nodes are
registered, removed and exported like the other nodes of the command. Giving the option still needs the nodes of the
route it is on.

### Cooldown bypass

`Cooldown.bypassPermission("myplugin.cooldown.bypass")` names an exact node, without the prefix. It is registered,
removed and exported with the command that sets the cooldown, with the default `OP` and the kind `cooldown-bypass` in
`permissionsJson()`. Child commands that inherit the cooldown do not list it again. Set the bypass permission before
registering the command. On Spigot and Paper a bypass node that already exists, because `plugin.yml` declares it or
another registration added it, is left as is: it keeps its default, children and description, and the registration
does not remove it.

## Defaults

The default decides who has the node when no permission plugin says otherwise. It is CommandLib's
`DefaultPermission`:

| `DefaultPermission` | Meaning |
|---|---|
| `OP` | Operators only. The default of a root command. |
| `ALL` | Everyone. |
| `NONE` | No one, operators included, until it is granted. |

Inheritance: a child command takes its parent's default, an argument branch takes its command's default, and a child
of an argument branch takes that branch's default. Set it with `permission(DefaultPermission)`, or together with a
node or a description:

```java
class HomeCommand extends Command {
    HomeCommand() {
        super("home");
        permission(DefaultPermission.ALL, "Teleport to your home");

        addChildren(new Command("set") {{
            // inherits ALL
            execute(ctx -> ctx.sendSuccess("Home set"));
        }}, new Command("admin") {{
            permission(DefaultPermission.OP);
            execute(ctx -> ctx.sendSuccess("Admin"));
        }});
    }
}
```

The description is registered with the permission where the platform supports one.

### Per platform

| `DefaultPermission` | Spigot / Paper (Bukkit `PermissionDefault`) | Forge / NeoForge 1.18+ (default resolver), Fabric | Forge 1.16.5 (`DefaultPermissionLevel`) | Velocity (unset node) |
|---|---|---|---|---|
| `ALL` | `TRUE` | `true` | `ALL` | `true` |
| `OP` | `OP` | the player has permission level 4 | `OP`, decided by Forge's permission handler | `false` |
| `NONE` | `FALSE` | `false` | `NONE` | `false` |

Platform overloads exist as well: `permission(PermissionDefault)` on Spigot and Paper (only `TRUE`, `FALSE`, and `OP`;
`NOT_OP` throws `IllegalArgumentException`) and `permission(DefaultPermissionLevel)` on Forge 1.16.5.

Velocity has no operators, so `OP` behaves like `NONE` there: players need the node from a permission plugin. The
proxy console has every permission. On Forge / NeoForge / Fabric the console, RCON and data pack functions, which run as
the server, pass every check, and so does the source that compiles data pack functions, which the function permission
level guards as it does vanilla's commands. Other non-player sources, such as command blocks and an entity that
`/execute as` runs as, get a node by its default and their own permission level: `ALL` passes, `NONE` fails, and `OP`
needs level 4, so a command block (level 2) cannot run an `OP` command. On Spigot and Paper
the Bukkit sender is asked, except while the server compiles data pack functions, when the check passes and the
function permission level applies.

## Registration and reload

### Registration contract

What `CommandLib.register(...)` and the other entry points promise differs by platform:

| | Spigot | Paper | Forge / NeoForge / Fabric | Velocity |
|---|---|---|---|---|
| Invalid definition (optional argument before a required one, name clash, and so on) | `register` throws | `register` and `bootstrap` throw | `register` throws | `register` throws |
| First `register` after startup | Works | Throws `IllegalStateException` unless the plugin's first `register` was during its initialization (constructor, `onLoad`, `onEnable`) | Works: the commands are added to the running server's dispatcher | Works |
| `unregister()` | Yes | Yes, except for `bootstrap` registrations | No: a registration lasts for the game's lifetime | Yes |
| A label another plugin or mod already has | Replaced: CommandLib's command takes the label in Bukkit's command map | Kept by the other plugin: Paper's registrar, which adds the commands at startup and on every data reload, gives CommandLib's command only `<plugin>:<label>`. A `register` after startup takes the label until the next data reload | Merged: Brigadier adds both commands' children under one literal | Velocity's `CommandManager#register` decides |
| Two registrations declare the same node with different defaults | The later registration replaces the permission (a [cooldown bypass](#cooldown-bypass) node keeps the existing one); once it is unregistered, the oldest registration left that declares the node registers it again | The later registration replaces the permission (a cooldown bypass node keeps the existing one); once it is unregistered, the oldest registration left that declares the node registers it again | The first registration's default wins | The first registration's default wins |
| `register`, `unregister` | Main thread, or `IllegalStateException` | Main thread (Folia: the global region thread), or `IllegalStateException` | Not checked: register during mod initialization or on the server thread | Any thread |
| `refresh`, `refreshAll` | Main thread, or `IllegalStateException` | Main thread (Folia: a region or the global region thread), or `IllegalStateException` | Any thread; the tree is sent on the server thread | Not available |
| `diagnostics()` | Main thread, or `IllegalStateException` | Any thread | Any thread | Any thread |

Rejecting an invalid definition in `register` on every platform is new in 0.18.0: Paper used to build a registration
made during plugin initialization only when the `COMMANDS` event fired, and Forge, NeoForge and Fabric in
`RegisterCommandsEvent` or the command registration callback, so the error was logged there and `register` returned
normally. The rule for the same node with different defaults in two registrations is the current behavior, not a
promise: it may change in a later release.

On Paper, a plugin enabled after the server started, such as by a plugin manager, has missed the `COMMANDS` event that
adds its commands. On the next tick CommandLib adds them to Paper's dispatcher itself. When it cannot read that
dispatcher, it calls `Bukkit.reloadData()` instead, which fires the event again but also reloads every data pack, as
`/minecraft:reload` does. On Folia, which has no data reload, it logs a warning and the commands come with the next
server start.

### Spigot

`CommandLib.register(...)` registers every node in Bukkit's `PluginManager` with its default and description,
replacing a permission of the same name (except a cooldown bypass node, see [Cooldown bypass](#cooldown-bypass)).
`unregister()` and the plugin's disable remove them again, take their
defaults away from players, and recalculate online players' permissions. A permission that a later registration has
replaced under the same name since stays, since that registration still uses it. Plugin reloads therefore start from a
clean state. When a registration removes a node that another registration of the same copy of CommandLib still
declares, the oldest of those registers it again with its own default and description, and removes it when it is
unregistered itself. `/minecraft:reload` keeps the permissions.

### Paper

`CommandLib.register(...)` from `onEnable` behaves like Spigot, also when a registration removes a node that another
still declares. `CommandLib.bootstrap(context, ...)` from a `PluginBootstrap` runs before Bukkit's plugin manager
exists, so the nodes are **not** registered with it. CommandLib
applies their defaults itself unless a permission plugin sets the node or the permission is declared in
`paper-plugin.yml`. Bootstrap commands cannot be unregistered.

### Forge / NeoForge

On 1.18+ the nodes are `PermissionNode<Boolean>` objects in the `commandlib` namespace, so the full node name that
permission handlers see is **`commandlib.<node>`**, for example `commandlib.mymod.command.spawn`. They are added in
`PermissionGatherEvent.Nodes`. A registration after that event uses the node's default resolver.

Forge's permission API answers only the nodes registered in that event, so a player's check of any other node, such
as one your code checks with `ctx.getActor().hasPermission(...)`, never reaches the permission handler: it passes only
for players with permission level 4, and LuckPerms cannot grant it. The nodes CommandLib registers, cooldown bypass
nodes included, are not affected. On 1.16.5, Fabric, Spigot, Paper and Velocity the handler answers any node.

On 1.16.5 the nodes are registered with `PermissionAPI.registerNode(node, level, "")` under their own name, without the
`commandlib.` part, when the server has started.

### Fabric

Fabric has no permission registry, so nothing is registered. A player's check asks
[fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) with the node's own name, without a
`commandlib.` part, and the default from the table above as its fallback: `Permissions.check(player, node, default)`.
CommandLib does not ship fabric-permissions-api. Include it in your mod (`include`) for permission mods such as
LuckPerms to answer; without it the default decides.

### Velocity

Velocity has no registry of permissions, so nothing is registered. `CommandLib.register(plugin, proxy, ...)` keeps the
defaults of its nodes and applies them when `PermissionSubject#getPermissionValue(node)` is `UNDEFINED`, that is when
no permission plugin sets the node. A `TRUE` or `FALSE` value always wins. When several registrations declare a node,
the oldest one that is not unregistered decides its default, so a later one cannot open an `OP` node to everyone;
`unregister()` drops the defaults of its registration.

## Where permissions are checked

| Place | Check |
|---|---|
| Client command tree, parsing | Each literal and argument node has a `requires` predicate. A sender without the node does not see it and the server parses the input as if it did not exist, reporting an unknown command or argument. Each option token requires the nodes of its options, including every flag of a combined token such as `-fs`. |
| Tab completion | The client completes commands and literals from its command tree, so hidden nodes are not completed, and an argument's suggestions are empty without its branch node. The server's own completion (Brigadier's `CommandDispatcher#getCompletionSuggestions`, which answers a client's request for an argument's server-side suggestions) does not check requirements, so its answer may still list a hidden literal next to that argument. |
| Generated help | Only routes the sender may use are listed, and only options the sender may give. |
| Execution | The runner checks every node of the route again before parsing: the command's, each parent command's, every argument branch's and every given option's, so input parsed earlier, such as a function's, does not run once the sender lost one of them. A failure sends `You do not have permission to execute this command.` |

A [`visibleWhen`](#refreshing-the-command-tree) predicate is checked in the same places as the command's or branch's
node.

Branches whose first arguments have the same names, such as `/give <player>` and `/give <player> <count>`, share the
nodes of those arguments. A shared node is shown to a sender who may use any of the branches, and the runner then
checks the branch the input runs. The shared arguments must parse the same way: arguments of different types under one
name are rejected at registration.

See [errors](errors.md#permission-denied) for what the sender sees.

## Refreshing the command tree

The client hides what the command tree it last received leaves out. The server sends the tree on join and when the
player's operator status changes, not when a permission changes, so a newly granted command stays hidden, and a revoked
one stays visible, until the tree is sent again. Send it again after the change:

```java
// Spigot / Paper: on the main thread, or it throws IllegalStateException
player.addAttachment(plugin, "myplugin.command.game.start", true);
CommandLib.refresh(player);
CommandLib.refreshAll(); // every online player

// Forge / NeoForge / Fabric: from any thread; the tree is sent on the server thread
CommandLib.refresh(serverPlayer);
CommandLib.refreshAll(); // every player on the running server
```

The tree lists every command on the server, not only CommandLib's, so one call covers every registration.

Velocity has no API to send the tree again and no `CommandLib.refresh`. It adds the proxy's commands to the tree the
backend server sends, so a player sees a permission change when the backend next sends it, such as on a server switch.

Prerequisites (`addPrerequisite`, `require`, `Command.player()`) run when the command runs and do not hide it. To show a
command or an argument branch only in some plugin state, such as `/game start` only before a game starts, give it a
`visibleWhen` predicate and refresh the players it affects when the state changes:

```java
class GameCommand extends Command {
    GameCommand(Game game) {
        super("game");

        addChildren(new Command("start") {{
            visibleWhen(ctx -> !game.isRunning());
            execute(ctx -> {
                game.start();
                CommandLib.refreshAll(); // the players now see /game stop instead
            });
        }}, new Command("stop") {{
            visibleWhen(ctx -> game.isRunning());
            execute(ctx -> {
                game.stop();
                CommandLib.refreshAll();
            });
        }}, new Command("kick") {{
            argument(new PlayerArgument("target")).visibleWhen(ctx -> game.isHost(ctx.getSender()))
                                                  .execute((target, ctx) -> game.kick(target));
        }});
    }
}
```

The predicate is checked where a permission is: a sender for whom it fails does not see the command or branch in the
command tree, in suggestions or in help, and the input is rejected as an unknown command or argument, also when the
state changed after the input was parsed. It gets a context with the sender and no arguments, and runs for each parse
and each player the tree is sent to, so keep it cheap. It does not replace a permission: the nodes are still registered
and checked. Velocity cannot send the tree again, so a hidden proxy command appears when the backend next sends it.

## LuckPerms

CommandLib asks the platform, and LuckPerms answers through it:

- Spigot / Paper: `CommandSender#hasPermission(node)`.
- Forge / NeoForge 1.18+: `PermissionAPI.getPermission(player, node)`.
- Forge 1.16.5: `PermissionAPI.hasPermission(player, node)`.
- Fabric: `Permissions.check(player, node, default)` of fabric-permissions-api, when a mod ships it.
- Velocity: `CommandSource#getPermissionValue(node)`, with the default above when it is `UNDEFINED`.

No LuckPerms-specific setup is needed. Grant the node names from the tables above:

```text
# Spigot / Paper
/lp group default permission set myplugin.command.spawn true
/lp group builder permission set myplugin.command.config.* true

# Forge / NeoForge 1.18+: the commandlib. namespace is part of the node
/lp group default permission set commandlib.mymod.command.spawn true

# Fabric
/lp group default permission set mymod.command.spawn true
```

Wildcards such as `myplugin.command.config.*` are LuckPerms' own feature. Granting `myplugin.command.config.*` does not
grant `myplugin.command.config` itself, which the route also needs.

LuckPerms on Bukkit refreshes players' command trees when their permissions change (its `update-client-command-list`
option), so newly granted commands appear without a rejoin. This has not been tested with CommandLib on every version.
Elsewhere, call [`CommandLib.refresh`](#refreshing-the-command-tree) after the change.

## Testing permissions

`FakeSender` in every testing artifact has every permission unless restricted:

```java
FakeSender allowed = FakeSender.player("Alice")
                               .permissions("myplugin.command.config", "myplugin.command.config.key");
FakeSender denied = FakeSender.player("Bob")
                              .denyPermissions("myplugin.command.config.key");
```

The testing artifacts do not apply `DefaultPermission` defaults, except `velocity-testing`, where a node the sender
leaves unset is granted only if its default is `ALL`, as on a proxy: grant or deny nodes explicitly. See
[testing](testing.md).
