# Kotlin DSL

The Kotlin DSL builds the same `Command` objects as the fluent Java API, so commands written with it have the same
tree, permission nodes, help, options and test support. It adds blocks with typed argument references, options as
delegated properties and `suspend` executors and suggestion providers.

```kotlin
import dev.marulabs.commandlib.CommandLib
import dev.marulabs.commandlib.Options
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.kotlin.*
import kotlinx.coroutines.withContext
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin

class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        CommandLib.register(this, command("heal") {
            description("Heal players")
            permission(PermissionDefault.OP)
            val silent by flag("silent", 's', "Do not tell the players")

            argument(IntegerArgument("amount"), PlayerArgument("targets").variadic()) { amount, targets ->
                execute { ctx ->
                    targets.get().forEach { it.health = minOf(it.health + amount.get(), 20.0) }
                    if (!silent) ctx.sendSuccess("Healed ${targets.get().size} players")
                }
            }
            subcommand("self") {
                requirePlayer()
                argument(IntegerArgument("amount").optional(20)) { amount ->
                    execute { ctx -> ctx.player.health = minOf(ctx.player.health + amount.get(), 20.0) }
                }
            }
            subcommand("stats") {
                executeSuspend { ctx ->
                    val healed = withContext(ctx.asyncDispatcher) { database.countHeals() }
                    ctx.sendSuccess("$healed heals so far")
                }
            }
        })
    }
}
```

```text
/heal 5 Steve Alex        heals Steve and Alex by 5
/heal -s 5 Steve          heals Steve by 5, says nothing
/heal self                heals the sender by 20
/heal stats
/heal help
```

## Installation

| Platform | Artifact | Brings |
|---|---|---|
| Spigot | `spigot-kotlin` | `spigot`, `common-kotlin` |
| Paper | `paper-kotlin` | `paper`, `common-kotlin` |
| Velocity | `velocity-kotlin` | `velocity`, `common-kotlin` |

```kotlin
dependencies {
    implementation("com.github.Maru32768.CommandLib:spigot-kotlin:latest.release")
    // Only for executeSuspend, addMiddlewareSuspend, addSuggestionProviderSuspend and the dispatchers.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
```

- The modules are compiled for Kotlin 2.1 and need a Kotlin 2.0 or later compiler. Their POMs depend on
  `kotlin-stdlib` 2.1.21, so the plugin has to ship the standard library, as every Kotlin plugin does.
- `kotlinx-coroutines-core` is a `compileOnly` dependency: the coroutine functions live in their own class, so a
  plugin that never calls them does not need the library at runtime. A plugin that calls them must ship it.
- Shade `common-kotlin` together with `common` (the platform artifact): the DSL reads CommandLib's current command
  context through a package-private class, so both must be loaded by the same class loader. Relocating
  `dev.marulabs.commandlib` relocates both.
- Forge, NeoForge and Fabric have no Kotlin artifact yet. `common-kotlin` is platform-neutral; the platform part is the three
  functions in `common-kotlin/src/platform/kotlin/dev/marulabs/commandlib/kotlin/Commands.kt`, written against
  `dev.marulabs.commandlib.Command`, the class name every platform uses, so a mod can copy them.

## Commands

