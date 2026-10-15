# Migrating from ACF

This guide maps [Aikar's Annotation Command Framework](https://github.com/aikar/commands)
([wiki](https://github.com/aikar/commands/wiki)) concepts to CommandLib for Spigot and Paper plugins. The examples use
`acf-paper` names on the left and CommandLib's `spigot` / `paper` artifacts on the right.

The main difference in style: ACF reads annotated methods of a `BaseCommand` and resolves each parameter through a
context registered by type. CommandLib has no annotations and no registry; a command is a `Command` subclass whose
arguments are objects, and the executor gets their values as typed lambda parameters. CommandLib commands are always
Brigadier nodes, so the client validates and colors the input while the player types.

Features CommandLib does not have yet are marked **Not available**.

## Setup

| ACF | CommandLib |
|---|---|
| `new PaperCommandManager(plugin)` / `BukkitCommandManager` | No manager: shade the `spigot` or `paper` artifact and relocate `dev.marulabs.commandlib` |
| `manager.registerCommand(new MyCommand())` | `CommandLib.register(plugin, commands...)` in `onEnable` |
| `manager.unregisterCommand(...)` | `CommandLib#unregister()`; plugin disable does it automatically |
| `manager.enableUnstableAPI("brigadier")` | Not needed; commands are always Brigadier nodes |
| — | `CommandLib.bootstrap(context, commands...)` in `PluginBootstrap#bootstrap` (Paper) |

```java
// ACF
@CommandAlias("res|residence")
@CommandPermission("res.use")
public class ResidenceCommand extends BaseCommand {
    @Default
    public void onDefault(CommandSender sender) {
        sender.sendMessage("Residence");
    }

    @Subcommand("list")
    @CommandCompletion("@players")
    public void onList(Player player, @Optional OnlinePlayer target) {
        Player who = target == null ? player : target.getPlayer();
        player.sendMessage("Residences of " + who.getName());
    }
}

// onEnable
PaperCommandManager manager = new PaperCommandManager(this);
manager.registerCommand(new ResidenceCommand());
```

```java
// CommandLib
class ResidenceCommand extends Command {
    ResidenceCommand() {
        super("res");
        addAliases("residence");
        permission("res.use");

        execute(ctx -> ctx.sendMessage("Residence"));
        addChildren(new Command("list") {{
            argument(new PlayerArgument("target").optional(Command.playerRequirer())).execute((target, ctx) -> {
                ctx.sendMessage("Residences of " + target.getName());
            });
        }});
    }
}

// onEnable
CommandLib.register(this, new ResidenceCommand());
```

`PlayerArgument` suggests players by default, so `@CommandCompletion("@players")` needs no counterpart. Without a
target, the console gets the requirer's failure message.

## Command structure

| ACF | CommandLib |
|---|---|
| `extends BaseCommand` + `@CommandAlias("res\|residence")` | `extends Command`, `super("res")`, `addAliases("residence")` |
| `@Default` method | `execute(...)` on the command |
| `@Subcommand("list")` | `addChildren(new Command("list") {{ ... }})` |
| `@Subcommand("list\|ls")` | `addAliases("ls")` in the child |
| `@Subcommand("admin reload")` | A `reload` child inside an `admin` child |
| `@CommandAlias("rl")` on a subcommand (a root alias of it) | **Not available**; register a second root command that calls the same code |
| `@CatchUnknown` | **Not available**; unknown input gets vanilla's `Incorrect argument for command` |
| `@Private` | **Not available**; a command the sender has no permission for is already hidden |
| `@Description("...")` | `description("...")` on commands and branches |
| `@Syntax("<player>")` | Generated from the argument names |
| Command replacements (`%perm`, `getCommandReplacements()`) | Plain Java constants |

## Parameters and contexts

| ACF parameter | CommandLib argument |
|---|---|
| `int`, `long`, `float`, `double`, `boolean`, wrapper types | `IntegerArgument`, `LongArgument`, `FloatArgument`, `DoubleArgument`, `BooleanArgument`, optionally with `min, max` |
| `String` as the last parameter (rest of the input) | `new StringArgument(name, StringArgument.Type.PHRASE)` |
| `@Single String`, or a `String` before other parameters | `new StringArgument(name, StringArgument.Type.WORD)` |
| `@Values("a\|b") String` | `new LiteralArgument(name, List.of("a", "b"))` |
| `@Split(",") String[]` | **Not available** as a delimited string; `new StringArgument(name, StringArgument.Type.WORD).variadic()` takes space-separated values |
| An enum | `new EnumArgument<>(name, MyEnum.class)` |
| `OnlinePlayer`, `Player` (not first) | `PlayerArgument` (names and selectors) |
| `OnlinePlayer[]` | `PlayersArgument` (`List<Player>`) |
| `OfflinePlayer`, `World`, `EntityType`, `ChatColor` | `OfflinePlayerArgument`, `WorldArgument`, `EntityTypeArgument`, `ChatColorArgument` |
| `Location` | `LocationArgument` (vanilla coordinates, `~` and `^`) |
| `@Name("player")` | The argument's name |
| `@Flags("other")`, `@Flags("itemheld")` (resolver modifiers) | **Not available**; use a separate argument or a requirer |

See the [argument catalog](../arguments.md) for every built-in argument.

### `@Optional` and `@Default`

| ACF | CommandLib |
|---|---|
| `@Default("7") Integer number` | `new IntegerArgument("number").optional(7)` |
| `@Optional OnlinePlayer target` (`null` when omitted) | **Not available** as `null`; `optional(Command.playerRequirer())` defaults to the sender, or declare a second branch without it |

Only the last arguments of a branch can be optional, as in ACF.

### Custom contexts

```java
// ACF
manager.getCommandContexts().registerContext(Arena.class, c -> {
    String name = c.popFirstArg();
    Arena arena = arenas.byName().get(name);
    if (arena == null) {
        throw new InvalidCommandArgument("No arena " + name);
    }
    return arena;
});
```

```java
// CommandLib
class JoinCommand extends Command {
    JoinCommand(ArenaManager arenas) {
        super("join");

        requirePlayer();
        argument(new ObjectArgument<Arena>("arena", arenas::byName)).execute((arena, ctx) -> arena.join(ctx.getPlayer()));
    }
}
```

`ObjectArgument` reads the map for every parse and suggests its keys; `ArenaManager` and `Arena` stand for your own
code. There is no registry of types: build the argument where it is used, or write a factory method that returns a
configured argument.

| ACF context kind | CommandLib |
|---|---|
| `registerContext` (consumes input) | An argument: `ObjectArgument`, `NameableObjectArgument`, `EnumArgument`, or a subclass of `Argument` |
| `registerIssuerOnlyContext` (from the sender only) | `require(ctx -> ...)`, which hands the value to the executor |
| `registerIssuerAwareContext` (input, or the sender when omitted) | An argument with `optional(ctx -> ...)` |
| `InvalidCommandArgument("...")` | `ArgumentValidationException("...")` from `validator(...)`, or `CommandPrerequisiteException("...")` from an `Extractor` |

## Issuers

| ACF | CommandLib |
|---|---|
| `CommandSender sender` as the first parameter | `ctx.getSender()` |
| `Player player` as the first parameter (player only) | `requirePlayer()` on the command, then `ctx.getPlayer()`; or `require(Command.playerRequirer())` for a typed player |
| `ConsoleCommandSender` | `requireConsole()`, then `ctx.getConsole()` |
| `BukkitCommandIssuer` | `ctx` itself: `ctx.getSender()`, `ctx.sendMessage(...)` |
| `sender.sendMessage(...)` | `ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), `sendFailure` (red) |

```java
// CommandLib: a typed player value
class HomeCommand extends Command {
    HomeCommand() {
        super("home");

        require(Command.playerRequirer()).execute((player, ctx) -> player.teleport(player.getWorld()
                                                                                         .getSpawnLocation()));
    }
}
```

Forge / NeoForge have no `requirePlayer()` or `playerRequirer()`.

## Permissions

| ACF | CommandLib |
|---|---|
| `@CommandPermission("res.use")` | `permission("res.use")` |
| No annotation (anyone) | `permission(DefaultPermission.ALL)`; root commands default to OP |
| `@CommandPermission` on the class | `permission(...)` on the root command; children get generated nodes and inherit the default |
| Completions hidden without the permission | Commands, subcommands, and argument branches a sender lacks are left out of the sender's command tree |

CommandLib registers a Bukkit permission for every node, with its default and description. See
[permissions](../permissions.md).

## Completions

| ACF | CommandLib |
|---|---|
| `@CommandCompletion("@players")`, `@worlds`, `@mobs`, `@chatcolors` | The default suggestions of `PlayerArgument`, `WorldArgument`, `EntityTypeArgument`, `ChatColorArgument` |
| `@CommandCompletion("a\|b\|c")` | `addSuggestionProvider(sb -> sb.suggest("a").suggest("b").suggest("c"))`, or `LiteralArgument` |
| `@range:1-64` | `addSuggestionProvider(sb -> IntStream.rangeClosed(1, 64).forEach(i -> sb.suggest(String.valueOf(i))))` |
| `@nothing` | `clearSuggestionProviders()` |
| `registerCompletion("warps", c -> ...)` | `setSuggestionProvider(sb -> ...)` on the argument |
| `registerAsyncCompletion(...)` | `sb.await(future.thenAccept(list -> list.forEach(sb::suggest)))` |
| `c.getInput()`, `c.getContextValue(...)` | `sb.getLatestInput()`, `sb.getArgument("earlier")` |
| Completion configs (`@foo:key=value`) | Parameters of your own provider method |

```java
// CommandLib
argument(new StringArgument("warp", StringArgument.Type.WORD).setSuggestionProvider(sb -> warps.names()
                                                                                      .forEach(sb::suggest)))
        .execute((warp, ctx) -> ctx.sendMessage("Warping to " + warp));
```

CommandLib does not filter what a provider adds; filter by `sb.getLatestInput()` yourself. See
[suggestions](../suggestions.md).

## Conditions

| ACF | CommandLib |
|---|---|
| `addCondition("daytime", c -> ...)` + `@Conditions("daytime")` | A `Prerequisite<CommandContext>` added with `addPrerequisite(...)` |
| Parameter conditions `@Conditions("limits:min=5,max=10")` | `validator(...)` on the argument, or `new IntegerArgument(name, 5, 10)` |
| `throw new ConditionFailedException("...")` | `throw new CommandPrerequisiteException("...")` in a prerequisite, `ArgumentValidationException("...")` in a validator |

```java
// CommandLib: a reusable condition
final class Conditions {
    static final Prerequisite<CommandContext> DAYTIME = ctx -> {
        long time = ctx.getPlayer()
                       .getWorld()
                       .getTime();
        if (time > 12300) {
            throw new CommandPrerequisiteException("Target world must be currently day.");
        }
    };
}

class DarkenCommand extends Command {
    DarkenCommand() {
        super("darken");

        requirePlayer();
        addPrerequisite(Conditions.DAYTIME);
        execute(ctx -> ctx.sendSuccess("Darkened"));
    }
}
```

Prerequisites run in the order they are added, and a command's prerequisites also apply to its children unless a child
calls `disableParentPrerequisite()`.

## Options

**Not in ACF.** CommandLib has typed flags and value options placed after the command name:

```java
class ScanCommand extends Command {
    ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            boolean isForce = ctx.getOption(force);
        });
    }
}
```

`/scan -f Steve`, `/scan --force Steve`. See [options](../options.md).

Also not in ACF: `confirm()` asks the sender to run the command again before it executes, and `cooldown(Duration)`
keeps a player from running it again too soon. See [confirmation and cooldown](../../README.md#confirmation-and-cooldown).

## Help

| ACF | CommandLib |
|---|---|
| `@HelpCommand` + `CommandHelp help` + `help.showHelp()`, `enableUnstableAPI("help")` | Built in: `/<command> help`; a command without an executor sends its usage |
| `@Description` | `description(...)`; it appears in the generated usage |
| Syntax advice on wrong input | Brigadier's vanilla error with the `...<--[HERE]` context line; see [errors](../errors.md) |
| Bukkit `/help` entry | `serverHelp(shortText[, fullText])` |

## Locales and messages

| ACF | CommandLib |
|---|---|
| `MessageKeys`, `getLocales().addMessageBundle(...)`, `loadYamlLanguageFile(...)` | **Not available**. CommandLib's own messages are fixed English text |
| `usePerIssuerLocale(true)` | `ctx.getLocale()` / `ctx.getLanguage()` return the sender's client locale for your own messages |
| `{@@key}` in `@Description` | `description(ctx -> messages.get(ctx.getLocale(), "key"))` |
| `setFormat(MessageType.ERROR, ...)` | **Not available**; failures are red, `sendSuccess` green, `sendWarn` yellow |
| `setDefaultExceptionHandler(...)` | **Not available** globally; `addUncaughtExceptionHandler((e, ctx) -> ...)` on a command or argument |

```java
class HelloCommand extends Command {
    HelloCommand(Messages messages) {
        super("hello");

        description(ctx -> messages.get(ctx.getLocale(), "hello.description"));
        execute(ctx -> ctx.sendMessage(messages.get(ctx.getLocale(), "hello.reply")));
        addUncaughtExceptionHandler((e, ctx) -> ctx.sendFailure(messages.get(ctx.getLocale(), "error")));
    }
}
```

`Messages` stands for your own translation lookup.

## Dependency injection

**Not available** as a container. `manager.registerDependency(...)` and `@Dependency` fields become constructor
parameters: `CommandLib.register(this, new JoinCommand(arenaManager))`.

## Testing

`spigot-testing` / `paper-testing` run commands without a server:
`new CommandTester(ResidenceCommand::new, "myplugin.command")`, then `tester.execute("res list Steve", sender)`. See
[testing](../testing.md).

## Not available yet

Tracked in the roadmap: an annotation API, dependency injection, reusable named conditions, a locale and message key
system for CommandLib's own messages and help, message formats, catch-unknown handlers, hidden commands, root aliases
for subcommands, and global contexts and completions by type. Asynchronous execution is `executeAsync(...)`, see
[execution](../execution.md#async-executors).

See also: [Migrating from CommandAPI](from-commandapi.md), [Cloud](from-cloud.md), [Lamp](from-lamp.md).
