# Optional and Variadic Arguments

## Goal

Let a branch accept omitted trailing arguments and a repeated last argument without one branch per form, keep the
typed callbacks free of casts, and keep the Brigadier tree native so clients parse and suggest each token with the
argument's own type.

```java
argument(new StringArgument("item"), new IntegerArgument("count").optional(1)).execute((ctx, item, count) -> {});
argument(new PlayerArgument("target").optional(Command.player())).execute((ctx, target) -> {});
argument(new StringArgument("reason"), new PlayerArgument("targets").variadic()).execute((ctx, reason, targets) -> {});
```

## API

- `CommonArgument#optional(T)` and `optional(Extractor<C, ? extends T>)` mark the argument optional and return the
  argument itself, so the type stays `T` and the fluent chain continues. The extractor form computes the default per
  execution; a `CommandPrerequisiteException` it throws is sent to the sender like a prerequisite failure. The default is
  used as is: validators and transformers apply only to typed input. A default must not be null.
- `CommonArgument#variadic()` / `variadic(int maxCount)` return a new `VariadicArgument<T, C>`, which is a
  `CommonArgument<List<T>, C, ...>`. The existing `Unary`…`Hept` branches therefore type the callback parameter as
  `List<T>` without new branch classes. Settings made before `variadic()` apply to each value (validator, transformer,
  suggestions, parser); settings made on the returned argument apply to the list. `variadic().optional(List.of())`
  makes it optional.

Both are state on the argument, like its executor and validators, so one instance reused in several branches is
optional in all of them.

## Constraints

Checked when the command tree is built (registration), with an `IllegalStateException` naming the command and the
argument:

- A required argument cannot follow an optional one, so optional arguments end the branch.
- A variadic argument must be the last argument of its branch.
- A branch ending with an optional or variadic argument cannot have child commands. Children would only be reachable
  after the full argument list, which reads as a bug rather than a feature.
- A command cannot have both its own executor and a branch whose arguments are all optional, nor two such branches,
  since each would handle the bare command.

## Command Tree

Optional arguments add no nodes. Every argument node already executes; the runner of a node uses the branch executor
when every argument after the node is optional, and parsing fills an omitted optional argument with its default. A
branch whose arguments are all optional becomes the runner of the command literal itself (and of option-only input),
including its permission check.

Brigadier has no repetition, and a cycle of children breaks tree copying (`fillUsableCommands`, usage generation), while
a redirect loop hands the executor only the last child context. A variadic argument therefore repeats its node
`maxCount` times (16 by default) in a chain under the same name, each node executable. Clients see `<targets>` for each
value. The cost is `maxCount` nodes per branch in the tree sent to clients, multiplied by the option nodes that copy the
branch.

## Parsing Values

Brigadier keeps one `ParsedArgument` per name, so the context holds only the last value of the chain, while
`getNodes()` keeps every node with its range. `VariadicArgument` walks those nodes, parses each token again with the
element's `ArgumentType`, and lets the element parse it from a Brigadier context holding only that value. The element's
`parseImpl` reads by name as it always does, so every existing argument works as an element without changes. The
platform context's handle and the element's input are replaced only for that call, so validators still see earlier
arguments and options, and incorrect-input messages point at the failing token. `getInput(name)` of a variadic
argument returns all of its tokens.

Parsing a token again is cheap and depends only on the public `ArgumentType#parse(StringReader)`, not on how a platform
stores parsed values (Paper's API mirror, Spigot's NMS types, Forge's vanilla types).

## Help

Generated help shows `<name>` for required, `[name]` for optional, `<name...>` for variadic and `[name...]` for an
optional variadic argument. Permission nodes are unchanged: they join the argument names of the branch.

## Not Done

- An argument between optional ones (`<a> [b] <c>`) or after a variadic one; both make an input ambiguous.
- Defaults that run the validator, and an `Optional<T>` form without a default.
- A variadic argument without an upper bound.
