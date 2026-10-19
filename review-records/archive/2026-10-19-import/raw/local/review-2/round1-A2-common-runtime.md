# Round 1 review A2: common runtime, messages and docs

Scope: `git diff 2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` restricted to the common execution/runtime/messages/docs
classes (CommandRunner, CommonCommand, CommonCommandContext, Cooldown, Confirmation, ContextKey, CommandDefaults,
ChainedCommandContext, CurrentCommandContext, CommandScheduler, CommandError/Failure, ErrorFormatter, Caption(s),
CaptionSender, CommandHelp, Help/Usage formatters, HelpMessageAction, CommandDocs, CommandLibDiagnostics, Json,
Snapshots, PlatformAdapter*, FunctionCompilationMarker, FunctionLoadErrorFilter, `command/`, `util/`, lang resources).

Method: the `code-review` skill cannot take a commit range (it reviewed `HEAD~2..HEAD`, which is integration-test only and
outside this area), so the recipe was applied by hand: every file in the area was read in full, together with the code
it connects to (CommandNodeCreator, VariadicArgument, CommandTree, the testers, the platform schedulers). No source
was edited. One throwaway javac check was run in the scratchpad (A2-2).

No `@Deprecated` member, "kept for compatibility" alias or old-format fallback was found in this area. The
compatibility leftovers that remain are A2-2 and A2-5. A2-4, A2-6 and A2-7 cover duplicate or dead API.

---

## A2-1: an AssertionError in a command is swallowed by the testers when the ErrorFormatter sends nothing

