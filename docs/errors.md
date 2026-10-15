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
   preprocesses, and finally the executor. CommandLib collects the messages of an error at this stage and throws them
   to the platform as a `CommandSyntaxException`, see [Results and failures](#results-and-failures).

The runner works in this order and stops at the first failure:

| Step | What runs | Failure reported as |
|---|---|---|
| 1 | Permission of the command node | `You do not have permission to execute this command.` |
| 2 | Option values and option dependencies (`requires(...)`) | The option message, see [Option errors](#option-errors) |
| 3 | For each argument branch on the path: branch permission, then each argument's parse → `validator` → `transformer`, or the default of an omitted optional argument | The permission message, the argument's `ArgumentParseException`, or the `CommandPrerequisiteException` of an `optional(extractor)` default |
| 4 | Prerequisites (`addPrerequisite`, `require(...)`, `requirePlayer()`, `requireConsole()`) | The `CommandPrerequisiteException` message |
| 5 | No executor on this node: the generated help is sent instead | (help text, not an error) |
| 6 | `cooldown(...)`, then `confirm(...)`, then the preprocesses (`addPreprocess`) | The cooldown message, the confirmation request, or the messages the preprocess sent before returning `false` |
| 7 | The executor | A `CommandPrerequisiteException` message, or the generic unexpected-error message |

## Results and failures

The runner returns the value the executor set with `ctx.setResult(int)` to Brigadier, `1` unless set, and `1` for the
generated help. `/execute store result` stores it.

A failure at any step above throws a `CommandSyntaxException` whose message joins the failure messages, one per line.
The platform reports it as it reports a vanilla command's error: the sender gets it in red, except a silent source such
as a function and each target of a forked `/execute as @a ... run`. `/execute store result` and `/execute store success`
both store `0`. A failure that sent no message, such as a preprocess returning `false` silently, only returns `0`,
since the platform would send an empty line for it; `store success` then stores `1`.

Messages the executor sent before throwing still reach the sender directly. `ctx.sendSuccess(...)` is vanilla command
feedback (see the README): a player does not get it while the `sendCommandFeedback` gamerule is off, and
`sendSuccess(message, true)` also goes to the other operators and, with `logAdminCommands`, the server log.

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
child is named or aliased `help`.

### Argument value rejected by CommandLib (`ArgumentParseException`)

The token is syntactically valid, but converting it to the Java value fails in the argument's `parseImpl`. Examples:
an unknown name for `EnumArgument`, `LiteralArgument`, `ObjectArgument`, `NameableObjectArgument`, `TeamArgument`,
`ObjectiveArgument`, `OfflinePlayerArgument`; an unknown key for `AdvancementArgument`, `LootTableArgument`,
`RecipeArgument`; an unknown registry key for Spigot's `SoundArgument`, `BiomeArgument`, `AttributeArgument`.

CommandLib uses two message shapes:

- `ArgumentParseException.ofIncorrectInput(...)` imitates the vanilla error: a red translatable
  `command.unknown.argument` ("Incorrect argument for command"), then a line with the input before the argument in
  gray (at most the last 10 characters, prefixed with `...`), the rejected token underlined in red, and the
  translatable `command.context.here` ("<--[HERE]") in red italics. Used by the enum, literal, object, nameable object,
  team, offline player, and Paper world arguments.
- A plain red message, such as `minecraft:foo is not a known advancement`, `foo is not a known objective`,
  `foo is not valid namespaced key`, `Steve is not found or not valid UUID`, `no player found`, `@x is invalid selector`.

The message is the same on every platform. The translatable parts are translated by the client.

If the argument has an `additionalParser(...)`, it gets the raw input first; the error is reported only when it returns
`null`.

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
  - `new ArgumentValidationException("message", "more lines"...)` sends each string in red.
  - `new ArgumentValidationException(ctx -> ...)` sends whatever the consumer sends.
  - `ArgumentValidationException.ofIncorrectInput(name, ctx, input)` sends the vanilla-style message.

```java
argument(new IntegerArgument("amount").validator((amount, ctx) -> {
    if (amount % 2 != 0) {
        throw new ArgumentValidationException("The amount must be even.");
    }
})).execute((amount, ctx) -> ctx.sendSuccess("ok " + amount));
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
2. **Execution.** The runner checks the command node and every argument branch on the path again before parsing them,
   and sends `You do not have permission to execute this command.` in red when the check fails. Normally the
   visibility check rejects the input first; this message appears when the check result changes between parsing and
   execution, or when the input was parsed for another source.

Non-player sources:

| Platform | Console | Command blocks, `/execute` as an entity | Data pack functions |
|---|---|---|---|
| Spigot | `CommandSender#hasPermission` of the console | `hasPermission` of the Bukkit sender | Always allowed while functions are compiled; the function permission level guards them |
| Paper | `CommandSender#hasPermission` | `hasPermission` of the Bukkit sender | Always allowed while functions are compiled |
| Forge / NeoForge | Always allowed | Allowed unless the source entity is a player | Always allowed |

### Prerequisite failed (`CommandPrerequisiteException`)

Thrown by a `Prerequisite` (`addPrerequisite`), an `Extractor` (`require(...)`), or by an executor written as
`execute(ctx -> ...)`. The message is sent in red and the command [fails](#results-and-failures).

| Source | Message |
|---|---|
| `new CommandPrerequisiteException()` | `You do not have permission to execute this command.` |
| `new CommandPrerequisiteException("message", "more lines"...)` | each string, in red |
| `new CommandPrerequisiteException(ctx -> ...)` | whatever the consumer sends |
| `requirePlayer()`, `Command.playerRequirer()` (Spigot, Paper) | `This command can only be executed by a player.` |
| `requireConsole()`, `Command.consoleRequirer()` (Spigot, Paper) | `This command can only be executed from the console.` |

Prerequisites of a parent command apply to its children unless the child calls `disableParentPrerequisite()`.

The typed executors (`execute((a, b, ctx) -> ...)`) take `java.util.function` style consumers that cannot throw
checked exceptions. Calling `ctx.sendFailure(...)` and returning sends the message but does not fail the command
(`store success` stores `1`). To fail it, use the `execute(ctx -> ...)` form and read the values with
`ctx.getArgument(argument)`.

### Preprocess cancelled

`addPreprocess(Predicate)` returning `false` makes the command fail with the messages the preprocess sent before
returning, and stops it without a message when it sent none. A preprocess that returns `true` has its messages sent
when it returns. Preprocesses run only when the node has an executor; the generated help skips them.

### Cooldown and confirmation

`cooldown(...)` and `confirm(...)` run as the first part of the preprocess step, so they too run only when the node has
an executor. A sender on cooldown gets `You must wait 1m 30s before using this command again.`, and a first execution
of a command with a confirmation gets `Run the command again within 10s to confirm. [Confirm]` in yellow, where
`[Confirm]` runs the command again when clicked. Both make the command [fail](#results-and-failures), so a function or
each target of a forked `/execute as @a ... run` does not get them. `Cooldown#message` and `Confirmation#message`
replace them. The cooldown starts only when the confirmation and the preprocesses pass. See the README for the API.

### Option errors

| Case | Reported by | Sender sees |
|---|---|---|
| Option after a regular argument (`/scan Steve -f`), unknown option token, or a value option without a value | Brigadier | Vanilla "Incorrect argument for command" or the argument type's error, with the context line |
| Invalid option value (`--limit abc`, out of the option's range) | Brigadier | The value type's vanilla error |
| Option of a mutually exclusive group given after another of the group (`mutuallyExclusive(...)`) | Brigadier | Vanilla "Incorrect argument for command", since the rest of the group is removed from the tree |
| `required()` option missing | CommandLib (`OptionParseException`) | `Missing required option --tag.` in red, or the help when no executor is at the end of the input |
| `requires(other)` not met | CommandLib (`OptionParseException`) | `--reason requires --force.` in red |
| `requires(other, value)` not met | CommandLib (`OptionParseException`) | `--threads requires --mode to be parallel.` in red |

See [options](options.md) for the option syntax.

### Unexpected exception

An exception thrown by the executor, other than `CommandPrerequisiteException`:

- its stack trace is printed to the server console;
- the sender gets `An unexpected error occurred trying to execute that command.` and `Check the console for details.`
  in red;
- every `UncaughtExceptionHandler` added with `addUncaughtExceptionHandler` on the command (and, for an argument node,
  on the argument) is called with the exception and the context;
- the command [fails](#results-and-failures) with those two lines.

An unchecked exception thrown outside the executor (in a parser, validator, transformer, prerequisite, or preprocess)
is printed, passed to the uncaught exception handlers, and rethrown to Brigadier. The platform then reports it with its
own generic error (vanilla: `An unexpected error occurred trying to execute that command`, with the exception message
on hover for operators).

An exception thrown by a suggestion provider is printed and rethrown; the client gets no suggestions for that request.

## Messages are English

The CommandLib-generated texts above (permission, prerequisite, unexpected error, option dependency, and the plain
argument messages) are fixed English strings. Only the vanilla-style "Incorrect argument for command" / "<--[HERE]"
parts and the vanilla messages passed through are translated by the client. Localization of CommandLib's own messages
is not implemented yet.

## In tests

`CommandTester#execute` in every testing artifact throws a `RuntimeException` whose cause is the
`CommandSyntaxException` when Brigadier rejects the input (unknown command, unknown subcommand, invalid syntax,
permission hidden node). Everything CommandLib reports itself is captured by `FakeSender#getSentMessages()` /
`getSentMessageTexts()`: a failure's messages are sent to the sender one by one, as they were sent, and `execute`
returns `0`. Otherwise `execute` returns the command's result. Translatable parts read as English text in `spigot-testing` and `modded-testing` and as
translation keys (`command.unknown.argument`, `command.context.here`) in `paper-testing`. See [testing](testing.md).
