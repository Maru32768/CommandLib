# Annotated Commands

The annotation API writes a command as a class with annotated methods. It builds an ordinary `Command` through the
fluent API, so the help, the permission nodes, the options, the suggestions and the tests are the same as for the same
command written fluently. The design is in [design/annotation-api.md](design/annotation-api.md).

It is available for Spigot, Paper, Velocity, Forge, NeoForge and Fabric through `AnnotatedCommands`; for a context of
your own, create an `AnnotationParser` yourself (see [Other contexts](#other-contexts)).

## Example

```java
import dev.marulabs.commandlib.annotation.*;

@Command(value = "arena", aliases = "ar")
@Description("Manage arenas")
public final class ArenaCommand {
    private final List<String> arenas = new ArrayList<>();

    @Subcommand("create")
    @Description("Create an arena")
    @Permission(defaultPermission = DefaultPermission.ALL, description = "Create arenas")
    public void create(CommandContext ctx,
                       @Flag(value = "force", shortName = 'f') @Description("Replace an existing arena") boolean force,
                       String name,
                       @Range(min = 1, max = 64) @Default("16") int size) {
        arenas.add(name);
        ctx.sendSuccess("Created " + name + " with " + size + " slots");
    }

    @Subcommand("delete")
    @Confirm
    public void delete(CommandContext ctx, @Suggest(method = "arenaNames") String name) {
        arenas.remove(name);
    }

    @Subcommand("invite")
    public void invite(CommandContext ctx, String arena, List<Player> players) {
    }

    @Subcommand("join")
    public void join(CommandContext ctx, String arena, @Optional Player player) {
        ctx.sendSuccess(player.getName() + " joined " + arena);    // the sender when omitted
    }

    @Subcommand("list")
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    public void list(CommandContext ctx,
                     @Option(value = "limit", shortName = 'n') @Range(min = 1, max = 100) @Default("10") int limit) {
    }

    @Subcommand("stats")
    public CompletionStage<?> stats(CommandContext ctx, String arena) {
        return ctx.supplyAsync(() -> database.games(arena))
                  .thenAccept(games -> ctx.sendSuccess(arena + " has " + games + " games"));
    }

    Iterable<String> arenaNames() {
        return arenas;
    }
}
```

```java
// In onEnable, as for any command.
CommandLib.register(this, AnnotatedCommands.parse(new ArenaCommand()));
```

This is the same command as:

```java
public final class ArenaCommand extends Command {
    public ArenaCommand() {
        super("arena");
        addAliases("ar");
        description("Manage arenas");

        addChildren(new Command("create") {{
            description("Create an arena");
            permission(DefaultPermission.ALL, "Create arenas");
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                         .description("Replace an existing arena"));
            argument(new StringArgument("name"), new IntegerArgument("size", 1, 64).optional(16)).execute((name, size, ctx) -> {});
        }}, new Command("delete") {{
            confirm();
            argument(new StringArgument("name").setSuggestionProvider(sb -> arenas.forEach(sb::suggest))).execute((name, ctx) -> {});
        }}, new Command("invite") {{
            argument(new StringArgument("arena"), new PlayerArgument("players").variadic()).execute((arena, players, ctx) -> {});
        }}, new Command("join") {{
            argument(new StringArgument("arena"), new PlayerArgument("player").optional(playerRequirer())).execute((arena, player, ctx) -> {});
        }}, new Command("list") {{
            cooldown(Duration.ofMinutes(1));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            execute(ctx -> {});
        }}, new Command("stats") {{
            argument(new StringArgument("arena")).executeAsync((arena, ctx) -> ...);
        }});
    }
}
```

`spigot-testing`, `paper-testing` and `modded-testing` check that the two give the same tree, permissions, suggestions,
help and execution results (`AnnotationStyleTest`).

## Parameter names

Argument names come from the parameter names, which Java keeps only when the class is compiled with `-parameters`:

```kotlin
// build.gradle.kts
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
```

Without it, name each argument with `@Name("target")`, and give `@Flag` and `@Option` their name. Parsing fails with
`compile with -parameters or annotate it with @Name` for an argument without a name.

## Commands and methods

| Annotation | On | Fluent equivalent |
|---|---|---|
| `@Command(value, aliases)` | the class | `new Command(value)`, `addAliases(...)` |
| `@Execute` | a method | `execute(...)` without arguments, `argument(...).execute(...)` with them |
| `@Subcommand("start")`, `@Subcommand("arena create")` | a method | `addChildren(new Command("start") ...)`; the path creates missing commands |
| `@Description("...")` | class, method, option parameter | `description(...)` of the command, the subcommand, the branch of an `@Execute` method, or the option |
| `@Permission(value, defaultPermission, description)` | class, method | `permission(...)` of the command, the subcommand, or the branch of an `@Execute` method; attributes left out keep the generated node and the inherited default |
| `@Confirm(seconds = 10)` | class, `@Subcommand` method | `confirm(Confirmation.within(...))` |
| `@Cooldown(value, unit)` | class, `@Subcommand` method | `cooldown(Duration)` |
| `@Condition({"daytime", ...})` | class, `@Subcommand` method | `addPrerequisite(...)` with each prerequisite registered under the name with `registerCondition` |

- Every method with arguments adds an argument branch to its command, so several methods can share a path:
  `@Subcommand("give") void give(Player target)` and `@Subcommand("give") void give(Player target, int count)`.
  At most one of them may run the command without arguments, and only one may give `@Description`, `@Permission`,
  `@Confirm`, `@Cooldown`, `@Condition` or aliases for the subcommand.
- The class describes the command itself. On an `@Execute` method, `@Description` and `@Permission` describe its
  argument branch, so a method without arguments cannot have them.
- Methods are added in the order of their path and then their name, because reflection does not give the order of the
  source. Help lists subcommands and branches in that order.
- Methods may be private and static. Methods of superclasses are not read.

## Parameters

Each parameter is, in this order:

1. **The context**, when its type is `CommandContext` (or a supertype such as `CommonCommandContext`).
2. **A value from the context**, when annotated `@Sender`: `@Sender Player player` is the sender, and fails the command
   with "This command can only be executed by a player." for anyone else.
3. **An option**, when annotated `@Flag` or `@Option` ([options](options.md)).
4. **An argument**, created by the factory registered for its type. A `List<T>` parameter, which must be the last
   argument, is a variadic argument of `T` (`new XArgument(name).variadic()`).
5. **A value from the context** for its type when no argument has the type, such as `CommandSender`.

A value from the context is read with `ctx.get(type)`: the value the parsed command provides with the registered
extractor, run once per execution, or the value a middleware put. A sender mapped to a type of your own, a service and
a middleware's value are all parameters this way; see [context values](context-values.md#annotated-commands).

| Parameter type | Argument |
|---|---|
| `String` | `StringArgument` (`@Greedy` for `StringArgument.Type.PHRASE`) |
| `int`, `long`, `float`, `double` | `IntegerArgument`, `LongArgument`, `FloatArgument`, `DoubleArgument` (bounds from `@Range`) |
| `boolean` | `BooleanArgument` |
| an enum | `EnumArgument` |
| `Duration`, `UUID` | `DurationArgument`, `UUIDArgument` |
| `Player`, `OfflinePlayer`, `Entity`, `World`, `Location`, `GameMode`, `EntityType`, `Biome`, `Sound`, `Attribute`, `Enchantment`, `Particle`, `PotionEffect`, `ItemStack`, `BlockData`, `NamespacedKey`, `Advancement`, `LootTable`, `Recipe`, `Team`, `Objective`, `DisplaySlot`, `ChatColor` (Paper: `NamedTextColor`) | the argument of the same name (Spigot, Paper) |
| `Player`, `RegisteredServer` | `PlayerArgument`, `ServerArgument` (Velocity) |
| `ServerPlayer`, `GameProfile`, `Entity`, `ServerLevel`, `Location` (CommandLib's), `BlockPos`, `GameType`, `EntityType`, `Biome`, `SoundEvent`, `ParticleOptions`, `ItemStack`, `BlockInput`, `CompoundTag`, `ResourceLocation`, `LootTable`, `PlayerTeam`, `Objective`, `ChatFormatting` | `PlayerArgument`, `GameProfileArgument`, `EntityArgument`, `WorldArgument`, `LocationArgument`, `BlockPositionArgument`, `GameModeArgument`, `EntityTypeArgument`, `BiomeArgument`, `SoundArgument`, `ParticleArgument`, `ItemStackArgument`, `BlockStateArgument`, `NBTCompoundArgument`, `ResourceLocationArgument`, `LootTableArgument`, `TeamArgument`, `ObjectiveArgument`, `ChatColorArgument` (Forge, NeoForge, Fabric) |
| `Enchantment`, `MobEffect`, `Attribute` (`Holder<Enchantment>`, `Holder<MobEffect>`, `Holder<Attribute>` on 1.20.5+), `Advancement`, `Recipe` (`AdvancementHolder`, `RecipeHolder`, `DisplaySlot` on 1.20.2+) | `EnchantmentArgument`, `EffectArgument`, `AttributeArgument`, `AdvancementArgument`, `RecipeArgument`, `ScoreboardDisplaySlotArgument` (Forge, NeoForge, Fabric) |

Context values: `CommandSender`, `ConsoleCommandSender` and `Player` on Spigot and Paper; `CommandSource`,
`ConsoleCommandSource` and `Player` on Velocity; `CommandSourceStack` and `ServerPlayer` on Forge, NeoForge and Fabric.

### Argument annotations

| Annotation | Effect |
|---|---|
| `@Name("target")` | The argument name, used in help and in the branch's permission node |
| `@Default("16")` | Makes the argument optional with this value: a string, a number, `true` / `false`, or an enum constant name |
| `@Optional` | Makes the argument optional; an omitted value is the context value of the type (a `Player` defaults to the sender, as `optional(playerRequirer())`), or else `0`, `false`, `""`, or an empty list for a `List` |
| `@Range(min, max)` | Bounds of a numeric argument |
| `@Suggest({"a", "b"})`, `@Suggest(method = "names")` | Replaces the suggestions with fixed values and / or the values of a method of the class, which returns an `Iterable<String>` or takes the `SuggestionBuilder` |
| `@Greedy` | A string argument that takes the rest of the input |

Optional arguments must be the last arguments of a method, and a variadic one the very last; registration fails
otherwise, as for fluent commands ([optional and variadic arguments](design/optional-variadic-arguments.md)).

### Options

| Parameter | Option |
|---|---|
| `@Flag("force") boolean force` | `Options.flag("force")` |
| `@Flag(value = "verbose", shortName = 'v', max = 3) int verbose` | `Options.count("verbose", 'v', 3)` |
| `@Option("limit") @Default("10") @Range(min = 1, max = 100) int limit` | `Options.integer("limit", 10, 1, 100)` |
| `@Option("tag") String tag` | `Options.string("tag", "")` |
| `@Option(value = "tag", repeatable = 3) List<String> tags` | `Options.string("tag", "").repeatable(3)` |
| `@Option(value = "env", required = true) String env` | `Options.string("env", "").required()` |

`@Option` takes `boolean`, `int`, `long`, `float`, `double` and `String`; without `@Default` the default is `false`,
`0` or `""`. Both take `aliases`, and `@Description` on the parameter describes the option. Options belong to the
command of the method; methods of one command that declare the same option must declare it identically.

### Results

A method returns `void`, `int` (`ctx.setResult(...)`, for `/execute store result`) or a `CompletionStage`, which runs
the method as `executeAsync` ([execution](execution.md)). A `CommandPrerequisiteException` thrown by the method fails the
command with its message; other exceptions are reported as an executor's are.

## Customizing the parser

`AnnotatedCommands.parser()` returns a new parser to register further types before parsing:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface Online {
}

Command command = AnnotatedCommands.parser()
        // A type of your own, or a replacement for a built-in one.
        .registerArgument(Arena.class, p -> new ObjectArgument<>(p.name(), arenas.byName()))
        // A value the method gets from the context, such as the sender as your own type.
        .registerContextValue(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)))
        // A value a middleware puts, or a command the parsed one is added to provides.
        .registerContextValue(Account.class)
        // A service: a parameter of the type, and the fields annotated @Dependency.
        .registerDependency(Economy.class, economy)
        // A custom annotation that configures the argument of a parameter.
        .registerAnnotation(Online.class, Player.class, (online, argument) -> argument.validator(Player::isOnline))
        // A named prerequisite for @Condition("daytime").
        .registerCondition("daytime", Conditions.DAYTIME)
        .parse(new ShopCommand());
```

- `registerArgument(type, factory)` receives an `AnnotatedParameter` with the argument name, the value type and the
  parameter's annotations, so a factory can read annotations of its own. `Enum.class` stands for every enum without its
  own factory.
- `registerAnnotation(annotation, valueType, handler)` applies after the factory and `@Suggest`, in registration order,
  and before the argument is made variadic or optional, so on a `List` parameter it configures each value. It returns
  the argument to use, which keeps the parameter's name.
- `registerCondition(name, prerequisite)` names a `Prerequisite` for `@Condition`. Parsing fails for a name nobody
  registered. Fluent commands add the same constant with `addPrerequisite`, see
  [errors](errors.md#reusable-conditions).
- `registerContextValue` and `registerDependency` are described in [context values](context-values.md#annotated-commands):
  the parsed command provides the registered values, so fluent code added to it reads them with `ctx.get(type)`.
- The parsed command is an ordinary `Command`: add middlewares, prerequisites or children with the fluent API before
  registering it.

## Errors

Parsing throws `IllegalArgumentException` naming the method and the parameter, for example:

```text
ArenaCommand#create(CommandContext, boolean, String, int): parameter size has @Default("big"), which is not a valid Integer
ArenaCommand#show(Thread): parameter thread has no argument type for java.lang.Thread; register one with registerArgument
ArenaCommand#run() has @Description but neither @Execute nor @Subcommand
```

## Other contexts

For a context of your own, create the parser and register the types:

```java
AnnotationParser<CommandContext, Command> parser = new AnnotationParser<>(CommandContext.class, Command::new)
        .registerArgument(Integer.class, p -> new IntegerArgument(p.name()))
        .registerArgument(String.class, p -> new StringArgument(p.name()));
```
