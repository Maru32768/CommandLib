# CommandLib User Guide

This reference is for downstream Spigot or Paper plugin projects that depend
on CommandLib. It is not repository-maintenance guidance for CommandLib itself.

## Choosing the Right Artifact

| Artifact | Target platform | Java |
|---|---|---|
| `spigot` | Spigot, Bukkit-compatible Paper, Mohist | 11+ |
| `paper` | Paper 1.21.0+ (official command/lifecycle API) | 21+ |

Use the `paper` artifact to take advantage of Paper's official command
registration API and Adventure component messages. Use `spigot` for Spigot,
older Paper versions (up to 1.20.6), or Mohist.

## Registration

### Spigot

When no permission prefix is specified, registration uses the plugin name in
lowercase plus `.command`. For example, plugin `MyPlugin` uses
`myplugin.command`.

```java
class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandLib.register(this, "myplugin.command", new MyCommand());
    }
}
```

### Paper

On Paper 1.21.0+, register commands the same way as Spigot — in `onEnable()`.

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandLib.register(this, "myplugin.command", new MyCommand());
    }
}
```

The `Command` and `CommandContext` classes in the `paper` artifact are in the
same package (`net.kunmc.lab.commandlib`) as in `spigot`, so command class
source is portable between the two artifacts with no import changes. The key
differences are the registration timing and the message path:
`CommandContext.sendSuccess(String)` uses Adventure `Component` internally on
Paper.

This makes generated command and argument branch permissions use nodes such as
`myplugin.command.spawn` and `myplugin.command.config.key`.

## Permissions

Register commands with a plugin-specific prefix, then set command or argument
branch permissions as needed.

```java
class MyCommand extends Command {
    MyCommand() {
        super("spawn");

        permission("myplugin.command.spawn");
        permission("myplugin.command.spawn", DefaultPermission.NONE);
        permission(DefaultPermission.OP);
    }
}
```

Root commands default to OP. Subcommands inherit their parent command's default
permission unless they override it. Argument branches inherit the parent
command's default permission, and child commands under an argument branch inherit
the argument branch default.

Argument branches also get generated permission nodes by default. With
registration prefix `myplugin.command`, `config <key>` generates and checks
`myplugin.command.config.key`. Use `argument(...).permission(...)` to override
the generated node or default metadata. Argument branch permissions affect
execution, tab completion, generated help, and the permission nodes registered
by CommandLib.

```java
import net.kunmc.lab.commandlib.DefaultPermission;

class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).execute((key, ctx) -> {
            ctx.sendMessage("config " + key);
        });

        // Override the generated node/default for this argument branch.
        argument(new StringArgument("secret")).permission(DefaultPermission.OP, "Access secret config keys")
                                              .execute((secret, ctx) -> {
                                                  ctx.sendMessage("secret " + secret);
                                              });
    }
}
```

## Subcommands And Prerequisites

Prerequisites are inherited by children by default. Define shared checks on the
parent command:

```java
class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandLib.register(this, "myplugin.command", new Command("game") {{
            addPrerequisite(ctx -> {
                if (!ctx.getSender()
                        .hasPermission("myplugin.game")) {
                    throw new CommandPrerequisiteException("No permission");
                }
            });
            addChildren(new StartCommand(), new StopCommand());
        }});
    }
}
```

Argument chains can also have child commands. Prefer this only for vanilla-like
or compatibility syntax where the target comes before the action.

The `child(...)` factory receives an `ArgRef<T>` for each parent argument, not
the parsed runtime value. Call `ref.get()` inside the child's executor or
suggestion actions to read the value. In code that runs after the callback
returns (such as a scheduled task), resolve the value first or use
`ref.get(ctx)`:

```java
class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).description("Select a config key")
                                           .child(key -> new Command("get") {{
                                               execute(ctx -> {
                                                   ctx.sendMessage("get " + key.get());
                                               });
                                           }})
                                           .child(key -> new Command("set") {{
                                               argument(new StringArgument("value")).execute((value, ctx) -> {
                                                   ctx.sendMessage("set " + key.get() + " to " + value);
                                               });
                                           }});
    }
}
```

Calling `child(...)` repeatedly appends multiple child commands under the same
argument branch.