`command(name) { ... }` creates a command and runs the block with the command as its receiver. Every method of the
fluent API can be called in it: `description`, `permission(...)` (including Bukkit's `PermissionDefault` overloads),
`addAliases`, `requirePlayer()`, `confirm()`, `cooldown(...)`, `addMiddleware(...)`, `option(...)`, `execute { ctx -> }`
and `executeAsync { ctx -> }`.

`subcommand(name) { ... }` adds a child command (`/heal stats`). Inside an `argument(...)` block it adds a child
after the branch's arguments (`/config <key> get`).

The blocks are marked with a `@DslMarker`, so a block cannot call the outer command without naming it: inside
`argument(...) { }`, `description("...")` sets the branch's description, and inside `subcommand("x") { }` it sets the
subcommand's.

A class that extends `Command` can use the same functions in its `init` block:

```kotlin
class HealCommand : Command("heal") {
    init {
        argument(PlayerArgument("target")) { target -> execute { ctx -> target.get().health = 20.0 } }
    }
}
```

## Arguments

`argument(arg1, ..., argN) { ref1, ..., refN -> }` takes one to seven arguments, adds the branch and runs the block
with the branch (`ArgumentBranch`) as its receiver. Each lambda parameter is an `ArgRef<T>` typed from its argument,
so `PlayerArgument` gives `ArgRef<Player>` and `PlayerArgument(...).variadic()` gives `ArgRef<List<Player>>`; no
casts and no lookups by name.

For eight or more arguments, use `arguments(arg1, ..., argN) { }`: it takes any number of arguments and runs the block
with the branch as its receiver, without references. Keep the arguments in variables and read each value typed from
its argument with `ctx[arg]`, also in a `subcommand` of the branch:

```kotlin
val world = WorldArgument("world")
val name = StringArgument("name")
arguments(x1, y1, z1, x2, y2, z2, world, name) {
    execute { ctx -> ctx.sendMessage("${ctx[name]}: ${ctx[x2] - ctx[x1]} wide") }
    subcommand("preview") { execute { ctx -> ctx.sendMessage("${ctx[name]} in ${ctx[world].name}") } }
}
```

Read a value in any callback of the command (executor, suggestion provider, prerequisite, help) with `ref.get()`.
`ctx[ref]` reads it from a context, which also works where no callback is running on the thread, such as in work given
to an executor of your own:

```kotlin
argument(StringArgument("key")) { key ->
    description("Select a config key")
    permission(DefaultPermission.OP)
    subcommand("get") { execute { ctx -> ctx.sendMessage("${key.get()} = ${config[key.get()]}") } }
    subcommand("set") {
        argument(StringArgument("value")) { value ->
            execute { ctx -> config[ctx[key]] = ctx[value] }
        }
    }
}
```

### Default values

A Kotlin lambda cannot declare default parameter values, so the default lives on the argument, which maps to the same
`optional(...)` as in Java. The reference then always has a value:

```kotlin
argument(StringArgument("item"), IntegerArgument("count").optional(1)) { item, count ->
    execute { ctx -> ctx.sendMessage("${item.get()} x${count.get()}") }    // /give apple -> apple x1
}
argument(PlayerArgument("target").optional { ctx -> ctx.player }) { target -> ... }    // the sender by default
```

Only the last arguments of a branch can be optional or variadic, as in Java ([arguments](arguments.md)).

### Required values

`require(extractor) { }` runs the block with the fluent `require(...)` as its receiver. It adds no branch: its
`execute { value, ctx -> }` and `executeSuspend { value, ctx -> }` set the command's own executor, which then runs only
when the extractor gives a value and receives the value. A command has one executor, so calling `execute { }` on the
command as well throws `IllegalStateException`. A `subcommand` of it is checked by the extractor too:

```kotlin
require(Command.playerRequirer()) {
    execute { player, ctx -> player.health = 20.0 }
    subcommand("food") { execute { ctx -> ctx.player.foodLevel = 20 } }
}
```

It also works inside an `argument(...)` block, after the branch's arguments, where it sets the executor of that
branch.

## Options

`flag(name, shortName, description)` registers a flag. Any registered option, as `option(...)` returns it, is also a
delegated property whose value is read from the running callback:

```kotlin
command("scan") {
    val force by flag("force", 'f', "Run even if safety checks fail.")
    val limitOption = option(Options.integer<CommandContext>("limit", 'n', 10, 1, 100).description("Maximum number of targets."))
    val limit by limitOption

    argument(PlayerArgument("target")) { target ->
        execute { ctx ->
            if (force) ctx.sendWarn("Forced")
            ctx.sendMessage("Scanning ${target.get().name}, up to $limit, given: ${ctx.hasOption(limitOption)}")
        }
    }
}
```

Reading `force` outside a callback, for example in the command block itself, throws `IllegalStateException`.
`ctx[option]` reads an option from a context. Everything else about options (kinds, `repeatable`, `required`,
`mutuallyExclusive`, `requires`) is the Java API, described in [options](options.md).

## Context values

`provide { ctx -> ... }` provides a value keyed by the lambda's return type, `ctx.put(value)` sets one keyed by the
value's type, and `ctx.get<T>()` / `ctx.getOrNull<T>()` read them, so a sender mapped to your own type needs no cast:

```kotlin
command("team") {
    provide { ctx -> games.of(ctx.player) }    // GamePlayer
    subcommand("show") { execute { ctx -> ctx.sendMessage(ctx.get<GamePlayer>().team) } }
}
```

See [context values](context-values.md) for the order in which values are read, inheritance and named keys.

## Coroutines

`executeSuspend { ctx -> }` on a command or an argument branch sets a suspending executor. It is an `executeAsync`
executor ([execution](execution.md)) whose stage is a coroutine started with kotlinx-coroutines' `future { }`:

- The block starts on the sync thread and resumes there after each suspension, through `ctx.syncDispatcher`, which uses
  the platform's `CommandScheduler`. The sync thread is the server thread on Spigot, Paper and Forge / NeoForge /
  Fabric. Velocity has no main thread, so its sync executor runs a task on the calling thread: the block starts on the
  thread that runs the command and may resume on any thread, such as the one that completed what it waited for.
  Messages and the platform API may be used between suspensions.
- `yield()` gives the sync thread back: the block resumes on a later tick, so a long loop can spread its work over
  several ticks.
- `withContext(ctx.asyncDispatcher) { ... }` runs blocking work, such as a database query, on the scheduler's async
  executor. Do not use the platform API there.
- Both dispatchers keep the command's context current, so `ref.get()` and the option properties work after the
  coroutine resumes, also on the async executor. Other dispatchers, such as `Dispatchers.IO`, do not: use `ctx[ref]`
  there, or read the values first.
- The command is done when the block returns. An exception it throws is reported like an executor's: a
  `CommandPrerequisiteException` sends its message, anything else the generic error and the
  `UncaughtExceptionHandler`s. A cancelled coroutine reports nothing, but a `withTimeout` that runs out is reported as a
  timeout (`The command timed out.`).
- `/execute store` gets the result set before the first suspension, as for any async command.
- The coroutine has its own `Job`, created by the `CoroutineScope(...)` it is started in, but nothing cancels that job:
  disabling the plugin or unregistering the command does not stop the coroutine, and the Bukkit scheduler then refuses
  its remaining sync work. Bound slow work with `withTimeout`.

`addMiddlewareSuspend { ctx, next -> }` adds a suspending [middleware](execution.md#middleware). It starts on the sync thread
and resumes there, like `executeSuspend`; `next()` runs the remaining middlewares and the executor and returns once
they are done, throwing what they threw. Return without calling `next()` to skip the executor:

```kotlin
addMiddlewareSuspend { ctx, next ->
    val banned = withContext(ctx.asyncDispatcher) { bans.contains(ctx.actor.name) }
    if (banned) throw CommandPrerequisiteException("You are banned.")
    next()
}
```

`addSuggestionProviderSuspend { sb -> }` adds a suspending suggestion provider to an argument:

```kotlin
val warp = StringArgument("warp").addSuggestionProviderSuspend { sb ->
    withContext(Dispatchers.IO) { warps.findNames(sb.latestInput) }.forEach { sb.suggest(it) }
}
argument(warp) { warp -> execute { ctx -> teleport(ctx.player, warp.get()) } }
```

It starts on the thread that asks for suggestions and continues on whichever thread resumes it; the suggestions are
sent once it returns ([suggestions](suggestions.md#asynchronous-suggestions)). Suggestion contexts have no scheduler,
so `ctx.syncDispatcher` and `ctx.asyncDispatcher` are not available in it; do not use the platform API after
suspending.

## Testing

The commands are ordinary `Command`s, so the testers take them as they are. Create commands with `PlayerArgument` or
other NMS-backed arguments inside the tester, through the supplier constructor:

```kotlin
CommandTester(::healCommand, "myplugin.command").use { tester ->
    tester.execute("heal Steve", FakeSender.player("Admin"))
}
```

`executeSuspend` completes before `execute` returns with the testers' default executors. To check the state in
between, give the builder a scheduler that queues the async tasks, as in [execution](execution.md#testing).

## Fluent API equivalents

| Kotlin DSL | Fluent API |
|---|---|
| `command("heal") { ... }` | `new Command("heal") {{ ... }}` |
| `subcommand("all") { ... }` | `addChildren(new Command("all") {{ ... }})` |
| `argument(a, b) { ra, rb -> execute { ctx -> ra.get() } }` | `argument(a, b).execute((va, vb, ctx) -> ...)` |
| `arguments(a, ..., h) { execute { ctx -> ctx[a] } }` | `argument(a, ..., h).execute(ctx -> ctx.getArgument(a))` |
| `argument(a) { ra -> subcommand("get") { ... } }` | `argument(a).child(ra -> new Command("get") {{ ... }})` |
| `val force by flag("force", 'f')` and `force` | `option(Options.flag("force", 'f'))` and `ctx.getOption(force)` |
| `executeSuspend { ctx -> ... }` | `executeAsync(ctx -> ctx.supplyAsync(...).thenAccept(...))` |
| `require(extractor) { execute { value, ctx -> ... } }` | `require(extractor).execute((value, ctx) -> ...)` |
| `addMiddlewareSuspend { ctx, next -> ...; next() }` | `addMiddleware((ctx, next) -> ... next.proceed() ...)` |
| `provide { ctx -> games.of(ctx.player) }`, `ctx.get<GamePlayer>()` | `provide(GamePlayer.class, ctx -> ...)`, `ctx.get(GamePlayer.class)` |
| `arg.addSuggestionProviderSuspend { sb -> ... }` | `arg.addSuggestionProvider(sb -> sb.await(...))` |

The repository tests build a command with subcommands, options, optional and variadic arguments and an
argument-first child both ways and check that the tree, the help and the results are the same on Spigot, Paper and
Velocity (`common-kotlin/src/platformTest`).
