# Command Options Design

## Context

CommandLib argument declarations describe positional parameters — values that
appear in a fixed sequence after the command name. Some commands need optional
modifier flags that the caller may omit, providing defaults when absent. These
cannot be cleanly expressed as positional arguments.

This design note records how CommandLib models those optional modifiers, how the
public API is expected to behave, and which internal boundaries are intentional.

## Goals

- Support Unix-style optional flags and value options before positional
  arguments.
- Keep the caller-facing API type-safe: each option returns a typed value from
  the context.
- Allow a caller to distinguish "option was supplied" from "option was omitted
  but has a default value".
- Allow options to declare dependencies on other options or option values.
- Show options clearly in help output.

## Non-Goals

- Options are not inherited from parent commands. Each command declares its own
  option set.
- Option ordering relative to other options is not enforced; any order is valid.
- Options after positional arguments are not supported. Brigadier argument nodes
  follow option literal nodes in the tree, so an option token placed after an
  argument would not match.

## Public API Shape

### Declaring Options

Options are created through the `Options` factory and registered on a command
with `CommonCommand#option(...)`:

```java
public final class ScanCommand extends Command {
    public ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force =
            option(Options.flag("force", 'f').description("Force execution"));
        CommandOption<Integer, CommandContext> limit =
            option(Options.integer("limit", 'n', 10, 1, 100).description("Max count"));

        argument(new StringArgument("target")).execute((target, ctx) -> {
            boolean f = ctx.getOption(force);   // true if -f or --force was given
            int n     = ctx.getOption(limit);   // supplied value or default 10
        });
    }
}
```

`option(...)` returns the typed `CommandOption<T, C>` so the variable can be
closed over in the executor lambda.

### Option Types

`Options` provides factory methods for each supported primitive type:

| Factory                         | Java type  | Notes                            |
|---------------------------------|------------|----------------------------------|
| `flag(name)`                    | `Boolean`  | No value token; default `false`  |
| `flag(name, shortName)`         | `Boolean`  | Same with single-char alias      |
| `bool(name, default)`           | `Boolean`  | Explicit `true`/`false` value    |
| `integer(name, default)`        | `Integer`  | Optional min/max range           |
| `longValue(name, default)`      | `Long`     |                                  |
| `floatValue(name, default)`     | `Float`    |                                  |
| `doubleValue(name, default)`    | `Double`   |                                  |
| `string(name, default)`         | `String`   | Single-word Brigadier string     |

Every factory has an overload that also takes a `char shortName` as a second
parameter.

### Command Syntax

Options must be placed before positional arguments. The caller may use either
the long form or the short form:

```
/scan --force --limit 20 alex
/scan -f -n 20 alex
```

Multiple no-value flags may be combined under a single short token:

```
/scan -fv alex       # same as -f -v alex
/scan -vf alex       # same as -v -f alex
```

Combining short names is only valid for flags (no-value options). Value options
always stand alone because they must be followed by their value token:

```
/scan -n 20 -f alex
```

### Reading Options from Context

```java
final class Example {
    <T> void read(CommandContext ctx, CommandOption<T, CommandContext> option) {
        T   value   = ctx.getOption(option);    // value or default if absent
        boolean has = ctx.hasOption(option);    // true only if explicitly supplied
    }
}
```

`hasOption` returns `false` when the option was omitted, even if the omitted
default value happens to equal an explicitly supplied value. Use `hasOption` when
the distinction matters (e.g. "did the caller ask for verbose output?").

### Option Constraints

An option can declare that it requires another option to be present:

```java
public final class ExportCommand extends Command {
    public ExportCommand() {
        super("export");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
        CommandOption<String, CommandContext> reason =
            option(Options.string("reason", 'r', "").requires(force));
    }
}
```

If `--reason` is supplied without `--force`, the command sends a failure message
and stops:

```
--reason requires --force.
```

An option can also require another option to hold a specific value:

```java
public final class ExportCommand extends Command {
    public ExportCommand() {
        super("export");

        CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
        CommandOption<Integer, CommandContext> limit =
            option(Options.integer("limit", 'n', 10).requires(mode, "parallel"));
    }
}
```

