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
argument nodes. `OptionGraph` (built by `CommandNodeCreator` for each command)
generates them.

### Token Generation

The graph tracks how often each option was used on the path so far, the
*state*. An option remains available while it was used fewer than
`maxOccurrences` times and no option of one of its mutually exclusive groups
was used. For each available option these literal tokens are created:

- `--name` for the name and every alias
- `-x` if a short name exists
- `-xx`, `-xxx`, … up to the remaining occurrences, for a counted flag

For all pairs, triples, … of available no-value options with short names that
do not exclude each other, every permutation of combined tokens is created
(`-fv`, `-vf`, `-fvw`, `-fwv`, …). The permutation approach lets Brigadier match
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

Every option node has an `OptionNode` requirement, which lets everyone use the
node and names the command's graph, the options a literal gives, the option a
value node holds, and the state after the node. The runner identifies the
command's own option nodes by it, so the option tokens of a parent command on
the same path (an argument child such as `/config -f <key> set -f`) are not
counted for the child. Platforms keep a node's requirement object when they copy
or convert the node, as Paper's dispatcher mirror (`ApiMirrorRootNode`) does,
while the node object itself is replaced.

#### States and redirects

The nodes after a token depend only on the state it reaches, not on the order
in which the options were given. The graph is built depth first, children in
order. The first token that reaches a state holds that state's children, the
*hub* of the state; every other token that reaches the same state is built with
`redirect(hub)` and has no children of its own. A value token keeps its literal
and value node, and the value node redirects.

```
/scan
  <argument nodes>
  --force          (hub of {force})
    <argument nodes>
    --verbose      (hub of {force, verbose})
      <argument nodes>
    -v             → redirect to the hub of {force, verbose}
  -f               → redirect to the hub of {force}
  --verbose        (hub of {verbose})
    <argument nodes>
    --force        → redirect to the hub of {force, verbose}
    -f             → redirect to the hub of {force, verbose}
  -v               → redirect to the hub of {verbose}
  -fv, -vf         → redirect to the hub of {force, verbose}
```

The argument nodes of every branch are built once, merged as the command literal
would merge branches whose first arguments share a name, and added to every
state as the same node objects.

The server tree then has one node per token of each state, and so has what a
player receives. Vanilla's `Commands#sendCommands` copies the tree for each
player and copies a node once for every path that reaches it
(`fillUsableCommands`), but sends a redirect as a reference to the copy of its
target. Without redirects, a command with six short flags made 5,490 server
nodes and 491,607 copies for each player; with them it makes 5,490 copies.

The copy resolves a redirect through a map from server node to copied node,
filled as it goes, so a hub must be copied before any redirect to it. The graph
only redirects to hubs built earlier in its depth-first order, and the argument
nodes, which are first in each state, contain no option nodes of the command.
Every walk of the shared subtree, under the command literal, an alias, or a
`<plugin>:<label>` copy, therefore meets each hub first. The states form a DAG
(each token raises a count), so no redirect loops back.

#### Reading the options after a redirect

A redirect makes Brigadier parse the rest of the input in a new context, the
child of the context before it. Brigadier executes the last context of the
chain, which has no link back to its parents, so after `/scan -f -v Alex` the
runner's context holds only `Alex`. Suggestions get the first context of the
input, with the chain below it.

`OptionGraph#recover` restores the options at execution. The executed context's
root node is the hub of the state the options reached, which its `OptionNode`
requirement names. The options end at the space before the context, and the
graph parses them again from the input, from the empty state, as Brigadier
matched them: each token is a literal of the state up to the next space, and a
value literal is followed by its value, parsed with the option's argument type.
The start is one of the last `Σ maxOccurrences × (1 or 2)` token starts; the
earliest start that parses to the hub's state is taken, since the token before
the true start is the command's literal and no option token. The literals of the
command's path (its name or an alias, optionally with a namespace, and the
names of its parent commands) typed before the options are added back, so
`CommandContext#getInputs()` stays `[scan, Alex]`. The runner then works on a
`ChainedCommandContext` of the options and the executed context, which reads an
argument from the last part with an argument node of that name, since
Brigadier's context does not expose its argument map. Suggestions read the chain
from the first context as one `ChainedCommandContext` as well.

A command keeps the tree without redirects, in which the paths with the same
state share their children, when its options cannot be parsed again from the
input:

- an argument child (a command under an argument, such as `/config <key> set`),
  whose inherited arguments are parsed before its options and would be lost by
  the redirect too; parsing them again would need the platform's argument types
  and the original source;
- a command with a value option whose type is not one of Brigadier's primitives
  (`bool`, `integer`, `long`, `float`, `double`, and `string` other than greedy),
  since a platform type may parse differently on the server than in common.

The re-parse runs only when options were given, and only over the option
tokens.

#### Platforms

- Spigot and modded register the nodes in the vanilla dispatcher, which parses,
  executes and sends them as described above, also on 1.16.5, whose
  `fillUsableCommands` uses the same map.
- Paper converts the nodes into its vanilla dispatcher with
  `ApiMirrorRootNode#unwrapNode`, which caches each conversion, so the DAG and
  the redirects keep their shape. It drops a node's redirect modifier, which is
  why the recovery uses none. Paper's `fillUsableCommands` would flatten a
  redirect whose target was not copied yet; the order above avoids that.
- Velocity executes with its Brigadier fork, which parses redirects as Mojang's
  does and, when input ends at a redirect node, runs the target's command; a
  redirecting option token has the same runner as its hub. Its
  `CommandGraphInjector` copies each node once (`done` map) and copies the
  redirect target, so the client tree keeps the shape too.

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

Options after arguments would need option tokens under every argument node, and
the arguments of a branch can be optional or variadic, so the tree would grow
with the arguments as well.

## Execution Flow

`CommandRunner.run` first asks the option graph to recover the options before
the last redirect (see above). `parseOptions(ctx)` then reads the parsed nodes
from `ctx.getHandle().getNodes()` in input order and collects the occurrences of
each option from the nodes whose `OptionNode` requirement names the command's
graph: a literal gives its flags, once per occurrence (`-vv` is two occurrences
of `-v`, `-fv` one of each), and a value node gives the internal name of its value.

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
The tokens of each state still grow with the permutations (1,962 tokens for six
short flags before any is given), so the docs keep recommending few short flags.

### Redirects between option states

The option states share their nodes through redirects rather than by repeating
the subtree after every order of the options. Alternatives that were considered:

- **Shared children without redirects.** The server keeps one node per token of
  each state, but every platform copies the tree for each player per path, so the
  copy grows with the orders of the options (491,607 nodes for six short flags).
  Commands that cannot be read back after a redirect still use it.
- **One hub for all states.** A single node whose option tokens redirect to
  itself would make the tree linear in the number of tokens, but the tree would
  then accept an option more often than allowed and mutually exclusive options
  together, and keep suggesting them. Moving those checks to the runner would
  change Brigadier's errors into option errors and break the client's marking.
- **A greedy string or a custom argument type for the options.** A greedy tail
  would make the client stop highlighting and suggesting the positional
  arguments after the options. A custom argument type for combined flags would
  compete with a word argument for the same token, and needs a type the client
  knows on every platform, which Spigot's NMS and the modded loaders do not offer
  for library types.
- **A redirect modifier that passes the options on.** A modifier sees the context
  before the redirect and could store it for the runner, but Paper's dispatcher
  mirror drops redirect modifiers when it converts a literal node, and a stored
  context needs a side channel keyed by Brigadier's internal lists.

The cost of the redirects is the re-parse of the option tokens in the runner
when options are given. Brigadier also follows a redirect as soon as one
character follows the token, where it leaves the rest of other tokens unread, so
a trailing space after an option (`/scan -f `) makes it parse the empty rest
after the redirect. A word argument accepts the empty rest; the runner rejects
such an execution with Brigadier's "Incorrect argument" error, as Brigadier
rejected the unread space before. An argument that does not accept it, such as
an integer, leaves Brigadier with an empty context, which it reports as an
unknown command instead. Clients trim chat commands, and Bukkit's command map
and Velocity drop trailing spaces, so only input that reaches the dispatcher
unchanged sees the different message.

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
- Options before the last redirect read back in input order, also after an
  alias, a leading slash, a redirect before the command (`execute run`) and for
  a child command, with option values that look like option tokens
  (`OptionRedirectTest`)
- An argument child does not count the option tokens of its parent
- The tree size of short flags, counts and repeatable options, and the shared
  argument nodes (`OptionTreeSizeTest`)
