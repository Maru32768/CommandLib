# Migrating from Cloud

This guide maps [Incendo Cloud](https://cloud.incendo.org/) 2.x concepts to CommandLib for Spigot and Paper plugins.
The examples use `cloud-paper` / `cloud-bukkit` names on the left and CommandLib's `spigot` / `paper` artifacts on the
right. Both end up as Brigadier commands on modern servers, so command syntax and vanilla argument behavior carry over.

The main difference in style: Cloud builds commands with a builder on a `CommandManager` and reads values back by key
(`ctx.get("name")`). CommandLib declares a `Command` subclass and passes typed values to the executor.

Features CommandLib does not have yet are marked **Not available**.

## Setup

| Cloud | CommandLib |
|---|---|
| `LegacyPaperCommandManager.createNative(plugin, coordinator)`, `PaperCommandManager.builder()...buildOnEnable(plugin)` | No manager: shade the `spigot` or `paper` artifact and relocate `dev.marulabs.commandlib` |
| `manager.registerBrigadier()` | Not needed; commands are always Brigadier nodes |
| `manager.command(builder)` | `CommandLib.register(plugin, commands...)` in `onEnable` |
| `PaperCommandManager.builder()...buildBootstrapped(context)` | `CommandLib.bootstrap(context, commands...)` in `PluginBootstrap#bootstrap` (Paper) |
| `manager.deleteRootCommand("name")` | `CommandLib#unregister()` removes everything that registration added; plugin disable does it automatically |

```java
// Cloud
@Override
public void onEnable() {
    LegacyPaperCommandManager<CommandSender> manager = LegacyPaperCommandManager.createNative(this,
            ExecutionCoordinator.simpleCoordinator());
    manager.registerBrigadier();
    manager.command(manager.commandBuilder("ping").handler(ctx -> ctx.sender().sendMessage("pong")));
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

## Commands and arguments

```java
// Cloud
manager.command(manager.commandBuilder("give")
        .required("target", PlayerParser.playerParser())
        .optional("amount", IntegerParser.integerParser(1, 64), DefaultValue.constant(1))
        .handler(ctx -> {
            Player target = ctx.get("target");
            int amount = ctx.get("amount");
            ctx.sender().sendMessage("Gave " + amount + " to " + target.getName());
        }));
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

| Cloud | CommandLib |
|---|---|
| `required(name, parser)` | An argument instance in `argument(...)` |
| `optional(name, parser, DefaultValue.constant(v))` | `.optional(v)` on the argument |
| `optional(name, parser, DefaultValue.dynamic(ctx -> ...))` | `.optional(ctx -> ...)` (an `Extractor`), such as `optional(Command.playerRequirer())` |
| `optional(name, parser)` + `ctx.optional(name)` | **Not available** as an empty value; give a default, or declare a second branch without the argument |
| `ctx.get(name)`, `ctx.getOrDefault(name, v)` | Typed executor parameters; `ctx.getArgument(name, Type.class)` in the builder style |
| `literal("start", "s")` | A child command: `addChildren(new Command("start") {{ addAliases("s"); ... }})` |
| `commandBuilder("game", "g")` (aliases) | `addAliases("g")` |
| `Command.Builder#meta(key, value)` | **Not available** |

Only the last arguments of a branch can be optional, as in Cloud. See [optional arguments](from-commandapi.md#optional-arguments).

### Parsers

| Cloud parser | CommandLib argument |
|---|---|
| `integerParser(min, max)`, `longParser`, `floatParser`, `doubleParser`, `booleanParser` | `IntegerArgument(name, min, max)`, `LongArgument`, `FloatArgument`, `DoubleArgument`, `BooleanArgument` |
| `stringParser()` (one word) | `new StringArgument(name, StringArgument.Type.WORD)` |
| `quotedStringParser()` | `new StringArgument(name)` (the default, `Type.PHRASE_QUOTED`) |
| `greedyStringParser()` | `new StringArgument(name, StringArgument.Type.PHRASE)` |
| `stringArrayParser()` | `new StringArgument(name, StringArgument.Type.WORD).variadic()` (`List<String>`) |
| `enumParser(MyEnum.class)` | `new EnumArgument<>(name, MyEnum.class)` |
| `uuidParser()`, `durationParser()` | `UUIDArgument`, `DurationArgument` (`java.time.Duration`) |
| `PlayerParser`, `SinglePlayerSelectorParser` | `PlayerArgument` (accepts names and selectors) |
| `MultiplePlayerSelectorParser`, `SingleEntitySelectorParser`, `MultipleEntitySelectorParser` | `PlayersArgument` (`List<Player>`), `EntityArgument`, `EntitiesArgument` (`List<Entity>`) |
| `OfflinePlayerParser`, `WorldParser`, `LocationParser`, `NamespacedKeyParser`, `EnchantmentParser`, `ItemStackParser` | `OfflinePlayerArgument`, `WorldArgument`, `LocationArgument`, `NamespacedKeyArgument`, `EnchantmentArgument`, `ItemStackArgument` |
| `MaterialParser` | **Not available**; `ItemStackArgument` for items, `BlockDataArgument` for blocks |
| `Location2DParser`, `BlockPredicateParser`, `ItemStackPredicateParser`, `eitherParser`, aggregate parsers | **Not available**. See [missing arguments](../arguments.md#missing-arguments-by-priority) |

See the [argument catalog](../arguments.md) for the full list and result types.

### Custom parsers

Cloud parsers implement `ArgumentParser<C, T>` and are registered by type in the `ParserRegistry`. CommandLib has no
registry: an argument is an object you put in the branch. For a lookup by name, use `ObjectArgument`,
`NameableObjectArgument`, or `EnumArgument`; to refine a built-in type, use `validator(...)`, `transformer(...)`, or
`additionalParser(...)`; otherwise subclass `Argument`.

```java
// Cloud
public final class ArenaParser implements ArgumentParser<CommandSender, Arena> {
    @Override
    public ArgumentParseResult<Arena> parse(CommandContext<CommandSender> ctx, CommandInput input) {
        String name = input.readString();
        Arena arena = arenas.byName().get(name);
        return arena == null ? ArgumentParseResult.failure(new IllegalArgumentException("No arena " + name)) : ArgumentParseResult.success(arena);
    }
}
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

`ObjectArgument` reads the map from the supplier for every parse and suggests its keys. `ArenaManager` and `Arena`
stand for your own code.

## Senders

| Cloud | CommandLib |
|---|---|
| `ctx.sender()` | `ctx.getSender()` (`CommandSender`) |
| `senderType(Player.class)` | `requirePlayer()` on the command, then `ctx.getPlayer()`; or `require(Command.playerRequirer())` for a typed player |
| `senderType(ConsoleCommandSender.class)` | `requireConsole()`, then `ctx.getConsole()` |
| `SenderMapper` / custom sender type for the whole manager | **Not available** globally. `require(ctx -> ...)` maps the sender per branch |
| `PaperSimpleSenderMapper`, `CommandSourceStack` | The `paper` artifact reads the `CommandSourceStack` itself; use `ctx.getSender()`, or `ctx.getHandle().getSource()` for the stack |

```java
// CommandLib: map the sender to your own type
class CoinsCommand extends Command {
    CoinsCommand(Profiles profiles) {
        super("coins");

        argument(new IntegerArgument("amount", 1, 1000)).require(ctx -> profiles.of(ctx.getSender()))
                                                         .execute((amount, profile, ctx) -> profile.addCoins(amount));
    }
}
```

`Profiles.of` stands for your own code; it may throw `CommandPrerequisiteException("...")` to refuse the sender.
Forge / NeoForge have no `requirePlayer()` or `playerRequirer()`.

## Subcommands

```java
// Cloud
Command.Builder<CommandSender> game = manager.commandBuilder("game");
manager.command(game.literal("start").handler(ctx -> { /* start */ }));
manager.command(game.literal("stop").handler(ctx -> { /* stop */ }));
```

```java
// CommandLib
class GameCommand extends Command {
    GameCommand() {
        super("game");

        addChildren(new Command("start") {{
            execute(ctx -> { /* start */ });
        }}, new Command("stop") {{
            execute(ctx -> { /* stop */ });
        }});
    }
}
```

A literal after an argument (`config <key> get`) is `argument(...).child(key -> new Command("get") {{ ... }})`, where
`key.get()` reads the parent argument; see the [CommandAPI guide](from-commandapi.md#subcommands).

## Permissions

| Cloud | CommandLib |
|---|---|
| `permission("myplugin.game")` | `permission("myplugin.game")`, or the generated `<prefix>.game` |
| No permission | `permission(DefaultPermission.ALL)`; root commands default to OP |
| `Permission.anyOf(...)`, `Permission.allOf(...)` | **Not available** as one node; check the extra nodes in `addPrerequisite(...)` |
| `PredicatePermission.of(sender -> ...)` (hides the command) | **Not available** as a visibility predicate. `addPrerequisite(ctx -> ...)` checks at execution and sends a message |
| `CommandFlag.Builder#withPermission(...)` | **Not available** for options |

CommandLib registers a Bukkit permission for every command, subcommand, and argument branch with its default and
description, and leaves the nodes a sender lacks out of the sender's command tree. See
[permissions](../permissions.md).

## Suggestions

| Cloud | CommandLib |
|---|---|
| `required(name, parser, SuggestionProvider.suggestingStrings("a", "b"))` | `.setSuggestionProvider(sb -> sb.suggest("a").suggest("b"))` on the argument |
| `SuggestionProvider.blockingStrings((ctx, input) -> list)` | `.setSuggestionProvider(sb -> list.forEach(sb::suggest))` |
| A parser's own suggestions plus extra ones | `addSuggestionProvider(...)` keeps the defaults and appends |
| `SuggestionProvider` returning a `CompletableFuture` | `sb.await(future.thenAccept(list -> list.forEach(sb::suggest)))` |
| `Suggestion` with a tooltip (`TooltipSuggestion`) | `sb.suggest(text, tooltip)`; on Paper `sb.suggest(text, PaperMessages.toMessage(component))` |
| `input.peekString()` / `input.lastRemainingToken()` | `sb.getLatestInput()` |
| `ctx.get("earlier")` in the provider | `sb.getArgument("earlier")` |
| `@Suggestions("name")` method | A provider on the argument instance |

CommandLib does not filter what a provider adds; filter by `sb.getLatestInput()` yourself. See
[suggestions](../suggestions.md).

## Flags

Cloud flags become CommandLib options. They are typed keys instead of string lookups, and they are written **before**
the positional arguments, not after them.

```java
// Cloud: /scan <target> [--force] [--limit <n>]
manager.command(manager.commandBuilder("scan")
        .required("target", PlayerParser.playerParser())
        .flag(manager.flagBuilder("force").withAliases("f"))
        .flag(manager.flagBuilder("limit").withAliases("n").withComponent(IntegerParser.integerParser(1, 100)))
        .handler(ctx -> {
            boolean force = ctx.flags().isPresent("force");
            int limit = ctx.flags().getValue("limit", 10);
        }));
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

| Cloud | CommandLib |
|---|---|
| Presence flag | `Options.flag(name[, short])` |
| Value flag (`withComponent(parser)`) | `Options.bool`, `integer`, `longValue`, `floatValue`, `doubleValue`, `string`; **not available** for other types such as players or enums |
| `asRepeatable()` + `ctx.flags().count(...)` | `Options.count(name, short, max)` |
| `asRepeatable()` + `getAll(...)` | `valueOption.repeatable(max)` (`List<T>`) |
| — | `required()`, `requires(other)`, `mutuallyExclusive(...)` |

See [options](../options.md).

## Execution coordinators

| Cloud | CommandLib |
|---|---|
| `ExecutionCoordinator.simpleCoordinator()` | Parsing, middlewares and the executor run on the thread where the platform dispatches the command (the server thread for players) |
| `ExecutionCoordinator.asyncCoordinator()`, `coordinatorFor(executor)` | `executeAsync(...)` per command or branch: parsing stays on the server thread, the work runs on `ctx.supplyAsync(...)` and comes back to it. See [execution](../execution.md#async-executors) |
| `futureHandler(...)` | The stage `executeAsync` returns: its exceptions go to the `UncaughtExceptionHandler`s; a middleware can map them |
| Suggestions executor | Providers run on the platform's thread; use `sb.await(...)` for slow lookups |

```java
// CommandLib: load off the main thread, reply on it
class StatsCommand extends Command {
    StatsCommand(Stats stats) {
        super("stats");

        argument(new StringArgument("name", StringArgument.Type.WORD)).executeAsync((name, ctx) ->
                ctx.supplyAsync(() -> stats.load(name))
                   .thenAccept(ctx::sendMessage));
    }
}
```

The command has returned by the time the work completes, so `/execute store` sees `1` (or what the executor set before
returning the stage), not a value the work computes.

## Pre- and postprocessors

| Cloud | CommandLib |
|---|---|
| `registerCommandPreProcessor(...)` (every command) | `addMiddleware((ctx, next) -> ...)` or `addPreprocess(ctx -> ...)` on a top-level command also covers its children (opt out with `disableParentMiddleware()`); no registration for every command at once. See [execution](../execution.md#middleware) |
| `registerCommandPostProcessor(...)` | A middleware's code before `next.proceed()` runs right before the executor, and a stage chained on it runs after; `addPrerequisite(...)` (inherited, `disableParentPrerequisite()`) runs before the help too |
| `cloud-processors-confirmation` | `confirm()`, or `confirm(Confirmation.within(timeout))`: the sender runs the same input again (or clicks `[Confirm]`) |
| `cloud-processors-cooldown` | `cooldown(Duration)`, or `cooldown(Cooldown.of(duration).bypassPermission(...))`; kept in memory per unique id |
| `ParameterInjectorRegistry`, `ctx.inject(...)`, Guice injection service | **Not available**; pass dependencies to the command's constructor |

See [confirmation and cooldown](../../README.md#confirmation-and-cooldown) in the README for the messages and the order of the checks.

## Errors and captions

| Cloud | CommandLib |
|---|---|
| `exceptionController().registerHandler(...)` for `NoPermissionException`, `InvalidSyntaxException`, `InvalidCommandSenderException`, ... | **Not available**. CommandLib sends fixed English messages; Brigadier syntax errors are the platform's vanilla messages. See [errors](../errors.md) |
| An exception thrown by the handler | `addUncaughtExceptionHandler((e, ctx) -> ...)` on the command or argument |
| `ArgumentParseResult.failure(...)` from a parser | `throw new ArgumentValidationException("...")` from `validator(...)` |
| Failing from the handler | `throw new CommandPrerequisiteException("...")` from `execute(ctx -> ...)`, or `ctx.sendFailure("..."); return;` |
| `captionRegistry().registerProvider(...)`, `cloud-translations` bundles, `LocaleExtractor` | **Not available** for CommandLib's own messages. `ctx.getLocale()` gives the sender's locale for your messages, and `description(ctx -> ...)` makes help text per sender |
| `MinecraftExceptionHandler` (minecraft-extras) | Not needed: failures are sent in red like vanilla errors |

```java
argument(new IntegerArgument("amount").validator((amount, ctx) -> {
    if (amount % 2 != 0) {
        throw new ArgumentValidationException("The amount must be even.");
    }
})).execute((amount, ctx) -> ctx.sendSuccess("ok " + amount));
```

## Help

| Cloud | CommandLib |
|---|---|
| `MinecraftHelp` (minecraft-extras), `createHelpHandler()` | Built in: `/<command> help`, and the usage is sent when a node has no executor |
| `CommandDescription` / `Description.of(...)` on a command or component | `description(...)` on commands and branches |
| Bukkit `/help` entry | `serverHelp(shortText[, fullText])` |

## Annotations

**Not available.** CommandLib has no annotation parser; each `cloud-annotations` method becomes a `Command` subclass.

| `cloud-annotations` | CommandLib |
|---|---|
| `@Command("give <target> [amount]")` | `super("give")` and `argument(new PlayerArgument("target"), new IntegerArgument("amount").optional(1))` |
| `@Command("game start")` | `addChildren(new Command("start") {{ ... }})` inside `game` |
| `@Argument("target")` | The argument's name |
| `@Default("1")` | `.optional(1)` |
| `@Flag("force")` | `option(Options.flag("force"))` |
| `@Permission("...")` | `permission("...")` |
| `@CommandDescription("...")` | `description("...")` |
| `@Suggestions("name")`, `@Parser` | A suggestion provider or argument instance on the branch |
| `@Confirmation` | `confirm()` |
| `@ProxiedBy` (a root alias of a subcommand) | **Not available**; `addAliases(...)` adds aliases on the same level only |

## Kotlin

Cloud's `cloud-kotlin-extensions` (builder DSL) and `cloud-kotlin-coroutines` (suspending handlers) correspond to
CommandLib's [Kotlin DSL](../kotlin.md): `command("name") { ... }` blocks with typed argument references and options as
delegated properties, `executeSuspend { ctx -> }` for suspending handlers, which resume on the server thread through
the platform's scheduler, and `addSuggestionProviderSuspend { sb -> }` for suspending suggestions.

## Testing

CommandLib's `spigot-testing` / `paper-testing` run commands without a server:
`new CommandTester(GiveCommand::new, "myplugin.command")`, then `tester.execute("give Steve 3", sender)`. See
[testing](../testing.md).

## Not available yet

Tracked in the roadmap: an annotation API, a global sender mapper, a platform-wide middleware
registration, injection, customizable and translatable built-in
messages, permission predicates that hide commands, typed value options beyond primitives and strings, and the
arguments listed as missing in the [catalog](../arguments.md).

See also: [Migrating from CommandAPI](from-commandapi.md), [Lamp](from-lamp.md), [ACF](from-acf.md).
