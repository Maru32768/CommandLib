# Argument Child Command Design

## Context

CommandLib originally modeled command trees as either command children or a
linear argument chain:

```text
/root
/root child
/root child <argument>
/root <argument>
/root <argument> <argument>
```

Minecraft's Brigadier tree also allows a literal command node under an argument
node:

```text
/root <argument> child
/root <argument> child <argument>
```

This design note records how CommandLib represents that shape, how the fluent
API is expected to behave, and which internal boundaries are intentional.

## Goals

- Allow an `Argument` node to have one or more child `Command` nodes.
- Keep the user-facing API chainable:

  ```java
  argument(n, p)
      .description("...")
      .execute((nValue, pValue, ctx) -> {
      })
      .child((n, p) -> new Command("sub") {{
      }});
  ```

- Let child commands read parent arguments in a typed way:

  ```java
  Integer parsedN = n.get();
  ```

- Keep internal tree storage types out of the public API where possible.
- Preserve the existing command/argument execution behavior for literal-first
  commands.

## Non-Goals

- This is not intended to make argument-first command design the default
  recommendation. Literal-first command shapes are still easier to read for most
  plugin commands.
- This does not introduce a separate Brigadier abstraction layer. The existing
  `CommonCommand`, `Arguments`, and `CommandNodeCreator` model remains the source
  of truth.
- This does not require a raw-input parse fallback for parent arguments.
  Brigadier can resolve parent arguments in argument-child trees after
  `CommandExecutor` rebuilds the root context.

## Public API Shape

`CommonCommand#argument(...)` returns an `ArgumentBranch` object. The branch is a
configuration handle for the argument chain that was just registered.

Typed overloads return arity-specific branch classes:

- `UnaryArgumentBranch`
- `BiArgumentBranch`
- `TriArgumentBranch`
- `TetraArgumentBranch`
- `QuintArgumentBranch`
- `HexaArgumentBranch`
- `HeptArgumentBranch`

These classes exist so the branch can expose typed `execute(...)` and typed
child factories:

```java
argument(new IntegerArgument("n"), new PlayerArgument("p"))
    .execute((parsedN, parsedP, ctx) -> {
    })
    .child((n, p) -> new Command("sub") {{
        execute(ctx -> {
            Integer parsedN = n.get();
            Player parsedP = p.get();
        });
    }});
```

