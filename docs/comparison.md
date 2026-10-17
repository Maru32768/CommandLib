# Comparison with Other Command Libraries

This page compares CommandLib with [CommandAPI](https://docs.commandapi.dev/), [Cloud](https://cloud.incendo.org/),
[Lamp](https://github.com/Revxrsal/Lamp), [ACF](https://github.com/aikar/commands) and
[Paper's Brigadier command API](https://docs.papermc.io/paper/dev/command-api/basics/registration/) on seven fixed use
cases. For each one it shows the CommandLib definition, the closest equivalent in each library, and compares the
amount of definition, the string keys and casts the executor needs, the setup, how far it can be extended, and what
can be tested without a server. It also lists where the other libraries are better.

The CommandLib side is code that compiles and runs: the [`examples`](../examples) module of this repository writes each
use case with the fluent API, the [Java builder](java-builder.md), [annotations](annotations.md) and the
[Kotlin DSL](kotlin.md), and its tests run the four through `CommandTester`, check that they give the same command tree,
help, messages and results, and compare that transcript with a snapshot in
[`examples/src/test/resources/snapshots`](../examples/src/test/resources/snapshots). Run them with
`./gradlew -Pcommandlib.typedNms=false :examples:test`.

The other libraries' snippets are **not compiled**. They were written from the official documentation and, where the
documentation is silent, from the official source, checked in October 2026 against CommandAPI 12.1.0, Cloud 2.0.1
(`cloud-paper`) / 2.1.0 (core, annotations), Lamp 4.0.0-rc.19, ACF 0.5.1-SNAPSHOT and Paper 1.21.x. Each is marked:

- **Verified**: the API and the behavior are on the linked page or source file.
- **Partly verified**: the calls are documented, but this combination of them is inferred.
- **Unverified**: from memory or inference; treat it as a sketch.

Corrections and newer versions are welcome; this page is only as good as the evidence it links.

## Summary

| Use case | CommandLib | CommandAPI | Cloud | Lamp | ACF | Paper |
|---|---|---|---|---|---|---|
| 1. Admin options (`-s`, `--reason`) | Typed options, before the arguments or also after them (`optionsAnywhere()`) | No flags; optional literals, order-dependent | Flags, any order | `@Flag` / `@Switch`, any order | No flags; parse `String[]` yourself | No flags; a branch per combination |
| 2. Optional and variadic arguments | `optional(v)`, `variadic()` (space-separated values) | `withOptionalArguments`, `ListArgument` | `optional(..., DefaultValue)`; no generic variadic | `@Default`, greedy `List<T>` | `@Default`, comma-separated arrays | A branch per optional argument |
| 3. Argument-first tree | `argument(...).child(...)`; annotations: `@Subcommand("<team> add")` | `CommandTree` | Literal after an argument | `@Command("team <name> add")` | Not supported | Native |
| 4. Async I/O, back to the main thread | `executeAsync` + `ctx.supplyAsync`, `executeSuspend` | Manual scheduler | Coordinators; main thread is manual | `CompletionStage` return; main thread is manual | Manual scheduler | Manual scheduler |
| 5. Dependency injection | `provide(Type.class, ...)` per command tree, `ctx.get(Type.class)`; annotations: `registerDependency`, `@Dependency`, `registerContextValue` | Constructor | Parameter injectors, Guice, `SenderMapper` | `@Dependency`, context parameters, sender resolvers | `registerDependency`, issuer-aware contexts | Constructor |
| 6. Per-locale messages | Captions, bundles, the sender's locale, localized help headings | None | Captions, translations, locale extractor | None in v4 | Locales, message keys, per-issuer locale | None in the command API |
| 7. Result for `/execute store` | `ctx.setResult`, `int` return | `int` return | Always 1 | Always 1 | Always true / 1 | `int` return |
| Executor reads values | Typed parameters | `args.get("name")` + cast, or `getByArgument` | `ctx.get("name")` (builder), parameters (annotations) | Parameters | Parameters | `getArgument("name", Type.class)` |
| Test without a server | `CommandTester` per platform (tree, help, suggestions, results, locale, async) | Test toolkit on MockBukkit | None official | None official | None official | None official |

The sections below give the detail and the sources.

## How the CommandLib side was measured

| Use case | Fluent | Builder | Annotation | Kotlin | Executor keys / casts |
|---|---|---|---|---|---|
| 1. [`ban`](../examples/src/main/java/dev/marulabs/commandlib/examples/ban) | 15 | 20 | 12 | 10 | none |
| 2. [`kit`](../examples/src/main/java/dev/marulabs/commandlib/examples/kit) | 20 | 24 | 16 | 16 | none |
| 3. [`team`](../examples/src/main/java/dev/marulabs/commandlib/examples/team) | 27 | 33 | 31 | 25 | none |
| 4. [`stats`](../examples/src/main/java/dev/marulabs/commandlib/examples/stats) | 9 | 14 | 13 | 10 | none |
| 5. [`balance`](../examples/src/main/java/dev/marulabs/commandlib/examples/balance) | 20 | 23 | 25 | 16 | none; the account is read by type, `ctx.get(Economy.Account.class)` |
| 6. [`home`](../examples/src/main/java/dev/marulabs/commandlib/examples/home) | 23 | 27 | 18 | 22 | none |
| 7. [`score`](../examples/src/main/java/dev/marulabs/commandlib/examples/score) | 16 | 18 | 13 | 11 | none (the untyped executor reads `ctx.getArgument(argument)`) |

The numbers count the lines of each file that are not blank, comments, imports or the package line, including the
class declaration and constructor, formatted by the repository's formatter, which breaks Java chains into aligned lines.
Shared services (`Teams`, `StatsStore`, `Economy`, `HomeCaptions`) are not counted. The other libraries' line counts
below are of the snippets on this page, which are written compactly and not formatted, so compare them by order of
magnitude only.

Every CommandLib variant is registered the same way, `CommandLib.register(plugin, command)` in `onEnable`, and tested
the same way, `new CommandTester(() -> command, "myplugin.command")`. The annotated variants are parsed first with
`AnnotatedCommands.parse(new AnnotatedBanCommand())` and need `-parameters`, or `@Name` on each argument.

## 1. Admin command with options

`/ban [-s|--silent] [-r|--reason <reason>] <target>`, for operators.

```kotlin
// CommandLib, Kotlin DSL (examples/.../ban/KotlinBanCommand.kt)
command("ban") {
    description("Ban a player")
    permission(DefaultPermission.OP, "Ban players")
    val silent by flag("silent", 's', "Do not announce the ban")
    val reason by option(Options.string<CommandContext>("reason", 'r', "No reason").description("Why the player is banned"))
    argument(PlayerArgument("target")) { target ->
        execute { ctx -> ctx.sendSuccess("Banned ${target.get().name}: $reason" + if (silent) " (silent)" else "") }
    }
}
```

```java
// CommandLib, annotations (examples/.../ban/AnnotatedBanCommand.java)
@Command("ban")
@Description("Ban a player")
@Permission(defaultPermission = DefaultPermission.OP, description = "Ban players")
public final class AnnotatedBanCommand {
    @Execute
    public void ban(CommandContext ctx,
                    @Flag(value = "silent", shortName = 's') @Description("Do not announce the ban") boolean silent,
                    @Option(value = "reason", shortName = 'r') @Default("No reason") @Description("Why the player is banned") String reason,
                    Player target) {
        ctx.sendSuccess("Banned " + target.getName() + ": " + reason + (silent ? " (silent)" : ""));
    }
}
```

The generated help lists the options with their descriptions, `/ban -s -r griefing Steve` and
`/ban --silent --reason "being rude" Alex` work, and the permission node `myplugin.command.ban` is registered with
`OP` as its default ([snapshot](../examples/src/test/resources/snapshots/ban.snap)).

```java
// Cloud, builder: about 9 lines, string keys
manager.command(manager.commandBuilder("ban").permission("example.admin")
    .required("player", PlayerParser.playerParser())
    .flag(manager.flagBuilder("silent").withAliases("s"))
    .flag(manager.flagBuilder("reason").withAliases("r").withComponent(StringParser.greedyFlagYieldingStringParser()))
    .handler(ctx -> {
        Player target = ctx.get("player");
        boolean silent = ctx.flags().isPresent("silent");
        String reason = ctx.flags().getValue("reason", "No reason");
    }));
```

Partly verified: flags, aliases and values on [core](https://cloud.incendo.org/core/) (Flags), `CommandFlag.java` in
[Incendo/cloud](https://github.com/Incendo/cloud). The annotation form is
`@Command("ban <player>") void ban(CommandSender s, Player player, @Flag(value = "silent", aliases = "s") boolean silent, @Flag("reason") String reason)`
([annotations](https://cloud.incendo.org/annotations/), verified), without keys.

```java
// Lamp: about 5 lines, no keys
@Command("ban")
@CommandPermission("example.admin")
public void ban(BukkitCommandActor actor, Player target,
                @Switch(value = "silent", shorthand = 's') boolean silent,
                @Optional @Flag(value = "reason", shorthand = 'r') String reason) { ... }
```

Verified: `Flag.java` and `Switch.java` in
[Lamp v4](https://github.com/Revxrsal/Lamp/tree/v4/common/src/main/java/revxrsal/commands/annotation), and the test
`AutocompleteFlagsTest.java`; `@CommandPermission` on
[Bukkit](https://foxhut.gitbook.io/lamp-docs/platforms/bukkit-spigot-paper).

```java
// CommandAPI: about 10 lines, keys and casts, options only in this order
new CommandAPICommand("ban").withPermission("example.admin")
    .withArguments(new EntitySelectorArgument.OnePlayer("player"))
    .withOptionalArguments(new MultiLiteralArgument("silent", "--silent", "-s"))
    .withOptionalArguments(new LiteralArgument("--reason").combineWith(new GreedyStringArgument("reason")))
    .executes((sender, args) -> {
        Player target = (Player) args.get("player");
        boolean silent = args.getOptional("silent").isPresent();
        String reason = (String) args.getOrDefault("reason", "No reason");
    })
    .register();
```

Partly verified: `withOptionalArguments` and `combineWith` on
[optional arguments](https://docs.commandapi.dev/create-commands/arguments/optional-arguments),
[multi literal arguments](https://docs.commandapi.dev/create-commands/arguments/types/literal/multiliteral-arguments).
Unverified: that an optional `MultiLiteralArgument` and a `LiteralArgument` heading `combineWith` are accepted at
runtime. CommandAPI has no flags argument in a release; one is proposed in an open pull request
([#602](https://github.com/CommandAPI/CommandAPI/pull/602)).

- **ACF** has no command-line flags (`@Flags` passes modifiers to a context resolver, such as `"other"`,
  [annotations](https://github.com/aikar/commands/wiki/Annotations), verified). A trailing `String[] rest` takes the
  rest of the input, which the method parses by hand (about 15 lines, partly verified).
- **Paper** has no flags. Each accepted order and alias is its own branch of literals, so two options in any order
  take a tree of every combination (about 18 lines for one order, unverified as a pattern; the pieces are on
  [arguments and literals](https://docs.papermc.io/paper/dev/command-api/basics/arguments-and-literals/)).

**Compared.** Lamp and Cloud's annotations are the shortest. Cloud and Lamp accept the flags anywhere in the input and
Cloud adds greedy flag values; CommandLib takes the options **before** the positional arguments (`/ban Steve -s` is
rejected) unless the command calls `optionsAnywhere()` (`@OptionsAnywhere`), and also has permissions per option and
any argument as an option's value (`Options.of`). CommandLib's options are typed (`ctx.getOption(option)`, a delegated property in Kotlin) and come with
`required()`, `requires(...)`, `mutuallyExclusive(...)` and generated help, which the builder styles of Cloud and the
others do with string keys or not at all.

## 2. Optional and variadic arguments

`/kit give <name> [count=1]` and `/kit send <players...>`.

```java
// CommandLib, fluent (examples/.../kit/FluentKitCommand.java)
addChildren(new Command("give") {{
    description("Give yourself a kit");
    argument(new StringArgument("name"),
             new IntegerArgument("count", 1, 64).optional(1)).execute((name, count, ctx) -> {
                 ctx.sendSuccess("Gave " + count + " x " + name);
             });
}}, new Command("send") {{
    description("Send the starter kit to players");
    argument(new PlayerArgument("players").variadic()).execute((players, ctx) -> { /* List<Player> */ });
}});
```

The annotated form is `give(CommandContext ctx, String name, @Range(min = 1, max = 64) @Default("1") int count)` and
`send(CommandContext ctx, List<Player> players)`. `/kit send Steve Alex` gives both players, each value parsed as a
vanilla player argument, so names and selectors mix. `/kit give apple 99` is rejected by Brigadier at the client and
the server ([snapshot](../examples/src/test/resources/snapshots/kit.snap)).

```java
// Cloud: about 5 lines
manager.command(manager.commandBuilder("kit").literal("give")
    .required("name", StringParser.stringParser())
    .optional("count", IntegerParser.integerParser(1, 64), DefaultValue.constant(1))
    .handler(ctx -> give(ctx.get("name"), ctx.<Integer>get("count"))));
```

Verified: `DefaultValue.constant` on [core](https://cloud.incendo.org/core/). Cloud has no generic variadic parser:
`MultiplePlayerSelectorParser` takes one selector token, `StringArrayParser` the rest of the input as `String[]`
(verified, [Bukkit parsers](https://cloud.incendo.org/minecraft/bukkit/)).

```java
// Lamp: about 4 lines
@Subcommand("give") public void give(BukkitCommandActor actor, String name, @Default("1") @Range(min = 1, max = 64) int count) { }
@Subcommand("send") public void send(BukkitCommandActor actor, List<Player> players) { }
```

Verified: `@Default` on [improving the greet command](https://foxhut.gitbook.io/lamp-docs/introduction/improving-our-greet-command);
a `List` parameter is greedy unless `@Sized` bounds it (`CollectionParameterTypeFactory.java` in Lamp v4).

```java
// CommandAPI: about 8 lines, keys and casts
new CommandAPICommand("give")
    .withArguments(new StringArgument("name"))
    .withOptionalArguments(new IntegerArgument("count", 1, 64))
    .executesPlayer((player, args) -> {
        String name = (String) args.get("name");
        int count = (int) args.getOrDefault("count", 1);
    });
```

Verified: [optional arguments](https://docs.commandapi.dev/create-commands/arguments/optional-arguments).
`args.getByArgument(argument)` reads a value without key or cast when the argument is kept in a variable
([command arguments](https://docs.commandapi.dev/create-commands/arguments/command-arguments), verified).
`ListArgumentBuilder` builds a list of values from a fixed set, finished with `buildGreedy()` (the last argument) or
`buildText()` (one quoted string) ([list arguments](https://docs.commandapi.dev/create-commands/arguments/types/list-arguments),
verified).

- **ACF**: `@Default("1") int count`, and `OnlinePlayer[] players` from **one** comma-separated token
  (`/kit send Steve,Alex`) (verified: [annotations](https://github.com/aikar/commands/wiki/Annotations),
  `BukkitCommandContexts.java`).
- **Paper**: an optional argument is a second `executes` one level up, repeating the handler call; a list is a
  selector argument such as `ArgumentTypes.players()` (verified:
  [argument suggestions](https://docs.papermc.io/paper/dev/command-api/basics/argument-suggestions/),
  [entities and players](https://docs.papermc.io/paper/dev/command-api/arguments/entity-player/)).

**Compared.** Lamp, Cloud's annotations and ACF are as short as CommandLib's annotations. CommandLib's variadic
argument is a space-separated list of any argument's values with typed results, which none of the others has as a
general feature; Lamp's greedy `List<T>` is the closest. Only the last arguments of a branch can be optional or
variadic, as in CommandAPI and Cloud.

## 3. Argument-first tree

`/team <team> add <player>` and `/team <team> remove <player>`, the team looked up once.

```kotlin
// CommandLib, Kotlin DSL (examples/.../team/KotlinTeamCommand.kt)
argument(ObjectArgument<MutableSet<String>>("team") { teams.byName() }) { team ->
    subcommand("add") {
        argument(PlayerArgument("player")) { player ->
            execute { ctx -> team.get().add(player.get().name) }
        }
    }
    subcommand("remove") { /* the same with remove */ }
}
```

Fluent: `argument(new ObjectArgument<>("team", teams::byName)).child(team -> new Command("add") {{ ... }}).child(...)`.
Each child reads the team through its `ArgRef`, `team.get()`, and gets its own permission node
(`myplugin.command.team.add`) and help line ([snapshot](../examples/src/test/resources/snapshots/team.snap)).
With annotations, a placeholder in the path is the argument the child follows, and each method takes its value as the
parameter of the same name
([`AnnotatedTeamCommand`](../examples/src/main/java/dev/marulabs/commandlib/examples/team/AnnotatedTeamCommand.java),
which registers the `ObjectArgument` for `Set` parameters):

```java
@Subcommand("<team> add") public void add(CommandContext ctx, Set<String> team, Player player) { team.add(player.getName()); }
@Subcommand("<team> remove") public void remove(CommandContext ctx, Set<String> team, Player player) { team.remove(player.getName()); }
```

```java
// Cloud: about 3 lines
var team = manager.commandBuilder("team").required("name", StringParser.stringParser());
manager.command(team.literal("add").required("player", PlayerParser.playerParser()).handler(ctx -> add(ctx.get("name"), ctx.get("player"))));
manager.command(team.literal("remove").required("player", PlayerParser.playerParser()).handler(ctx -> remove(ctx.get("name"), ctx.get("player"))));
```

Partly verified: [core](https://cloud.incendo.org/core/) says literals may follow variable components and builders are
reusable. With annotations: `@Command("team <name> add <player>")`.

```java
// Lamp: about 2 lines
@Command("team <name> add") public void add(BukkitCommandActor actor, String name, Player player) { }
@Command("team <name> remove") public void remove(BukkitCommandActor actor, String name, Player player) { }
```

Verified: [subcommands](https://foxhut.gitbook.io/lamp-docs/introduction/commandactor-command-and-subcommand) (the
`bank <player> add` example).

```java
// CommandAPI: about 7 lines, keys and casts
new CommandTree("team").then(new StringArgument("name")
    .then(new LiteralArgument("add").then(new EntitySelectorArgument.OnePlayer("player")
        .executes((sender, args) -> add((String) args.get("name"), (Player) args.get("player")))))
    .then(new LiteralArgument("remove").then(new EntitySelectorArgument.OnePlayer("player")
        .executes((sender, args) -> remove((String) args.get("name"), (Player) args.get("player"))))))
    .register();
```

Partly verified: [command trees](https://docs.commandapi.dev/create-commands/command-trees).

- **ACF** has no argument-first subcommands (`@Subcommand` matches literal prefixes); the workaround is one method with
  `@Values("add|remove") String action`, which loses the separate permissions and help (verified:
  [annotations](https://github.com/aikar/commands/wiki/Annotations)).
- **Paper**: native, `Commands.argument("name", word()).then(Commands.literal("add").then(...))` (partly verified:
  [command tree](https://docs.papermc.io/paper/dev/command-api/basics/command-tree/)), with
  `getArgument("name", String.class)` in each executor.

**Compared.** Lamp's path placeholders are the shortest; CommandLib's annotations use the same syntax. CommandLib's
tree parses the team once, as a typed value shared by the children, where the others look up the name in each
handler.

## 4. Async I/O with thread return

`/stats <player>` loads the kills from a blocking store and replies on the server thread.

```java
// CommandLib, fluent (examples/.../stats/FluentStatsCommand.java)
argument(new StringArgument("player")).executeAsync((player, ctx) -> ctx.supplyAsync(() -> store.kills(player))
                                                                        // Back on the server thread.
                                                                        .thenAccept(kills -> ctx.sendSuccess(player + " has " + kills + " kills")));
```

```kotlin
// CommandLib, Kotlin DSL
argument(StringArgument("player")) { player ->
    executeSuspend { ctx ->
        val kills = withContext(ctx.asyncDispatcher) { store.kills(player.get()) }
        ctx.sendSuccess("${player.get()} has $kills kills")
    }
}
```

An annotated method returns the `CompletionStage`. `StatsExampleTest` gives the tester a scheduler that queues both
executors' tasks and checks, for each style, that nothing is sent when `execute` returns, that the query runs in an
async task and that the reply is sent by the sync task after it. On Folia the sync executor is the thread of the
command's source ([execution](execution.md#threads)).

```java
// Cloud: about 4 lines
manager.command(manager.commandBuilder("stats").required("player", StringParser.stringParser())
    .futureHandler(ctx -> CompletableFuture.supplyAsync(() -> store.kills(ctx.get("player")))
        .thenAccept(kills -> Bukkit.getScheduler().runTask(plugin, () -> ctx.sender().sendMessage(kills + " kills")))));
```

Partly verified: `futureHandler` and the execution coordinators (`simpleCoordinator()`, `asyncCoordinator()`,
`coordinatorFor(executor)`) on [core](https://cloud.incendo.org/core/); the official Bukkit example returns to the main
thread with `runTask` itself. `cloud-kotlin-coroutines` adds `suspendingHandler`
([coroutines](https://cloud.incendo.org/kotlin/coroutines/), verified).

```java
// Lamp: about 4 lines
@Command("stats")
public CompletableFuture<Void> stats(BukkitCommandActor actor, String player) {
    return CompletableFuture.supplyAsync(() -> store.kills(player))
        .thenAccept(kills -> Bukkit.getScheduler().runTask(plugin, () -> actor.reply(kills + " kills")));
}
```

Verified: `CompletionStageResponseHandler.java` in Lamp v4; v4 has no `@Async`.

- **CommandAPI**, **ACF** and **Paper** have no async executor: the executor starts the work with the Bukkit scheduler
  and schedules the reply itself (CommandAPI and Paper: verified by absence on
  [normal executors](https://docs.commandapi.dev/create-commands/executors/normal-executors) and
  [executors](https://docs.papermc.io/paper/dev/command-api/basics/executors/); ACF: unverified). All three have async
  suggestions.

**Compared.** CommandLib is the only one of these that returns to the server thread by itself, through a scheduler the
tests can replace, and its middlewares wrap the whole async execution. Cloud chooses the thread for parsing and
execution with coordinators, which CommandLib does not have: CommandLib always parses on the server thread. With every
library, `/execute store` sees the result set before the work finished.

## 5. Dependency injection

`/balance` and `/balance pay <target> <amount>` need the plugin's `Economy` and the sender's account; a sender without
an account is refused.

```java
// CommandLib, fluent (examples/.../balance/FluentBalanceCommand.java)
public FluentBalanceCommand(Economy economy) {
    super("balance");
    // The sender's account, mapped once per execution for this command and its children.
    provide(Economy.Account.class, ctx -> economy.account(ctx.getSender()));
    execute(ctx -> ctx.sendMessage("You have " + ctx.get(Economy.Account.class).balance() + " coins"));
    addChildren(new Command("pay") {{
        argument(new PlayerArgument("target"), new IntegerArgument("amount", 1, 1000)).execute((target, amount, ctx) -> {
            economy.pay(ctx.get(Economy.Account.class), target.getName(), amount);
        });
    }});
}
```

```java
// CommandLib, annotations (examples/.../balance/AnnotatedBalanceCommand.java)
AnnotatedCommands.parser()
                 .registerDependency(Economy.class, economy)    // sets @Dependency fields
                 .registerContextValue(Economy.Account.class, ctx -> economy.account(ctx.getSender()))
                 .parse(new AnnotatedBalanceCommand());

@Dependency
private Economy economy;

@Subcommand("pay")
public void pay(CommandContext ctx, Economy.Account account, Player target, @Range(min = 1, max = 1000) int amount) {
    economy.pay(account, target.getName(), amount);
}
```

In Kotlin, `provide { ctx -> economy.account(ctx.sender) }` keys the value by the lambda's type and
`ctx.get<Economy.Account>()` reads it. A provider that throws `CommandPrerequisiteException` refuses the sender with its
message, also from a typed executor ("You have no account." in the
[snapshot](../examples/src/test/resources/snapshots/balance.snap)). Middlewares can `put` values for the executor too
([context values](context-values.md)). A provider belongs to a command tree; values for every command of a plugin go
in a `CommandDefaults` passed to `CommandLib.register`.

```java
// Cloud: about 3 lines
manager.parameterInjectorRegistry().registerInjector(Account.class, (ctx, annotations) -> economy.account(ctx.sender()));
new AnnotationParser<>(manager, CommandSender.class).parse(new BalanceCommands());
// A sender type of your own for the whole manager: SenderMapper.create(sender -> new Payer(sender), Payer::sender)
```

Verified: [annotations](https://cloud.incendo.org/annotations/) (injection), `SenderMapper.java`. Cloud also has a Guice
injection service and `ctx.inject(Type.class)` in builder handlers.

```java
// Lamp: about 4 lines
BukkitLamp.builder(plugin).dependency(Economy.class, economy).build();
public final class BalanceCommands {
    @Dependency private Economy economy;
}
```

Verified: `Lamp.java` (`dependency(Class<T>, T)`), [dependency injection](https://foxhut.gitbook.io/lamp-docs/how-to/dependency-injection)
(whose `dependency(object)` form does not match the v4 source), and context parameters resolved by type
([context parameters](https://foxhut.gitbook.io/lamp-docs/how-to/context-parameters)).

```java
// ACF: about 5 lines
manager.registerDependency(Economy.class, economy);
manager.getCommandContexts().registerIssuerOnlyContext(Account.class, c -> economy.account(c.getSender()));
public final class BalanceCommand extends BaseCommand {
    @Dependency private Economy economy;
    @Subcommand("pay") public void onPay(Account account, OnlinePlayer target, int amount) { }
}
```

Partly verified: `registerDependency`, `@Dependency` and the issuer-only contexts are on
[dependency injection](https://github.com/aikar/commands/wiki/Dependency-Injection) and
[command contexts](https://github.com/aikar/commands/wiki/Command-Contexts); the resolver body is inferred.

- **CommandAPI** and **Paper** have no injection: pass the service to the class that builds the command
  (verified by absence).

**Compared.** Lamp, Cloud and ACF register a service or a sender type once for every command of the manager, and
Cloud's `SenderMapper` changes the sender type itself, so handlers take `Payer` instead of `CommandSender`. CommandLib's
values are typed, mapped once per execution, readable from middlewares, prerequisites and suggestions as well, and
scoped to a command tree, which is one call per top-level command in a plugin with many; the sender type stays the
platform's.

## 6. Per-locale messages

`/home set <name>` and `/home go <name>` reply in the sender's language, and so do the description and the headings of
the generated help.

```java
// CommandLib, fluent (examples/.../home/FluentHomeCommand.java)
public static final Caption SET = Caption.of("example.home.set", "Home {name} set.");

captions(Captions.create().bundle("lang/homes", HomeCaptions.class.getClassLoader()));    // lang/homes_ja.properties
description(ctx -> ctx.caption(HomeCaptions.DESCRIPTION));
argument(new StringArgument("name")).execute((name, ctx) -> ctx.sendSuccess(HomeCaptions.SET, Map.of("name", name)));
```

A player whose client is Japanese gets `ホーム base を設定しました。` and the help heading `使い方:`; CommandLib's own
messages, such as the cooldown, the options' errors and the permission message, come in English and Japanese
([snapshot](../examples/src/test/resources/snapshots/home.snap)). With annotations, `@CaptionBundle("lang/homes")` and
`@Description(value = "Manage your homes", caption = "example.home.description")` on the class do the same.

```java
// Cloud: about 5 lines
LocaleExtractor<CommandSender> locale = sender -> sender instanceof Player p ? p.locale() : Locale.ENGLISH;
manager.captionRegistry().registerProvider(TranslationBundle.core(locale));
manager.captionRegistry().registerProvider(TranslationBundle.resourceBundle("lang.homes", locale, getClassLoader()));
manager.captionFormatter(ComponentCaptionFormatter.miniMessage());
// in a handler: ctx.formatCaption(Caption.of("example.home.set"), CaptionVariable.of("name", name))
```

Verified: [localization](https://cloud.incendo.org/localization/), `TranslationBundle.java`. `cloud-translations` has
community translations of Cloud's messages into many languages.

```java
// ACF: about 4 lines
manager.addSupportedLanguage(Locale.JAPANESE);
manager.getLocales().loadYamlLanguageFile("lang_ja.yml", Locale.JAPANESE);
manager.usePerIssuerLocale(true);
// in a command: getCurrentCommandIssuer().sendInfo(HomeKeys.SET, "{name}", name); @Description("{@@example.home.description}")
```

Partly verified: the calls are on [locales](https://github.com/aikar/commands/wiki/Locales) and in `MessageKeys.java` /
`CommandIssuer.java`; `HomeKeys` stands for the plugin's own `MessageKeyProvider` enum.

- **Lamp v4** has no localization; the v3 `Translator` is gone. Localizing means overriding the exception handler's
  messages and looking the locale up yourself (verified: `CommandActor.java` has no locale in v4).
- **CommandAPI** and **Paper** have none: resolve the locale in the executor, `CommandAPI.failWithString(i18n(player.locale(), key))`,
  or send Adventure `Component.translatable(...)` with a `GlobalTranslator` (unverified as a pattern; failures on
  [handle failures](https://docs.commandapi.dev/create-commands/executors/handle-failures), verified).

**Compared.** Cloud and ACF have localization as complete as CommandLib's, with more languages for their own messages
(Cloud's translations; ACF's message keys and formats). CommandLib ships English and Japanese, resolves the locale
without setup, and localizes the generated help headings; CommandAPI, Lamp and Paper leave it to the plugin.

## 7. Vanilla result

`/execute store result score ... run score <player>` stores the player's score; an unknown player stores 0.

```java
// CommandLib, annotations (examples/.../score/AnnotatedScoreCommand.java)
@Execute
public int score(CommandContext ctx, String player) throws CommandPrerequisiteException {
    Integer score = SCORES.get(player);
    if (score == null) {
        throw new CommandPrerequisiteException("No score for " + player);
    }
    ctx.sendSuccess(player + " has " + score + " points");
    return score;
}
```

The other styles call `ctx.setResult(score)`. `CommandTester#execute` returns the stored value: 42, 7 and 0 in the
[snapshot](../examples/src/test/resources/snapshots/score.snap). The fluent and builder variants use the untyped
executor `execute(ctx -> ...)` with `ctx.getArgument(argument)` and throw the checked `CommandPrerequisiteException`;
a typed executor, which cannot throw it, fails the same way with `throw ctx.fail(...)`
([errors](errors.md#prerequisite-failed-commandprerequisiteexception)). Kotlin has no checked exceptions, so its typed
block throws it.

```java
// CommandAPI: about 4 lines
new CommandAPICommand("score")
    .withArguments(new StringArgument("player"))
    .executes((sender, args) -> { return scores.get((String) args.get("player")); })
    .register();    // registered in onLoad so that functions can use it
```

Verified: [resulting executors](https://docs.commandapi.dev/create-commands/executors/resulting-executors), which shows
`/execute store result`; a failure is `throw CommandAPI.failWithString(...)`.

```java
// Paper: about 3 lines
Commands.literal("score").then(Commands.argument("player", StringArgumentType.word())
    .executes(ctx -> scores.get(StringArgumentType.getString(ctx, "player"))))
```

Verified: `int run(CommandContext)` on [executors](https://docs.papermc.io/paper/dev/command-api/basics/executors/);
a failure throws a `CommandSyntaxException`.

- **Cloud** always returns 1 to Brigadier (`CloudBrigadierCommand.java` in
  [cloud-minecraft](https://github.com/Incendo/cloud-minecraft), verified).
- **Lamp** always returns `SINGLE_SUCCESS` (`BrigadierParser.java` in Lamp v4, verified).
- **ACF** returns Bukkit's `boolean`; its Brigadier support builds the client tree only (`BukkitRootCommand.java`,
  `PaperBrigadierManager.java`, verified).

**Compared.** CommandLib, CommandAPI and Paper give a real result; CommandAPI's and Paper's are a plain `return`, and
CommandLib's needs `setResult` or the untyped executor in Java.

## Setup

| Library | Dependency and packaging | Registration |
|---|---|---|
| CommandLib | `spigot`, `paper`, `velocity`, a Forge / NeoForge / Fabric artifact, or a `-kotlin` artifact (JitPack today); shade and relocate `dev.marulabs.commandlib` ([packaging](packaging.md)) | `CommandLib.register(plugin, commands...)` in `onEnable`; annotations need `-parameters` |
| CommandAPI | `commandapi-paper-shade` / `-spigot-shade`, relocation recommended; or the CommandAPI plugin as a dependency | `CommandAPI.onLoad(config)`, `onEnable()`, `onDisable()` when shaded, then `.register()` per command ([setup](https://docs.commandapi.dev/dev-setup/setup), verified) |
| Cloud | `cloud-paper` and modules such as `cloud-annotations`, `cloud-minecraft-extras`; relocation advised by the official example | Build a `PaperCommandManager` (or `LegacyPaperCommandManager` with `registerBrigadier()`), then `manager.command(...)` ([Paper](https://cloud.incendo.org/minecraft/paper/), verified) |
| Lamp | `lamp.common`, `lamp.bukkit` (and `lamp.brigadier`) from Maven Central; `-parameters` required | `BukkitLamp.builder(plugin).build().register(new Commands())` ([readme](https://foxhut.gitbook.io/lamp-docs), verified) |
| ACF | `acf-paper` 0.5.1-SNAPSHOT from Aikar's repository; shading and relocating `co.aikar.commands` and `co.aikar.locales` required; `-parameters` | `new PaperCommandManager(plugin).registerCommand(new Command())` ([Gradle setup](https://github.com/aikar/commands/wiki/Gradle-Setup), verified) |
| Paper | `paper-api` only | A `LifecycleEvents.COMMANDS` handler in `onEnable` or a bootstrapper ([registration](https://docs.papermc.io/paper/dev/command-api/basics/registration/), verified) |

Paper needs no dependency at all and is the only one without shading. CommandLib's setup is one call, without a manager
object, but it is not on Maven Central yet.

## Testing without a server

| Library | Official support | Covers |
|---|---|---|
| CommandLib | `common-testing`, `spigot-testing`, `paper-testing`, `velocity-testing`, Forge / NeoForge / Fabric testing artifacts | Tree snapshots, help, permissions, options, suggestions, results, sender locale, async with a controllable scheduler, `CommandLibExtension` for JUnit 5 ([testing](testing.md)) |
| CommandAPI | `commandapi-paper-test-toolkit` with MockBukkit | `assertCommandSucceeds` / `Fails`, the parsed argument values (`assertCommandSucceedsWithArguments`), suggestions and tooltips ([testing](https://docs.commandapi.dev/test/intro), verified) |
| Cloud | None; a `CommandManager` subclass and `commandExecutor().executeCommand(...)` work in plain tests | Parsing and handlers, without the platform (unverified as a downstream pattern) |
| Lamp | None; `Lamp.builder().build()` with a fake actor works, as Lamp's own tests do | Dispatch and completion (verified: `AutocompleteFlagsTest.java`) |
| ACF | None published | — |
| Paper | None; MockBukkit's Brigadier support is partial | — |

Every snapshot of the `examples` module is made by the testing artifacts alone. What they do not cover is the
platform: registration in Bukkit's command map or Paper's lifecycle, `DefaultPermission` defaults (except Velocity's,
which CommandLib applies itself), `/execute` itself and
real registries; the repository's integration tests run those on real servers. CommandAPI's toolkit runs on MockBukkit,
which models more of the server (players, worlds) than CommandLib's testers do, and can assert the parsed argument
values directly.

## Extensibility

| Library | Custom argument types | Hooks |
|---|---|---|
| CommandLib | Subclass `Argument`; `ObjectArgument` / `EnumArgument` for lookups; `validator`, `transformer`, `additionalParser` to refine a built-in type; `registerArgument` and `registerAnnotation` for annotations | Middlewares (sync and async), prerequisites, preprocesses, confirmation, cooldown, `Captions#sender`, uncaught exception handlers |
| CommandAPI | `CustomArgument` around a built-in base argument; about 60 built-in vanilla argument types | Requirements, failure messages ([custom arguments](https://docs.commandapi.dev/create-commands/arguments/types/custom-arguments), verified) |
| Cloud | `ArgumentParser` (and `FutureArgumentParser`), registered by type; `firstOf`, aggregate parsers; Brigadier mappings | Pre- and postprocessors, exception controller, confirmation, cooldown and requirement modules ([core](https://cloud.incendo.org/core/), verified) |
| Lamp | `ParameterType` and factories; Brigadier types with `@BType` | Conditions, parameter validators, annotation replacers, response handlers, permission factory ([custom parameter types](https://foxhut.gitbook.io/lamp-docs/how-to/custom-parameter-types), verified) |
| ACF | Context resolvers (input, issuer-aware, issuer-only) | Conditions, completions, replacements ([command contexts](https://github.com/aikar/commands/wiki/Command-Contexts), verified) |
| Paper | `CustomArgumentType` over a native type | Requirements ([custom arguments](https://docs.papermc.io/paper/dev/command-api/basics/custom-arguments/), verified) |

CommandLib registers nothing globally: an argument is an object in the branch, and an annotation parser's
registrations apply to the commands it parses. Cloud, Lamp and ACF register a type once for every command, which is
less code in a large plugin.

## Where the others are better

- **Lamp** is the most concise for annotated commands: options anywhere in the input without opting in, and
  injection with one call.
- **Cloud** has flags anywhere in the input by default and with greedy values, execution coordinators that move
  parsing and execution off the main thread, a manager-wide sender mapper and injectors, and community translations.
- **ACF** has per-issuer locales with message keys and formats, issuer-aware contexts, and reusable named conditions.
- **CommandAPI** has the largest catalog of typed vanilla arguments, a plain `int` return for `/execute store`, and a
  test toolkit on MockBukkit that asserts the parsed values.
- **Paper** needs no dependency and no shading, follows vanilla exactly, and is the reference for what Brigadier can do.

## CommandLib's constraints in these use cases

- Options come before the positional arguments unless the command calls `optionsAnywhere()`, and never follow the
  values of a variadic argument.
- Annotations do not cover `requires(option, predicate, description)`, captions other than one properties bundle, or
  `java.util.Optional<T>` parameters; the parsed command takes the first two through the fluent API.
- Context values are provided per command tree, per annotation parser or per registration (`CommandDefaults`), and
  the sender type stays the platform's.
- Help is a child of commands only: `/team red help` is rejected, while `/team red` shows the help of `/team`.
- Parsing always runs on the server thread; there is no coordinator to move it.
- CommandLib's own messages are translated into English and Japanese only.
- Java line counts are higher than Lamp's or ACF's for the same command, and the builder style is the longest.

## Sources

The research behind the snippets used the pages linked in each section and these sources:
[CommandAPI docs](https://docs.commandapi.dev/), [Cloud docs](https://cloud.incendo.org/) and
[Incendo/cloud](https://github.com/Incendo/cloud) / [cloud-minecraft](https://github.com/Incendo/cloud-minecraft),
[Lamp docs](https://foxhut.gitbook.io/lamp-docs) and [Lamp v4 source](https://github.com/Revxrsal/Lamp/tree/v4),
[ACF wiki](https://github.com/aikar/commands/wiki) and [source](https://github.com/aikar/commands), and
[Paper command API docs](https://docs.papermc.io/paper/dev/command-api/basics/registration/).

See also the migration guides: [CommandAPI](migration/from-commandapi.md), [Cloud](migration/from-cloud.md),
[Lamp](migration/from-lamp.md), [ACF](migration/from-acf.md).
