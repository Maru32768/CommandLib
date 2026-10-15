# Migrating from Lamp

This guide maps [Lamp](https://github.com/Revxrsal/Lamp) 4.x ([docs](https://foxhut.gitbook.io/lamp-docs)) concepts
to CommandLib for Spigot and Paper plugins. The examples use `lamp.bukkit` names on the left and CommandLib's `spigot` /
`paper` artifacts on the right.

The main difference in style: Lamp reads annotated methods and turns each parameter into an argument. CommandLib has no
annotations; a command is a `Command` subclass whose arguments are objects, and the executor gets their values as typed
lambda parameters. The command tree is real Brigadier on Spigot as well as Paper, from 1.16.5 to 26.x.

Features CommandLib does not have yet are marked **Not available**.

## Setup

| Lamp | CommandLib |
|---|---|
| `BukkitLamp.builder(plugin)...build()` | No builder: shade the `spigot` or `paper` artifact and relocate `dev.marulabs.commandlib` |
| `lamp.register(new MyCommands())` | `CommandLib.register(plugin, commands...)` in `onEnable` |
| Compile with `-parameters` for parameter names | Not needed: each argument is named in its constructor |
| `lamp.brigadier` module, Brigadier on Paper only after 1.19.1 | Always Brigadier, on Spigot and Paper |
| — | `CommandLib.bootstrap(context, commands...)` in `PluginBootstrap#bootstrap` (Paper) |

```java
// Lamp
public final class PingCommand {
    @Command("ping")
    public void ping(BukkitCommandActor actor) {
        actor.reply("pong");
    }
}

@Override
public void onEnable() {
    Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this).build();
    lamp.register(new PingCommand());
}
```

```java
// CommandLib
@Override
public void onEnable() {
    CommandLib.register(this, new Command("ping") {{
        execute(ctx -> ctx.sendMessage("pong"));
    }});
}
```

## Commands and parameters

```java
// Lamp
@Command("give")
public void give(BukkitCommandActor actor, Player target, @Default("1") @Range(min = 1, max = 64) int amount) {
    actor.reply("Gave " + amount + " to " + target.getName());
}
```

```java
// CommandLib
class GiveCommand extends Command {
    GiveCommand() {
        super("give");

        argument(new PlayerArgument("target"), new IntegerArgument("amount", 1, 64).optional(1)).execute((target, amount, ctx) -> {
            ctx.sendSuccess("Gave " + amount + " to " + target.getName());
        });
    }
}
```

Typed executors take up to seven arguments. Beyond that, use `argument(builder -> ...)` with `ctx.getArgument(...)`.

### Parameter types

| Lamp parameter | CommandLib argument |
|---|---|
| `int`, `long`, `float`, `double`, `boolean` with `@Range(min, max)` | `IntegerArgument(name, min, max)`, `LongArgument`, `FloatArgument`, `DoubleArgument`, `BooleanArgument` |
| `String` as the last parameter (rest of the input) | `new StringArgument(name, StringArgument.Type.PHRASE)` |
| `@Single String`, or a `String` before other parameters | `new StringArgument(name, StringArgument.Type.WORD)`; `new StringArgument(name)` accepts a quoted phrase |
| `@Length(min, max) String` | `StringArgument` with `validator(s -> s.length() >= min && s.length() <= max)` |
| An enum | `new EnumArgument<>(name, MyEnum.class)` |
| `Player` | `PlayerArgument` (names and selectors) |
| `EntitySelector<Player>`, `EntitySelector<Entity>` | `PlayersArgument` (`List<Player>`), `EntitiesArgument` (`List<Entity>`) |
| `OfflinePlayer`, `World`, `EntityType` | `OfflinePlayerArgument`, `WorldArgument`, `EntityTypeArgument` |
| `UUID` | `UUIDArgument` |
| `List<T>`, arrays, `@Delimiter` | `variadic()` on the element argument (`List<T>`): each value is its own token, not one delimited string |
| A custom type with a `ParameterType` | `ObjectArgument`, `NameableObjectArgument`, or a subclass of `Argument`; see [custom types](#custom-parameter-types) |

See the [argument catalog](../arguments.md) for every built-in argument, including registry and vanilla types Lamp
does not have.

### `@Optional` and `@Default`

| Lamp | CommandLib |
|---|---|
| `@Default("1") int amount` | `new IntegerArgument("amount").optional(1)` |
| `@Default("me") Player target`, Kotlin `@Optional target: Player = actor.requirePlayer()` | `new PlayerArgument("target").optional(Command.playerRequirer())` |
| `@Optional Player target` (`null` when omitted) | **Not available** as `null`; give a default, or declare a second branch without the argument |

```java
class HealCommand extends Command {
    HealCommand() {
        super("heal");

        argument(new PlayerArgument("target").optional(Command.playerRequirer())).execute((target, ctx) -> {
            target.setHealth(20);
        });
    }
}
```

A console sender without a target gets the requirer's failure message. Only the last arguments of a branch can be
optional.

### Custom parameter types

```java
// Lamp
public final class QuestParameterType implements ParameterType<BukkitCommandActor, Quest> {
    @Override
    public Quest parse(MutableStringStream input, ExecutionContext<BukkitCommandActor> context) {
        String name = input.readString();
        Quest quest = questManager.quest(name);
        if (quest == null) {
            throw new CommandErrorException("No such quest: " + name);
        }
        return quest;
    }
}

// BukkitLamp.builder(this).parameterTypes(builder -> builder.addParameterType(Quest.class, new QuestParameterType(questManager)))
```

```java
// CommandLib
argument(new ObjectArgument<Quest>("quest", questManager::byName)).execute((quest, ctx) -> ctx.sendMessage(quest.name()));
```

`questManager.byName()` stands for your own code returning a `Map<String, Quest>`; `ObjectArgument` reads it for
every parse and suggests its keys. There is no registry of types: build the argument where it is used, or write a
factory method that returns a configured argument. To refine a built-in type, use `validator(...)`,
`transformer(...)`, or `additionalParser(...)`.

## Actors and senders

| Lamp | CommandLib |
|---|---|
| `BukkitCommandActor actor` parameter | The `ctx` parameter: `ctx.getSender()`, `ctx.sendMessage(...)` |
| `actor.reply("...")`, `actor.error("...")` | `ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), `sendFailure` (red) |
| `Player sender` as the first parameter (player only) | `requirePlayer()` on the command, then `ctx.getPlayer()`; or `require(Command.playerRequirer())` for a typed player |
| `ConsoleCommandSender sender` | `requireConsole()`, then `ctx.getConsole()` |
| `actor.requirePlayer()` | `Command.playerRequirer()` |
| A custom actor type or `ContextParameter` | `require(ctx -> ...)` maps the sender to your type per branch |

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

## Subcommands

| Lamp | CommandLib |
|---|---|
| `@Command("quest")` on the class, `@Subcommand("create")` on a method | `super("quest")` and `addChildren(new Command("create") {{ ... }})` |
| `@Command("quest create")` | The same child command |
| `@Subcommand("<quest> delete")` | `argument(...).child(quest -> new Command("delete") {{ ... }})` |
| `@Command({"gamemode creative", "gmc"})` | `addAliases(...)` aliases on the same level; a root alias of a subcommand is **not available**, register a second root command that calls the same code |
| Orphan commands (path chosen at runtime) | Pass the name to your `Command` subclass's constructor |
| `@SecretCommand` | **Not available**; a command the sender has no permission for is already hidden |

```java
class QuestCommand extends Command {
    QuestCommand() {
        super("quest");

        addChildren(new Command("create") {{
            argument(new StringArgument("name", StringArgument.Type.WORD)).execute((name, ctx) -> ctx.sendMessage("created " + name));
        }});
        argument(new StringArgument("quest", StringArgument.Type.WORD)).child(quest -> new Command("delete") {{
            execute(ctx -> ctx.sendMessage("deleted " + quest.get()));
        }});
    }
}
```

`quest` in `child(...)` is an `ArgRef<String>`; read it with `quest.get()` inside the child's callbacks.

## Permissions

| Lamp | CommandLib |
|---|---|
| `@CommandPermission("quests.command")` | `permission("quests.command")` |
| No annotation (anyone) | `permission(DefaultPermission.ALL)`; root commands default to OP |
| `@CommandPermission` on the class | `permission(...)` on the root command; children get generated nodes and inherit the default |
| Custom permission annotations (`@RequiresGroup`) | **Not available**; use a prerequisite |

CommandLib registers a Bukkit permission for every command, subcommand, and argument branch, and running a route needs
the node of every step on it. See [permissions](../permissions.md).

## Flags and switches

Lamp flags become CommandLib options. They are typed keys, and they are written right after the command name, before
the positional arguments, not anywhere in the input.

```java
// Lamp: /scan <target> [--force] [--limit <n>]
@Command("scan")
public void scan(BukkitCommandActor actor, Player target, @Switch("force") boolean force,
                 @Flag(value = "limit", shorthand = 'n') @Default("10") int limit) {
}
```

```java
// CommandLib: /scan [options] <target>
class ScanCommand extends Command {
    ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
        CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            boolean isForce = ctx.getOption(force);
            int maxCount = ctx.getOption(limit);
        });
    }
}
```

| Lamp | CommandLib |
|---|---|
| `@Switch boolean` | `Options.flag(name[, short])` |
| `@Flag` with a primitive or `String` | `Options.bool`, `integer`, `longValue`, `floatValue`, `doubleValue`, `string` |
| `@Flag` with another type (`Player`, an enum) | **Not available** |
| `@Flag` without `@Default` / `@Optional` (required) | `.required()` |
| — | `Options.count`, `repeatable(max)`, `requires(...)`, `mutuallyExclusive(...)` |

See [options](../options.md).

## Suggestions

| Lamp | CommandLib |
|---|---|
| `@Suggest({"100", "200"})` | `addSuggestionProvider(sb -> sb.suggest("100").suggest("200"))` on the argument |
| `@SuggestWith(DefaultWorlds.class)` | `setSuggestionProvider(sb -> ...)` on the argument |
| `suggestionProviders(p -> p.addProvider(World.class, ...))` (every parameter of a type) | **Not available** globally; set the provider on each argument, or in a factory method |
| `addProviderForAnnotation(...)`, `addProviderFactory(...)` | **Not available** |
| `context` in the provider | `sb.getContext()`, `sb.getArgument("earlier")`, `sb.getLatestInput()` |

```java
// CommandLib
argument(new WorldArgument("world").setSuggestionProvider(sb -> Bukkit.getWorlds()
                                                                      .stream()
                                                                      .map(World::getName)
                                                                      .filter(name -> name.startsWith("world"))
                                                                      .forEach(sb::suggest))).execute((world, ctx) -> {
    ctx.getPlayer()
       .teleport(world.getSpawnLocation());
});
```

`setSuggestionProvider` replaces the default suggestions; `addSuggestionProvider` keeps them. Slow lookups use
`sb.await(...)`. See [suggestions](../suggestions.md).

## Validators and conditions

| Lamp | CommandLib |
|---|---|
| Parameter validators | `validator(...)` on the argument; throw `ArgumentValidationException("...")` for your own message |
| `CommandCondition` + a custom annotation | A `Prerequisite<CommandContext>` added with `addPrerequisite(...)` |
| `throw new CommandErrorException("...")` | `throw new CommandPrerequisiteException("...")` from `execute(ctx -> ...)` or a prerequisite |
| `@Cooldown(value = 3, unit = TimeUnit.SECONDS)` | **Not available** |
| Hooks | **Not available** globally; `addPreprocess(ctx -> ...)` on a command also covers its children |

```java
// CommandLib: a reusable condition
final class Conditions {
    static final Prerequisite<CommandContext> OPPED = ctx -> {
        if (!ctx.getSender()
                .isOp()) {
            throw new CommandPrerequisiteException("You must be an operator to execute this command.");
        }
    };
}

