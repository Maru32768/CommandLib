# Migrating from CommandAPI

This guide maps [CommandAPI](https://docs.commandapi.dev/) concepts to CommandLib for Spigot and Paper plugins. The
examples use CommandAPI 9.x/10.x names on the left and CommandLib's `spigot` / `paper` artifacts on the right. Both
libraries register Brigadier commands, so command syntax, selectors, and vanilla argument behavior carry over.

Features CommandLib does not have yet are marked **Not available**.

## Setup

| CommandAPI | CommandLib |
|---|---|
| Depend on the CommandAPI plugin, or shade it and call `CommandAPI.onLoad(...)`, `onEnable()`, `onDisable()` | Shade the `spigot` or `paper` artifact and relocate `dev.marulabs.commandlib`; no lifecycle calls |
| `command.register()` | `CommandLib.register(plugin, commands...)` in `onEnable` |
| `register("namespace")` | The plugin name is the namespace: `/<plugin>:<label>` |
| Paper bootstrap registration | `CommandLib.bootstrap(context, commands...)` in `PluginBootstrap#bootstrap` (Paper) |
| `CommandAPI.unregister("name")` | `CommandLib#unregister()` removes everything that registration added; plugin disable does it automatically |

```java
// CommandAPI
@Override
public void onEnable() {
    new CommandAPICommand("ping").executes((sender, args) -> {
        sender.sendMessage("pong");
    }).register();
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

CommandAPI lists arguments and reads them back by name or index from `CommandArguments`. CommandLib passes typed values
to the executor, so there is no lookup and no cast.

```java
// CommandAPI
new CommandAPICommand("message")
        .withArguments(new EntitySelectorArgument.OnePlayer("target"), new GreedyStringArgument("text"))
        .executes((sender, args) -> {
            Player target = (Player) args.get("target");
            String text = (String) args.get("text");
            target.sendMessage(text);
        })
        .register();
```

```java
// CommandLib
class MessageCommand extends Command {
    MessageCommand() {
        super("message");

        argument(new PlayerArgument("target"), new StringArgument("text", StringArgument.Type.PHRASE)).execute((target, text, ctx) -> {
            target.sendMessage(text);
        });
    }
}
```

Typed executors take up to seven arguments. Beyond that, `argument(List.of(a, b, ..., h)).execute(ctx -> ...)` reads each value
typed with `ctx.getArgument(a)` (see [Java builder](../java-builder.md#eight-or-more-arguments)).

### Argument names

| CommandAPI | CommandLib |
|---|---|
| `IntegerArgument`, `LongArgument`, `FloatArgument`, `DoubleArgument`, `BooleanArgument` (with min / max) | Same names, `new IntegerArgument(name, min, max)` |
| `StringArgument` (one word) | `new StringArgument(name, StringArgument.Type.WORD)` |
| `TextArgument` (quoted) | `new StringArgument(name)` (the default, `Type.PHRASE_QUOTED`) |
| `GreedyStringArgument` | `new StringArgument(name, StringArgument.Type.PHRASE)` |
| `MultiLiteralArgument` | `new LiteralArgument(name, List.of(...))` (a string argument with fixed values) |
| `LiteralArgument` | A child command: `addChildren(new Command("literal"))` |
| `EntitySelectorArgument.OnePlayer` / `ManyPlayers` | `PlayerArgument` / `PlayersArgument` (`List<Player>`) |
| `EntitySelectorArgument.OneEntity` / `ManyEntities` | `EntityArgument` / `EntitiesArgument` (`List<Entity>`) |
| `PlayerProfileArgument` | `OfflinePlayersArgument`, `UUIDsArgument` |
| `LocationArgument` (precise) | `LocationArgument`; `new LocationArgument(name, false)` keeps integer coordinates uncentered |
| `LocationArgument` (block position) | `BlockPositionArgument` (a `Location` with integer coordinates) |
| `BlockPredicateArgument` | Same name (`Predicate<Block>`; Paper 1.21.8 or later on Paper) |
| `ItemStackPredicateArgument` | `ItemPredicateArgument` (`Predicate<ItemStack>`) |
| `WorldArgument`, `ItemStackArgument`, `EnchantmentArgument`, `ParticleArgument`, `SoundArgument`, `BiomeArgument`, `LootTableArgument`, `RecipeArgument`, `AdvancementArgument`, `NamespacedKeyArgument`, `TeamArgument`, `ObjectiveArgument`, `UUIDArgument`, `EntityTypeArgument`, `ChatColorArgument` (`NamedTextColor` on Paper) | Same names; see the [argument catalog](../arguments.md) for result type differences |
| `BlockStateArgument` | `BlockDataArgument` (`BlockData`) |
| `PotionEffectArgument` | `PotionEffectArgument` (returns a `PotionEffect`; use `getType()`) |
| `ScoreboardSlotArgument` | `ScoreboardDisplaySlotArgument` |
| Registry-backed arguments without a CommandLib class | `RegistryArgument<>(name, registry)` over a Bukkit `Registry` (Spigot) or a Paper `RegistryKey` (Paper) |
| `CustomArgument` | `ObjectArgument`, `NameableObjectArgument`, `EnumArgument`, or a subclass of `Argument` |
| `ListArgument` | `variadic()` on the element argument (see [optional arguments](#optional-arguments)) |
| `IntegerRangeArgument` / `DoubleRangeArgument` | Same names; they return Guava `Range<Integer>` / `Range<Double>` instead of `IntegerRange` / `DoubleRange` |
| `TimeArgument` | Same name (`Integer` game ticks, `minimum` as the second constructor argument); `DurationArgument` reads real time (`java.time.Duration`, such as `1h30m`) instead |
| `ChatComponentArgument` / `AdventureChatComponentArgument` | `ComponentArgument`: one Bungee `BaseComponent` on Spigot instead of a `BaseComponent[]`, an Adventure `Component` on Paper |
| `ChatArgument` / `AdventureChatArgument` | `MessageArgument`: the same component types, selectors resolved, without the signature (see [the catalog](../arguments.md#text-nbt-and-other-advanced-arguments)) |
| `NBTCompoundArgument` | `NBTCompoundArgument`: an SNBT `String` on Spigot and Paper, so NBT-API is not needed; pass it to `NBT.parseNBT(...)` for an NBT-API object |
| `RotationArgument`, `AngleArgument`, `AxisArgument`, `Location2DArgument`, `FunctionArgument`, `CommandArgument`, `MapArgument`, `MathOperationArgument`, `ObjectiveCriteriaArgument`, `ScoreHolderArgument` | **Not available.** See [missing arguments](../arguments.md#missing-arguments-by-priority) |

### Optional arguments

```java
// CommandAPI
new CommandAPICommand("heal")
        .withOptionalArguments(new EntitySelectorArgument.OnePlayer("target"))
        .executesPlayer((player, args) -> {
            Player target = (Player) args.getOrDefault("target", player);
            target.setHealth(20);
        })
        .register();
```

```java
// CommandLib
class HealCommand extends Command {
    HealCommand() {
        super("heal");

        argument(new PlayerArgument("target").optional(player())).execute((target, ctx) -> {
            target.setHealth(20);
        });
    }
}
```

`optional(defaultValue)` takes a fixed default, and `optional(extractor)` computes one; here a console sender without a
target gets the requirer's failure message. Only the last arguments of a branch can be optional. `ListArgument`
becomes `variadic()` on the element argument, such as `new PlayerArgument("targets").variadic()` for a
`List<Player>`: each value is its own token with the element's parsing and suggestions, not one delimited string.

## Executors

| CommandAPI | CommandLib |
|---|---|
| `executes((sender, args) -> ...)` | `execute(ctx -> ...)` or the typed `execute((a, b, ctx) -> ...)`; the sender is `ctx.getSender()` |
| `executesPlayer((player, args) -> ...)` | `addPrerequisite(Command.player())` on the command, then `ctx.getPlayer()`; or `require(Command.player())` for a typed player |
| `executesConsole(...)` | `addPrerequisite(Command.console())`, then `ctx.getConsole()` |
| `executesCommandBlock`, `executesProxy`, `executesNative`, `executesEntity` | **Not available** as separate executors; check `ctx.getSender()` (`BlockCommandSender`, ...) yourself |
| Resulting executors (`return n`), `/execute store` | `ctx.setResult(n)` in the executor; `1` unless set, `0` for a failure. See [errors](../errors.md#results-and-failures) |
| `throw CommandAPI.failWithString("...")` | `throw new CommandPrerequisiteException("...")` from `execute(ctx -> ...)`, `throw ctx.fail("...")` from any executor, or `ctx.sendFailure("..."); return;` |
| Message helpers | `ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), `sendFailure` (red), with `String`, a native component, or `RichText` |

