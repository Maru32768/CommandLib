# Round 1 / A1: common parsing, tree, options, arguments

Scope: `git diff 2c8ffd7a..HEAD` limited to common/src/main: OptionGraph, OptionNode, Options, CommandOption, CommandNodeCreator, ArgumentCommandNodeCreator, CommandNodeChildren, Arguments, ArgRef, CommonArgument, VariadicArgument, ArgumentSuggestionProvider, CommandTree, CommandTreeRenderer, CommandBuilder, CommandNameValidator, and the `argument/`, `annotation/`, `suggestion/`, `branch/` and `exception/` packages.

Method: the `code-review` skill ran on the last commit instead of this range, and its findings are out of scope, so I reviewed by hand. A sub-reviewer covered `annotation/` (A1-11 to A1-18). I confirmed bugs with throwaway tests in `common/src/test`, run with `:common:test` and then deleted. The worktree is clean.

Backward-compatibility lens: there are no `@Deprecated` members, no "legacy" or "compat" comments, no old-name aliases and no format fallbacks in this area. The only near-duplicates are internal (A1-7, A1-8).

---

## A1-1 bug, high: a branch without an executor replaces the runner of a branch with one on the shared argument node
- **Where:** `ArgumentCommandNodeCreator.java:67` (`builder.executes(runner(...))` runs for every node, even when `executorFor(argument)` is null). Also `CommandNodeCreator.java:141-157` (the `runsAt` check only looks at branches that have an executor) and `:192-212` (merge order).
- **What:** Brigadier's `addChild` merge copies `child.getCommand()` onto the existing node whenever it is non-null. A `CommandRunner` with a null executor is non-null, so the branch added later wins. When that branch only leads to a child, the input that ends at the argument shows help instead of running.
- **Evidence (confirmed by a throwaway test):**
  ```java
  argument(new CommonStringArgument<>("team")).execute((t, ctx) -> ctx.sendMessage("exec " + t));
  argument(new CommonStringArgument<>("team")).child(add);
  ```
  - `x red` prints the usage screen, which even lists `/x <team>` as runnable.
  - `x red add` works.
  - With the two branches swapped it works.
  - The annotation parser always sorts the `""` path (`@Execute`) before `"<team> ..."`. So `@Execute void e(@Name("team") String t)` combined with `@Subcommand("<team> add")` always breaks (the sub-reviewer confirmed this).
- **Fix:** when building the node for `argument`, take the runner of the branch in `branches` that shares the prefix and has `executorFor(...) != null` at that position. Fall back to the help runner only when none has one. Alternatively, sort the branches so that those with an executor at a merged position are added last. Add a regression test for both orders.

## A1-2 bug, medium: `CommandNameValidator.sameType` treats every `StringArgumentType` as the same type
- **Where:** `CommandNameValidator.java:107-119`
- **What:** The check assumes that "a type without its own equals has no settings". Brigadier 1.0.18's `StringArgumentType` has no `equals`, but `word()`, `string()` and `greedyString()` accept different input. So a word branch and a greedy branch that share a name pass validation and merge into one node. The longer branch is sorted first and wins.
- **Evidence (confirmed):** branches `<text: WORD> <to: WORD>` and `<text: PHRASE>`.
  - Registration succeeds.
  - `say hello` runs the greedy branch.
  - `say hello big world` fails with "Incorrect argument ... hello big <--[HERE]", so the greedy branch never gets more than one word.
- **Fix:** special-case `StringArgumentType` and compare `getType()`. More generally, treat a type without `equals` as equal only when it has no instance fields, or compare `ArgumentType#getExamples()` as a cheap fallback. Add a test.

## A1-3 bug, low: the error caret for an option value whose argument rejects it can point at the wrong token
- **Where:** `CommonArgument.java:321-333`. `parseNode` builds the value handle with `List.of(node)`, and that node is named `__commandlib_option_<name>`, not the argument's name. The consequence shows in `ArgumentParseException.java:49-62` and `:77-93`.
- **What:** `inputBefore` finds no node with `argumentName` and falls back to `incorrectTokenStart`. That searches from the end of the only node, which is the end of the value itself, so it finds the next occurrence of the same text or else the last one in the input.
- **Evidence (confirmed):** option `--n` made from `Options.of` with `validator(x -> !x.equals("bad"))`, plus a word argument.
  - `cmd --n bad bad` puts the caret on the second `bad` (the argument): `...d --n bad §nbad`.
  - `cmd --n bad other` is correct.
