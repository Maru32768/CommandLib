# A3: production correctness of the annotation API and the Kotlin DSL

Reviewer: Agent A3 (independent GA review)
Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (uncommitted comment-only edits ignored)
Scope: `common/.../annotation/*`, the `AnnotatedCommands` bridges (spigot, paper, velocity, modded), `common-kotlin`
(Dsl.kt, Coroutines.kt, ContextValues.kt, CurrentCommandContexts.kt, platform/Commands.kt), and the build files of
spigot-kotlin, paper-kotlin and velocity-kotlin.

Experiments ran in a `git archive HEAD` copy at `%TEMP%/clr-A3` (deleted afterwards):
`:common:test --tests A3ProbeTest` and `:velocity-kotlin:test --tests A3CoroutineProbeTest`. The probe sources are
quoted under each finding.

---

## A3-01: P2. The command dispatchers run the block in place, so `yield()` overflows the stack and never yields to the server

- **Component:** common-kotlin, coroutines
- **Location:** `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/Coroutines.kt:24-34` (`CommandContextDispatcher.dispatch`),
  `:112` (suggestion dispatcher with `Runnable::run`). The inline behavior comes from
  `spigot/.../BukkitCommandScheduler.java:19` and `paper/.../BukkitCommandScheduler.java:26` (run at once on the
  primary thread), `velocity/.../VelocityCommandScheduler.java` (`sync` is always `Runnable::run`), and the testers'
  default scheduler.
