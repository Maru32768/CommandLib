# Help, Usage and Error Formatting

CommandLib generates a help message for every command and reports its own failures with fixed messages. Three hooks
replace how they look: a **help formatter** for the whole help, a **usage formatter** for each usage line, and an
**error formatter** for each failure CommandLib reports. Each is set on a command and applies to its argument branches
and to the child commands that set none of their own, as [captions](localization.md) do; set them on each top-level
command to use them for a whole plugin.

## The generated help

The help is sent where the input ends at a command or argument branch without an executor (`/arena`, or
`/msg Steve` before the text) and for `/<command> help`, after the permission, option, argument and prerequisite
checks (see [errors](errors.md#missing-argument)):

```
--------------------------------------------------
Manage arenas
Usage:
  /arena [options] create: Create an arena
  /arena [options] delete
  /arena [options] <arena> join: Join the arena
  /arena [options] <arena>: Show an arena

Options:
  -f, --force: Replace an existing arena

Examples:
  /arena create lobby 16
--------------------------------------------------
```

It lists, in this order:

1. the child commands;
2. the routes through the command's argument branches to their child commands, down to each route's end;
3. the command's own argument branches;
4. a line with only `[options]` when the command has options but none of the above.

Only the routes the sender has every permission for are listed (see [permissions](permissions.md)). An argument shows
as `<name>`, an optional one as `[name]`, a variadic one as `<name...>`. The headings and notes (`Usage:`, `Options:`,
`Examples:`, `options`, `required`, `up to {count} times`) are [captions](localization.md#built-in-captions).

### Examples

`examples(...)` adds whole inputs that the command's help lists under `Examples:`:

```java
public final class ArenaCommand extends Command {
    public ArenaCommand() {
        super("arena");
        examples("/arena create lobby 16", "/arena lobby join");
        // ...
    }
}
```

A leading `/` is added when an example has none. The examples belong to the command they are set on; its children
list their own. [`CommandDocs`](diagnostics.md#command-documentation) lists them too.

## Help formatter

`helpFormatter(HelpFormatter)` replaces the whole help. The formatter gets the context and a `CommandHelp`, the help as
the sender would see it, and sends what it wants:

```java
helpFormatter((ctx, help) -> {
    ctx.sendMessage(RichText.text(help.description()).color(0xFFAA00));
    for (CommandHelp.Usage usage : help.usages()) {
        // The line as the usage formatter made it, or build one from the parts.
        ctx.sendMessage(RichText.text(usage.plainText())
                                .click(ClickAction.suggestCommand(usage.plainText()))
                                .hover(RichText.text(usage.description())));
    }
    help.examples().forEach(example -> ctx.sendMessage("e.g. " + example));
});
```

| `CommandHelp` | Holds |
|---|---|
| `path()` | The tokens from the root to this command: the root literal as the sender typed it, so an alias stays, the child literals, and before a command that follows an argument branch, that branch's arguments (`arena <arena> join`) |
| `description()` | The command's description for the sender, empty without one |
| `usages()` | The usage lines in the order above, filtered by the sender's permissions |
| `options()` | The command's `CommandOption`s: `names()`, `shortName()`, `hasValue()`, `isRequired()`, `maxOccurrences()`, `description()` |
| `examples()` | The examples set with `examples(...)` |

| `CommandHelp.Usage` | Holds |
|---|---|
| `path()` | The path of the command, as above |
| `tokens()` | The tokens after it: `[options]` when the command has options, argument tags and child literals |
| `description()` | The description of the route's command or argument branch |
| `line()` | The line as the command's usage formatter formats it |
| `plainText()` | The usage without colors and description, such as `/arena create [options] <name> [size]` |

A `CommandHelp.Token` has a `kind()` (`LITERAL`, `ARGUMENT` or `OPTIONS`), a `name()`, `optional()` and `variadic()`,
and `plainText()` gives it as usages show it.

The formatter's context is the command's, so `ctx.caption(...)`, the sender and the platform's own messages (a Paper
`Component`, a Forge `Component`) are there. `HelpFormatter.DEFAULT` is CommandLib's help; a formatter can send its own
lines before or after calling it.

## Usage formatter

`usageFormatter(UsageFormatter)` formats each usage line, both in the default help and in
[syntax advice](#syntax-advice). It returns a text, which may hold `§` color codes:

```java
usageFormatter((ctx, usage) -> "§e" + usage.plainText()
        + (usage.description().isEmpty() ? "" : " §7- " + usage.description()));
```

`UsageFormatter.DEFAULT` is the line of the default help: the usage in colors, indented by two spaces, then the
description after a colon.

## Error formatter

`errorFormatter(ErrorFormatter)` decides what the sender gets for each failure CommandLib reports itself. It gets the
context and a `CommandError`:

| `CommandError` | Holds |
|---|---|
| `kind()` | What failed, see below |
| `exception()` | The exception that carries the failure, null for `PERMISSION_DENIED` |
| `usage()` | For an argument error, the usage of the argument branch that failed |
| `sendDefault()` | Sends the messages CommandLib sends for it by default |

| `CommandError.Kind` | When | Default message |
|---|---|---|
| `PERMISSION_DENIED` | The runner's own check of the command or an argument branch on the path fails | `You do not have permission to execute this command.` |
| `OPTION` | A required option is missing or an option dependency is not met | The `OptionParseException` message |
| `ARGUMENT` | An argument's value is rejected while converting it | The `ArgumentParseException` message |
| `VALIDATION` | A validator rejects an argument's value | The `ArgumentValidationException` message |
| `PREREQUISITE` | A prerequisite, an extractor, a middleware, the executor or the default value of an omitted optional argument throws `CommandPrerequisiteException` | Its message |
| `TIMEOUT` | The executor's stage fails with a `TimeoutException` | `The command timed out.` |
| `UNEXPECTED` | Any other exception of a middleware, the executor or the help | `An unexpected error occurred trying to execute that command` and `Check the console for details.` |

```java
errorFormatter((ctx, error) -> {
    if (error.kind() == CommandError.Kind.PERMISSION_DENIED) {
        ctx.sendFailure("Ask a moderator for access.");
        return;
    }
    ctx.sendFailure("[Arena] The command failed:");
    error.sendDefault();
});
```

What the formatter sends is the failure's message, as `ctx.sendFailure` in a failing command is: the platform reports
it in red, a silent source does not get it, and `/execute store success` stores 0. A formatter that sends nothing makes
the failure only return 0. The stack trace of an unexpected exception is printed and the `UncaughtExceptionHandler`s
are called whatever the formatter does. A failure of an async command after the dispatch returned also goes through it,
and its messages go to the sender directly.

Not every error reaches it:

- **Brigadier's errors**, such as an unknown command or subcommand, trailing input, an integer out of range or a
  malformed selector, are reported by the platform before CommandLib runs (see [errors](errors.md#who-reports-an-error)).
  CommandLib never sees them, so neither the error formatter nor syntax advice can change them.
- **The cooldown and confirmation messages and a preprocess's messages** have their own hooks: `Cooldown#message`,
  `Confirmation#message` and the messages the preprocess sends.
- **A missing argument** is not an error: the help is sent, through the help formatter.

### Syntax advice

`ErrorFormatter.SYNTAX_ADVICE` sends the default messages and, after an `ARGUMENT` or `VALIDATION` error, the
`Usage:` heading and the usage line of the argument branch whose argument failed:

```java
errorFormatter(ErrorFormatter.SYNTAX_ADVICE);
```

```
Incorrect argument for command
...rena mode foo<--[HERE]
Usage:
  /arena mode [options] <mode> <rounds>
```

The line is that branch's, the closest to what the sender typed, and the sender has its permission, since the runner
checked it before parsing. It is not the default, so that CommandLib's errors keep the vanilla shape unless a plugin
asks for it. Errors Brigadier reports itself get no advice, as above.

## In tests

The testers run the formatters as a server does. `FakeSender#getSentMessageTexts()` has the texts the help formatter
sent and, after a failure, those of the error formatter. See [testing](testing.md).
