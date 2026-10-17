# Documentation exports and diagnostics

CommandLib can describe the commands it registers: as Markdown or JSON for your plugin's documentation, as a list of
every permission node for server admins, and at runtime as a diagnostics report of the platform and the registrations.

## Command documentation

`CommandDocs` documents top-level commands with the permission prefix they are registered with. The registration
returns the same object for its own commands:

```java
// Spigot / Paper
CommandLib registration = CommandLib.register(this, new SpawnCommand(), new ConfigCommand());
CommandDocs docs = registration.docs();

// Anywhere, also in a build script or a unit test, without registering
CommandDocs docs = CommandDocs.of("myplugin.command", new SpawnCommand(), new ConfigCommand());

Files.writeString(Path.of("commands.md"), docs.markdown());
Files.writeString(Path.of("commands.json"), docs.json());
Files.writeString(Path.of("permissions.md"), docs.permissionsMarkdown());
Files.writeString(Path.of("permissions.json"), docs.permissionsJson());
```

| Method | Output |
|---|---|
| `markdown()` | A `## /route` section per command and child command, depth first: description, aliases, permission, usages, arguments, options and the [examples](help.md#examples). |
| `json()` | `permissionPrefix` and a `commands` array with the same content as a tree: each command has its `examples`, `branches` (argument branches with their `arguments` and `children`) and `children`. |
| `permissionsMarkdown()` | A table of every permission node with its default, the route it guards and its description. |
| `permissionsJson()` | The same as a `permissions` array of `node`, `default`, `description`, `route` and `kind` (`command`, `argument`, `option` or `cooldown-bypass`). |
| `tree()` | Indented text, one line per command, option and argument branch, as `CommandTester#commandTree()` renders it in the [testing artifacts](testing.md#command-tree-snapshots). |

Each call reads the commands again. Arguments are shown as `<name>`, optional ones as `[name]` and variadic ones as
`<name...>`; argument types are the simple class names, so an anonymous subclass shows the class it extends. A command
that has no executor of its own shows its help, so it lists no usage of itself. Descriptions set with
`description(ctx -> ...)` depend on the sender and are left out (`null` in JSON). Color codes are removed.

For this command:

```java
class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");
        description("Manage the configuration");
        addAliases("cfg");
        permission(DefaultPermission.ALL, "Use /config");
        option(Options.flag("force", 'f')
                      .aliases("yes")
                      .description("Skip confirmation"));
        execute(ctx -> ctx.sendSuccess("..."));
        argument(new StringArgument("key")).permission(DefaultPermission.OP, "Access config keys")
                                           .description("Show a key")
                                           .execute((key, ctx) -> ctx.sendSuccess(key));
    }
}
```

`markdown()` returns:

```markdown
## /config

Manage the configuration

- Aliases: `cfg`
- Permission: `myplugin.command.config` (default `ALL`)

| Usage | Description | Permission |
| --- | --- | --- |
| `/config [options]` | Manage the configuration | `myplugin.command.config` (`ALL`) |
| `/config [options] <key>` | Show a key | `myplugin.command.config.key` (`OP`) |

Arguments:

| Argument | Type | Notes |
| --- | --- | --- |
| `key` | `StringArgument` |  |

Options:

| Option | Value | Notes | Description |
| --- | --- | --- | --- |
| `-f`, `--force`, `--yes` |  |  | Skip confirmation |
```

The `Permission` column names the node of the last step of the usage. A usage needs the nodes of every step on its
route, so `/config <key>` also needs `myplugin.command.config`.

## Permission export

`permissionsMarkdown()` and `permissionsJson()` list every node CommandLib registers and checks for the commands, built
by the [node rules](permissions.md#node-names): one per command and child command, one per option that calls
`permission(...)`, and one per argument branch, in the order of the command tree, with the default after
[inheritance](permissions.md#defaults). A node set with `permission("...")` is listed as set, once for each route that uses it.

```markdown
| Permission | Default | Route | Description |
| --- | --- | --- | --- |
| `myplugin.command.config` | `ALL` | `/config` | Use /config |
| `myplugin.command.config.key` | `OP` | `/config <key>` | Access config keys |
```

```json
{
  "permissionPrefix": "myplugin.command",
  "permissions": [
    {
      "node": "myplugin.command.config",
      "default": "ALL",
      "description": "Use /config",
      "route": "/config",
      "kind": "command"
    }
  ]
}
```

The default is CommandLib's `DefaultPermission`; see [per platform](permissions.md#per-platform) for what it means on
each platform. On Forge / NeoForge 1.18+, permission handlers see the nodes with a `commandlib.` namespace in front
(`commandlib.myplugin.command.config`); the export lists them without it, as `permission(...)` takes them.

## Runtime diagnostics

`CommandLib.diagnostics()` reports what this copy of CommandLib registered and how. Call it on the main server thread.

```java
CommandLibDiagnostics diagnostics = CommandLib.diagnostics();
getLogger().info(diagnostics.report());
```

| Method | Content |
|---|---|
| `platform()` | `key: value` lines about the platform. |
| `registrations()` | One `Registration` per `register` (or Paper `bootstrap`) call not unregistered yet: `owner()`, `path()`, `labels()` and `docs()`. |
| `summary()` | The platform lines and one entry per registration with its prefix, path, labels and number of permission nodes. |
| `commands()` | The `tree()` of every registration. |
| `permissions()` | Every permission node of every registration with its default and route. |
| `report()` | `summary()`, `commands()` and `permissions()`. |

The platform lines are:

| Platform | Lines |
|---|---|
| Spigot (`spigot` artifact) | Artifact, CommandLib package (shows a relocation), server and Minecraft version, Java, the command dispatcher (the vanilla dispatcher through NMS, or Paper's dispatcher when the spigot jar runs on Paper 1.20.6+), each typed NMS module for the server's version with its status (`in use`, `available`, or why it cannot be used), and every NMS wrapper without an implementation. |
| Paper (`paper` artifact) | Artifact, CommandLib package, server and Minecraft version, Java, the registration API, and whether a `COMMANDS` event has been seen. |
| Forge / NeoForge / Fabric | Artifact, CommandLib package, loader version, Minecraft version, Java, how commands and permission nodes are registered, and whether a server runs. |
| Velocity | Artifact, CommandLib package, proxy name and version, Java, and the registration API. |

A registration's path tells how its commands reach the dispatcher:

| Platform | Paths |
|---|---|
| Spigot | `vanilla dispatcher and Bukkit command map`; on Paper 1.20.6+ `Paper dispatcher, added again by a COMMANDS lifecycle handler on data reloads`, or `... on ServerResourcesReloadedEvent` when registered after the plugin enabled. |
| Paper | `COMMANDS lifecycle handler` (`, waiting for the event` before it fires), or `bootstrap COMMANDS lifecycle handler`. |
| Forge / NeoForge / Fabric | `server dispatcher` while a server runs, `waiting for the server` before. The owner is the permission prefix, since registration takes no mod id. |
| Velocity | `BrigadierCommand`. The owner is the plugin id. |

A plugin or mod that shades and relocates CommandLib has its own copy, which reports only its own registrations. A
CommandLib that several plugins share reports all of them.

### Diagnostics command

`CommandLibDiagnosticsCommand` sends the report to a sender. CommandLib never registers it; register it like your
other commands, under a name of your choice:

```java
// Spigot / Paper
CommandLib.register(this, new SpawnCommand(), new CommandLibDiagnosticsCommand("myplugin-diagnostics"));

// Forge / NeoForge / Fabric
CommandLib.register("mymod.command", new SpawnCommand(), new CommandLibDiagnosticsCommand("mymod-diagnostics"));

// Velocity
CommandLib.register(this, proxy, new SendCommand(), new CommandLibDiagnosticsCommand("myproxy-diagnostics"));
```

| Input | Sends |
|---|---|
| `/myplugin-diagnostics` | `summary()` |
| `/myplugin-diagnostics commands` | `commands()` |
| `/myplugin-diagnostics permissions` | `permissions()` |

Its permission nodes follow the usual rules (`myplugin.command.myplugin-diagnostics`, `...commands`,
`...permissions`) and default to `OP`, like any root command. A summary on Spigot 1.21.4 looks like:

```text
CommandLib diagnostics
Platform:
  Artifact: spigot
  CommandLib package: com.example.myplugin.libs.commandlib
  Server: CraftBukkit 4402-Spigot-... (MC: 1.21.4)
  Minecraft: 1.21.4
  Java: 21.0.5
  Command dispatcher: vanilla Brigadier dispatcher through NMS
  Typed NMS module spigot_1_21_4: in use
Registrations: 1
  MyPlugin: prefix myplugin.command, vanilla dispatcher and Bukkit command map
    labels: spawn, myplugin:spawn, myplugin-diagnostics, myplugin:myplugin-diagnostics
    permissions: 5
```
