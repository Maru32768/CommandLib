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
help and execution results (`AnnotationStyleTest`). `velocity-testing` only checks that an annotated command runs and
parses its arguments (`AnnotatedCommandsTest`).

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
| `@Subcommand("<team> add")` | a method | `argument(team).child(team -> new Command("add") ...)`; see [argument-first paths](#argument-first-paths) |
| `@Description("...")` | class, method, option parameter | `description(...)` of the command, the subcommand, the branch of an `@Execute` method, or the option |
| `@Description(value = "Manage homes", caption = "myplugin.home.description")` | class, method | `description(ctx -> ctx.caption(Caption.of(caption, value)))`, the description in the sender's locale ([localization](localization.md)) |
| `@Permission(value, defaultPermission, description)` | class, method, option parameter | `permission(...)` of the command, the subcommand, the branch of an `@Execute` method, or the option; attributes left out keep the generated node and the inherited default |
| `@VisibleWhen("running")` | class, method | `visibleWhen(...)` of the command, the subcommand, or the branch of an `@Execute` method, with the predicate registered under the name with `registerVisibility` ([permissions](permissions.md#refreshing-the-command-tree)) |
| `@Confirm(seconds = 10)` | class, `@Subcommand` method | `confirm(Confirmation.within(...))` |
| `@Cooldown(value, unit)` | class, `@Subcommand` method | `cooldown(Duration)` |
| `@Condition({"daytime", ...})` | class, `@Subcommand` method | `addPrerequisite(...)` with each prerequisite registered under the name with `registerCondition` |
| `@Middleware({"audit", ...})` | class, `@Subcommand` method | `addMiddleware(...)` with each middleware registered under the name with `registerMiddleware`, in this order ([execution](execution.md#middleware)) |
| `@OptionsAnywhere` | class, `@Subcommand` method | `optionsAnywhere()`: the options may follow the arguments ([options](options.md#options-after-the-arguments)) |
| `@MutuallyExclusive({"json", "yaml"})` | class, `@Subcommand` method, repeatable | `mutuallyExclusive(json, yaml)` of the command's options with these names |
| `@Examples({"/arena create lobby 16"})` | class, `@Subcommand` method | `examples(...)` ([help](help.md)) |
| `@CaptionBundle("lang/homes")` | class, `@Subcommand` method | `captions(Captions.create().bundle("lang/homes", classLoader))` with the class loader of the parsed class |

- Every method with arguments adds an argument branch to its command, so several methods can share a path:
  `@Subcommand("give") void give(Player target)` and `@Subcommand("give") void give(Player target, int count)`.
  At most one of them may run the command without arguments, and only one may give the annotations that configure
  the subcommand (everything in the table but `@Execute` and `@Subcommand`) or aliases.
- The class describes the command itself. On an `@Execute` method, `@Description`, `@Permission` and `@VisibleWhen`
  describe its argument branch, so a method without arguments cannot have them; the other command annotations go on
  the class. The class inherits each command annotation of its superclasses that it does not give itself (they are
  `@Inherited`); annotations on interfaces do not apply.
- Methods are added in the order of their path and then their name, because reflection does not give the order of the
  source. Help lists subcommands and branches in that order.
- Methods may be private and static. Methods of superclasses and interfaces, default or abstract, are read too; for a
  method a subclass overrides, the annotations of its most derived declaration that has any apply, and the call runs
  the override. An override that narrows the parameter types of a generic method, such as `run(String)` for
  `run(P)`, must repeat the annotations.

### Argument-first paths

A placeholder such as `<team>` in a `@Subcommand` path is an argument that the rest of the path follows, as
`argument(...).child(...)` does ([argument children](design/argument-child-command.md)):

```java
@Command("team")
public final class TeamCommand {
    @Execute
    public void list(CommandContext ctx) {
    }

    @Subcommand("<team>")
    @Description("Show a team")
    public void show(CommandContext ctx, Team team) {
    }

    @Subcommand("<team> add")
    public void add(CommandContext ctx, Team team, Player player) {
    }

    @Subcommand("<team> remove")
    public void remove(CommandContext ctx, Team team, Player player) {
    }
}
```

```java
// The same command, fluently.
execute(ctx -> {});
argument(new TeamArgument("team")).description("Show a team")
                                  .execute((team, ctx) -> {})
                                  .child(team -> new Command("add") {{
                                      argument(new PlayerArgument("player")).execute((player, ctx) -> {});
                                  }})
                                  .child(team -> new Command("remove") {{
                                      argument(new PlayerArgument("player")).execute((player, ctx) -> {});
                                  }});
```

- The method takes the placeholder's value as its argument parameter of the same name (`Team team` for `<team>`).
  The argument is made by the factory of the parameter's type, once for the path, so every method below `<team>`
  must declare the parameter alike (the same type and annotations). The children read the parsed value, which is
  parsed once.
- The arguments of a path share one namespace, so each placeholder appears once in the path, and the method's other
  argument parameters take names other than the placeholders'; parsing fails otherwise.
- Adjacent placeholders form one argument branch: `@Subcommand("<x> <y> go")` is `argument(x, y).child(...)`.
- A path that ends in a placeholder, such as `"<team>"` above, executes the branch. The method takes no other
  arguments; `@Description`, `@Permission` and `@VisibleWhen` on it describe the branch, and the other command
  annotations go on a method of the command, or on the class.
- `/team red` without a method for `"<team>"` shows the help, as a fluent branch without an executor does.

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
| `Duration` | `DurationArgument` |
| `UUID` | `UUIDArgument` (Spigot, Paper, Forge, NeoForge, Fabric; Velocity has none) |
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
| `@Range(min, max)` | Bounds of a numeric argument; whole numbers for an integer type, and `min` not above `max`. The default, `@Default` or the `0` of `@Optional` or of an `@Option` without `@Default`, must be within them |
| `@Suggest({"a", "b"})`, `@Suggest(method = "names")` | Replaces the suggestions with fixed values and / or the values of a method of the class, which returns an `Iterable<String>` or takes the `SuggestionBuilder` |
| `@Greedy` | A string argument that takes the rest of the input |
| `@Variadic(max = 5)` | A `List` argument that accepts up to 5 values, `variadic(5)` |

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
| `@Option("world") World world` | `Options.of("world", new WorldArgument("world"))`, required |
| `@Option(value = "target", shortName = 't') @Optional Player target` | `Options.of("target", 't', new PlayerArgument("target").optional(playerRequirer()))` |
| `@Option("mode") @Default("creative") GameMode mode` | `Options.of("mode", new GameModeArgument("mode").optional(GameMode.CREATIVE))` |
| `@Flag("silent") @Permission(defaultPermission = DefaultPermission.OP) boolean silent` | `Options.flag("silent").permission(DefaultPermission.OP)` |
| `@Option("reason") @Permission("myplugin.ban.reason") String reason` | `Options.string("reason", "").permission("myplugin.ban.reason")` |
| `@Option("reason") @Requires("force") String reason` | `Options.string("reason", "").requires(force)` |
| `@Option("threads") @Requires(value = "mode", equalTo = "parallel") int threads` | `Options.integer("threads", 0).requires(mode, "parallel")` |

A `boolean`, `int`, `long`, `float`, `double` or `String` parameter is a value option of that type; without
`@Default` its default is `false`, `0` or `""`. A parameter of any other type takes the argument registered for the
type as the option's value, with `@Suggest` and the registered annotations, as `Options.of` does
([options](options.md#kinds)): it is required unless `@Default` or `@Optional` makes the argument optional. Both take
`aliases`, and `@Description` on the parameter describes the option. `required = true` cannot be combined with
`@Default` or `@Optional`.

- `@Permission` on an option gives it its own node: `value` as the exact node, or the generated
  `<command node>.option.<name>` with `defaultPermission`; at least one of them, and no description.
- `@Requires` names another option of the command by its long name: `requires(other)` needs it to be given, and
  `equalTo` (converted like `@Default`) `requires(other, value)`. Repeat it for several requirements.
- `@MutuallyExclusive` on the class or a `@Subcommand` method names the options of its command.
- Options belong to the command of the method; methods of one command that declare the same option must declare it
  identically.

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
        // A named middleware for @Middleware("audit").
        .registerMiddleware("audit", new AuditMiddleware(getLogger()))
        // A named predicate for @VisibleWhen("open").
        .registerVisibility("open", ctx -> shop.isOpen())
        .parse(new ShopCommand());
```

- `registerArgument(type, factory)` receives an `AnnotatedParameter` with the argument name, the value type and the
  parameter's annotations, so a factory can read annotations of its own. `Enum.class` stands for every enum without its
  own factory.
- `registerAnnotation(annotation, valueType, handler)` applies after the factory and `@Suggest`, in registration order,
  and before the argument is made variadic or optional, so on a `List` parameter it configures each value. It returns
  the argument to use, which keeps the parameter's name. Parsing fails for the annotation on a parameter that is not
  an argument: the context, a `@Sender` or context value, a `@Flag`, or an `@Option` of a built-in value type.
- `registerCondition(name, prerequisite)` names a `Prerequisite` for `@Condition`. Parsing fails for a name nobody
  registered. Fluent commands add the same constant with `addPrerequisite`, see
  [errors](errors.md#reusable-conditions).
- `registerMiddleware(name, middleware)` and `registerVisibility(name, predicate)` name a `CommandMiddleware` for
  `@Middleware` and a predicate for `@VisibleWhen` the same way; a name nobody registered fails parsing too.
- `registerContextValue` and `registerDependency` are described in [context values](context-values.md#annotated-commands):
  the parsed command provides the registered values, so fluent code added to it reads them with `ctx.get(type)`.
- The parsed command is an ordinary `Command`: add what the annotations do not cover, such as
  `requires(option, predicate, description)`, captions built in code, or `disableParentMiddleware()`, with the fluent
  API before registering it.

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
