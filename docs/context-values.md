# Context Values: Sender Mapping, Middleware Values and Dependencies

A command execution carries typed values besides its arguments and options. A command **provides** them, a
middleware **puts** them, and every part of the execution **gets** them by type, without casts:

```java
public final class TeamCommand extends Command {
    public TeamCommand(Games games) {
        super("team");

        // The sender as the plugin's own type, mapped once per execution.
        provide(GamePlayer.class, ctx -> games.of(playerRequirer().extract(ctx)));

        addChildren(new Command("show") {{
            execute(ctx -> ctx.sendMessage(ctx.get(GamePlayer.class).team()));
        }}, new Command("join") {{
            argument(new StringArgument("team")).execute((team, ctx) -> ctx.get(GamePlayer.class).join(team));
        }});
    }
}
```

The same mechanism covers three things other libraries give separate APIs: a sender mapper (Cloud's `SenderMapper`,
Lamp's custom actors, ACF's issuer-only contexts), values a middleware attaches for the executor (context
enrichment), and the services a command needs (Lamp's and ACF's `@Dependency`, Cloud's injectors). The design is in
[design/execution-pipeline.md](design/execution-pipeline.md#context-values).

## The API

| Where | Method | Does |
|---|---|---|
| Command | `provide(GamePlayer.class, ctx -> ...)` | Provides the value to the command, its argument branches and its child commands |
| Command | `provide(ContextKey<T> key, ctx -> ...)` | The same for a named key |
| Context | `ctx.put(GamePlayer.class, value)`, `ctx.put(key, value)` | Sets the value for the rest of the execution |
| Context | `ctx.get(GamePlayer.class)`, `ctx.get(key)` | The value; `IllegalStateException` when nothing provides or put it |
| Context | `ctx.find(GamePlayer.class)`, `ctx.find(key)` | The value as an `Optional`, empty when nothing provides or put it |

A `ContextKey<T>` names a value: `ContextKey.of(Account.class)` is the key of the type, which the `Class` overloads
use, and `ContextKey.of("started", Instant.class)` a second value of a type that already has one. Keys are equal when
their name and type are equal, so a key may be created where it is used or kept in a constant. A primitive type stands
for its wrapper (`ctx.get(int.class)` reads the `Integer`).

`get` reads, in this order:

1. The value put with `put`, by a middleware or earlier code of the execution.
2. The value the command's provider gives. The provider runs the first time the key is read in the execution, and its
   value is kept for the rest of it, so a sender mapped in a middleware is the same object in the executor.

Providers are inherited by argument branches and child commands, including argument-first children
(`argument(...).child(...)`). A child's provider for the same key replaces the parent's. `disableParentMiddleware()`
does not affect providers, since they are not middlewares.

Values can be read wherever the execution has a context: middlewares, executors, prerequisites (`addPrerequisite`,
`require(...)`), the defaults of omitted optional arguments (`optional(ctx -> ...)`), argument validators and
suggestion providers (`sb.getContext().get(...)`), and the generated help. Values put by a middleware exist only from
that middleware on; suggestion and help contexts run no middleware, so they have only the provided values.

## Sender mapping

Map the platform's sender to your own type with a provider on the top-level command:

```java
provide(GamePlayer.class, ctx -> games.of(playerRequirer().extract(ctx)));
```

- A `CommandPrerequisiteException` thrown by the provider fails the command with its message, here "This command can
  only be executed by a player." for the console. `ctx.get` throws it on although it does not declare it, so this
  works in the typed executors (`(team, ctx) -> ...`), which cannot throw checked exceptions.
- For an executor that takes the mapped sender as a typed parameter, read it with `require`:

  ```java
  require(ctx -> ctx.get(GamePlayer.class)).execute((player, ctx) -> player.leave());
  ```

- `playerRequirer()` exists on every platform. It gives a `Player` on Spigot, Paper and Velocity, and a
  `ServerPlayer` on Forge, NeoForge and Fabric.

## Values from middlewares

A middleware attaches what it computed for the later middlewares and the executor:

```java
static final ContextKey<Instant> STARTED = ContextKey.of("started", Instant.class);

addMiddleware((ctx, next) -> {
    ctx.put(STARTED, Instant.now());
    ctx.put(Account.class, accounts.load(ctx.getSender()));    // or replace a provided value
    return next.proceed();
});
execute(ctx -> ctx.sendMessage("Balance " + ctx.get(Account.class).balance()));
```

A middleware that needs the work of an async lookup puts the value before it calls `proceed()`; the rest of the chain
runs on the sync thread ([execution](execution.md#middleware)).

## Dependencies

A provider that ignores the context gives a service to every executor of the command tree:

```java
provide(Economy.class, ctx -> economy);
execute(ctx -> ctx.get(Economy.class).deposit(ctx.getPlayer(), 10));
```

For a fluent command, passing the service to its constructor (`new ShopCommand(economy)`) does the same without a
lookup; a provider is useful when the commands that need it are deep in a tree built elsewhere, or for annotated
commands.

### Annotated commands

`AnnotationParser` reads context values for parameters, so the method gets the mapped sender, a middleware's value or
a service as a parameter:

```java
@Command("team")
public final class TeamCommand {
    @Dependency
    private Games games;    // set when the class is parsed

    @Subcommand("join")
    public void join(GamePlayer player, String team) {    // the mapped sender, then an argument
        games.join(player, team);
    }

    @Subcommand("stats")
    public void stats(CommandContext ctx, Account account) {    // put by a middleware
        ctx.sendMessage("Balance " + account.balance());
    }
}

Command team = AnnotatedCommands.parser()
        .registerDependency(Games.class, games)
        .registerContextValue(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)))
        .registerContextValue(Account.class)
        .parse(new TeamCommand());
team.addMiddleware((ctx, next) -> {
    ctx.put(Account.class, accounts.load(ctx.getSender()));
    return next.proceed();
});
```

| Parser method | Parameters of the type | The parsed command |
|---|---|---|
| `registerContextValue(type, extractor)` | Read with `ctx.get(type)` | Provides the value with the extractor |
| `registerContextValue(type)` | Read with `ctx.get(type)` | Provides nothing: a middleware puts the value, or a command the parsed one is added to provides it |
| `registerDependency(type, instance)` | Get the instance | Provides the instance; fields annotated `@Dependency` of the type are set to it |

- A parameter of a registered type needs no annotation; `@Sender` marks one whose type also has an argument factory,
  such as `@Sender Player player` against a `Player` argument.
- The parsed command provides the values it registers, so fluent code added to it reads them too, and a middleware
  added to it can replace them with `put`.
- Parsing fails for a parameter type that has neither an argument factory nor a context value, and for a `@Dependency`
  field without a registered dependency or one that is static or final. An execution that reaches a method whose
  value of `registerContextValue(type)` nobody gave fails like an executor exception.
- `@Dependency` sets fields of the class and its superclasses with reflection. NullAway users can exclude them from
  the initialization check with `-XepOpt:NullAway:ExcludedFieldAnnotations=dev.marulabs.commandlib.annotation.Dependency`.

### Guice, Dagger and other containers

CommandLib depends on no container. Let the container create the command class through its constructor, and pass the
services that executions read to `registerContextValue`, `registerDependency` or `provide`:

```java
// Guice: a command class with an @Inject constructor that takes its services.
Games games = injector.getInstance(Games.class);
Command team = AnnotatedCommands.parser()
        .registerContextValue(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)))
        .parse(injector.getInstance(GuiceTeamCommand.class));

// Or hand a binding to a fluent command tree.
provide(Economy.class, ctx -> injector.getInstance(Economy.class));
```

With Dagger, inject the command classes from a component (`component.teamCommand()`) and pass the result to `parse` or
to `CommandLib.register`.

## Scope

Providers belong to a command and its subtree. For values shared by every command of a plugin, give them to the
registration with `CommandDefaults`, which also holds registration-wide middlewares
([execution](execution.md#registration-wide-middlewares-and-providers)):

```java
CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class)
        .provide(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)));
CommandLib.register(this, defaults, List.of(new TeamCommand(games), new ArenaCommand(arenas)));
```

A command that provides the same key replaces the registration's provider for its subtree. Testers take the same
defaults with `CommandTester.builder().defaults(defaults)`.

## Threads

The values belong to the sync thread, like the sender: put and read them in middlewares and executors, and read what
async work needs before starting it ([execution](execution.md#thread-safety-of-the-context)). A provider runs on the
thread that first reads its key.

## Kotlin

The [Kotlin DSL](kotlin.md) adds reified forms:

```kotlin
command("team") {
    provide { ctx -> games.of(ctx.player) }    // keyed by the lambda's type, GamePlayer
    addMiddleware { ctx, next ->
        ctx.put(accounts.load(ctx.sender))     // keyed by the value's type, Account
        next.proceed()
    }
    subcommand("show") {
        execute { ctx -> ctx.sendMessage("${ctx.get<GamePlayer>().team} ${ctx.getOrNull<Account>()?.balance}") }
    }
}
```

A Kotlin provider may throw `CommandPrerequisiteException` like a Java one. Give the type explicitly with
`provide(GamePlayer::class.java) { ctx -> ... }` when the lambda returns a subtype.

## Testing

The testers run the providers and middlewares as a server does, so a command that maps its sender is tested with
`CommandTester` like any other. To replace a provided value in a test, add a middleware that puts the test's value, or
build the command with the test's services.
