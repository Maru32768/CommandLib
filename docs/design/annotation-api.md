# Annotation API

## Goal

Let a command be written as a class with annotated methods, as in Lamp, ACF or cloud-annotations, without a second
command model: an annotated command and the same command written fluently must give the same help, permission nodes,
options, suggestions, execution and tests. User documentation: [annotations.md](../annotations.md).

## Approach: a translator onto the fluent API

`AnnotationParser<C, T>` in `common` reads a class and calls the public fluent API of `CommonCommand`:
`new Command(name)`, `addAliases`, `addChildren`, `option`, `argument(builder -> ...)`, `execute`/`executeAsync`,
`permission`, `description`, `confirm`, `cooldown`. The result is an ordinary platform `Command`; nothing below it knows
it came from annotations. That gives the equivalence by construction: the command tree, the help, the permission
configs and the docs exports are computed from the same objects as for a fluent command, and every rule of the core
(optional arguments last, a variadic argument last, unique option names, command name validation) is checked by the
same code at the same time.

The parser uses the builder form `argument(Consumer<ArgumentBuilder<C>>)`, which takes any number of arguments, and an
`AsyncCommandExecutor` that reads each value with `ctx.getArgument(argument)` / `ctx.getOption(option)` and invokes
the method. The typed `Unary`…`Hept` branches are not needed, so methods are not limited to seven arguments.

`T` is the platform command and `C` its context; the parser is created with the context class and a command factory
(`Command::new`). Each platform has an `AnnotatedCommands` class (Spigot, Paper, Velocity) that returns a parser with the
platform's argument types and sender types registered. The annotations themselves are platform independent.

## Reflection, not an annotation processor

The class is read with reflection when `parse` is called, which is at plugin enable, before registration.

- An annotation processor would give the source order of methods, compile-time errors and no reflection at runtime, but
  needs a processor artifact, IDE and build configuration, generated sources per platform, and cannot see the argument
  types a plugin registers at runtime.
- Reflection runs once per command at startup, so its cost is negligible next to building the Brigadier tree, and it
  sees the parser's registries, so custom types and annotations need no build step.
- Invalid signatures fail at `parse` with an `IllegalArgumentException` naming the class, the method and the
  parameter. This is the same moment the core reports its own registration errors, so the failure mode does not change.

Consequences of reflection:

- **Order.** `getDeclaredMethods()` has no defined order, and help lists children and branches in the order they are
  added. The parser sorts methods by subcommand path, then name, then parameter count, so the output is deterministic.
  The fluent equivalent of an annotated command adds its children in that order.
- **Parameter names.** Java keeps them only with `-parameters`. The parser uses `Parameter#getName()` when
  `isNamePresent()`, otherwise the parameter needs `@Name`, and a missing name is a parse error rather than a silent
  `arg0`. Options take their name from the annotation, falling back to the parameter name.
- **Access.** Methods are made accessible with `setAccessible`, so they may be private. Plugins run on the class path,
  where this always works.

## Mapping

| Element | Fluent call |
|---|---|
| `@Command(value, aliases)` on the class | the root `T`, `addAliases` |
| `@Subcommand("a b")` | child commands along the path, created once and shared by the methods of that path |
| method without argument parameters | `execute` / `executeAsync` on its command (at most one per command) |
| method with argument parameters | one `argument(...)` branch on its command |
| `@Description`, `@Permission`, `@Confirm`, `@Cooldown`, `aliases` | on the command: from the class for the root, from one `@Subcommand` method for a subcommand |
| `@Description`, `@Permission` on an `@Execute` method | on its argument branch |
| `@Flag` / `@Option` parameter | `Options.flag` / `count` / `bool` / `integer` / `longValue` / `floatValue` / `doubleValue` / `string`, `aliases`, `description`, `repeatable`, `required`, registered on the method's command |
| other parameter | an argument from the factory of its type; `List<T>` → `variadic()`; `@Default` → `optional(value)`; `@Optional` → `optional(contextValue)` or the empty value of the type |
| return `int` / `CompletionStage` | `ctx.setResult` / the stage of `executeAsync` |

Parameters resolve in a fixed order: the context type, `@Sender`, options, registered argument types, then context
values for types without an argument. `Player` is both an argument type and a context value, so an unannotated
`Player` is an argument and `@Sender Player` is the sender; `CommandSender` has no argument and needs no annotation.

Command annotations of a subcommand may come from only one method of the path. Merging them across methods would make
the result depend on the method order, and two different `@Description`s have no right answer.

Options are per command in the core, so all methods of a command share them. A second method declaring an option of the
same name gets the same `CommandOption` when its annotations and type are equal, and a parse error otherwise.

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
- `registerContextValue(Class, Extractor)`: values resolved from the context, for senders and for services such as an
  economy, which is the first step of dependency injection.
- `registerAnnotation(Class<A>, Class<V>, ArgumentAnnotationHandler)`: custom parameter annotations. A handler gets the
  annotation and the argument typed as `CommonArgument<V, C, ?>`, so `argument.validator(Player::isOnline)` needs no
  cast, and returns the argument to use. Handlers apply after the factory and `@Suggest` and before `variadic()` and
  `optional(...)`, so they configure each value of a variadic argument, matching the core rule that settings made
  before `variadic()` apply to each value.

## Not done

- Forge and NeoForge have no `AnnotatedCommands`; a mod creates the parser with its own registrations.
- `@Subcommand` on nested classes, inherited methods, and an explicit method order (`@Order`).
- Annotations for middlewares (`addMiddleware`), prerequisites, option dependencies (`requires`, `mutuallyExclusive`)
  and argument-first children (`child(...)`). The parsed command is an ordinary `Command`, so these can be added
  fluently after parsing.
- A variadic maximum other than the default, and `java.util.Optional<T>` parameters.
- `@Range` bounds beyond 2^53 for `long` arguments lose precision, since the bounds are `double`.