class AdminCommand extends Command {
    AdminCommand() {
        super("admin");

        addPrerequisite(Conditions.OPPED);
        execute(ctx -> ctx.sendSuccess("ok"));
    }
}
```

A prerequisite on a command also applies to its children unless a child calls `disableParentPrerequisite()`.

## Dependency injection

**Not available** as a container. `@Dependency` fields and `builder.dependency(...)` become constructor parameters:

```java
class ArenaCommand extends Command {
    ArenaCommand(ArenaManager arenas) {
        super("arena");

        addPrerequisite(ctx -> {
            if (!arenas.isReady()) {
                throw new CommandPrerequisiteException("The arena is not ready.");
            }
        });
        execute(ctx -> ctx.sendMessage("ready"));
    }
}

// onEnable: CommandLib.register(this, new ArenaCommand(arenaManager));
```

## Responses, errors, and help

| Lamp | CommandLib |
|---|---|
| Response handlers (a returned value is sent) | **Not available**; send it with `ctx.sendMessage(...)`. `ctx.setResult(int)` sets the value `/execute store result` stores |
| `ExceptionHandler`, `@HandleException` | **Not available** globally. `addUncaughtExceptionHandler((e, ctx) -> ...)` on a command or argument handles unexpected exceptions |
| Lamp's built-in error messages | CommandLib's are fixed English text; see [errors](../errors.md) |
| `Help.ChildrenCommands` / `Help.RelatedCommands` parameter | Built in: `/<command> help`, and the usage is sent when a node has no executor |
| `@Description("...")` | `description("...")` on commands and branches |
| `@Usage("...")` | Generated from the tree |

## Kotlin

Lamp reads Kotlin default arguments as optional parameters. CommandLib has no Kotlin DSL; its Java API can be used
from Kotlin, with `init` blocks in place of Java's double brace initialization:

```kotlin
class GreetCommand : Command("greet") {
    init {
        argument(PlayerArgument("target").optional(Command.playerRequirer())).execute { target, ctx ->
            target.sendMessage("Welcome, ${target.name}!")
        }
    }
}
```

Coroutine (`suspend`) handlers are **not available**.

## Testing

`spigot-testing` / `paper-testing` run commands without a server:
`new CommandTester(GiveCommand::new, "myplugin.command")`, then `tester.execute("give Steve 3", sender)`. See
[testing](../testing.md).

## Not available yet

Tracked in the roadmap: an annotation API (with custom annotations), a Kotlin DSL with default arguments, dependency
injection, cooldowns and confirmation, middleware and hooks, response handlers, global parameter types and suggestion
providers, customizable and translatable built-in messages, and options with non-primitive values.

See also: [Migrating from CommandAPI](from-commandapi.md), [Cloud](from-cloud.md), [ACF](from-acf.md).
