# Annotation API

## Goal

Let a command be written as a class with annotated methods, as in Lamp, ACF or cloud-annotations, without a second
command model: an annotated command and the same command written fluently must give the same help, permission nodes,
options, suggestions, execution and tests. User documentation: [annotations.md](../annotations.md).

## Approach: a translator onto the fluent API

`AnnotationParser<C, T>` in `common` reads a class and calls the public fluent API of `CommonCommand`:
`new Command(name)`, `addAliases`, `addChildren`, `option`, `argument(List)`, `execute`/`executeAsync`,
`permission`, `description`, `confirm`, `cooldown`, `child`, `addMiddleware`, `visibleWhen`, `mutuallyExclusiveOptions` and
the others named below. The result is an ordinary platform `Command`; nothing below it knows
it came from annotations. That gives the equivalence by construction: the command tree, the help, the permission
configs and the docs exports are computed from the same objects as for a fluent command, and every rule of the core
(optional arguments last, a variadic argument last, unique option names, command name validation) is checked by the
same code at the same time.

The parser uses the list form `argument(List<CommonArgument<?, C, ?>>)`, which takes any number of arguments, and an
`AsyncCommandExecutor` that reads each value with `ctx.getArgument(argument)` / `ctx.getOption(option)` and invokes
the method. The typed `Unary`…`Hept` branches are not needed, so methods are not limited to seven arguments.

`T` is the platform command and `C` its context; the parser is created with the context class and a command factory
(`Command::new`). Each platform has an `AnnotatedCommands` class (Spigot, Paper, Velocity, and the Forge / NeoForge /
Fabric nodes) that returns a parser with the platform's argument types and sender types registered. The annotations
themselves are platform independent. The parser matches a parameter by its raw type, so on 1.20.5+ the modded parser
registers `Holder` once and picks the enchantment, mob effect or attribute argument from the holder's type argument.

## Reflection, not an annotation processor

The class is read with reflection when `parse` is called, which is at plugin enable, before registration.

- An annotation processor would give the source order of methods, compile-time errors and no reflection at runtime, but
  needs a processor artifact, IDE and build configuration, generated sources per platform, and cannot see the argument
  types a plugin registers at runtime.
- Reflection runs once per command at startup, so its cost is negligible next to building the Brigadier tree, and it
  sees the parser's registries, so custom types and annotations need no build step.
- A signature the parser cannot map, or annotations that contradict each other, fail at `parse` with an
  `IllegalArgumentException` naming the class, the method and the parameter. The constraints of the arguments
  themselves, such as optional arguments being last, a variadic argument being the very last and the placement of a
  greedy string, are the core's: they fail when the command is registered, as for a fluent command. Both happen at
  startup, so the failure mode does not change.

Consequences of reflection:

- **Order.** `getDeclaredMethods()` has no defined order, and help lists children and branches in the order they are
  added. The parser sorts methods by subcommand path, then name, then parameter count, so the output is deterministic.
  The fluent equivalent of an annotated command adds its children in that order.
- **Parameter names.** Java keeps them only with `-parameters`, and Kotlin with `-java-parameters`. The parser uses `Parameter#getName()` when
  `isNamePresent()`, otherwise the parameter needs `@Name`, and a missing name is a parse error rather than a silent
  `arg0`. Options take their name from the annotation, falling back to the parameter name.
- **Inheritance.** Methods of the superclasses and the interfaces are read too. An overridden method counts once, with
  the annotations of its most derived declaration that has them, and a call runs the override as usual.
- **Access.** Methods are made accessible with `setAccessible`, so they may be private. Plugins run on the class path,
  where this works for their own classes; a method that cannot be opened, such as an inherited JDK method named by
  `@Suggest(method = ...)`, fails parsing.

## Mapping

| Element | Fluent call |
|---|---|
| `@Command(value, aliases)` on the class | the root `T`, `addAliases` |
| `@Subcommand("a b")` | child commands along the path, created once and shared by the methods of that path |
| `@Subcommand("<x> a")` | `argument(x)` on the command before the placeholder, created once for the path, and `child(a)` on it; adjacent placeholders are one branch |
| method without argument parameters | `execute` / `executeAsync` on its command (at most one per command) |
| method with argument parameters | one `argument(...)` branch on its command |
| `@Subcommand` path ending in placeholders | `executeAsync` on the placeholders' branch (at most one method) |
| `@Description`, `@Permission`, `@VisibleWhen`, `@Confirm`, `@Cooldown`, `@Condition`, `@Middleware`, `@OptionsAnywhere`, `@MutuallyExclusiveOptions`, `@Examples`, `@CaptionBundle`, `aliases` | on the command: from the class for the root, from one `@Subcommand` method for a subcommand |
| `@Description`, `@Permission`, `@VisibleWhen` on an `@Execute` method, or a path ending in placeholders | on its argument branch |
| `@Description(caption = "key")` | `description(ctx -> ctx.caption(Caption.of(key, value)))` |
| `@Flag` / `@Option` parameter | `Options.flag` / `count` / `bool` / `integer` / `longValue` / `floatValue` / `doubleValue` / `string`, or `Options.of(name, argument)` for any other type, `aliases`, `description`, `permission`, `repeatable`, `required`, registered on the method's command; `@Requires` → `requires(other[, value])` |
| other parameter | an argument from the factory of its type; `List<T>` → `variadic()`, or `variadic(max)` with `@Variadic`; `@Default` → `optional(value)`; `@Optional` → `optional(contextValue)` or the empty value of the type |
| return `int` / `CompletionStage` | `ctx.setResult` / the stage of `executeAsync` |