- **Description:** `CommandContextDispatcher.dispatch` calls `executor.execute { runInCommandContext(ctx, block) }`.
  It does not override `isDispatchNeeded`, which therefore returns true. When the coroutine already runs on the sync
  thread (Bukkit's primary thread, or any thread on Velocity), the sync executor runs `block` at once, inside
  `dispatch`. kotlinx-coroutines 1.8.0 forbids this. The KDoc of `CoroutineDispatcher.dispatch`
  (`commonMain/CoroutineDispatcher.kt:123` in the 1.8.0 sources jar) says: *"This method must not immediately call
  block. Doing so may result in StackOverflowError when dispatch is invoked repeatedly, for example when yield is
  called in a loop. In order to execute a block in place, it is required to return false from isDispatchNeeded…"*
  The suggestion dispatcher at `:112` always runs `Runnable::run` in place, so it breaks the same rule.
  This has two effects:
  1. Each `yield()` (and each ping-pong between coroutines on this dispatcher) adds stack frames instead of returning
     to an event loop. A long loop throws `StackOverflowError`.
  2. On Spigot and Paper, `yield()` in `executeSuspend` never gives the server thread back. The usual coroutine
     idiom for spreading work across ticks (`for (...) { work(); yield() }`) therefore blocks the main thread and
     then crashes. docs/kotlin.md:215 says the block "resumes there after each suspension through
     `ctx.syncDispatcher`", which users will read as normal dispatcher semantics.
- **Trigger:** `executeSuspend { repeat(200_000) { yield() }; ctx.sendMessage("done") }`. A smaller loop on the
  primary thread also triggers effect 2.
- **Evidence (Verified-by-repro, tester scheduler; production path Verified-by-trace):**
  ```kotlin
  val loop = command("loop") { executeSuspend { ctx -> repeat(200_000) { yield() }; ctx.sendMessage("done") } }
  CommandTester(loop, "test.command").use { it.execute("loop", sender) }
  ```
  Output: `Caused by: java.lang.ExceptionInInitializerError: Exception java.lang.StackOverflowError [in thread
  "Test worker @coroutine#1"]` and `PROBE loop messages [command.failed, Check the console for details.]`.
  The Bukkit primary-thread path takes the same in-place branch (`BukkitCommandScheduler.sync`).
- **Existing coverage:** none. `KotlinDslTest` exercises only one suspension and resume.
- **Confidence:** high.
- **Suggested correction:** follow the kotlinx contract. Two options:
  - Let `CommandScheduler` report whether the caller is already on the sync thread, return `false` from
    `isDispatchNeeded` in that case, and delegate to `Dispatchers.Unconfined.dispatch` when called anyway.
  - Have `dispatch` always enqueue, for example with `Bukkit.getScheduler().runTask` on Bukkit, so that `yield()`
    gives the tick back. Override `dispatchYield` at least.
  For the suggestion dispatcher, use `Dispatchers.Unconfined` together with a `ThreadContextElement` that keeps
  `CurrentCommandContext` set. A `ThreadContextElement` would also replace the hand-written context propagation in
  the command dispatchers. Add a regression test with a yield loop.

## A3-02: P2. `@Greedy` before other arguments parses and registers, but the command can never run

- **Component:** annotation API (`AnnotationParser`). The core argument validation has the same gap.
- **Location:** `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java:772-774` (the only
  `@Greedy` check is "is a String"), `:415` (`command.argument(arguments)`), `:498-525` (placeholder branches). No check
  in `CommandNodeCreator`/`CommonCommand` either: `grep -i greedy common/src/main` matches only
  `CommonStringArgument.Type.PHRASE`.
- **Description and contract:** docs/design/annotation-api.md:39 says *"Invalid signatures fail at `parse` with an
  `IllegalArgumentException` naming the class, the method and the parameter."* docs/annotations.md:241 describes
  `@Greedy` as "takes the rest of the input". A greedy string consumes the rest of the line, so any argument after it,
  and any child below a greedy placeholder, can never be reached. The parser accepts three such signatures:
  - `void run(@Greedy String reason, int n)`: the command is unreachable and always shows usage.
  - `@Subcommand("<reason> go") void go(@Greedy String reason)`: the child `go` is unreachable.
  - `@Greedy List<String> words`: the result is a one-element "variadic" list holding the whole rest of the line,
    which is silently not variadic.
- **Evidence (Verified-by-repro):** `A3ProbeTest` uses the same parser shape as `AnnotationParserTest`:
  ```
  PROBE g hello 5        -> [---, Usage:, /g <reason> <n>, ---]
  PROBE g hello world 5  -> [---, Usage:, /g <reason> <n>, ---]
  PROBE v a b c          -> [ran [a b c]]          // @Greedy List<String>
  PROBE p why go         -> [---, Usage:, /p <reason> go, ---]
  ```
  Parsing and registration both succeed in all three cases.
- **Existing coverage:** `invalid_signatures_fail_with_the_method_and_the_parameter` has no `@Greedy` placement case.
- **Confidence:** high.
- **Suggested correction:** in `addMethod`, reject a `@Greedy` parameter that is not the last argument of the method.
  Reject `@Greedy` on a placeholder that is followed by more path, and `@Greedy` together with `List`. Better still,
  add the "greedy must be last and childless" check to the core registration, so fluent commands get it too. Add cases
  to the invalid-signature test.

## A3-03: P3. `withTimeout`, which the docs recommend, ends the command with no message and no log

- **Component:** common-kotlin, coroutines; docs/kotlin.md
- **Location:** `Coroutines.kt:49-50` (`future {}`), `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:294`
  (a `CancellationException` reports nothing), docs/kotlin.md:225-228.
- **Description:** on the JVM, kotlinx's `TimeoutCancellationException` is a
  `java.util.concurrent.CancellationException`. When it escapes the `executeSuspend` block, `future {}` completes with
  it, and `CommandRunner.toFailure` treats it as a deliberate cancellation, so it sends nothing and notifies no
  `UncaughtExceptionHandler`. The docs say "A cancelled coroutine reports nothing", then tell users to "Bound slow work
  with `withTimeout`". A user who follows that advice gets a silent failure. The fluent `TimeoutException` path, by
  contrast, sends `Caption.TIMEOUT`.
- **Evidence (Verified-by-repro):** `executeSuspend { withTimeout(0) { "never" }; ctx.sendMessage("after") }` printed
  `PROBE timeout messages []`.
- **Existing coverage:** none.
- **Confidence:** high on the behavior. Severity is P3 because the docs half-describe it.
- **Suggested correction:** in `startCommand`, catch `TimeoutCancellationException` and rethrow it as
  `java.util.concurrent.TimeoutException`, which maps to `Caption.TIMEOUT`. Alternatively, document that the timeout
  must be caught, for example with `withTimeoutOrNull`.

## A3-04: P3. Modded `holderArgument` handles `List<Holder<X>>`, but the parser rejects that type first

- **Component:** modded `AnnotatedCommands`, `AnnotationParser.elementType`
- **Location:** `AnnotationParser.java:1177-1186` (`elementType` accepts only a `Class` element),
  `modded/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java:152-173` (1.20.5+ branch, comment at
  :156 "The holder of a variadic argument is the element of the List.").
- **Description:** for `List<Holder<Enchantment>> enchantments` (a variadic argument, or a repeatable `@Option`), the
  element type is a `ParameterizedType`. `elementType` therefore throws "needs a List of a class, such as
  List<String>" before `holderArgument` is called. The variadic branch in `holderArgument` is dead code, and on
  1.20.5+ an enchantment, effect or attribute argument cannot be variadic or repeatable through annotations. A single
  `Holder<Enchantment>` works.
- **Evidence:** Verified-by-trace. I did not build modded modules, as instructed.
- **Existing coverage:** none. The modded `AnnotationStyleTest` covers only the arena command.
- **Suggested correction:** have `elementType` return the raw class of a parameterized element (`Holder.class`), so the
  registered factory reads the full type from `AnnotatedParameter.parameter()`. Otherwise drop the dead branch and
  document the limit.

## A3-05: P3. Platform parity and documentation gaps of the bridges

- **Component:** velocity/modded `AnnotatedCommands`, docs/annotations.md
- **Location:** `velocity/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java:42-97`;
  docs/annotations.md:222 and :108-117.
- **Description:**
  - docs/annotations.md lists `Duration`, `UUID → DurationArgument, UUIDArgument` as a general row, but Velocity has
    no `UUIDArgument` and its bridge does not register `UUID`. A `UUID` parameter fails at parse on Velocity.
  - The Velocity bridge has no annotation test at all. `AnnotationStyleTest` exists only for Spigot, Paper and modded,
    and modded lacks the guild (placeholder, middleware, visibility, `@Requires`) case that Spigot and Paper have.
  - The annotation docs give only Java's `-parameters`. A Kotlin class needs `-java-parameters` or `@Name`, and a
    `suspend fun` gets the unhelpful error "no argument type for kotlin.coroutines.Continuation". docs/kotlin.md does
    not mention the annotation API.

  Spigot and Paper register the same argument set (Paper swaps `ChatColor` for `NamedTextColor`), so the
  Spigot/Paper parity holds.
- **Confidence:** Verified-by-trace (Subjective for the docs points).
- **Suggested correction:** mark the `UUID` row "not on Velocity". Add a Velocity smoke test of
  `AnnotatedCommands.parse`. Add a Kotlin note (`-java-parameters`, no `suspend` methods) to the annotation docs.

---

## Checked and found correct (no finding)

- **Method order:** path, then name, then parameter count, then `toGenericString`. It is deterministic. Synthetic and
  bridge methods (including Kotlin `$annotations` and `$default`) are filtered out.
- **Parameter names:** `isNamePresent` is checked. Errors suggest `-parameters` or `@Name`. Option names derived from
  parameter names fail the same way.
- **Primitives and boxing:** `MethodType.wrap`. Every primitive option or argument path yields a non-null value:
  `EMPTY_VALUES`, a parsed `@Default`, or a count or boolean flag.
- **Generics:** raw `List`, wildcard and nested-generic elements are rejected with a clear error (see A3-04 for the
  Holder consequence). `java.util.Optional`, `Collection` and `ArrayList` fail as having no argument type.
- **Static, private and inherited members:** static and private methods work through `setAccessible`, and
  `InaccessibleObjectException` is wrapped. Superclass methods are ignored, as documented. Superclass `@Dependency`
  fields are injected. `@Command` is not `@Inherited`, so proxies and anonymous subclasses fail clearly.
- **Annotation types:** all have RUNTIME retention, and their targets match the docs.
- **Exception unwrapping:** `InvocationTargetException` unwraps to a `RuntimeException` or `Error` and is rethrown.
  A checked exception becomes a failed stage. A null `CompletionStage` gets a named message. A
  `CommandPrerequisiteException` from an extractor or the method sends its message.
- **Invalid combinations rejected at parse:**
  - both `@Execute` and `@Subcommand`; empty `@Subcommand`; aliases on a placeholder path;
  - `@Flag` together with `@Option`; a flag type that is not boolean or int; `max` on a boolean flag;
  - `repeatable` without `List`, and `List` without `repeatable`; `@Default` or `@Optional` on a repeatable or
    variadic parameter; `@Range` on a non-number; `@Variadic` on a non-`List`;
  - `@Sender` combined with argument annotations; command annotations on `@Execute` methods or placeholder branches;
    branch annotations on an `@Execute` method without arguments;
  - two methods configuring one command; a second executor; a placeholder declared differently, or an option declared
    differently, in two methods;
  - unknown `@Condition`, `@Middleware` or `@VisibleWhen` names; `@Requires` or `@MutuallyExclusive` naming a missing
    option (deferred until all options exist); `equalTo` on a repeatable option; an invalid `@Default` or `equalTo`.
- **`@Sender` at runtime:** `@Sender Player` from the console fails through `playerRequirer` with its message.
  `@Optional Player` defaults to the sender. Providers are lazy (`CommonCommand.provide`), so providing every
  registered context value on the root does not fail console executions.
- **Class-level annotations:** `@Permission` and `@Description` (with caption), `@Cooldown`
  (`TimeUnit.toChronoUnit`, Java 9+, toolchain is 11), `@Confirm`, `@CaptionBundle` (the class loader of the parsed
  class) and `@Middleware` (in declared order) are wired as documented.
- **Kotlin DSL:**
  - `@DslMarker` on the receiver types stops outer-command calls inside branch blocks.
  - `executeSuspend`, `addMiddlewareSuspend` and `RequiredArgumentBranch.executeSuspend` return stages that
    `CommandRunner` reports (prerequisite → message, other → generic error, cancellation → silent).
  - The plugin-disable behavior (no parent job, the scheduler refuses the work) is documented in
    docs/kotlin.md:227 and in `BukkitCommandScheduler`'s Javadoc. I accept it as a known limit.

## Coverage

- **In depth:**
  - `AnnotationParser.java` (all 1400 lines) and every annotation type;
  - the Spigot and Paper bridges (diffed), the Velocity bridge, and the modded bridge including its Stonecutter
    branches;
  - Coroutines.kt, Dsl.kt, ContextValues.kt, CurrentCommandContexts.kt and platform/Commands.kt;
  - `CommandRunner`'s async completion path, `CurrentCommandContext`, and the Bukkit and Velocity schedulers;
  - docs/annotations.md and the Coroutines section of docs/kotlin.md.
- **Skimmed:**
  - docs/design/annotation-api.md;
  - `AnnotationParserTest` (test list and harness), `KotlinDslTest`, the AnnotationStyleTest parity across platforms;
  - `CommandOption.of`, `repeatable` and `validate`, `CommonCommand.provide` Javadoc;
  - the build files of spigot-kotlin and velocity-kotlin.
- **Not examined:**
  - Folia `syncDispatcher` behavior when the entity scheduler retires a task;
  - the internals of `SuggestionBuilder.await` and `awaitAll` error handling;
  - `ArgRef` (it has uncommitted edits);
  - the paper-kotlin build file in detail;
  - fluent-core validation of optional or variadic ordering, alias collisions and literal-name validity (left to the
    core reviewers);
  - modded behavior at runtime (not built, as instructed).