Failure message:

```
--limit requires --mode to be parallel.
```

A custom predicate overload is also available for conditions that cannot be
expressed as a single equality check:

```java
public final class ExportCommand extends Command {
    public ExportCommand() {
        super("export");

        CommandOption<Integer, CommandContext> threads = option(Options.integer("threads", 't', 0));
        CommandOption<Boolean, CommandContext> parallel =
            option(Options.flag("parallel", 'p').requires(threads, value -> value > 0, "positive"));
    }
}
```

Constraints are only checked when the option itself was supplied. An option
that was omitted never triggers its constraint, even if the required peer is
also absent.

### Option Descriptions

```java
public final class ScanCommand extends Command {
    public ScanCommand() {
        super("scan");

        option(Options.flag("force", 'f').description("Force execution"));
        option(Options.integer("limit", 'n', 10).description("Maximum count"));
    }
}
```

Descriptions appear in the help output under the `Options:` section.

## Internal Model

### `CommandOption<T, C>`

Stores:

- `name`: long name, used as `--name` token and as a human-readable label.
- `shortName`: optional single `Character`, used as `-x` token.
- `defaultValue`: returned by `getOption` when the option was not supplied.
- `type`: Brigadier `ArgumentType<?>` for value options; `null` for flags.
- `parser`: `BiFunction<C, String, T>` that reads the value from the Brigadier
  context when the option was supplied.
- `internalName`: `"__commandlib_option_" + name`; used as the Brigadier node
  name for the value argument to avoid collisions with user argument names.
- `validators`: accumulated `requires(...)` constraints.

`hasValue()` returns `true` when `type != null`.

### `Options` (factory)

Stateless utility class. Each factory method creates a `CommandOption` with the
appropriate Brigadier `ArgumentType` and the matching `getBool`/`getInteger`/…
extractor as the parser function.

### `CommonCommand#option(...)`

Validates uniqueness of `name` and `shortName` within the command, then adds the
option to an internal `List<CommandOption<?, C>>`. Duplicate names or short names
throw `IllegalArgumentException`.

### `CommonCommandContext` — option storage

```java
public abstract class CommonCommandContext<S, C> {
    private final Map<CommandOption<?, ?>, Object> optionValues = new LinkedHashMap<>(); // explicit values
    private final Set<CommandOption<?, ?>> presentOptions = new LinkedHashSet<>();       // options that were supplied
}
```

`setOptionValue(option, value)` writes to both structures. `getOption` reads
`optionValues` (falling back to `defaultValue`). `hasOption` tests
`presentOptions`.

## Brigadier Integration

Options are realized as literal `CommandNode` branches placed before the
argument nodes. `CommandNodeCreator.createOptionCommands(...)` generates them.

### Token Generation

For each remaining (not yet selected) option, two literal tokens are created:

- `--name` (always)
- `-x` if a short name exists

For all pairs, triples, … of no-value options with short names, every
permutation of combined tokens is created (`-fv`, `-vf`, `-fvw`, `-fwv`, …).
This is done by `collectShortOptionPermutations`. The permutation approach lets
Brigadier match any combined flag regardless of character order without requiring
a custom parser.

### Node Structure

#### Flag option

```
--force                      → literal node, executes directly
  <argument nodes>
  <remaining option tokens>
```

#### Value option

```
--limit                      → literal node, no direct executor
  __commandlib_option_limit  → required argument node (IntegerArgumentType), executes
    <argument nodes>
    <remaining option tokens>
```

The internal name prefix `__commandlib_option_` prevents the argument node name
from appearing in `argumentNameToInputArgMap`, which is filtered in
`CommonCommandContext.collectInputs`.

#### Recursive combinations

`createOptionCommands` is called recursively with a `selectedOptions` set that
grows on each descent. This means that after matching `-f`, the subtree offers
`--verbose`, `-v`, and any remaining option tokens, but not `-f` or `--force`
again. The result is a combinatorial tree that accepts any subset of options in
any order.

### Why Options Precede Arguments