Parameters resolve in a fixed order: the context type, `@Sender`, options, registered argument types, then context
values for types without an argument. `Player` is both an argument type and a context value, so an unannotated
`Player` is an argument and `@Sender Player` is the sender; `CommandSender` has no argument and needs no annotation.

Command annotations of a subcommand may come from only one method of the path. Merging them across methods would make
the result depend on the method order, and two different `@Description`s have no right answer.

Options are per command in the core, so all methods of a command share them. A second method declaring an option of the
same name gets the same `CommandOption` when its annotations and type are equal, and a parse error otherwise.
`@Requires` and `@MutuallyExclusiveOptions` name options by their long name, which another parameter or another method of the
command may declare, so the parser applies them after every method is added. The core reads both only when a command
runs or its tree is built, so applying them after registering the options gives the same command.

An option of a type other than the primitives and `String` is `Options.of(name, argument)` with the argument the
parameter would get as an argument (factory, `@Suggest`, registered annotations, `@Default` / `@Optional`), named
after the option. Its being required follows from the argument, as in the core: an argument that is not optional makes
a required option.

### Placeholders

Lamp and Cloud write argument-first trees as path placeholders (`"team <name> add"`), and so does the parser:
`@Subcommand("<team> add")` is `argument(team).child(add)`. The alternatives were nested classes, which need a second
way of declaring a command and of passing the parsed value down, and an attribute naming the parent argument, which
splits one path over two places. The placeholder keeps the path readable as the input it accepts.

A placeholder binds to the method's argument parameter of the same name, so the method still receives every value it
uses as a parameter, read with `ctx.getArgument(argument)` like the core's `ArgRef`. The branch is keyed by the path up
to the placeholder and created by the first method in parse order; the others must declare the parameter with the same
type and annotations, the rule options follow, since one argument object serves every child. Adjacent placeholders form
one branch, as `argument(a, b).child(...)` does. A path that ends in a placeholder executes that branch and takes no
other arguments, because more arguments would make a second branch starting with the same argument.

Named registries (`registerCondition`, `registerMiddleware`, `registerVisibility`) hold the objects an annotation cannot
hold, so `@Middleware("audit")` and `@VisibleWhen("open")` add the same instances a fluent command adds. A name nobody
registered fails parsing.

`@Default` values are converted when the class is parsed (string, numbers, booleans, enum constants), so a bad default
fails at startup. An `@Optional` argument without `@Default` takes the context value registered for its type, which
maps `@Optional Player` to `optional(playerRequirer())`, the documented "defaults to the sender" pattern, or the empty
value of the type.

`@Range` is read by the platform's numeric factories, since the bounds belong to the Brigadier `ArgumentType` (and the
client's input check), not to a validator.

## Exceptions

The executor invokes the method and unwraps `InvocationTargetException`: a `CommandPrerequisiteException`, an unchecked
exception or an error is rethrown as if the executor threw it, so the sender sees the same failure as with a lambda; a
checked exception fails the returned stage, which the core reports like any executor exception. A method declared to
return a `CompletionStage` that returns null fails with a clear message.

## Extension points

- `registerArgument(Class, ArgumentFactory)`: the type registry, keyed by the boxed type (or the element type of a
  `List`), with `Enum.class` as the fallback for enums. A later registration replaces an earlier one, so a plugin can
  override a built-in mapping. The factory receives an `AnnotatedParameter` (name, value type, annotations), so it can
  read annotations of its own.
- `registerContextValue(Class, Extractor)`: values resolved from the context with `ctx.get(type)`, for mapped senders
  and services. The parsed root provides them (`provide(type, extractor)`), so they are the same values fluent code
  and middlewares of the command read ([context values](execution-pipeline.md#context-values)).
  `registerContextValue(Class)` declares a type a middleware or a parent command gives, and
  `registerDependency(Class, instance)` a constant that also sets `@Dependency` fields.
- `registerAnnotation(Class<A>, Class<V>, ArgumentAnnotationHandler)`: custom parameter annotations. A handler gets the
  annotation and the argument typed as `CommonArgument<V, C, ?>`, so `argument.validator(Player::isOnline)` needs no
  cast, and returns the argument to use. Handlers apply after the factory and `@Suggest` and before `variadic()` and
  `optional(...)`, so they configure each value of a variadic argument, matching the core rule that settings made
  before `variadic()` apply to each value.

## Not done

- `@Subcommand` on nested classes, and an explicit method order (`@Order`).
- `requires(option, predicate, description)`, captions other than one properties bundle, and
  `disableParentMiddleware()` / `disableParentPrerequisite()`. The parsed command is an ordinary `Command`, so these
  can be added fluently after parsing.
- Prerequisites per argument branch: the core has prerequisites per command only, so `@Condition` stays on the class
  and `@Subcommand` methods.
- `java.util.Optional<T>` parameters: the core requires an optional argument's default to be non-null, so an absent
  value has no representation without a change to the core.
- `@Range` bounds beyond 2^53 for `long` arguments lose precision, since the bounds are `double`.