- Category: bug
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:344-350` (`fail(CommandFailure)`), with
  `toFailure` at `:307-312`
- What is wrong: `toFailure` records the unexpected exception with `.unexpected(e)`. Then `fail(CommandFailure)` returns
  0 without throwing when the failure has no messages. Every tester (`common-testing/.../CommandTester.java:68-72`, and
  the same code in the paper, spigot, modded and velocity testers) calls `rethrowAssertionError()` only in its
  `catch (CommandFailure e)`, so the AssertionError never reaches it.
- Failure scenario: a command sets
  `errorFormatter((ctx, e) -> { if (e.kind() != CommandError.Kind.UNEXPECTED) e.sendDefault(); })` to stay quiet about
  internal errors, and an executor runs `assertThat(x).isEqualTo(y)`, which fails. `tester.execute(...)` returns 0 and
  the test passes. The documented promise ("An AssertionError the command throws ... is rethrown after them") is broken.
- Evidence: code trace through `runInContext`. Line 188 calls `fail(toFailure(ctx, failure))`, and `fail` returns
  before the throw because `hasMessages()` is false.
- Fix: throw whenever an AssertionError was recorded, e.g. `if (!failure.hasMessages() && !failure.hasAssertionError())
  return 0;` (the platform only meets this in tests). Or keep the AssertionError on the context so the testers can read
  it without a thrown failure.

## A2-2: `addPreprocess(Consumer)` makes every expression-lambda preprocess ambiguous; delete it

- Category: backcompat
- Severity: medium
- Location: `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:532-550`
- What is wrong: `addPreprocess(Consumer<C>)` and `addPreprocess(Predicate<C>)` both accept an implicitly typed
  one-parameter lambda whose body is an expression, such as `ctx -> check(ctx)` or `ctx -> allowed[0]`. For such a
  lambda Java has no most-specific rule, so javac reports "reference to addPreprocess is ambiguous". The Consumer
  overload is the pre-middleware API kept from the base. It only wraps the consumer in a predicate that returns true,
  which `addMiddleware((ctx, next) -> { ...; return next.proceed(); })` or a block lambda already covers.
- Evidence: a scratch `A.java` with the same two overloads fails with
  `error: reference to p is ambiguous ... both method p(Consumer<String>) and method p(Predicate<String>) match` for
  `p(s -> check(s))` and `p(s -> s.isEmpty())`. The repo works around it already: `CommandResultTest.java:91,105,119`
  and `CommonCommandExecutionTest.java:29` cast to `(Predicate<TestCommandContext>)`.
- Fix: delete `addPreprocess(Consumer)`, and its mirror in CommandBuilder or Kotlin if any (`CommandBuilderTest:29`
  lists it). Keep `addPreprocess(Predicate)`. Update the samples and integration plugins that pass block lambdas without
  a return value (`samples/forge/.../PreprocessCommand.java`, `integration-test/shared/*/MainCommand.java`) so they
  return `true` or use `addMiddleware`.

## A2-3: a confirmed command can never be confirmed by running the same function again (unconfirmed)

- Category: spec-question
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/Confirmation.java:301-307`, with
  `CommandRunner.java:76` (`ctx.parse(context.getNodes())`)
- What is wrong: forked siblings are recognised by the identity of `context.getNodes()`. A data pack function compiles
  each line once and reuses its parsed context on every run. Before 1.20.3 that is `ParseResults.getContext()`, whose
  `CommandContextBuilder.build()` passes the same `nodes` list. From 1.20.3 it is the compiled `ContextChain`. So two
  separate runs of the same function line have the same `parse()`.
- Failure scenario: `mypack:reset` contains `arena reset`, which has `confirm()`. A player runs `/function mypack:reset`
  and is asked to confirm, then runs `/function mypack:reset` again within the timeout. `previous.parse == ctx.parse()`,
  so the second run is treated as "another target of the same forked execution". It puts the pending entry back and
  fails silently, so the command never confirms from the function. The button still works, because it runs the inner
  command directly.
- Evidence: code reading and Brigadier/vanilla knowledge. Not checked on a real server.
- Fix: decide whether confirmation from functions is supported. If it is, compare the parse together with the
  execution, for example by storing the parse identity and the tick or game time, or a per-dispatch token set in
  `CommandRunner.run`, instead of the node list alone. If it is not, document it on `Confirmation`.

## A2-4: two walks produce the permission list; delete `CommonCommand.permissionConfigs`

- Category: improvement
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:939-956`, against
  `CommandTree.java:10-14,111-129`
- What is wrong: CommandTree says "the one walk of the command classes lives here, so every rendering agrees on routes
  and permission nodes". But registration uses a second walk, `CommonCommand.permissionConfigs`, called by
  paper/spigot/modded `Command.permissions` and by `velocity/CommandLib:149` and `modded/CommandLib:87`. The two walks
  order the nodes differently: permissionConfigs lists all branches, then the children, then the argument children,
  while CommandTree interleaves each branch with its children. Any future change to one walk, such as a new permission
  kind, can silently miss the other, so `docs().permissionsJson()` and the registered permissions can drift.
  `permissionConfig(String)` and `permissionName(String)` are public, yet only the same package calls them.
- Fix: have the platforms take the list from `CommandTree.permissions(CommandTree.of(commands, prefix))`, or from a
  package-private method on CommandDocs. Delete `permissionConfigs`, make `permissionConfig` and `permissionName`
  package-private, and consider making the `PermissionConfig` constructor package-private.

## A2-5: the default usage line keeps a byte-compatible colour position for the old output

- Category: backcompat
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/DefaultHelpFormatter.java:76-86`
- What is wrong: `hasArgument`/`hasLiteral` exist only to put the two-space padding before `§b` for a command's own
  argument-branch lines and after it for the others ("have always been indented inside the color"). Spaces have no
  colour, so both forms look the same. The branch exists only to reproduce the pre-formatter `HelpMessageAction` string
  exactly (base `HelpMessageAction.java:58` vs `:78`).
- Fix: always emit `ChatColorUtil.AQUA + PADDING + sb`, delete the two flags, and update the help snapshots.

## A2-6: `UnknownCommandActor` is dead code

- Category: improvement
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/UnknownCommandActor.java`
- What is wrong: this package-private singleton is referenced nowhere: not in main, tests or other modules. A grep
  matches only its own file.
- Fix: delete it. `CommandActorType.UNKNOWN` stays, because FakeSender uses it.

## A2-7: the index accessors of inputs are documented as arguments but count literals, and may differ by platform

- Category: doc
- Severity: low (the platform difference is unconfirmed)
- Location: `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java:57-93,109-122`, with
  `HelpMessageAction.java:329-331`
- What is wrong: `getInput(int)` says "the input of the argument at the given index", but `collectInputs` stores every
  node that is not an option, literals included. `getInputs()` for `/scan Alex` is `[scan, Alex]`
  (`docs/design/command-options.md:416`, `CommonCommandContextTest:21` uses `getInput(1)`). So `getInput(i)` and
  `getArgument(i)` index different things. HelpMessageAction also states that "some platforms (Paper 1.21+) omit the
  root literal from CommandContext.getNodes()". If that still holds, `getInput(0)` is the root literal on Spigot and the
  first argument on Paper. The map is also keyed by node name, so a literal and an argument with the same name overwrite
  each other.
- Fix: delete the index accessors (`getInput(int)`, `getInputs()`, and their `SuggestionBuilder` mirrors) in favour of
  `getInput(String)`, `getArgument(CommonArgument)` and ArgRef. Or restrict the map to `ArgumentCommandNode`s, so the
  indices match `getArgument(int)` on every platform, and fix the Javadoc. Re-check or delete the Paper 1.21+ comment in
  HelpMessageAction.

## A2-8: uncaught exception handlers are inherited by argument branches but not by child commands

- Category: spec-question
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:739-741,870-872`, consumed at
  `CommandNodeCreator.java:96,123,174` and `ArgumentCommandNodeCreator.java:191-192`
- What is wrong: an argument-branch runner gets `parent.uncaughtExceptionHandlers()` plus the argument's own handlers.
  A child command's runner gets only `command.uncaughtExceptionHandlers()`. A handler on the root, such as an error
  reporter, therefore fires for `/root <arg>` but not for `/root sub`. Middlewares, captions and formatters are all
  inherited down the tree, so this one stands out. `addUncaughtExceptionHandler` has no Javadoc and no null check (its
  parameter is also misnamed `executor`).
- Fix: inherit like `inheritedMiddlewares()` (the parent's handlers first, honouring `disableParentMiddleware` or
  always), add `Objects.requireNonNull`, and document it.

## A2-9: the usage in an argument error shows `[options]` the sender may not use

- Category: improvement
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java:283-288,343-346`
- What is wrong: `help()` drops options whose permission the sender lacks, and drops the `[options]` tag when none
  remain. `branchUsage` (used for `CommandError.usage()` and `ErrorFormatter.SYNTAX_ADVICE`) adds `[options]` whenever
  the command has any options. The same sender then sees `/scan [options] <target>` in the error and `/scan <target>`
  in the help.
- Fix: filter by option permission in `branchUsage` as `help()` does. CommandRunner already has the platform adapter
  and can pass a `hasPermission` predicate.

## A2-10: a throwing `visibleWhen` predicate hides the command in the tree but crashes its execution

- Category: improvement
- Severity: low
- Location: `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:80,93-106`, against
  `CommonCommand.java:163-171`
- What is wrong: `isVisibleTo` (tree, suggestions) prints a RuntimeException from the predicate and treats the command
  as hidden. `checkVisible` in `run` lets the exception escape. `run` then prints it, the platform logs it again, and
  the sender gets the generic "unexpected error" instead of the unknown-command error the Javadoc of `visibleWhen`
  promises.
- Fix: route `checkVisible` through the same catch, printing and treating the command as not visible, and throw
  `dispatcherUnknownCommand` / `dispatcherUnknownArgument`.

---

### Checked and found fine (no finding)

- CommandRunner async pipeline: middleware chain on the sync thread, late failure reporting, CancellationException
  and TimeoutException mapping, no double notification of the handlers, CommandFailure passthrough.
- `captureFailure` and `deliver`: a preprocess that throws still sends the messages it captured; no reachable
  nested capture was found.
- Cooldown reserve/start/release: `release` runs on the sync thread (the step re-syncs), the CHM `merge` is atomic,
  and a stale reservation expires.
- Captions lookup order, the translatable fallback for `UNEXPECTED_ERROR` when the locale has no text, the brace
  handling of the placeholder `replace`, and the static init order between Caption and Captions.
- CommandDefaults / providers precedence matches its Javadoc. ContextKey boxing and equality. ChainedCommandContext
  `copyFor` and `getArgument`.
- FunctionLoadErrorFilter: the overloads of AbstractFilter delegate to the two overridden methods, and the warm-up
  before installing avoids the Mohist recursion. FunctionCompilationMarker `reloaded` prefix check (the colon prevents a
  prefix collision).

Not checked: no Gradle build or test was run. A2-3 and the Paper part of A2-7 were not checked on a real server.
Platform-side code (the Folia scheduler dropping a task for a retired entity, the deliver wiring of each platform
context) was read only where it touches this area, and is left to the platform reviewers.