Brigadier resolves a path through the tree by matching one node at a time from
left to right. Because option literals branch from the command literal node
(before argument nodes), Brigadier will only match them if they appear before the
first argument token. Placing an option token after an argument token means
Brigadier has already descended into the argument branch, where option literals
do not exist.

This constraint is surfaced in tests:

```java
class CommandOptionTest {
    @Test
    void option_after_argument_is_rejected() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target", CommonStringArgument.Type.WORD)).execute((target, ctx) -> {
            });
        }});

        // Throws — option after argument is not a valid path
        assertThatThrownBy(() -> runner.execute("scan Alex -f")).isInstanceOf(CommandSyntaxException.class);
    }
}
```

## Execution Flow

`CommandRunner.parseOptions(ctx)` reads the already-matched Brigadier nodes
from `ctx.getHandle().getNodes()`:

1. **Flag options** — any node whose name starts with `-` but is not an internal
   name. Long tokens are matched against a `--name` → `CommandOption` map.
   Short tokens (`-x…`) iterate characters and match each against a `Character`
   → `CommandOption` map. For each matched no-value option,
   `ctx.setOptionValue(option, true)` is called.

2. **Value options** — any node whose name starts with
   `__commandlib_option_`. The matching option is found, and `option.parse(ctx)`
   calls the extractor function to read the value from the Brigadier context.

`validateOptions(ctx)` runs all `CommandOption.validate(ctx)` calls after
parsing. A constraint violation throws `ArgumentParseException` which is caught
by `CommandRunner.run`, sends the error message to the caller, and returns
without executing the command body.

## Help Output

`HelpMessageAction` produces option information in two places:

**Usage lines** — any argument usage line is prefixed with `[options]` when the
command has at least one option:

```
/scan [options] <target>
```

**Options section** — listed after the argument usage lines:

```
Options:
  -f, --force: Force execution
  -n <limit>, --limit <limit>: Maximum count
```

Format rules:

- If a short name exists: `-x <name>, --name <name>: description`
- If no short name:       `--name: description`
- The `<name>` value tag is only emitted for value options (`hasValue() == true`).
- If the description is empty, the colon and description are omitted.

## Design Decisions

### Per-command scope

Options are not inherited by child commands. A child command is a separate
command in the Brigadier tree and defines its own option set after its own
literal node. This keeps option visibility unambiguous: the options listed on a
command are exactly the ones accepted by that command, no more.

### Default value is always required

Every option must have a default value. This avoids `null` returns from
`getOption` and keeps callers from needing null checks. Use `hasOption` when
distinguishing "supplied with value X" from "omitted, defaulted to X" matters.

### Validation tied to presence

Constraints in `requires(...)` are only checked when the option was actually
supplied. This allows additive constraints ("if you pass `--reason` you must
also pass `--force`") without requiring callers to always supply both options or
think about which validation paths are active.

### Permutation-based combination matching

Generating all short-flag permutations is O(n!) in the worst case, but in
practice the number of no-value options with short names on a single command
is small (typically ≤ 4). The permutation approach requires no custom Brigadier
parsing and keeps the matching behavior consistent with single-option tokens.

### `hasValue` driven by `ArgumentType` presence

A `CommandOption` is a flag when `type == null` and a value option otherwise.
This single field drives the Brigadier node shape (literal vs. literal +
argument), the flag-combination logic (only no-value options can combine), and
the help format (value tag presence). Adding a new value type requires only a
new factory method in `Options`.

## Tests

Unit tests for options live in `common/src/test` (platform-independent) and
integration tests in `integration-test`.

Key cases:

- Flag present via short name, long name, combined short name
- Flag absent — `getOption` returns `false`, `hasOption` returns `false`
- Value option present via short name, long name
- Value option absent — `getOption` returns default, `hasOption` returns `false`
- `hasOption` distinguishes supplied-at-default from absent
- `requires(option)` passes and fails
- `requires(option, value)` passes and fails
- `requires(option, value)` uses default value of the required option when that
  option was omitted
- Value option without value token rejected (parse failure)
- Option after argument rejected
- Child command uses its own options, not parent options
- Help output includes `[options]` placeholder and `Options:` section