```java
// CommandLib: a typed player value
class HomeCommand extends Command {
    HomeCommand() {
        super("home");

        require(Command.player()).execute((player, ctx) -> player.teleport(player.getWorld()
                                                                                         .getSpawnLocation()));
    }
}
```

`Command.player()` and `Command.console()` exist on every platform. On Forge, NeoForge and Fabric, `player()` gives a `ServerPlayer`.

## Subcommands

```java
// CommandAPI
new CommandAPICommand("game")
        .withSubcommand(new CommandAPICommand("start").executes((sender, args) -> { /* start */ }))
        .withSubcommand(new CommandAPICommand("stop").executes((sender, args) -> { /* stop */ }))
        .register();
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

CommandAPI's `CommandTree` with `.then(...)` after an argument maps to `argument(...).child(ref -> new Command(...))`,
where `ref.get()` reads the parent argument inside the child:

```java
class ConfigCommand extends Command {
    ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).child(key -> new Command("get") {{
            execute(ctx -> ctx.sendMessage("get " + key.get()));
        }});
    }
}
```

Aliases: `withAliases("g")` → `addAliases("g")`.

## Permissions and requirements

| CommandAPI | CommandLib |
|---|---|
| `withPermission("myplugin.game")` | `permission("myplugin.game")`, or the generated `<prefix>.game` |
| `withPermission(CommandPermission.OP)` | `permission(DefaultPermission.OP)` (the default for root commands) |
| `withPermission(CommandPermission.NONE)` | `permission(DefaultPermission.ALL)` |
| `withoutPermission(...)` | **Not available** |
| `argument.withPermission(...)` | `argument(...).permission(...)` on the branch; every branch also gets a generated node |
| `withRequirement(sender -> ...)` (hides the command) | `visibleWhen(ctx -> ...)` on the command or the branch ([permissions](../permissions.md#refreshing-the-command-tree)) |
| `CommandAPI.updateRequirements(player)` | `CommandLib.refresh(player)` |

CommandLib registers a Bukkit permission for each node with its default and description. See
[permissions](../permissions.md) for node names and the LuckPerms setup.

```java
// CommandLib: prerequisite with a message
class ArenaCommand extends Command {
    ArenaCommand(ArenaManager arenas) {
        super("arena");

        addPrerequisite(ctx -> {
            if (!arenas.isReady()) {
                throw new CommandPrerequisiteException("The arena is not ready.");
            }
        });
        execute(ctx -> arenas.join(ctx.getPlayer()));
    }
}
```

`ArenaManager` stands for your own code.

## Suggestions

| CommandAPI | CommandLib |
|---|---|
| `replaceSuggestions(ArgumentSuggestions.strings(...))` | `setSuggestionProvider(sb -> sb.suggest(...))` |
| `includeSuggestions(...)` | `addSuggestionProvider(...)` |
| `ArgumentSuggestions.stringsWithTooltips(StringTooltip.ofString(text, tip))` | `sb.suggest(text, tip)`; on Paper `sb.suggest(text, PaperMessages.toMessage(component))` |
| `ArgumentSuggestions.stringsAsync(info -> future)` | `sb.await(future.thenAccept(list -> list.forEach(sb::suggest)))` |
| `info.previousArgs()` | `sb.getArgument(name)` |
| `info.currentArg()` | `sb.getLatestInput()` |

```java
// CommandAPI
new StringArgument("warp").replaceSuggestions(ArgumentSuggestions.strings(info -> warps.names()
                                                                                        .toArray(new String[0])));
