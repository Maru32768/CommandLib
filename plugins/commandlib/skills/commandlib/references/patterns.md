# CommandLib Usage Patterns

The patterns below apply to both the `spigot` and `paper` artifacts unless noted
otherwise. The `Command`, `Argument`, and option classes share the same public
API across both artifacts; only registration timing and the `CommandContext`
message path differ.

## Argument Style

Prefer typed argument instances with `argument(...).execute(...)` over the
builder style for normal command implementations.

```java
class MyCommand extends Command {
    MyCommand() {
        super("message");

        argument(new PlayerArgument("target"), new StringArgument("text")).execute((target, text, ctx) -> {
            target.sendMessage(text);
        });
    }
}
```

Use the builder style only when the typed shorthand cannot express the required
shape.

Options such as validation and suggestions are set via fluent methods on the
argument instance:

```java
class MyCommand extends Command {
    MyCommand() {
        super("cmd");

        argument(new PlayerArgument("target").validator(Player::isOp)
                                             .addSuggestionAction(sb -> sb.suggest("Steve"))).execute((player, ctx) -> {
            ctx.sendMessage("ok");
        });
    }
}
```

## Subcommand Shape

Prefer literal-first subcommands for ordinary plugin commands:

```text
/config get <key>
/config set <key> <value>
/arena start <name>
/arena delete <name>
```

Use argument-first subcommands only when the requested syntax needs it, such as
matching a vanilla-like command tree or an existing command contract:

```text
/config <key> get
/config <key> set <value>
```

Use typed argument instances for this shape. The `child(...)` factory receives
an `ArgRef<T>` for each parent argument, not the parsed value, because child
commands are built before a command is executed. Read the parent value with
`ref.get()` inside the child's executor, suggestion actions, or other command
callbacks. Outside of a callback (for example in a scheduled task), resolve the
value first (`String k = key.get();`) or use `ref.get(ctx)`.

The same argument branch can define multiple children by calling `child(...)`
multiple times:

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

Do not write the factory as if it receives runtime values:

```java
class MyCommand extends Command {
    MyCommand() {
        // Wrong: keyValue is an Argument object, not the parsed String value.
        argument(new StringArgument("key")).child(keyValue -> new Command("get") {{
            execute(ctx -> ctx.sendMessage(keyValue.toString()));
        }});
    }
}
```

## Permissions

Use `permission(...)` on commands for command-level access control. Argument
branches also receive generated permission nodes by default, so a route such as
`config <key>` is registered and checked separately from the base `config`
command.

Root commands default to OP. Subcommands inherit their parent command's default
permission. Argument branches inherit the parent command's default permission,
and child commands under an argument branch inherit the argument branch default.

```java
class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).execute((key, ctx) -> {
            ctx.sendMessage("config " + key);
        });
    }
}
```

Use `argument(...).permission(...)` to override the generated node or default
permission metadata. For default permission metadata on argument branches, use
CommandLib's `DefaultPermission` enum.

```java
import net.kunmc.lab.commandlib.DefaultPermission;

class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).permission(DefaultPermission.OP, "Access config keys");
    }
}
```

## Command Options

Use `CommandOption<T, CommandContext>` for options such as `-f`, `--force`, and
`--limit 10`. Register options on the command, then read them with
`ctx.getOption(option)`.

```java
class ScanCommand extends Command {
    ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                     .description("Force execution"));
        CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                     .description("Maximum count"));
        CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text")
                                                                     .description("Output format"));

        argument(new StringArgument("text", StringArgument.Type.WORD)).execute((text, ctx) -> {
            boolean isForce = ctx.getOption(force);
            int maxCount = ctx.getOption(limit);
            String outputFormat = ctx.getOption(format);
            boolean limitWasSpecified = ctx.hasOption(limit);
            ctx.sendMessage(text + ":" + isForce + ":" + maxCount + ":" + outputFormat + ":" + limitWasSpecified);
        });
    }
}
```

Supported forms:

```text
/scan alex
/scan -f alex
/scan --force alex
/scan -fv alex
/scan -n 20 alex
/scan --limit 20 alex
/scan -f -n 20 --format json alex
```

Important constraints:

- Options must appear immediately after the command or subcommand name, before
  regular arguments.
- For child commands, options belong to the most specific child command:
  `/game start -f arena`, not `/game -f start arena`.
- Value options use separated values only: `--limit 20` and `-n 20`; do not
  generate `--limit=20` or `-n20`.
- Prefer typed option keys over `ctx.getArgument(...)`.
- Use `ctx.hasOption(option)` when code must distinguish explicit presence from
  a default value.
- Add `.description(...)` for user-facing commands so help output is clear.
- Use `.requires(...)` for option dependencies.
