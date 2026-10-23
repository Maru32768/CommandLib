# CommandLib User Guide

This reference is for downstream Spigot, Paper or Velocity plugin projects and
Forge, NeoForge or Fabric mod projects that depend on CommandLib. It is not repository-maintenance guidance for CommandLib itself.

## Choosing the Right Artifact

| Artifact | Target platform | Java |
|---|---|---|
| `spigot` | Spigot, Paper up to 1.21.1, Mohist | 11+ |
| `paper` | Paper 1.21.0+ (official command/lifecycle API) | 21+ |
| `velocity` | Velocity 3.4+ proxy | 17+ |
| `forge-1.16.5`, `forge-1.18.2`, `forge-1.19.2`, `forge-1.20.1` | Forge, one artifact per Minecraft version | same as the Minecraft version |
| `neoforge-1.21.1`, `neoforge-1.21.4` | NeoForge | 21+ |
| `fabric-1.20.1`, `fabric-1.21.1`, `fabric-1.21.4` | Fabric, needs Fabric API (`modImplementation`) | same as the Minecraft version |
| `spigot-kotlin`, `paper-kotlin`, `velocity-kotlin` | Kotlin DSL on top of the platform artifact | as the platform |

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

A Paper plugin with a bootstrapper (`paper-plugin.yml`) can register from
`PluginBootstrap#bootstrap` instead, so the commands exist before data pack
functions are compiled and functions can use them. Bootstrap commands stay for
the server's lifetime and cannot be unregistered.

```java
public class MyBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        CommandLib.bootstrap(context, new MyCommand());
    }
}
```

The `Command` and `CommandContext` classes in the `paper` artifact are in the
same package (`dev.marulabs.commandlib`) as in `spigot`, so command class
source is portable between the two artifacts with no import changes. The key
differences are the registration (`CommandLib.bootstrap` exists only on Paper)
and the message path: `CommandContext.sendSuccess(String)` uses Adventure
`Component` internally on Paper.

This makes generated command and argument branch permissions use nodes such as
`myplugin.command.spawn` and `myplugin.command.config.key`.

### Forge, NeoForge and Fabric

There is no plugin object: the permission prefix is the first argument.
Register from the mod constructor on Forge / NeoForge and from
`onInitialize()` on Fabric. `Command` and `CommandContext` are in the same
package; the arguments return vanilla types (`ServerPlayer`, `ServerLevel`, ...).

```java
@Mod("mymod")
public final class MyMod {
    public MyMod() {
        CommandLib.register("mymod.command", new MyCommand());
    }
}

public final class MyFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandLib.register("mymod.command", new MyCommand());
    }
}
```

On Fabric, declare `fabric-command-api-v2` and `fabric-lifecycle-events-v1` (or
`fabric-api`) in `fabric.mod.json`.

### Velocity

Register from the proxy initialize event with the plugin instance and the
`ProxyServer`. The default prefix is `<plugin id>.command`. Velocity has only
proxy arguments (primitives, strings, duration, enum, literal, object,
`PlayerArgument`, `ServerArgument`); there are no world, entity or location
arguments.

```java
@Plugin(id = "myproxy", name = "MyProxy", version = "1.0.0")
public final class MyProxyPlugin {
    private final ProxyServer proxy;

    @Inject
    public MyProxyPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        CommandLib.register(this, proxy, new MyCommand());
    }
}
```

The README of the CommandLib repository also covers annotated commands
(`AnnotatedCommands`), the Kotlin DSL (`*-kotlin` artifacts) and
`executeAsync(...)`.

## Messages

`ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), and `sendFailure` (red) accept a `String`, a native
component, or a `RichText`. `RichText` is an immutable, platform-independent message that works the same on Spigot,
Paper, Velocity, and Forge / NeoForge / Fabric, so code shared between platforms can send colors, decorations, hover text, and click
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
or component passed in. `RichTexts.toBungee` (spigot), `RichTexts.toAdventure` (paper, velocity), and `RichTexts.toVanilla`
(Forge / NeoForge / Fabric) convert a `RichText` to the native component. For Paper-only plugins, Adventure and MiniMessage can
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
sources such as functions. `ctx.setResult(int)` sets what `/execute store result` stores (1 unless set). A command fails, storing 0
for `store result` and `store success`, when its executor throws `CommandPrerequisiteException`, its preprocess returns
false, or a permission, argument, or prerequisite check fails. `CommandTester#execute` returns the result, 0 for a
failure.

```java
argument(new PlayersArgument("targets")).execute((targets, ctx) -> {
    if (targets.isEmpty()) {
        throw ctx.fail("No player was found");
    }
    ctx.sendSuccess("Counted " + targets.size() + " players");
    ctx.setResult(targets.size());
});
```

## Server Help

`serverHelp(shortText)` sets the text the server's `/help` lists a top-level Bukkit command with, and
`serverHelp(shortText, fullText)` also replaces the whole `/help <command>` page. Without it the command is listed as
"A command by the <plugin> plugin."; `description(...)` is not used there. Only Spigot and Paper read it.

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

The bypass permission is registered with the command (default `OP`), so
permission plugins can grant it on every platform. On Forge / NeoForge 1.18+ a
node CommandLib did not register, such as one checked only with
`ctx.getActor().hasPermission(...)`, cannot be granted: only permission level 4
passes it there.

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