- **Fix:** in `parseNode`, put into the value handle a `ParsedCommandNode` whose node is named `name` (for example `RequiredArgumentBuilder.argument(name, type).build()` with the same range), as `VariadicArgument` effectively does. Alternatively, make `inputBefore` use `handle.getRange().getStart()` when the handle has exactly one node.

## A1-4 bug, low: option aliases added after `option(...)` skip the duplicate-name check
- **Where:** `CommandOption.java:161-170` and `CommonCommand.java:227-252`
- **What:** `option()` returns the same mutable object, so `option(Options.flag("b")).aliases("a")` while `--a` exists gets through. In `OptionGraph`, the `transitions.put` for the duplicate token overwrites, and Brigadier merges the two `--a` literals, keeping the first `OptionNode`. The alias then silently sets the wrong option. The Javadoc says to configure first, but nothing enforces it.
- **Evidence:** read the code; not run.
- **Fix:** move the duplicate long-name and short-name checks into `OptionGraph.of`, which already validates `requiredOptions` when the tree is built, and drop the check at registration time. Or make `aliases` throw after registration.

## A1-5 bug, low (unconfirmed): `variadic()` silently drops settings of the element that the variadic does not use
- **Where:** `VariadicArgument.java:382-391`
- **What:** The element's `optional(...)`, `execute(...)` and `addUncaughtExceptionHandler(...)` are ignored. `Arguments.executorFor` and `ArgumentCommandNodeCreator.uncaughtExceptionHandlers` read the `VariadicArgument`, not the element. So `new X("a").execute(...).variadic()` loses its executor with no error. The doc only says to configure the validator, transformer and suggestions on the element.
- **Fix:** throw from the constructor when `element.isOptional()`, `element.executor() != null` or the element has handlers. Alternatively, carry the executor and handlers over.

## A1-6 bug, low: `ArgumentParseException(String, String...)` leaves `getMessage()` null
- **Where:** `ArgumentParseException.java:124-131`
- **What:** `CommandPrerequisiteException` and `OptionParseException` now call `super(message)`, but `ArgumentParseException` and `ArgumentValidationException` do not. Logs, uncaught-exception handlers and test assertions see `null`.
- **Fix:** call `super(message)`. Also give the Caption constructor `super(caption.key() + " " + placeholders)`, as `OptionParseException` does.

## A1-7 improvement, low: the public `CommandOption` constructor exposes the internal argument name
- **Where:** `CommandOption.java:57-72`
- **What:** The constructor takes a `BiFunction<C, String /*internal node name*/, T>`. Its only users are `Options` (same package) and one test. Now that `Options.of(name, argument)` exists it is a second, lower-level way to build a valued option, and it leaks the `__commandlib_option_` naming.
- **Fix:** make it package-private, or delete it and route `Options.bool/integer/...` through a private factory.

## A1-8 improvement, low: `Arguments` has duplicate permission overloads
- **Where:** `Arguments.java:215-234`
- **What:** `permissionName(parent, prefix)` / `permissionName(prefix)` and `permissionConfig(parent, prefix)` / `permissionConfig(prefix)` both exist. Every caller passes the owning command as `parent`: `ArgumentCommandNodeCreator:74`, `HelpMessageAction:116` and `CommonCommand:947/951`.
- **Fix:** keep only the owner-based overloads and delete the `parent` parameter.

## A1-9 spec-question, low: `requires(option, value)` passes on the default value when the required option is not given
- **Where:** `CommandOption.java:320-340`
- **What:** The validator reads `ctx.getOption(option)`, which returns the default when the option is absent. With `--format json` requiring `--mode fast` and `--mode` defaulting to `fast`, `--format json` alone is accepted. `requires(option)` needs presence, but `requires(option, value)` does not.
- **Fix:** decide which is intended. If presence is intended, check `ctx.hasOption(option)` first, and document the choice either way.

