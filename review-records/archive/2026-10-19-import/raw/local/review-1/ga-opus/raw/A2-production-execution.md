# A2: Production correctness: execution pipeline, errors, help, localization

Reviewer: A2 (independent). Baseline `origin/master` 2c8ffd7a, reviewed HEAD 1b8f17dc. The uncommitted comment-only edits to
ArgRef.java and OptionGraph.java were ignored.

All repros ran as scratch JUnit tests in a disposable `git archive HEAD` copy
(`C:/Users/maru3/AppData/Local/Temp/clr-A2`, deleted afterwards), with
`./gradlew --configure-on-demand :common:test --tests dev.marulabs.commandlib.A2ScratchTest -i`. They used the
existing `TestCommandRunner` / `TestCommand` / `TestCommandSource` test fixtures in `../../../../common/src/test`. One helper,
`forkToRoot`, was added to the copy's `TestCommandRunner`; it registers `literal(x).fork(root, ctx -> nCopies(n, source))`
to stand in for `execute as @a run`. The output lines quoted below come from that run (BUILD SUCCESSFUL).

## Summary

| ID | Sev | Component | One line |
|---|---|---|---|
| A2-01 | P2 | HelpMessageAction (help path, syntax-advice usage) | The help's root literal is the first token of the whole input, so `/execute ... run game` shows `/execute start` |
| A2-02 | P2 | Confirmation | On Spigot/Paper, forks of the same input by the same sender confirm each other: half the targets of `/execute as @a run wipe` execute without any confirmation, and the requests are swallowed |
| A2-03 | P2 | Cooldown / CommonCommand#middlewares | Cooldown is check-then-act across the middleware chain: while an async middleware waits, the same player passes the check again and the executor runs twice |
| A2-04 | P2 | CommandNodeCreator#createHelpNode | `/<cmd> help` bypasses the runner: it skips prerequisites, swallows help/formatter exceptions (no message, no handlers, returns 0) and has no scheduler |
| A2-05 | P3 | visibleWhen contexts | Visibility contexts get no providers, so `visibleWhen(ctx -> ctx.get(X.class)...)` throws out of Brigadier's `requires` and kills the dispatch |
| A2-06 | P3 | CommandRunner#toFailure / capture | An ErrorFormatter that throws replaces the original exception: the handlers never see the executor's exception, and the formatter's exception goes to the platform raw |
| A2-07 | P3 | CommonCommand#preprocessMiddleware | Messages a preprocess sends before it throws (`throw ctx.fail(...)`) are silently dropped |
| A2-08 | P3 | CommandRunner#run / runInContext | Every unchecked exception before the middlewares is printed twice |
| A2-09 | P3 | Confirmation | All command blocks (name `@`) share one confirmation key, so one block's run confirms another's |
| A2-10 | P3 | HelpMessageAction | `[options]` and the options-only usage line are shown when the sender may give none of the options |
| A2-11 | P3 | exception/* | `CommandPrerequisiteException` / `ArgumentParseException(String…)` have a null `getMessage()`, so the `CommandError.exception()` and middleware-logging use cases get no text |

---

## A2-01 (P2): The help path takes the root literal from the start of the whole input, which is wrong under `execute … run` and other redirects

- **Component:** `HelpMessageAction.path` (also `branchUsage`, so `ErrorFormatter.SYNTAX_ADVICE` / `CommandError.usage()`)
- **Location:** `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java:164-173`. It is called from
  `help` (:32) and `branchUsage` (:135-140), and used by `CommandRunner.java:159`.
- **Description:** `path()` computes the root literal as the first space-separated token of `ctx.getHandle().getInput()`.
  Brigadier's `CommandContext#getInput()` is the full dispatched string for every context in a redirect chain. When a
  CommandLib command runs through a redirect (vanilla `/execute [as|at|…] run <cmd>`, or any `redirect`/`fork` such as
  a Paper label redirect), that first token is `execute`, not the command's label.
- **Violated contract:** `CommandHelp#path()` javadoc and docs/help.md say "the root literal as the sender typed it, so
  an alias stays". docs/help.md shows `/arena …` usage lines.
- **Trigger:** Any command with generated help (bare `/game` without an executor, or `/game help`) or a syntax-advice
  error, run as `/execute run game`, `/execute as @s run game`, from a function line `execute … run game`, and so on.
  This works on every platform where CommandLib nodes sit in the vanilla dispatcher (Spigot, Paper, Forge/NeoForge/Fabric).
- **Evidence (repro, Verified-by-repro):**
  ```java
  TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
      addChildren(new TestCommand("start") {{ execute(ctx -> ctx.sendMessage("started")); }});
      argument(new CommonStringArgument<>("map"), new CommonStringArgument<>("mode")).execute((m, mo, ctx) -> {});
      errorFormatter(ErrorFormatter.SYNTAX_ADVICE);
  }});
  runner.redirectToRoot("run");   // existing helper: literal("run").redirect(root), like execute run
  runner.execute("game"); runner.execute("run game"); runner.execute("run game help");
  ```
  Output (color codes garbled by the console):
  ```
  A2 DIRECT:        [..., §b  /game §estart,   §b/game §7<§emap§7> §7<§emode§7>, ...]
  A2 REDIRECT:      [..., §b  /run §estart,    §b/run §7<§emap§7> §7<§emode§7>, ...]
  A2 REDIRECT help: [..., §b  /run §estart,    §b/run §7<§emap§7> §7<§emode§7>, ...]
  ```
  On a server this shows `/execute start` and `/execute <map> <mode>`. Those lines are wrong and, when clicked or
  copied, run vanilla `/execute`.
- **Existing coverage:** `redirectToRoot` is used only for options and suggestions (OptionRedirectTest,
  OptionsAnywhereTest, CommonArgumentTest). No help or syntax-advice test runs through a redirect.
- **Confidence:** High.
- **Suggested correction:** Take the root literal from the context that contains the CommandLib command rather than from
  offset 0. For example, use the first part of the (flattened or recovered) context chain whose nodes include the
  command's literal or alias, or slice the input from that context's `getRange().getStart()` (which is the redirect
  target's start). Keep the Paper 1.21 fallback the comment mentions. Add a help test and a `SYNTAX_ADVICE` test through
  `redirectToRoot`.

## A2-02 (P2): Forked executions of the same input by the same sender confirm each other

- **Component:** `Confirmation.confirm`
- **Location:** `common/src/main/java/dev/marulabs/commandlib/Confirmation.java:70-91`. The key is at :72-74 and the
  consume-and-compare at :84-86.
- **Description:** The pending request is keyed by the sender (`actor.getUniqueId()`, else `type:name`) and compared on
  the full input. Brigadier runs a forked command (`/execute as @a run wipe`, `execute at @e run …`) once per fork, with
  the same input string. On Spigot and Paper, `CommandContext#getActor()` wraps the Bukkit sender
  (`NMSCommandListenerWrapper.getBukkitSender()` → `CommandSourceStack.source`, which `execute as` does not change). So
  every fork has the same key: fork 1 puts the request, fork 2 finds it and executes, fork 3 asks again, fork 4 executes,
  and so on. In forked mode Brigadier swallows the `CommandFailure` that carries the request, so the operator never
  sees a confirmation prompt, while half of the targets have already executed. On modded platforms the actor is the
  fork's entity, so every fork only asks (silently) and the behavior differs by platform.
- **Violated contract:** The `Confirmation` javadoc says "The first execution does not run the command; it sends a
  message asking the sender to run the same command again." docs/execution.md and the design doc treat it as a guard
  before the executor.
- **Trigger:** A command with `confirm(...)` run through any fork that yields two or more sources for one sender, on
  Spigot or Paper. Running it needs `/execute` access (operators, functions, command blocks), which is exactly where
  bulk destructive commands are run.
- **Evidence (Verified-by-repro in common, platform actor by trace):**
  ```java
  TestCommand command = new TestCommand("wipe") {{
      confirm(Confirmation.within(Duration.ofSeconds(10)).message((ctx, in) -> { seen.add("asked " + in); ctx.sendWarn("confirm " + in); }));
      execute(ctx -> ran.add("wiped"));
  }};
  runner.forkToRoot("each", 4);              // fork(root, ctx -> nCopies(4, source)), same player source
  int result = runner.dispatch("each wipe");
  ```
  Output: `A2 FORK result=2 ran=[wiped, wiped] asked=[asked /each wipe, asked /each wipe]`. Two of the four forks
  executed with no human confirmation, and the two requests never reached the sender (forked failures are dropped).
- **Existing coverage:** CommonCommandConfirmCooldownTest covers per-sender, timeout and different input. Nothing covers
  forks.
- **Confidence:** High for the common logic. Medium-high that Spigot and Paper share the actor across `execute as` forks
  (trace of `getActor()` → `getBukkitSender()`; not run on a server).
- **Suggested correction:** Bind a pending request to one dispatch and do not let a later fork of the same dispatch
  consume it. For example, key the confirmation per dispatch by recording the identity of the parse or root context
  that created the request, and refuse to confirm from the same dispatch. Alternatively, do not ask for confirmation in
  forked contexts (`getHandle().isForked()`) and fail every fork. Document the behavior for forks and command blocks,
  and use the same actor identity on every platform.

## A2-03 (P2): The cooldown check and the cooldown start are separated by the user middlewares, so concurrent executions both pass

- **Component:** `CommonCommand#middlewares`, `Cooldown.check` / `Cooldown.start`
- **Location:** `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java:760-781` (check at :766-768, start at
  :773-778); `Cooldown.java:121-140`.
- **Description:** The chain is `[cooldown check][confirmation][user middlewares][cooldown start]`, and the check only
  reads `endsAt`. When a user middleware does not proceed at once, the cooldown is not started. That happens whenever a
  middleware proceeds after async work: docs/execution.md names "after an async permission lookup" as the supported case,
  and CommandDefaults middlewares such as auditing or DB-backed checks do the same. A second run by the same player
  during that window passes the check too, and both reach the executor.
- **Violated contract:** The `Cooldown` javadoc says it "keeps the same sender from executing it again until it ends".
  The `CommonCommand#cooldown` javadoc says "Keeps a sender from executing the command again until cooldown ends".
- **Trigger:** A command with `cooldown(...)` and any middleware (own, inherited, or a `CommandDefaults` one) that
  completes `next.proceed()` later. The player spams the command, or a macro sends it twice within the middleware's
  latency. The impact is reward or economy duplication on `/kit`- or `/daily`-style commands, which are the typical
  cooldown use.
- **Evidence (Verified-by-repro):**
  ```java
  TestCommand command = new TestCommand("heal") {{
      cooldown(Duration.ofMinutes(5));
      addMiddleware((ctx, next) -> {                       // proceeds later, like an async lookup
          CompletableFuture<Void> done = new CompletableFuture<>();
          deferred.add(() -> next.proceed().whenComplete((x, e) -> done.complete(null)));
          return done;
      });
      execute(ctx -> ran.add("healed"));
  }};
  runner.execute("heal"); runner.execute("heal"); deferred.forEach(Runnable::run);
  ```
  Output: `A2 COOLDOWN RACE ran=[healed, healed] third=[You must wait 5m before using this command again.]`
- **Existing coverage:** The cooldown tests (CommonCommandConfirmCooldownTest) are all synchronous.
- **Confidence:** High.
- **Suggested correction:** Make the cooldown reservation atomic at the check. For example, `check` reserves the slot with
  `endsAt.compute` (or `putIfAbsent` with a now-based expiry) and the reservation is released if the chain fails before
  the executor, which keeps "a sender on cooldown is not asked to confirm, and a failed confirmation does not start
  it". Alternatively, move the start next to the check and roll it back on failure of the pre-executor stages. Add an
  async-middleware test.

## A2-04 (P2): The `/<command> help` node bypasses the CommandRunner pipeline

- **Component:** `CommandNodeCreator#createHelpNode`
- **Location:** `common/src/main/java/dev/marulabs/commandlib/CommandNodeCreator.java:72-89`
- **Description:** The generated `help` literal runs `helpAction.accept(ctx)` directly instead of going through a
  `CommandRunner`. As a result:
  1. **Prerequisites are skipped.** `/game` from a sender failing `addPrerequisite`/`requirePlayer` gets the prerequisite
     message, but `/game help` gets the full help. The parent and inherited prerequisites, the inherited argument
     branches' permission and parse checks, and the runner's visibility re-check (`checkVisible`) are skipped as well.
  2. **Failures are swallowed.** Any exception from the help or a custom `HelpFormatter`/`UsageFormatter` is printed and
     the node returns 0. The sender gets no message, the `UncaughtExceptionHandler`s are not called, and the
     `ErrorFormatter` is not used. The bare-input help path (`CommandRunner.java:172-179`) reports the same failure as
     `UNEXPECTED`.
  3. **No scheduler is set** (`ctx.scheduler` is never called), so a help formatter calling
     `ctx.syncExecutor()`/`supplyAsync` throws `IllegalStateException`, which point 2 then swallows.
- **Violated contract:** docs/help.md says the help is sent "for `/<command> help`, after the permission, option,
  argument and prerequisite checks". docs/design/execution-pipeline.md says checks "run before the generated help too
  … a sender who may not run a command should not get its help either". docs/help.md and errors.md list `UNEXPECTED`
  as "Any other exception of a middleware, the executor or the help".
- **Evidence (Verified-by-repro):**
  ```
  A2 PREREQ bare: [players only]
  A2 PREREQ help: [§7-----, §cUsage:, §b  /game §estart, §7-----]
  A2 FMT bare: result=0 [command.failed, Check the console for details.] handled=[java.lang.IllegalStateException: formatter broke]
  A2 FMT help: result=0 [] handled=[]
  ```
  The first pair comes from `addPrerequisite(ctx -> { throw new CommandPrerequisiteException("players only"); })`
  plus a child `start`. The second pair comes from `helpFormatter((ctx, help) -> { throw new IllegalStateException(...); })`
  plus `addUncaughtExceptionHandler`.
- **Existing coverage:** Help tests (CommandStructureTest, OptionPermissionTest, VisibilityTest, and others) run
  `X help` only for successful output.
- **Confidence:** High.
- **Suggested correction:** Execute the help child with a `CommandRunner` that has a null executor and the command's
  prerequisite, inherited arguments, option graph, scheduler, captions and handlers, as the bare runner is built at
  `CommandNodeCreator.java:101-111`. Then all three problems share one code path.

## A2-05 (P3): Visibility predicates get a context without providers, and their exceptions escape Brigadier's `requires`

- **Component:** `PlatformAdapter#createSourceContext` users and `CommandRunner#run`
- **Location:** `CommandNodeCreator.java:198-205`, `ArgumentCommandNodeCreator.java:47-51`,
  `HelpMessageAction.java:121-129` (contexts without providers set). `CommandRunner.java:73-77`: `checkVisible(ctx)`
  runs before `ctx.providers(providers)`.
- **Description:** The design (execution-pipeline.md, "Context values") presents providers as the sender-mapping
  mechanism, so `visibleWhen(ctx -> ctx.get(GamePlayer.class).isHost())` is the natural way to write a state predicate.
  None of the visibility contexts has the providers, so `ctx.get` throws `IllegalStateException`. Inside a node's
  `requires`, the exception propagates out of `CommandDispatcher.parse`, which fails the dispatch with a raw exception.
  On a server the same predicate runs when the command tree is sent to each player
  (`Commands#sendCommands` → `canUse`), so the failure would also hit tree sends. That last part is a hypothesis; it was
  not run on a server.
- **Evidence (Verified-by-repro for common):** `provide(String.class, ctx -> "host"); visibleWhen(ctx -> ctx.get(String.class).equals("host"));`
  → `A2 VISIBLE threw java.lang.IllegalStateException: No value for java.lang.String in this command execution; …`
- **Contract:** The `visibleWhen` javadoc only says "a context that has the sender but no arguments". docs/context-values.md
  does not list visibility among the places values can be read, so this is a gap rather than a stated guarantee.
- **Confidence:** High (common). Server impact is plausible.
- **Suggested correction:** Set the command's merged providers (and its captions) on the source contexts used for
  visibility, and call `checkVisible` after `ctx.providers(...)` in the runner. Alternatively, document that
  `ctx.get` is unavailable in `visibleWhen` and guard the predicate (treat an exception as not visible, and log it).

## A2-06 (P3): An exception thrown by the ErrorFormatter hides the original failure

- **Component:** `CommandRunner#toFailure` / `#capture`
- **Location:** `CommandRunner.java:283-315` (handlers notified after `capture` at :309-313), `:328-336`, `:191-199`,
  `:250-257`.
- **Description:** For an `UNEXPECTED` or `TIMEOUT` failure, `notifyUncaughtExceptionHandlers(e, ctx)` runs after
  `capture(...)`. If the command's `ErrorFormatter` throws, the following happens:
  - Sync path: the formatter's exception escapes to `runInContext`'s `catch (Throwable)`, the handlers get the formatter's
    exception instead of the executor's, and the formatter's exception is rethrown raw to Brigadier.
  - Late async path: `reportLateFailure` catches the exception and prints it, and the handlers never run.

  docs/help.md says "The stack trace of an unexpected exception is printed and the UncaughtExceptionHandlers are called
  whatever the formatter does."
- **Evidence (Verified-by-repro):** An executor throwing `executor broke` combined with an ErrorFormatter throwing
  `formatter broke` gives `A2 FORMATTER THROWS: threw java.lang.IllegalStateException: formatter broke handled=[formatter broke]`.
- **Confidence:** High.
- **Suggested correction:** Notify the handlers before formatting. Guard `command.errorFormatter().format` with a
  fallback to `error.sendDefault()` (and print or notify the formatter's exception separately).

## A2-07 (P3): A preprocess that sends messages and then throws loses those messages

- **Component:** `CommonCommand#preprocessMiddleware`
- **Location:** `CommonCommand.java:584-594`, `CommonCommandContext.java:638-650`
- **Description:** `captureFailure` collects the messages the predicate sends. If the predicate throws (for example
  `throw ctx.fail("refused")`, a provider's `CommandPrerequisiteException`, or a runtime exception), the collected list is
  discarded in the `finally` block and only the exception's message is reported. The `addPreprocess` javadoc says
  returning false "fails the command with the messages the check sent".
- **Evidence (Verified-by-repro):** `addPreprocess(ctx -> { ctx.sendMessage("context line"); throw ctx.fail("refused"); })`
  → `A2 PREPROCESS: [refused]`, so the `context line` message is lost.
- **Confidence:** High.
- **Suggested correction:** In `preprocessMiddleware`, catch the exception, then send (or fold into the failure) the
  messages already captured, and rethrow. Alternatively, have `captureFailure` return the partial failure alongside the
  exception.

## A2-08 (P3): Exceptions before the middlewares are printed twice

- **Component:** `CommandRunner#run` / `#runInContext`
- **Location:** `CommandRunner.java:191-199` (prints, notifies, rethrows) and `:79-84` (prints again, rethrows).
- **Evidence (Verified-by-repro):** A prerequisite throwing `IllegalStateException("A2boom")` gives
  `A2 PRINTS: 2 handled=[A2boom]`: two stack traces in `System.err` for one failure.
- **Suggested correction:** In `run`, print only what did not come from `runInContext`. For example, wrap only
  `createCommandContext`/`recover`/`checkVisible` in the print-and-rethrow, or mark already-reported throwables.

## A2-09 (P3): Command blocks share one confirmation

- **Component:** `Confirmation.confirm`
- **Location:** `Confirmation.java:72-74`
- **Description:** Senders without a UUID are keyed `type + ":" + name`. Command blocks are named `@` unless renamed, so
  every command block shares the key `COMMAND_BLOCK:@`. One block's first run is confirmed by a different block running
  the same input. The class javadoc says other senders "are told apart by their type and name", which is true but
  surprising for blocks.
- **Evidence (Verified-by-repro):** Two distinct `TestCommandSource("@", null, COMMAND_BLOCK, …)` give
  `A2 BLOCK a: [Run the command again within 10s to confirm. [Confirm]]` and then `A2 BLOCK b: [wiped]`.
- **Suggested correction:** Key block senders by their position (the source's position or world in the context), or
  document it. This could be combined with the fix for A2-02.

## A2-10 (P3): Help shows `[options]` when the sender may give none of the options

- **Component:** `HelpMessageAction#help`
- **Location:** `HelpMessageAction.java:34` (`optionTokens(command)` from unfiltered `command.options()`), `:68-71`
  (the options-only usage line from unfiltered options), `:74-80` (the options list *is* filtered).
- **Description:** When every option has a permission the sender lacks, each usage line still has the `[options]` tag,
  and a command with only options still gets a `/<cmd> [options]` line. The Options section is empty, so the help hints
  at options it does not list. permissions.md says "Generated help: Only routes the sender may use are listed, and only
  options the sender may give."
- **Verification:** Verified-by-trace.
- **Suggested correction:** Compute the visible options first, and use them for both the tag and the options-only line.

## A2-11 (P3): Prerequisite and argument exceptions carry no message

- **Component:** `exception/CommandPrerequisiteException`, `exception/ArgumentParseException`
- **Location:** `CommandPrerequisiteException.java:31-38` (the `String` constructor does not pass the message to
  `super`), and `ArgumentParseException.java:124`.
- **Description:** `getMessage()` is null for `new CommandPrerequisiteException("The database is down.")`. The new
  public surfaces that expose the exception (`CommandError.exception()` for ErrorFormatters, middleware post-hooks such
  as the docs' `log(ctx.getSender(), …, e)` example, and the UncaughtExceptionHandlers for wrapped cases) therefore log
  `CommandPrerequisiteException` with no text. This predates the reviewed range, but the range made the exception a
  public part of `CommandError`.
- **Verification:** Verified-by-trace.
- **Suggested correction:** Pass the first message to `super(message)` for the String constructors, and the caption key
  for the Caption constructors.

---

## Checked and found sound (no finding)

- `CurrentCommandContext`: push and pop are in `finally`, there is a stack for nested dispatch, and the `ThreadLocal`
  is removed when empty, so there is no leak after exceptions. Async completions run without it, as documented
  (ArgRef sync-only).
- Async pipeline: `step` turns middleware or executor exceptions into failed stages (including `Error`s via
  `supplyAsync`). Completed and pending paths match docs/execution.md. `CancellationException` reports nothing,
  `TimeoutException` sends a message with no stack trace, and the late failure is reported on the sync executor through
  the formatter. The result is 1 or the `setResult` value when the chain is pending.
- `captureFailure`: it is reset in `finally`, and nested captures on one context are not reachable from the built-in
  paths.
- `Captions.replace`: a single pass, so placeholder values (user input) are not re-expanded. There is no MessageFormat,
  so apostrophes and braces are safe. Locale fallback (`_ll_CC` → `_ll` → root → default) matches the javadoc. The
  built-in `UNEXPECTED_ERROR` falls back to vanilla `command.failed` as documented.
- `Json.writeString`: escapes `"`, `\`, and control characters. Only ints and booleans reach numbers.
- Cooldown and Confirmation maps: expired entries are purged on each start or confirm, so there is no unbounded
  growth. `ConcurrentHashMap` is used. Configuration setters are not volatile but are meant to be set before
  registration.
- Cooldown is consumed even when the executor fails: this matches the documented semantics ("starts when the command
  passes its middlewares and is about to execute"). It is Subjective and not reported.
- `PlatformAdapterHolder`: the adapter is stateless, and a race only creates a duplicate. `CommandDefaults` is per
  registration with no static state. `FunctionCompilationMarker` deliberately uses System properties across shaded copies.

## Coverage

- **Examined in depth:** CommandRunner, CommonCommandContext, CurrentCommandContext, Cooldown, Confirmation,
  CommandFailure, CommandError, ErrorFormatter, HelpMessageAction, CommandHelp, DefaultHelpFormatter, UsageFormatter,
  HelpFormatter, Captions, Caption, CaptionSender, lang/commandlib_en|ja.properties, CommandNodeCreator (help node,
  runners), CommonCommand (the middleware, preprocess, cooldown, confirm, provider, formatter, visibility and handler
  parts), command/AsyncCommandExecutor, command/CommandMiddleware, CommandScheduler, exception/CommandPrerequisiteException,
  docs/execution.md, docs/design/execution-pipeline.md, docs/help.md, and the relevant parts of docs/errors.md.
- **Skimmed:** ArgumentCommandNodeCreator (runner and suggestions), ChainedCommandContext, ContextKey, CommandDefaults,
  PlatformAdapter, PlatformAdapterHolder, Json, CommandDocs, CommandLibDiagnostics, Snapshots, FunctionLoadErrorFilter,
  FunctionCompilationMarker, exception/ArgumentParseException, the Spigot and modded actor implementations (only for
  A2-02), and docs/context-values.md and docs/permissions.md (the relevant sections).
- **Not examined:** OptionGraph internals (`recover`, option parsing), Arguments and the parse and validation path,
  CommandTree and CommandTreeRenderer, UnknownCommandActor, exception/OptionParseException and
  ArgumentValidationException, the annotation package, docs/localization.md and docs/diagnostics.md in full, and the
  platform schedulers (Bukkit, Folia, modded pool lifecycle on shutdown) and platform failure-message rendering.
  Nothing was run on a real server, so the platform-side claims in A2-02 and A2-05 are by trace.
