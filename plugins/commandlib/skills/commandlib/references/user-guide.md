# CommandLib User Guide

This reference is for downstream Spigot or Paper plugin projects that depend
on CommandLib. It is not repository-maintenance guidance for CommandLib itself.

## Choosing the Right Artifact

| Artifact | Target platform | Java |
|---|---|---|
| `spigot` | Spigot, Paper up to 1.21.1, Mohist | 11+ |
| `paper` | Paper 1.21.0+ (official command/lifecycle API) | 21+ |

Use the `paper` artifact to take advantage of Paper's official command
registration API and Adventure component messages. Use `spigot` for Spigot,
Paper up to 1.21.1, or Mohist. Newer Paper needs `paper`.

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
same package (`dev.marulabs.commandlib`) as in `spigot`, so command class
source is portable between the two artifacts with no import changes. The key
differences are the registration timing and the message path:
`CommandContext.sendSuccess(String)` uses Adventure `Component` internally on
Paper.

This makes generated command and argument branch permissions use nodes such as
`myplugin.command.spawn` and `myplugin.command.config.key`.

## Messages

`ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), and `sendFailure` (red) accept a `String`, a native
component, or a `RichText`. `RichText` is an immutable, platform-independent message that works the same on Spigot,
Paper, and Forge / NeoForge, so code shared between platforms can send colors, decorations, hover text, and click
actions:

```java
import dev.marulabs.commandlib.util.text.ClickAction;
import dev.marulabs.commandlib.util.text.RichText;

class DifficultyCommand extends Command {
    DifficultyCommand() {
        super("difficulty");

        execute(ctx -> {
            ctx.sendMessage(RichText.text("difficulty: ")
                                    .append(RichText.text("hard")
                                                    .color(0xFF5555)
                                                    .bold()
                                                    .hover(RichText.text("Click to change"))
                                                    .click(ClickAction.suggestCommand("/difficulty "))));
            ctx.sendFailure(RichText.translatable("commands.give.failed.toomanyitems", RichText.text("64")));
        });
    }
}
```

`sendSuccess` / `sendWarn` / `sendFailure` only color a message that has no color of its own and never change the value
or component passed in. `RichTexts.toBungee` (spigot), `RichTexts.toAdventure` (paper), and `RichTexts.toVanilla`
(Forge / NeoForge) convert a `RichText` to the native component. For Paper-only plugins, Adventure and MiniMessage can
be used directly: `ctx.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hello"))`.

### Translated Messages

CommandLib's own messages are captions sent in the sender's locale (English and Japanese built in). Translate them and
your own messages with `Caption` and `Captions`, given to each top-level command:

```java
static final Caption HOME_SET = Caption.of("myplugin.home.set", "Home {name} set.");

Captions captions = Captions.create()
                            .set(Locale.JAPANESE, Caption.PERMISSION_DENIED, "それはできません。")
                            // lang/messages.properties, lang/messages_ja.properties, ... read as UTF-8.
                            .bundle("lang/messages", plugin.getClass().getClassLoader());

class HomeCommand extends Command {
    HomeCommand(Captions captions) {
        super("home");
        captions(captions);
        argument(new StringArgument("name")).execute((name, ctx) -> ctx.sendSuccess(HOME_SET, Map.of("name", name)));
    }
}
```

`ctx.caption(caption[, placeholders])` returns the text, `new CommandPrerequisiteException(caption)` fails with it, and
`Captions#sender(...)` formats every caption (a prefix, MiniMessage on Paper). Tests set a player's locale with
`FakeSender#locale("ja_jp")`.

### Feedback And Results

`sendSuccess` is vanilla command feedback: hidden while the `sendCommandFeedback` gamerule is off and for silent
sources such as functions. `sendSuccess(message, true)` also broadcasts to operators and, with `logAdminCommands`, the
server log. `ctx.setResult(int)` sets what `/execute store result` stores (1 unless set). A command fails, storing 0
for `store result` and `store success`, when its executor throws `CommandPrerequisiteException`, its preprocess returns
false, or a permission, argument, or prerequisite check fails. `CommandTester#execute` returns the result, 0 for a
failure.

```java
argument(new PlayersArgument("targets")).execute((targets, ctx) -> {
    if (targets.isEmpty()) {
        throw ctx.fail("No player was found");
    }
    ctx.sendSuccess("Counted " + targets.size() + " players", true);
    ctx.setResult(targets.size());
});
```

## Server Help

`serverHelp(shortText)` sets the text the server's `/help` lists a top-level Bukkit command with, and
`serverHelp(shortText, fullText)` also replaces the whole `/help <command>` page. Without it the command is listed as
"A command by the <plugin> plugin."; `description(...)` is not used there. Forge / NeoForge ignore it.

```java
class SpawnCommand extends Command {
    SpawnCommand() {
        super("spawn");
        serverHelp("Teleports you to the spawn.");
    }
}
```

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
import dev.marulabs.commandlib.DefaultPermission;

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

To document the commands or list every generated node, use `CommandDocs`
(`registration.docs()`, or `CommandDocs.of(prefix, commands)` without
registering): `markdown()`, `json()`, `permissionsMarkdown()`,
`permissionsJson()`. For runtime inspection, `CommandLib.diagnostics()` returns
a report, and `new CommandLibDiagnosticsCommand("myplugin-diagnostics")` is an
opt-in command that sends it; CommandLib never registers it by itself.

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

Use `confirm()` for destructive commands and `cooldown(Duration)` to throttle
players. Both run after prerequisites, apply to children like a preprocess, and
take a `Confirmation` / `Cooldown` to change the timeout, the messages, the clock
(for tests), or a cooldown bypass permission:

```java
class ResetCommand extends Command {
    ResetCommand() {
        super("reset");
        confirm();
        cooldown(Cooldown.of(Duration.ofMinutes(5))
                         .bypassPermission("myplugin.cooldown.bypass"));
        execute(ctx -> ctx.sendSuccess("Reset."));
    }
}
```

Argument chains can also have child commands. Prefer this only for vanilla-like
or compatibility syntax where the target comes before the action.

The `child(...)` factory receives an `ArgRef<T>` for each parent argument, not
the parsed runtime value. Call `ref.get()` inside the child's executor or
suggestion providers to read the value. In code that runs after the callback
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