---

## Annotation package (sub-reviewer, confirmed with throwaway tests unless marked)

## A1-11 bug, high: `@Execute` plus `@Subcommand("<x> ...")` loses the executor
This is A1-1 seen from the parser.
- **Where:** `AnnotationParser.java:479` and `:562-589` (`branchAt`)
- **What:** The parser always adds the `@Execute` branch first.
  - `@Execute (team)` together with `@Subcommand("<team>")` also gives two executing branches on one argument, and nothing reports the conflict.
- **Fix:** the core fix in A1-1. If a parser-side change is wanted as well, attach the executor to the existing `PathBranch` with the same keys, and reject duplicate executors.

## A1-12 bug, medium: annotated methods inherited from a superclass are ignored
- **Where:** `AnnotationParser.java:345` (`getDeclaredMethods()`) and `:909` (`@Suggest(method=...)` lookup)
- **What:** `@Execute`, `@Subcommand` and `@Suggest` targets in a superclass are skipped without an error. This is inconsistent with `injectDependencies` (`:359`), which walks superclasses.
- **Fix:** walk the superclass chain and skip overridden signatures, or throw when a superclass declares such methods.

## A1-13 bug, low-medium: custom annotations from `registerAnnotation` are ignored on some parameters
- **Where:** handlers only run at `:848`. The `reject` lists at `:777`, `:1043` and `:1096` cover only built-in annotations.
- **What:** Affected parameters are `@Flag`, `@Option` of the built-in types (boolean, int, long, float, double, String), `@Sender` and context parameters.
- **Fix:** throw when such a parameter carries an annotation registered in `annotationHandlers`.

## A1-14 bug, low: `@Range` with fractional bounds is truncated on int and long
- **Where:** `:169-170` and `:1130-1131`
- **What:** `@Range(min = 1.5) int` accepts 1.
- **Fix:** use ceil for min and floor for max, or reject non-integral bounds. Also reject `min > max`.

## A1-15 bug, low: a placeholder that contains a space becomes literals
- **Where:** `segments` and `isPlaceholder`, `:1335-1343`
- **What:** `@Subcommand("<a b> c")` creates the literals `<a` and `b>`.
- **Fix:** reject segments that are only partly a placeholder, and reject `<>`.

## A1-16 bug, low (unconfirmed): some error messages name the parameter wrongly
- **Where:** `error(...)` (`:1319`) and `applyPermission` (`:753`)
- **What:** `error(...)` shows `argN` even when the parameter has `@Name`. `applyPermission` prints the raw JVM signature.
- **Fix:** use the `@Name` value, and use `describe(method)`.

## A1-17 spec-question, low: contradictory annotation combinations are accepted
- **What:**
  - `@Optional` together with `@Default`: `@Default` silently wins.
  - `@Option(required = true)` together with `@Default` or `@Optional`.
  - `declaration()` (`:1360`) compares all annotations, so the same placeholder declared once with `@Name` and once by a `-parameters` name counts as "declared differently".
- **Fix:** reject the first two combinations.

## A1-18 improvement, low: duplicated logic in the annotation parser
- **What:**
  - The caption-or-text description logic is written twice, at `:487-495` and `:642-649`.
  - `PARAMETER_ANNOTATIONS.subList(0, size - 1)` (`:777`) depends on `Sender` being the last entry.
  - The context-parameter check is duplicated at `:594` and `:769`.
- **Fix:** extract one helper for each.

---

### Checked and fine
- **OptionGraph:**
  - The state and hub redirect scheme and `recover` (region search, prefix literals, namespace stripping, aliases).
  - Mutually exclusive filtering in tokens and permutations.
  - The node cap.
  - Variadic sharing.
- **CommandNodeCreator:** the help-node suppression rules and the alias node children.
- **ArgumentCommandNodeCreator:** suggestion merging (`Suggestions.merge` removes duplicates of what is written into `sb` twice).
- **QuotableSuggestions and CommonDurationArgument:** parsing, overflow and unit order.
- **CommandTree and CommandTreeRenderer.**
- **branch/:** the new `executeAsync` overloads differ in lambda arity, so calls are not ambiguous.
