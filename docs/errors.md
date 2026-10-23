# Errors

This page specifies what happens when a command cannot run: which component detects the problem, which exception or
mechanism carries it, and what the sender sees on each platform. It describes the current behavior of the `common`,
`spigot`, `paper`, and `modded` (Forge / NeoForge) modules.

## Who reports an error

A command input goes through two stages, and each stage reports its own errors.

1. **Brigadier parsing (the platform).** The server's Brigadier dispatcher matches the input against the command tree
   and runs each argument type's parser. Errors at this stage never reach CommandLib code: the platform turns the
   `CommandSyntaxException` into its usual red error message with the `...<--[HERE]` context line.
2. **CommandLib execution.** When Brigadier finds a node that can run, CommandLib's runner takes over. It checks
   permissions again, reads options, converts each argument to its Java value, runs validators, prerequisites,
   the middlewares, and finally the executor. CommandLib collects the messages of an error at this stage and throws them
   to the platform as a `CommandSyntaxException`, see [Results and failures](#results-and-failures).

The runner works in this order and stops at the first failure:

| Step | What runs | Failure reported as |
|---|---|---|
| 1 | Permission of the command node | `You do not have permission to execute this command.` |
| 2 | Option values, the permission of each given option, and option dependencies (`requires(...)`) | The option message, see [Option errors](#option-errors), or the permission message |
| 3 | For each argument branch on the path: branch permission, then each argument's parse → `validator` → `transformer`, or the default of an omitted optional argument | The permission message, the argument's `ArgumentParseException`, or the `CommandPrerequisiteException` of an `optional(extractor)` default |
| 4 | Prerequisites (`addPrerequisite`, `require(...)`, such as `require(Command.player())`) | The `CommandPrerequisiteException` message |
| 5 | No executor on this node: the generated help is sent instead | (help text, not an error) |
| 6 | The middlewares: those of the registration (`CommandDefaults`), then `cooldown(...)`, then `confirm(...)`, then the middlewares and preprocesses (`addMiddleware`, `addPreprocess`) in the order they were added | The cooldown message, the confirmation request, the messages a preprocess sent before returning `false`, a middleware's `CommandPrerequisiteException` message, or the generic unexpected-error message |
| 7 | The executor (`execute` or `executeAsync`) | A `CommandPrerequisiteException` message, `The command timed out.` for a `TimeoutException`, or the generic unexpected-error message |

A middleware can map the failure of the steps after it, see [execution](execution.md#middleware). An
`errorFormatter(...)` replaces what the sender gets for each failure CommandLib reports at these steps, and can add the
usage line of the argument branch that failed; see [help, usage and error formatting](help.md#error-formatter).

## Results and failures

The runner returns the value the executor set with `ctx.setResult(int)` to Brigadier, `1` unless set, and `1` for the
generated help. `/execute store result` stores it.

An `executeAsync` command whose stage is still running when the dispatch returns gives the result set by then, 1
unless set, so `store success` stores 1; a later failure sends its messages to the sender directly and cannot change
what was stored. See [execution](execution.md#the-result-for-execute-store).

A failure at any step above throws a `CommandSyntaxException` whose message joins the failure messages, one per line.
The platform reports it as it reports a vanilla command's error: the sender gets it in red, except a silent source such
as a function and each target of a forked `/execute as @a ... run`. `/execute store result` and `/execute store success`
both store `0`. A failure that sent no message, such as a preprocess returning `false` silently, only returns `0`,
since the platform would send an empty line for it; `store success` then stores `1`.

Messages the executor sent before throwing still reach the sender directly. `ctx.sendSuccess(...)` is vanilla command
feedback (see the README): a player does not get it while the `sendCommandFeedback` gamerule is off.

## Error catalog

### Unknown command

The root literal does not exist, or the sender cannot use it (see [Permission denied](#permission-denied)). The
platform reports it; CommandLib is not involved.

| Platform | Sender sees |
|---|---|
| Spigot | The server's unknown-command message (vanilla `command.unknown.command` or Bukkit's `Unknown command. Type "/help" for help.`, depending on how the input reached the dispatcher) |
| Paper | Vanilla `Unknown or incomplete command, see below for error` with the context line |
| Forge / NeoForge | Vanilla `Unknown or incomplete command, see below for error` with the context line |

### Unknown subcommand or trailing input

The root literal matched, but the next token matches no child, or extra tokens follow a complete command
(`/game foo`, `/msg Steve hi there` when the text argument is a single word). Brigadier stops at the last node it could
match and reports `command.unknown.argument`.

| Platform | Sender sees |
|---|---|
| All | Vanilla `Incorrect argument for command` and the context line `.../game foo<--[HERE]`, in red |

### Invalid argument syntax

The argument type's own parser rejects the token, for example `abc` for an `IntegerArgument`, `200` for
`new IntegerArgument("n", 1, 100)`, an unclosed quote for `StringArgument.Type.PHRASE_QUOTED`, a malformed selector,
or an item id that does not exist for `ItemStackArgument`. This happens during Brigadier parsing.

| Platform | Sender sees |
|---|---|
| All | The vanilla message of the argument type, such as `Invalid integer 'abc'` or `Integer must not be more than 100, found 200`, and the context line, in red |

The client also validates many of these types while the player types, and colors the input red before it is sent.

### Missing argument

The input stops before the end of an argument branch, for example `/msg` or `/msg Steve` for
`argument(new PlayerArgument("target"), new StringArgument("text"))`.

Every command literal and every argument node of a branch is executable, so Brigadier never reports an incomplete
command for CommandLib commands. Instead the runner reaches a node without an executor and sends the **generated
help** of the command (the `Usage:` list), after checking permissions, options, the arguments typed so far, and
prerequisites. A command that has its own `execute(...)` runs that executor for the bare `/command` input instead.

The same help is available as `/<command> help` when the command has children, argument branches, or options, unless a
child is named or aliased `help`. See [help](help.md) for what it lists and how to format it.

### Argument value rejected by CommandLib (`ArgumentParseException`)

The token is syntactically valid, but converting it to the Java value fails in the argument's `parseImpl`. Examples:
an unknown name for `EnumArgument`, `LiteralArgument`, `ObjectArgument`, `NameableObjectArgument`, `TeamArgument`,
`ObjectiveArgument`, `OfflinePlayerArgument`; an unknown key for `AdvancementArgument`, `LootTableArgument`,
`RecipeArgument`; an unknown registry key for Spigot's `SoundArgument`, `BiomeArgument`, `AttributeArgument`, and
for `RegistryArgument` on Spigot and Forge / NeoForge; fewer ticks than the minimum of `TimeArgument` on servers before
1.19.4.

CommandLib uses two message shapes:

- `ArgumentParseException.ofIncorrectInput(...)` imitates the vanilla error: a red translatable
  `command.unknown.argument` ("Incorrect argument for command"), then a line with the input before the argument in
  gray (at most the last 10 characters, prefixed with `...`), the rejected token underlined in red, and the
  translatable `command.context.here` ("<--[HERE]") in red italics. Used by the enum, literal, object, nameable object,
  team, offline player, and Paper world arguments.
- A red message. Where vanilla has a message of the same meaning, it is vanilla's translatable one:
  `Unknown advancement: minecraft:foo`, `Unknown recipe: foo`, `Unknown scoreboard objective 'foo'`,
  `No player was found` (an `@a` or `@r` without players). The others are [captions](localization.md):
  `foo is not a valid namespaced key`, `foo is not a known loot table`, `foo is not a known sound`,
  `Steve is neither a known player nor a valid UUID`, `@x is not a valid selector`.

The message is the same on every platform. The translatable parts are translated by the client, the captions are sent
in the sender's locale.

If the argument has an `additionalParser(...)`, it gets the raw input after the parse fails; the error is reported only
when it also returns `null`.

### Argument value rejected by Minecraft while resolving (`CommandSyntaxException` in `parseImpl`)

Some vanilla argument types parse the token during Brigadier parsing but only resolve it when the command runs, and
throw a `CommandSyntaxException` then. Examples: a player name or selector that matches nobody (the four selector
arguments), an unknown team for Spigot's or Forge's `TeamArgument` without a custom scoreboard, an unknown dimension for
Spigot's `WorldArgument`.

CommandLib reports the exception as vanilla reports a command error, the same on every platform: the exception's
message in red, kept as the translatable component vanilla made so that the client translates it (`No player was
found` is `argument.entity.notfound.player`), then, when the exception carries its input and cursor, the context line
`...<input before the cursor><rest of the input underlined><--[HERE]`. The exceptions vanilla throws while resolving
carry no input, so they show only the message, as for a vanilla command. A plain Brigadier message (`LiteralMessage`,
from a custom argument type) is sent as its text. Each platform adapter converts the message: Spigot to a Bungee
`TranslatableComponent`, Paper to an Adventure component through `MessageComponentSerializer`, Forge / NeoForge keep
the vanilla component.

A player or entity selector that matches nothing fails with vanilla's `No player was found` / `No entity was found` on
every platform, for `PlayerArgument`, `PlayersArgument`, `EntityArgument`, and `EntitiesArgument`, as vanilla's
`EntityArgument.getPlayers` / `getEntities` reject an empty result. Spigot and Forge / NeoForge read the selector with
those methods. On Paper, `player()` / `entity()` throw the vanilla error themselves, while `players()` / `entities()`
return an empty list, so CommandLib's `PlayersArgument` / `EntitiesArgument` reject it with the same translatable error.

### Validator rejected the value (`ArgumentValidationException`)

`validator(...)` runs after a successful parse.

- `validator(Predicate)` and `validator(BiFunction<T, C, Boolean>)` report the vanilla-style "Incorrect argument for
  command" message with the context line (the `ofIncorrectInput` shape above).
- `validator(ArgumentValidator)` lets you throw your own `ArgumentValidationException`:
  - `new ArgumentValidationException()` sends `Invalid value.` in red.
  - `new ArgumentValidationException(caption, placeholders)` sends a [caption](localization.md) in red.
  - `new ArgumentValidationException("message", "more lines"...)` sends each string in red.
  - `new ArgumentValidationException(ctx -> ...)` sends whatever the consumer sends.
  - `ArgumentValidationException.ofIncorrectInput(name, ctx, input)` sends the vanilla-style message.

```java
argument(new IntegerArgument("amount").validator((ctx, amount) -> {
    if (amount % 2 != 0) {
        throw new ArgumentValidationException("The amount must be even.");
    }
})).execute((ctx, amount) -> ctx.sendSuccess("ok " + amount));
```

Validators also filter suggestions: the enum, object, nameable object, team, offline player, and UUID arguments drop
candidates that the validator rejects.

### Permission denied

Every command, subcommand, and argument branch has a permission node (see [permissions](permissions.md)). CommandLib
uses it in two places:

1. **Visibility.** The Brigadier node's `requires` predicate checks the node. A sender without the permission does not
   get the node in the client command tree, does not get it in tab completion, and the server parses the input as if
   the node did not exist: they see the [unknown command](#unknown-command) or [unknown subcommand](#unknown-subcommand-or-trailing-input)
   message of the platform. Generated help lists only the routes the sender may use.
2. **Execution.** The runner checks the command node, each option given, and every argument branch on the path again,
   and sends `You do not have permission to execute this command.` in red when the check fails. Normally the
   visibility check rejects the input first; this message appears when the check result changes between parsing and
   execution, or when the input was parsed for another source.

Non-player sources:

| Platform | Console | Command blocks, `/execute` as an entity | Data pack functions |
|---|---|---|---|
| Spigot | `CommandSender#hasPermission` of the console | `hasPermission` of the Bukkit sender | Always allowed while functions are compiled; the function permission level guards them |
| Paper | `CommandSender#hasPermission` | `hasPermission` of the Bukkit sender | Always allowed while functions are compiled |
| Forge / NeoForge / Fabric | Always allowed | The node's default with the source's permission level: `ALL` allowed, `NONE` refused, `OP` needs level 4 (a command block has 2) | Always allowed while functions are compiled |

### Prerequisite failed (`CommandPrerequisiteException`)

Thrown by a `Prerequisite` (`addPrerequisite`), an `Extractor` (`require(...)`), a middleware, or an executor, or
raised with `ctx.fail(...)`. The message is sent in red and the command [fails](#results-and-failures).

| Source | Message |
|---|---|
| `new CommandPrerequisiteException()` | `You do not have permission to execute this command.` |
| `new CommandPrerequisiteException("message", "more lines"...)` | each string, in red |
| `new CommandPrerequisiteException(caption[, placeholders])` | the [caption](localization.md) in the sender's locale, in red |
| `new CommandPrerequisiteException(ctx -> ...)` | whatever the consumer sends |
| `ctx.fail("message", "more lines"...)`, `ctx.fail(caption[, placeholders])` | the same as the constructor with those arguments |
| `Command.player()` | `This command can only be executed by a player.` |
| `Command.console()` | `This command can only be executed from the console.` |

Prerequisites of a parent command apply to its children unless the child calls `disableParentPrerequisite()`.

#### Reusable conditions

A condition shared by several commands is a `Prerequisite` constant; it is typed, so it reads the platform's context
without casts, and `and` combines two:

```java
public final class Conditions {
    public static final Prerequisite<CommandContext> DAYTIME = ctx -> {
        if (Command.player().extract(ctx).getWorld().getTime() >= 12000) {
            throw new CommandPrerequisiteException("This command works only by day.");
        }
    };
    public static final Prerequisite<CommandContext> STAFF_BY_DAY = DAYTIME.and(ctx -> { /* ... */ });
}

addPrerequisite(Conditions.DAYTIME);
```

Annotated commands name it: `AnnotationParser#registerPrerequisite("daytime", Conditions.DAYTIME)` and
`@Prerequisites("daytime")` on the class or a `@Subcommand` method ([annotations](annotations.md#customizing-the-parser)).

The typed executors (`execute((ctx, a, b) -> ...)`) take `java.util.function` style consumers that cannot throw
checked exceptions. `ctx.fail(...)` throws the `CommandPrerequisiteException` without declaring it, so it fails them
like any other executor; it never returns, and `throw ctx.fail(...)` lets the compiler see that the branch ends:

```java
argument(new PlayersArgument("targets")).execute((ctx, targets) -> {
    if (targets.isEmpty()) {
        throw ctx.fail("No player was found");
    }
    targets.forEach(x -> x.setHealth(20));
});
```

Calling `ctx.sendFailure(...)` and returning sends the message but does not fail the command (`store success` stores
`1`).

### Preprocess cancelled

`addPreprocess(Predicate)` returning `false` makes the command fail with the messages the preprocess sent before
returning, and stops it without a message when it sent none. A preprocess that returns `true` has its messages sent
when it returns. Preprocesses run only when the node has an executor; the generated help skips them.

### Cooldown and confirmation

`cooldown(...)` and `confirm(...)` are middlewares that run right after the registration's `CommandDefaults` middlewares, so they too run only when the node has an executor. A sender on cooldown gets `You must wait 1m 30s before using this command again.`, and a first execution
of a command with a confirmation gets `Run the command again within 10s to confirm. [Confirm]` in yellow, where
`[Confirm]` runs the command again when clicked. Both make the command [fail](#results-and-failures), so a function or
each target of a forked `/execute as @a ... run` does not get them. `Cooldown#message` and `Confirmation#message`
replace them. The cooldown starts only when the confirmation and the preprocesses pass. See the README for the API.

### Option errors

| Case | Reported by | Sender sees |
|---|---|---|
| Option after a regular argument (`/scan Steve -f`), unknown option token, or a value option without a value | Brigadier | Vanilla "Incorrect argument for command" or the argument type's error, with the context line |
| Invalid option value (`--limit abc`, out of the option's range) | Brigadier | The value type's vanilla error |
| Option of a mutually exclusive group given after another of the group (`mutuallyExclusiveOptions(...)`) | Brigadier | Vanilla "Incorrect argument for command", since the rest of the group is removed from the tree |
| `required()` option missing | CommandLib (`OptionParseException`) | `Missing required option --tag.` in red, or the help when no executor is at the end of the input |
| `requires(other)` not met | CommandLib (`OptionParseException`) | `--reason requires --force.` in red |
| `requires(other, value)` not met | CommandLib (`OptionParseException`) | `--threads requires --mode to be parallel.` in red |

See [options](options.md) for the option syntax.

### Unexpected exception

An exception thrown by the executor or a middleware, or a failed stage of an async executor, other than
`CommandPrerequisiteException`, `CancellationException` (reports nothing) and `TimeoutException` (sends
`The command timed out.` and calls the handlers):

- it is logged with the input and the sender, through the logger of the plugin that ships CommandLib on Spigot and
  Paper, the `CommandLib` SLF4J logger on Velocity and the `CommandLib` Log4j logger on Forge, NeoForge and Fabric. The
  server itself does not log an exception of a command;
- the sender gets `An unexpected error occurred trying to execute that command` (vanilla's translatable
  `command.failed`, unless `Caption.UNEXPECTED_ERROR` is given a text) and `Check the console for details.` in red;
- every `UncaughtExceptionHandler` added with `addUncaughtExceptionHandler` on the command (and, for an argument node,
  on the argument) is called with the context and the exception;
- the command [fails](#results-and-failures) with those two lines, or, for an async command that has already
  returned, sends them to the sender directly; the handlers then run on the sync thread.

An unchecked exception thrown before the middlewares (in a parser, validator, transformer, or prerequisite) is
logged, passed to the uncaught exception handlers, and rethrown to Brigadier. The platform then reports it with its
own generic error (vanilla: `An unexpected error occurred trying to execute that command`, with the exception message
on hover for operators).

An exception thrown by a suggestion provider, or by a stage it awaits, is logged. The client still gets the suggestions
of the other providers.

## Languages

The texts CommandLib sends itself (permission, prerequisite, timeout, option errors, cooldown, confirmation, the plain
argument messages, and the headings of the generated help) are [captions](localization.md): they are sent in the
sender's locale, CommandLib ships English and Japanese, and a plugin can replace each per locale with `Captions`. The
English texts in this page are their defaults. The vanilla-style "Incorrect argument for command" / "<--[HERE]" parts,
the vanilla messages CommandLib uses where they mean the same thing (`No player was found`, `Unknown advancement`,
`Unknown recipe`, `Unknown scoreboard objective`, `command.failed`), and the vanilla messages passed through are
translatable and translated by the client. Brigadier's own errors are the platform's.

## In tests

`CommandTester#execute` in every testing artifact throws a `RuntimeException` whose cause is the
`CommandSyntaxException` when Brigadier rejects the input (unknown command, unknown subcommand, invalid syntax,
permission hidden node). Everything CommandLib reports itself is captured by `FakeSender#getSentMessages()` /
`getSentMessageTexts()`: a failure's messages are sent to the sender one by one, as they were sent, and `execute`
returns `0`. Otherwise `execute` returns the command's result. Translatable parts read as English text in `spigot-testing` and `modded-testing` and as
translation keys (`command.unknown.argument`, `command.context.here`) in `paper-testing`. See [testing](testing.md).