The child factory receives an `ArgRef<T>` per parent argument, not parsed
values. Parsed values do not exist when the command tree is being constructed;
they only exist while a command callback runs. See
[Parent Argument References](#parent-argument-references).

## Why `child` Instead of `command`

The method is named `child(...)` because this API mutates the command tree by
adding a child node under the current argument branch. It also matches the
existing `addChildren(...)` and `children()` vocabulary.

`command(...)` was considered, but it is less precise in this API because
`Command` is both a type and a root/subcommand concept. `child(...)` reads as a
tree operation and keeps the DSL aligned with the rest of CommandLib.

## Internal Model

### `Arguments`

`Arguments` represents one argument chain owned by a command. It stores:

- the ordered `CommonArgument` list
- child commands attached to the terminal argument node
- an optional help description for the chain

For example:

```text
/a <n> <p> sub
```

is represented as:

```text
Command: a
  Arguments: <n> <p>
    Children:
      Command: sub
```

`Arguments` is intentionally package-private. It is an internal storage type,
not a user-facing API.

### `ArgumentBranch`

`ArgumentBranch` is the public configuration handle returned from
`argument(...)`.

It delegates the actual mutation to an `ArgumentBranchDelegate`:

```java
description(...)

execute(...)

child(...)

children(...)
```

The branch does not directly store `Arguments`. This keeps `Arguments` from
becoming public just because branch classes live in their own package.

### `ArgumentBranchDelegate`

`ArgumentBranchDelegate` is a narrow bridge from the public `branch` package back
to the internal `CommonCommand`/`Arguments` model.

It has only the operations needed by `ArgumentBranch`:

```java
void description(String description);

void execute(CommandHandler<C> action);

void addChildren(Collection<? extends T> children);
```

`CommonCommand` creates the delegate for a specific `Arguments` instance. This
keeps the package split clean:

- `net.kunmc.lab.commandlib.branch`: public branch API
- `net.kunmc.lab.commandlib`: internal command model and tree mutation

## Child Addition Flow

For a typed branch:

```java
argument(n, p).child((nRef, pRef) -> new Command("sub") {{
}});
```

the flow is:

1. `CommonCommand#argument(n, p)` creates and stores an `Arguments` instance.
2. `CommonCommand` creates an `ArgumentBranchDelegate` bound to that
   `Arguments`.
3. `BiArgumentBranch` stores the two `CommonArgument` instances.
4. `BiArgumentBranch#child(...)` wraps those argument instances in `ArgRef`
   and calls the factory with them.
5. The produced command is passed to `ArgumentBranch#child(...)`.
6. `ArgumentBranch` calls `delegate.addChildren(...)`.
7. `CommonCommand#addArgumentChildren(...)` validates the children, stores them
   in `Arguments`, and assigns parent metadata.

The parent metadata is important. An argument-child command has both:

- `parent`: the owning command
- `parentArguments`: the argument chain under which it was attached

Without `parentArguments`, help generation cannot reconstruct:

```text
/a <n> <p> sub
```

from the `sub` command alone.

## Multiple Children

The same argument branch can receive multiple children:

```java
argument(n, p)
    .child((nRef, pRef) -> new Command("sub") {{
    }})
    .child((nRef, pRef) -> new Command("sub2") {{
    }});
```

Internally this appends both commands to the same `Arguments#children` list:

```text
Arguments: <n> <p>
  Children:
    sub
    sub2
```

The resulting Brigadier tree is:

```text
a
  <n>
    <p>
      sub
      sub2
```

## Brigadier Node Creation

`CommandNodeCreator` converts the internal model into Brigadier nodes.

When an argument chain has children, the child command nodes are attached to the
terminal argument node:

```text
a
  <n>
    <p>
      sub
```

The key detail is that child commands inherit the argument chain that was parsed
before the literal child:

```java
List<Arguments<C>> executorArguments = appendArgument(inheritedArguments, arguments);
```

That inherited list is passed to child command creation so child executors parse
both:

- parent argument chains
- their own argument chains

This is what allows:

```text
/config difficulty set hard
```

to parse both `difficulty` and `hard`.

## Execution and Argument Parsing

`CommandExecutor` parses a list of `Arguments`, not just a single chain. That is
required because an argument-child command may need to parse parent arguments and
child arguments.

For argument-child commands, `CommandExecutor` rebuilds the Brigadier context
from the root before creating the CommandLib context. This is important because
Brigadier may split command execution context across parent/child command
contexts. Rebuilding from the root gives CommandLib a context that can resolve
the full executed command path.

Because of that, parent arguments can be read through the normal argument
implementation:

```java
Integer parsedN = n.get();
```

No raw-input fallback is required for the current implementation. If a future
change removes or changes root context rebuilding, argument-child parsing should
be revalidated with tests before adding any fallback behavior.

## Parent Argument References

Child factories receive `ArgRef<T>` rather than the parent `CommonArgument` or
its value:

```java
argument(new StringArgument("key")).child(key -> new Command("get") {{
    execute(ctx -> ctx.sendMessage("get:" + key.get()));
}});
```

`ArgRef` is a thin handle around the parent `CommonArgument`:

- `get(ctx)` reads the value through `ctx.getArgument(argument)`.
- `get()` reads the value from the command context currently being processed on
  this thread.

The current context is tracked by the package-private `CurrentCommandContext`,
a `ThreadLocal` stack. Callers never push or pop directly; they wrap the work in
`CurrentCommandContext.call(ctx, supplier)` or `run(ctx, runnable)`, which
restore the stack in `finally`. This is used around:

- `CommandRunner#run`: permission checks, option/argument parsing,
  prerequisites, preprocess, executors, and help fallback
- suggestion handling in `ArgumentCommandNodeCreator`: both the argument parsing
  that pre-populates the context (validators, transformers) and the suggestion
  actions
- the generated `help` literal node

A stack is used because a callback may dispatch another command on the same
thread. The `ThreadLocal` is removed when the stack becomes empty.

Calling `get()` outside of those callbacks (for example from a task scheduled by
an executor, or from work completed asynchronously through
`SuggestionBuilder#await`) throws `IllegalStateException` naming the argument.
Such code should resolve the value inside the callback or use `get(ctx)`.

### Suggestions

`ctx.getArgument(...)` only sees arguments stored by `Arguments#parse`. Suggestion
handling therefore parses every chain in `executorArguments` (inherited parent
chains first, then the current chain), not just the current chain. Each chain is
parsed best-effort in its own `try` so one failing conversion does not hide the
others; `Arguments#parse` stops at the first argument without input, so partially
typed chains are fine. This makes parent values readable from a child's
suggestion actions, matching execution behavior.

## Help Generation

Help output must include the full usage path for argument-child commands.

For root help:

```text
/a <n> <p> sub <b>
/a <n> <p> sub <float> sub
```

For help triggered inside `sub`, the prefix still needs to include the parent
arguments:

```text
/a <n> <p> sub <b>
```

`parentArguments` is used to reconstruct these prefixes. Help generation also
keeps normal children and argument children in one usage list; separating them
would imply a semantic distinction that does not exist in the command tree.

Descriptions are emitted in two places:

- command description before `Usage:`
- argument branch description after the usage line

Snapshot tests intentionally keep color codes because color is part of the
user-facing help contract.

## Tests

The common test suite should cover the shared behavior. Important cases:

- argument chain can have a child command
- argument-child command can have its own arguments
- child command can read parent arguments after child arguments are parsed
- the same argument branch can have multiple children
- help snapshots for:
    - root command input
    - parent arguments typed
    - child literal typed
    - child argument typed
    - command descriptions

Avoid tests that simulate impossible Brigadier behavior by making a custom
argument always fail. If a test exists to justify a parsing fallback, it should
use a real Brigadier command tree shape that fails without that fallback.

## Design Constraints

- Keep `Arguments` package-private unless there is a strong reason to expose it.
- Keep branch classes in `net.kunmc.lab.commandlib.branch` to avoid crowding the
  root package.
- Prefer arity-prefix names (`BiArgumentBranch`, `TriArgumentBranch`) over
  numbered names (`ArgumentBranch2`, `ArgumentBranch3`).
- Keep comments focused on why tree inheritance, help prefix reconstruction, or
  context rebuilding is needed.

## Open Questions

- Whether `ArgumentBranchDelegate` should remain public. It is public because
  `ArgumentBranch` has a public constructor, but it is not intended for typical
  library users.
- Whether `childCommand(...)` should be added as an alias for readability. The
  current recommendation is to keep only `child(...)` unless user confusion
  appears in real usage.
- Whether help ordering should remain insertion order or use a more explicit
  ordering policy. Current behavior follows insertion order where possible.
