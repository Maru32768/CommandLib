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

Two more shapes build on these:

| Factory or modifier             | Java type  | Notes                                       |
|---------------------------------|------------|---------------------------------------------|
| `count(name, max)`              | `Integer`  | Flag counted per occurrence; default `0`    |
| `valueOption.repeatable(max)`   | `List<T>`  | Values of up to `max` occurrences; `[]`     |

Further modifiers: `aliases(String...)` adds long names, `required()` makes a
missing option fail, and `CommonCommand#mutuallyExclusive(...)` allows at most
one option of a group. The user-facing reference is `docs/options.md`.

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

- `name` and `aliases`: long names, each used as a `--name` token. `name` is
  also the human-readable label. Names must be unquoted Brigadier words.
- `shortName`: optional single `Character`, used as `-x` token.
- `defaultValue`: returned by `getOption` when the option was not supplied.
- `type`: Brigadier `ArgumentType<?>` for value options; `null` for flags.
- `maxOccurrences`: how often the option may be given; `1` unless it was made
  by `count(...)` or `repeatable(...)`.
- `reader`: `BiFunction<C, List<String>, T>` that turns the internal argument
  names of the occurrences, in input order, into the value. A flag returns
  `true`, a count returns the number of occurrences, a value option reads the
  first name with its parser, and a repeatable option reads every name.
- `required` and `validators`: checked after parsing.

`hasValue()` returns `true` when `type != null`. `internalName(i)` is
`"__commandlib_option_" + name` for the first occurrence and appends `#n` for
the n-th, since Brigadier keeps one parsed value per argument name.

### `Options` (factory)

Stateless utility class. Each factory method creates a `CommandOption` with the
appropriate Brigadier `ArgumentType` and the matching `getBool`/`getInteger`/…
extractor as the parser function.

### `CommonCommand#option(...)` and `mutuallyExclusive(...)`

`option(...)` validates that no long name, alias or short name is used by
another option of the command, then adds the option to an internal
`List<CommandOption<?, C>>`. Duplicates throw `IllegalArgumentException`.

`mutuallyExclusive(...)` stores a group of registered options;
`excludes(a, b)` tells the node creator whether two options share a group.

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

The node creator tracks how often each option was used on the path so far. An
option remains available while it was used fewer than `maxOccurrences` times
and no option of one of its mutually exclusive groups was used. For each
available option these literal tokens are created:

- `--name` for the name and every alias
- `-x` if a short name exists
- `-xx`, `-xxx`, … up to the remaining occurrences, for a counted flag

For all pairs, triples, … of available no-value options with short names that
do not exclude each other, every permutation of combined tokens is created
(`-fv`, `-vf`, `-fvw`, `-fwv`, …). This is done by
`collectShortOptionPermutations`. The permutation approach lets Brigadier match
any combined flag regardless of character order without requiring a custom
parser.

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

A second `--limit` on the same path, when the option is repeatable, leads to
`__commandlib_option_limit#2`.

The internal name prefix `__commandlib_option_` prevents the argument node name
from appearing in `argumentNameToInputArgMap`, which is filtered in
`CommonCommandContext.collectInputs`.

#### Recursive combinations

`createOptionCommands` is called recursively with the occurrence counts of the
path. After matching `-f`, the subtree offers `--verbose`, `-v`, and any
remaining option tokens, but not `-f` or `--force` again unless the option is
repeatable. The nodes after a path depend only on those counts, so every path
with the same counts shares one list of child nodes (the argument node and the
remaining option tokens). The server tree then grows with the number of count
combinations, not with every order the options can be given in. Players still
receive the expanded tree, because the vanilla command packet copies a node
for every path, so the number of options and their maximums must stay small.

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

A loop back to the command node through a Brigadier redirect would allow
unbounded repetition, but the redirected context loses the arguments parsed
before it, which argument-child commands need. Repeated occurrences are
therefore bounded and unrolled.

## Execution Flow

`CommandRunner.parseOptions(ctx)` reads the already-matched Brigadier nodes
from `ctx.getHandle().getNodes()` in input order and collects the occurrences
of each option:

1. **Value options** — a node named `internalName(i)` of a value option is one
   occurrence of it.

2. **Flags** — any other node whose name starts with `-`. Long tokens are
   matched against a `--name` → `CommandOption` map that includes the aliases.
   Short tokens (`-x…`) iterate characters and match each against a
   `Character` → `CommandOption` map, so `-vvf` is two occurrences of `-v` and
   one of `-f`.

Each option with occurrences gets `ctx.setOptionValue(option, option.read(ctx,
names))`.

`validateOptions(ctx)` then checks every option: a required option that is
missing fails unless the runner has no executor (it shows help instead), and
the `requires(...)` constraints of a given option run. A violation throws
`OptionParseException`, which `CommandRunner` catches before parsing any
argument; it sends the messages as failures and returns without executing the
command body. Argument failures use `ArgumentParseException`.

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
- Each alias follows as `--alias <name>`.
- The `<name>` value tag is only emitted for value options (`hasValue() == true`).
- Notes follow the names in parentheses: `required` for a required option and
  `up to N times` when `maxOccurrences > 1`.
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
A required option keeps its default value but never returns it, and a
repeatable option ignores the element default and defaults to an empty list.

### Validation tied to presence

Constraints in `requires(...)` are only checked when the option was actually
supplied. This allows additive constraints ("if you pass `--reason` you must
also pass `--force`") without requiring callers to always supply both options or
think about which validation paths are active.

### Mutual exclusion in the tree, not at runtime

A mutually exclusive option is left out of the tree once another option of its
group was used, as an option that may not repeat is. The client then neither
suggests nor accepts it, which is consistent with the existing handling of a
repeated option, and the tree gets smaller. The cost is that the sender sees
Brigadier's generic "incorrect argument" message instead of a named conflict.

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
- Aliases parse and are suggested; duplicate names and aliases are rejected
- Required option missing fails with an option error; help still shows
- Count via `-vvv` and mixed tokens; occurrences above the maximum rejected
- Repeatable value option collects values in input order
- Mutually exclusive options rejected together, also in combined flags, and
  not suggested after one of them