```

```java
// CommandLib
new StringArgument("warp", StringArgument.Type.WORD).setSuggestionProvider(sb -> warps.names()
                                                                                      .forEach(sb::suggest));
```

See [suggestions](../suggestions.md).

## Validation

CommandAPI validates inside a `CustomArgument` parser or the executor. CommandLib adds validators to any argument;
a rejected value gets vanilla's "Incorrect argument for command" message, and the validator also filters suggestions.

```java
argument(new IntegerArgument("amount", 1, 64).validator(amount -> amount % 2 == 0))
        .execute((amount, ctx) -> ctx.sendSuccess("ok " + amount));
```

`transformer(...)` maps a parsed value, and `additionalParser(...)` accepts input the argument type rejects. See
[errors](../errors.md).

## Options

**Not in CommandAPI.** CommandLib has typed flags and value options placed after the command name:

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

## Help

| CommandAPI | CommandLib |
|---|---|
| `withShortDescription`, `withFullDescription` | `serverHelp(shortText[, fullText])` for the server's `/help` |
| `withUsage(...)` | Usage is generated from the tree: `/<command> help`, and sent automatically when arguments are missing |
| — | `description(...)` on commands and branches appears in the generated usage |

## Testing

| CommandAPI | CommandLib |
|---|---|
| `commandapi-spigot-test-toolkit` / `commandapi-paper-test-toolkit` with MockBukkit | `spigot-testing` / `paper-testing`; MockBukkit is optional |
| `MockCommandAPIPlugin.load()` | Not needed: `new CommandTester(MyCommand::new, "myplugin.command")` |
| `assertCommandSucceeds(player, "cmd")` | `tester.execute("cmd", sender)` and assert on `sender.getSentMessageTexts()` |
| `assertCommandFails(...)` (syntax error) | `assertThatThrownBy(() -> tester.execute(...))`; CommandLib's own failures are sent messages |
| Suggestion assertions | `tester.suggestions("cmd pa", sender).join().getList()` |
| Selector arguments not implemented | `spigot-testing` mocks names, `@s`, `@p`, `@r`, `@a`, `@e`; Forge / NeoForge testing parses real selectors |

See [testing](../testing.md).

## Not available

Per-sender executor variants. The arguments listed as missing in the [catalog](../arguments.md) are tracked in the
roadmap. Requirement-based visibility is `visibleWhen`, see [permissions and requirements](#permissions-and-requirements).

See also: the [comparison of seven use cases](../comparison.md), [Migrating from Cloud](from-cloud.md), [Lamp](from-lamp.md), [ACF](from-acf.md).
