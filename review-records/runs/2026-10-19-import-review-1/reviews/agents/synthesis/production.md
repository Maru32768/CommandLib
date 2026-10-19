# Production code findings

Baseline `2c8ffd7a` (origin/master) → HEAD `1b8f17dc`. Five independent Opus reviewers (A1–A5) each covered one
subsystem in a fresh context and wrote the sections below before seeing any other report. Their text is kept unedited
so the evidence and repro steps stay traceable. **Verification verdicts, severity corrections and deduplication are in
[`findings.md`](findings.md)**; where the two differ, `findings.md` wins.

| Section | Scope |
|---|---|
| A1 | common: options, `OptionGraph`, tree building, arguments, optional/variadic, suggestions |
| A2 | common: `CommandRunner`, middleware/async, cooldown/confirm, context values, captions, help/docs, diagnostics |
| A3 | common annotation API, platform `AnnotatedCommands`, Kotlin DSL and coroutines |
| A4 | Spigot / Paper / Folia, NMS registry, typed NMS modules and generator |
| A5 | Velocity, modded (Forge / NeoForge / Fabric) |


---

## A1 — Production correctness: command tree construction, arguments and options

Reviewer: Agent A1 (independent). Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc. Module: `common`.
Scope: OptionGraph, CommandOption, Options, OptionNode, CommandNodeCreator, ArgumentCommandNodeCreator,
CommandNodeChildren, CommandTree, CommandTreeRenderer, VariadicArgument, Arguments, CommonArgument, ArgRef, branch/*,
argument/*, CommandBuilder, CommandNameValidator, and the tree-building parts of CommonCommand.

Method: I read the code end to end (definition → `CommandNodeCreator` → `OptionGraph.build` → Brigadier parse →
`CommandRunner.run` → `OptionGraph.recover`/`occurrences` → `Arguments.parse`/`CommandOption.read`). I checked each
hypothesis with a scratch JUnit test in a disposable `git archive HEAD` copy, run with
`./gradlew --configure-on-demand :common:test --tests dev.marulabs.commandlib.A1ScratchTest`. The test uses the existing
`TestCommandRunner`/`TestCommandSource` harness. A copy of the scratch test is at
`C:\Users\maru3\AppData\Local\Temp\claude\C--Users-maru3-IdeaProjects-CommandLib-2\2c0d4491-f8db-4530-b789-35fb7dcdea5f\scratchpad\A1ScratchTest.java`.
The disposable copy was deleted afterwards.

### Summary

| ID | Sev | Title | Status |
|----|-----|-------|--------|
| A1-01 | P1 | `optionsAnywhere()`: an argument child after a branch of 2+ arguments loses the arguments parsed before an option redirect, so it fails with an unexpected error | Verified by repro |
| A1-02 | P2 | Branches that share a first argument name merge into one Brigadier node that keeps the longest branch's requirement, so a sender with the shorter branch's permission cannot use it | Verified by repro (pre-existing) |
| A1-03 | P2 | Branches that share an argument name but use different types are accepted, and the shorter branch then fails at runtime with `IllegalArgumentException` | Verified by repro (pre-existing) |
| A1-04 | P3 | Two branches that are executable for the same input are accepted; the shorter one silently wins | Verified by repro |
| A1-05 | P3 | A first argument named `help` merges with the generated help literal, so every input runs help | Verified by repro |
| A1-06 | P2 | The option tree grows exponentially, and nothing fails fast at registration | Verified by measurement |
| A1-07 | P3 | `repeatable(max)` silently drops `required()`, `requires(...)` validators and the required-ness of `Options.of` | Verified by repro |
| A1-08 | P3 | After a parent's option redirect, an argument child's `getInputs()` lacks the parent's literals | Verified by repro |
| A1-09 | P3 | Small validation gaps: short name `-`, a command added as a child of two parents, `requires(option)` naming an unregistered option, greedy values with options | Verified by trace |

---

### A1-01 — P1 — optionsAnywhere + an argument child after a multi-argument branch loses the inherited arguments

- **Component:** `OptionGraph.Builder#copy` / `CommandNodeCreator` (argument children) / `CommandRunner.run`
- **Location:** `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java:266-296` (the copy of an argument
  without child commands is built with `target(builder, state)`, so it may be a redirect), `OptionGraph.java:403-406`
  (`recover` returns early for `!redirects`), `OptionGraph.java:108` (`redirects = inheritedArguments.isEmpty()`, false
  for an argument child's graph), `Arguments.java:151-165` (a missing required argument stops parsing silently).
- **Description:** With `optionsAnywhere()`, every argument node is copied for each option state. A copy whose argument
  has no child commands goes through `target(...)`, so it becomes a *redirect* to the hub of `(counts, argument)` when
  another token reached that state first. Long names come before short names in `tokens()`, so `--force` is the hub
  and `-f` is a redirect. The copy of `k1` under `--force` is also a redirect, to the `--force` hub under `k1`.

  Brigadier starts a new context at each redirect and executes only the last context. For the parent command's own
  runner, `recover` parses the options and arguments before the redirect again. The argument child (`set`) has its own
  `OptionGraph` with `redirects == false`, so `recover` returns the context unchanged. Arguments that came before the
  last redirect (`k1`) are missing. `Arguments.parse` then stops at the missing required argument without an error.
  The child's executor reads the value through `ArgRef`/`getArgument` and throws
  `IllegalArgumentException: No such argument 'k1' exists on this command`. The sender gets the unexpected-error
  message.
- **Violated contract:** `../../../docs/options.md` ("Options After the Arguments"): `/config -f <key> set` must work, since
  options may come before the argument. The design note says that only the argument's *own* child commands do not
  follow the options after it. Options before the argument are allowed, and with one option even
  `/config --force k1 k2 set` fails. The OptionGraph Javadoc (lines 54-56) says an argument child keeps a tree without
  redirects. The child's own graph has none, but the parent's graph does put redirects on the child's path.
- **Trigger:** A command with `optionsAnywhere()`, at least one option, and a branch of two or more arguments that has
  an argument child. Any option given before the branch's last argument breaks it.
- **Evidence (scratch test `anywhereChildAfterRedirect`):**
  ```java
  new TestCommand("config") {{
      optionsAnywhere();
      CommandOption<Boolean, TestCommandContext> f = option(Options.flag("force", 'f'));
      option(Options.flag("verb", 'v'));
      argument(new Word("k1"), new Word("k2")).execute((a, b, ctx) -> ctx.sendMessage("own " + a + " " + b))
          .child((k1, k2) -> new TestCommand("set") {{
              execute(ctx -> ctx.sendMessage("set " + k1.get(ctx) + " " + k2.get(ctx) + " inputs=" + ctx.getInputs()));
          }});
  }}
  ```
  Output:
  ```
  [config k1 k2 set]            -> [set k1 k2 inputs=[config, k1, k2, set]]
  [config k1 --force k2 set]    -> [set k1 k2 inputs=[config, k1, k2, set]]
  [config --force k1 k2 set]    -> [command.failed, Check the console for details.]
  [config -f k1 k2 set]         -> [command.failed, ...]
  [config k1 -f k2 set]         -> [command.failed, ...]
  [config -v -f k1 k2 set]      -> [command.failed, ...]
  [config k1 -f -v k2]          -> [own k1 k2 f=true]       (parent runner recovers correctly)
  stderr: java.lang.IllegalArgumentException: No such argument 'k1' exists on this command
          at CommonCommandContext.getArgument(CommonCommandContext.java:141) ... ArgRef.get(ArgRef.java:57)
  ```
- **Existing coverage:** `OptionsAnywhereTest.an_argument_child_keeps_its_parent_options_after_the_argument` uses a
  one-argument branch. The copy of an argument that has child commands is never a redirect, so that test passes. No
  test covers a multi-argument branch with a child.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Do not let a copy that leads (through later copies) to an argument with child commands be
  a redirect. That is, in `copy()`, treat "has child commands anywhere below in the branch" like `commands` non-empty.
  Alternatively, let the argument child's runner recover with the parent graph: pass the parent's graph, or walk up to
  the hub of the parent graph that the context's root names. Add a regression test with a 2-argument branch and a
  child.

### A1-02 — P2 — A shared-prefix branch node takes the longest branch's requirement, visibility and suggestions

- **Component:** `CommandNodeCreator#toCommandNode`, `ArgumentCommandNodeCreator#buildArgument`
- **Location:** `CommandNodeCreator.java:158-181` (the branches are sorted by size descending, then
  `argumentNodes.addChild(...)`), `ArgumentCommandNodeCreator.java:269-278` (the per-branch `requires`/`suggests`).
- **Description:** `CommandNameValidator.addArgument` (lines 92-99) deliberately allows argument nodes with the same
  name at the same position, as "the shared prefix of variable-length branches". Brigadier's `CommandNode.addChild`
  merges a same-named child: it keeps the existing node's **requirement, type and suggestion provider** and replaces
  only `command`. The longest branch is added first. The shared node therefore requires the longest branch's
  permission node (`<cmd>.<a>.<b>`), and its `visibleWhen` too, instead of the shorter branch's `<cmd>.<a>`.
- **Violated contract:** `../../../docs/permissions.md`: "CommandLib gives every command, subcommand, and argument branch its
  own permission node". A sender who *has* a branch's node can still be shut out of it, and is shown nothing for it.
- **Trigger:** `argument(a)` plus `argument(a, b)` (same first name), and a sender who has `<cmd>.a` but not
  `<cmd>.a.b`. The same happens when the longer branch has a `visibleWhen` that fails: the shorter branch is hidden as
  well. The shorter branch's custom suggestions are also ignored for the shared node.
- **Evidence (scratch `sharedPrefixPermission`):** The source has `test.command.give` and `test.command.give.player`.
  ```
  [give Steve]   -> THROWS CommandSyntaxException: Incorrect argument for command at position 5: give <--[HERE]
  ```
  The runner would allow the short branch, but Brigadier rejects it before the runner runs.
- **Existing coverage:** `CommandStructureTest.variable_length_branches_sharing_an_argument_prefix_are_accepted` and
  `shorter_variable_length_branch_uses_its_own_executor` run with an all-permissions source, so they do not catch it.
- **Pre-existing:** Yes. Baseline `CommandNodeCreator` had the same sort and merge. `visibleWhen` is new in the range
  and adds the visibility half of the problem.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Make the shared node's requirement the OR of the requirements of all branches that share
  it, and merge or union their suggestion providers. The runner already checks the permission of the branch it
  executes. Alternatively, reject different permission nodes or visibilities on a shared prefix at registration.

### A1-03 — P2 — Same-named branch arguments of different types are accepted and fail at runtime

- **Component:** `CommandNameValidator#addArgument`, `CommandNodeCreator` merge
- **Location:** `CommandNameValidator.java:92-99`, `CommandNodeCreator.java:158-179`
- **Description:** The merge keeps the first (longest) branch's `ArgumentType` for the shared name. It then installs the
  shorter branch's runner (`child.command = node.getCommand()`). The shorter branch parses its argument with its own
  `CommonArgument` from a value of the other type.
- **Trigger:** `argument(new Int("x"))` together with `argument(new Word("x"), new Int("y"))`.
- **Evidence (scratch `sharedPrefixDifferentTypes`):**
  ```
  [t 5]     -> THROWS java.lang.IllegalArgumentException: Argument 'x' is defined as String, not int
  [t abc]   -> THROWS java.lang.IllegalArgumentException: Argument 'x' is defined as String, not int
  [t abc 3] -> [word abc 3]
  ```
  The exception escapes `CommandRunner.run` as an uncaught exception. The validators, ranges and suggestions of the
  two branches also silently come from the first argument when the types are equal but the instances differ.
- **Existing coverage:** None.
- **Pre-existing:** Yes.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** In `CommandNameValidator.addArgument`, also record the argument instance or its type.
  Reject a same-named argument at the same path unless it is the same `CommonArgument` instance, or at least the same
  `ArgumentType` class and an equal configuration. The docs should say that shared prefixes must reuse the same
  argument instance.

### A1-04 — P3 — Two branches executable for the same input are accepted

- **Location:** `CommandNodeCreator.java:119-126` checks only the bare-command case. `Arguments.validateTrailingArguments`
  checks a single branch.
- **Description:** `../../../docs/design/optional-variadic-arguments.md` rejects two actions for the bare command "since each
  would handle the bare command". The same ambiguity at a deeper depth is not rejected. In
  `argument(w)` + `argument(w, n.optional(7))`, the input `a x` runs the shorter branch. The longer branch's default
  is dead code, and the order of definition decides the result without any error. The same happens for two identical
  branches: the later one's executor silently replaces the earlier one's.
- **Evidence (scratch `ambiguousExecutable`):** `[a x] -> [short x]`, `[a x 1] -> [long x1]`.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Extend the bare-command check to every shared path. Reject two branches that are
  executable at the same merged node.

### A1-05 — P3 — A first argument named `help` merges with the generated help literal

- **Location:** `CommandNodeCreator.java:44-46` adds `createHelpNode` after the argument nodes.
  `CommandNameValidator` does not know about the generated `help` child.
- **Description:** Brigadier keys children by name, so the `help` literal merges into the existing `help` *argument*
  node and replaces its command with the help action. `shouldAddHelpChild` checks only literal children named `help`.
  The same merge can happen when an argument name equals an option token (`-f`, `--force`) at the command node.
- **Evidence (scratch `helpNamedArgument`):** `argument(new Word("help")).execute(...)`. Both `faq foo` and
  `faq help` print the usage instead of running the executor.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Skip the help child when a first argument is named `help`, or reject the name in
  `CommandNameValidator.validateChildren`. Also reject argument names that start with `-` where the command has
  options.

### A1-06 — P2 — Exponential option-tree growth with no fail-fast guard

- **Location:** `OptionGraph.java:333-393` (`tokens`, `permutations`). The states are `Π(max_i + 1)` (2^n for n flags
  or value options). Each state has every permutation of the remaining short flags.
- **Description:** The docs warn about this ("six short flags already make about 5,500 nodes"), but nothing stops a
  command that would build millions of nodes. Registration then stalls or runs out of memory, and the per-player
  command packet becomes huge.
- **Evidence (scratch `manyFlags` / `manyValueOptions`, `OptionTreeSizeTest.nodes`):**
  ```
  short flags 6 ->   5,426 nodes (0.4 s)
  short flags 7 ->  37,522 nodes (0.9 s)
  short flags 8 -> 298,626 nodes (3.7 s)        (~e^2·n! growth: 9 ≈ 2.7M, 10 ≈ 27M, so OOM)
  value options (with short names) 8 -> 4,098; 10 -> 20,482; 12 -> 98,306 nodes
  ```
  With `optionsAnywhere()` the figures grow further with the number of arguments. Under an argument child, where the
  tree has no redirects, the per-player copy is multiplied by the orders of the options.
- **Existing coverage:** `OptionTreeSizeTest` asserts upper bounds for 4 and 6 flags only.
- **Confidence:** High for the numbers. The severity depends on how many options real commands use.
- **Suggested correction:** Count the states and tokens before building, and throw `IllegalStateException` above a
  documented budget, for example 10k nodes. The message should name the command and suggest long-only flags.
  Optionally, stop offering combined short tokens beyond k flags.

### A1-07 — P3 — `repeatable(max)` silently drops modifiers

- **Location:** `CommandOption.java:266-288`
- **Description:** The returned option copies only the aliases, the description, the permission and the suggestion
  argument. `required()` called before `repeatable` is lost, and so are `requires(...)` validators. For `Options.of`,
  the argument's required-ness is lost too, because `option.argument` is not set. The Javadoc mentions the limit, but
  `../../../docs/options.md` shows help output `(required, up to 5 times)`. The user-facing docs say only "call it before
  `requires(...)`", which reads as if `required()` survives.
- **Evidence (scratch `repeatableRequired`):** `Options.string("tag",'t',"").required().repeatable(3)` →
  `isRequired() == false`. `/r` runs with `tags=[]` instead of failing with "Missing required option".
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Copy `required` and the validators, or throw in `repeatable` when modifiers that it would
  drop are already set.

### A1-08 — P3 — An argument child after a parent option redirect sees a truncated context

- **Location:** `OptionGraph.java:403-406`. The child's graph never recovers, and the child's `CommandRunner` gets only
  the last Brigadier context.
- **Description:** This is the non-anywhere relative of A1-01. The arguments are all after the redirect, so the values
  are correct. However, `getInputs()` (and anything else that reads the parsed nodes) loses the parent's literals and
  options. `OptionRedirectTest` asserts that `getInputs()` "stays `[scan, Alex]`" for the command's own runner, but an
  argument child gets `[a, set]` instead of `[config, a, set]`.
- **Evidence (scratch `argumentChildAfterRedirect`):** `[config -v -f a set] -> [set a q=false inputs=[a, set]]`.
  Compare `[config k1 k2 set] -> inputs=[config, k1, k2, set]` in A1-01.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Use the same fix as A1-01, so that the child runner recovers through the parent graph.

### A1-09 — P3 — Minor validation gaps (verified by trace)

1. `CommandOption` accepts the short name `-` (`StringReader.isAllowedInUnquotedString('-')`). It produces the token
   `--`, and combined tokens like `--f` collide with the long token of an option named `f`. `stateTransitions.put`
   overwrites one of them, and Brigadier merges the two literals (`CommandOption.java:77`, `OptionGraph.java:342-369`).
   Reject `-` as a short name.
2. `addChildren` does not reject a command that already has a parent (`CommonCommand.java:144-171`). The same instance
   added under two parents takes the second parent everywhere: permission node, inherited middlewares and
   prerequisites, help route.
3. `CommandOption.requires(option)` does not check that `option` is registered on the same command
   (`CommandOption.java:290-299`). An unregistered option makes the check always fail at runtime.
4. A greedy argument, used positionally with `optionsAnywhere()` or as an `Options.of` value, swallows the option
   tokens that follow it. `../../../docs/options.md` mentions only variadic arguments as the place where options cannot follow.

### Areas checked and found correct

- `OptionGraph.recover`/`findRegion`/`step` match Brigadier's literal-first `getRelevantNodes` semantics. I tested
  quoted values that contain the command name and option tokens, option values that equal the command name, anywhere
  inputs with quoted positional arguments, `ban -f ban -s ban`, and the trailing-space rejection. All parse as
  Brigadier does. The state's position (node identity) prevents false matches from earlier candidate starts.
- Option permissions under anywhere: copies keep the branch requirement, and restricted tokens (also combined, `-fs`)
  are rejected and hidden (scratch `restrictedOptionAnywhere`).
- Options with an optional variadic under anywhere (scratch `optionsWithOptionalAndVariadic`): values and suggestions
  are correct, and options do not follow the variadic values.
- `CommonDurationArgument`: overflow and malformed input return null, units are ordered, and suggestions only offer
  parseable completions.
- `ArgRef`: a thread-local read through `CurrentCommandContext`, with no shared mutable state. `OptionGraph` state is
  written only during build and read-only afterwards.
- `CommandBuilder` and the `branch/*` classes are thin delegations and look correct.

### Coverage

- **In depth:** `OptionGraph.java`, `OptionNode.java`, `CommandOption.java`, `CommandNodeCreator.java`,
  `ArgumentCommandNodeCreator.java`, `Arguments.java`, `CommandNameValidator.java`, `CommonCommand.java`
  (registration and tree parts), `CommandRunner.java` (run, option parsing and validation paths),
  `ChainedCommandContext.java`, `VariadicArgument.java`, `CommonArgument.java`, `CommonDurationArgument.java`,
  `ArgRef.java`.
- **Skimmed:** `Options.java`, `CommandTree.java`, `CommandTreeRenderer.java`, `CommandNodeChildren.java`,
  `CommandBuilder.java`, `branch/UnaryArgumentBranch.java`, `branch/ArgumentBranch.java`,
  `branch/RequiredArgumentBranch.java`, `CommonEnumArgument.java`, `CommonCommandContext.java` (input collection only),
  `HelpMessageAction.java` (route only), and the tests `OptionsAnywhereTest`, `OptionRedirectTest`,
  `OptionTreeSizeTest` and `CommandStructureTest` (shared-prefix tests), plus `../../../docs/options.md`,
  `docs/design/command-options.md`, `docs/design/optional-variadic-arguments.md` and `docs/permissions.md` (checks).
- **Not examined:**
  - The other typed branches (`Bi`..`Hept`, `Required*`) beyond the pattern.
  - The remaining `argument/*` classes (numeric, boolean, literal, object, nameable).
  - `ArgumentSuggestionProvider` beyond its use.
  - Platform conversions: whether Paper's API-mirror argument types parse in `OptionGraph.parseValue` the same way the
    vanilla dispatcher does after a redirect. This is a hypothesis-level risk for `recover` on Paper, not tested.
  - The vanilla `fillUsableCommands` hashing cost on real servers.
  - Re-registration and unregistration in the platform modules.

---

## A2: Production correctness: execution pipeline, errors, help, localization

Reviewer: A2 (independent). Baseline `origin/master` 2c8ffd7a, reviewed HEAD 1b8f17dc. The uncommitted comment-only edits to
ArgRef.java and OptionGraph.java were ignored.

All repros ran as scratch JUnit tests in a disposable `git archive HEAD` copy
(`C:/Users/maru3/AppData/Local/Temp/clr-A2`, deleted afterwards), with
`./gradlew --configure-on-demand :common:test --tests dev.marulabs.commandlib.A2ScratchTest -i`. They used the
existing `TestCommandRunner` / `TestCommand` / `TestCommandSource` test fixtures in `../../../common/src/test`. One helper,
`forkToRoot`, was added to the copy's `TestCommandRunner`; it registers `literal(x).fork(root, ctx -> nCopies(n, source))`
to stand in for `execute as @a run`. The output lines quoted below come from that run (BUILD SUCCESSFUL).

### Summary

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

### A2-01 (P2): The help path takes the root literal from the start of the whole input, which is wrong under `execute … run` and other redirects

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

### A2-02 (P2): Forked executions of the same input by the same sender confirm each other

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

### A2-03 (P2): The cooldown check and the cooldown start are separated by the user middlewares, so concurrent executions both pass

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

### A2-04 (P2): The `/<command> help` node bypasses the CommandRunner pipeline

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

### A2-05 (P3): Visibility predicates get a context without providers, and their exceptions escape Brigadier's `requires`

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

### A2-06 (P3): An exception thrown by the ErrorFormatter hides the original failure

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

### A2-07 (P3): A preprocess that sends messages and then throws loses those messages

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

### A2-08 (P3): Exceptions before the middlewares are printed twice

- **Component:** `CommandRunner#run` / `#runInContext`
- **Location:** `CommandRunner.java:191-199` (prints, notifies, rethrows) and `:79-84` (prints again, rethrows).
- **Evidence (Verified-by-repro):** A prerequisite throwing `IllegalStateException("A2boom")` gives
  `A2 PRINTS: 2 handled=[A2boom]`: two stack traces in `System.err` for one failure.
- **Suggested correction:** In `run`, print only what did not come from `runInContext`. For example, wrap only
  `createCommandContext`/`recover`/`checkVisible` in the print-and-rethrow, or mark already-reported throwables.

### A2-09 (P3): Command blocks share one confirmation

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

### A2-10 (P3): Help shows `[options]` when the sender may give none of the options

- **Component:** `HelpMessageAction#help`
- **Location:** `HelpMessageAction.java:34` (`optionTokens(command)` from unfiltered `command.options()`), `:68-71`
  (the options-only usage line from unfiltered options), `:74-80` (the options list *is* filtered).
- **Description:** When every option has a permission the sender lacks, each usage line still has the `[options]` tag,
  and a command with only options still gets a `/<cmd> [options]` line. The Options section is empty, so the help hints
  at options it does not list. permissions.md says "Generated help: Only routes the sender may use are listed, and only
  options the sender may give."
- **Verification:** Verified-by-trace.
- **Suggested correction:** Compute the visible options first, and use them for both the tag and the options-only line.

### A2-11 (P3): Prerequisite and argument exceptions carry no message

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

### Checked and found sound (no finding)

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

### Coverage

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

---

## A3: production correctness of the annotation API and the Kotlin DSL

Reviewer: Agent A3 (independent GA review)
Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (uncommitted comment-only edits ignored)
Scope: `common/.../annotation/*`, the `AnnotatedCommands` bridges (spigot, paper, velocity, modded), `common-kotlin`
(Dsl.kt, Coroutines.kt, ContextValues.kt, CurrentCommandContexts.kt, platform/Commands.kt), and the build files of
spigot-kotlin, paper-kotlin and velocity-kotlin.

Experiments ran in a `git archive HEAD` copy at `%TEMP%/clr-A3` (deleted afterwards):
`:common:test --tests A3ProbeTest` and `:velocity-kotlin:test --tests A3CoroutineProbeTest`. The probe sources are
quoted under each finding.

---

### A3-01: P2. The command dispatchers run the block in place, so `yield()` overflows the stack and never yields to the server

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

### A3-02: P2. `@Greedy` before other arguments parses and registers, but the command can never run

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

### A3-03: P3. `withTimeout`, which the docs recommend, ends the command with no message and no log

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

### A3-04: P3. Modded `holderArgument` handles `List<Holder<X>>`, but the parser rejects that type first

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

### A3-05: P3. Platform parity and documentation gaps of the bridges

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

### Checked and found correct (no finding)

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

### Coverage

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

---

## A4 — Production correctness: Bukkit / Spigot / Paper / Folia integration and NMS

Reviewer: Agent A4. Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc. Read-only review; no Gradle run. All file:line
references are at HEAD.

### Findings

#### A4-01 — P2 — Spigot `CommandLib.unregister()` is not idempotent; calling it from `onDisable()` throws

- Component: spigot `CommandLib`
- Location: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:715-717`, `:784-802`, `:845-853`
- Description: `unregister(boolean)` has no "already unregistered" guard. Its `finally` block always runs
  `updatePlayerCommandsLater()` (a `BukkitRunnable.runTask(plugin)`) and `compileFunctionsWithCommands()` when called
  through the public `unregister()`. Bukkit fires `PluginDisableEvent` before `JavaPlugin.setEnabled(false)` /
  `onDisable()`, so the `onPluginDisable` handler (`:650-680`) has already unregistered every instance by the time
  `onDisable()` runs, and `isEnabled()` is already false inside `onDisable()`. A plugin that does the natural
  `onDisable() { commandLib.unregister(); saveData(); }` then hits `CraftScheduler.validate` ->
  `IllegalPluginAccessException("Plugin attempted to register task while disabled")`. Bukkit logs it as "Error occurred
  while disabling", and every statement after `unregister()` in `onDisable()` is skipped, which can include saving data.
  A second runtime `unregister()` also triggers a needless tree resend, `recalculatePermissions` for every player, and
  a full function recompilation.
- Contract: the paper artifact makes the same call a no-op (`paper/.../CommandLib.java:528-537` returns early on
  `State.UNREGISTERED`). Spigot and Paper should behave the same way, and an unregister method is expected to be safe
  to call twice.
- Trigger: `commandLib.unregister()` in `onDisable()`, or called twice at runtime.
- Evidence: Verified-by-trace (the event fires before `onDisable()` in `JavaPluginLoader.disablePlugin`; `runTask`
  validates `plugin.isEnabled()`).
- Test coverage: none. spigot has no lifecycle unit test, and the integration `ScenarioTest`/`RuntimePermissionTest`
  call `unregister()` only once, at runtime.
- Confidence: high.
- Suggested correction: keep an `unregistered` flag (or check `INSTANCES.remove(this)`'s return value) and return
  early. Also skip scheduling when `!plugin.isEnabled()`.

#### A4-02 — P2 — Paper artifact: a first `register()` after initialization always fails under the documented packaging; the "Direct" fallback is effectively dead code

- Component: paper `CommandLib` / `PluginRegistrationState.Mode.Direct`
- Location: `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:52`, `:327-348`, `:573-633`;
  `PluginRegistrationState.java:196-201`
- Description: when Paper refuses the COMMANDS handler (any call after `onEnable` returns), `doRegister` falls back to
  the static `serverDispatcher`. Only a COMMANDS handler that `register()` installed **in the same copy of
  CommandLib** sets it (`:581`); `bootstrap()` handlers do not. docs/packaging.md requires every plugin to shade and
  relocate CommandLib, so each copy belongs to one plugin. A plugin whose first registration is late therefore always
  gets `IllegalStateException("... no dispatcher is cached yet ...")`. If it registered earlier, its mode is already
  `Handled` and the `Direct` branch is never taken. `reregisterDirectStates` re-adds `Direct` registrations only from
  other plugins' handlers in the same copy, which also do not exist under relocation. The diagnostics text
  ("added to the live dispatcher (registered after the plugin's initialization) ...", docs/diagnostics.md:154) and
  the unit test `fallback_registration_is_restored_after_reload` (`paper-testing/.../CommandLibLifecycleTest.java:258`)
  cover a setup that cannot happen in production: two plugins sharing one unrelocated copy.
- Contract / parity: the spigot artifact on Paper 1.20.6+ supports late registration and keeps it across
  `/minecraft:reload` through `ServerResourcesReloadedEvent` (`spigot/.../CommandLib.java:160-191`). The paper artifact
  already has a reflective lookup of the live dispatcher (`PaperDispatcher.live`, used by `addIfCommandsEventMissed`)
  but does not use it here.
- Trigger: a shaded plugin that registers its first CommandLib commands after `onEnable`, for example
  config-driven commands created by `/myplugin reload` or in a delayed task.
- Evidence: Verified-by-trace.
- Test coverage: only the unrelocated multi-plugin mock test above. No integration target registers late on the paper
  artifact without an earlier init-time registration.
- Confidence: high for reachability. Severity is P2 because it fails fast with a clear message rather than silently.
- Suggested correction: in the `Unhandled` fallback, use `PaperDispatcher.live(serverDispatcher)`. To survive data
  reloads, re-add the commands on `ServerResourcesReloadedEvent` (the listener already receives that event) instead of
  relying on another plugin's handler. Otherwise, document that late first registration is unsupported and remove
  `Mode.Direct`, `reregisterDirectStates` and the misleading diagnostics text.

#### A4-03 — P3 — Spigot unregistration removes labels by name, without checking who owns them now, and never restores what registration displaced

- Component: spigot `CommandLib`
- Location: `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:381-396` (enable: `knownCommands.remove(label)`
  and `CommandNodeChildren.replace(vanillaRoot, ...)`), `:856-882`, `:958-963` (unregister)
- Description: registration takes the label unconditionally. It replaces another plugin's command-map entry and
  replaces a vanilla node of the same name in the vanilla dispatcher, which destroys that node. Unregistration then
  removes the label from `knownCommands`, the synced root and the vanilla root without checking that the entry is
  still this registration's. Effects:
  1. When two plugins that each shade CommandLib define the same label, disabling or unregistering the first one at
     runtime (PlugMan, a runtime `unregister()`) deletes the second plugin's live command.
  2. A displaced vanilla or other-plugin command (for example a CommandLib `/give` or `/spawn`) is not restored, so
     the label disappears until restart.

  The paper artifact's `Handled` mode avoids (1) for labels Paper did not grant, because it records the labels
  `registrar.register` returned.
- Trigger: label collisions combined with a runtime unregister or plugin disable.
- Evidence: Verified-by-trace. Behavior on a real server is not reproduced.
- Test coverage: none. `MinecraftNamespacedCommandRemovalTest` checks wrapper identity only for the `minecraft:`
  label.
- Confidence: medium (edge case, but realistic with plugin managers).
- Suggested correction: remove a label only while `knownCommands.get(label)` is this registration's wrapper and the
  root child is this registration's node, as `removeMinecraftNamespacedCommand` already does. Optionally remember
  displaced entries and put them back. Document the label-conflict policy and how it differs between the artifacts.

#### A4-04 — P3 — `register` / `unregister` do not enforce the thread contract that `refresh` does

- Component: spigot and paper `CommandLib`
- Location: `spigot/.../CommandLib.java:115-147`, `:715`; `paper/.../CommandLib.java:138-168`, `:400`
  (compare `requirePrimaryThread` at spigot `:707-713` and paper `:387-394`)
- Description: registration mutates `SimpleCommandMap.knownCommands` (a `HashMap`), the help map, the plugin manager's
  permission maps and the static `INSTANCES` `LinkedHashSet`, which is commented "Main thread only". It does this
  without a thread check. Permission plugins and config reloads often run asynchronously, so a call from such a
  thread races the main thread silently. `refresh()` fails fast for exactly this reason. docs/execution.md states the
  global-region-thread rule only for Folia.
- Evidence: Verified-by-trace (no check present). The race itself is not reproduced.
- Test coverage: none.
- Confidence: medium.
- Suggested correction: call `requirePrimaryThread()` (on Folia, the global tick thread) at the top of `register` and
  `unregister`, and document it for Spigot and Paper as well.

#### A4-05 — P3 — Paper `MessageArgument` silently skips selector resolution when its reflection fails, and no test exercises resolution

- Component: paper `argument/MessageArgument`
- Location: `paper/src/main/java/dev/marulabs/commandlib/argument/MessageArgument.java:55-86`
- Description: selector resolution reads Paper's resolver through `getClass().getMethod("message").invoke(...)` and
  then a declared `resolveComponent` method by its Mojang name. If the first lookup or the invoke fails (for example
  the implementation record is not public, or a future Paper renames it), the code returns `null` and the raw text is
  used. `@p` then reaches the command unresolved, with no log. The spigot artifact always resolves selectors through
  vanilla `MessageArgument.getMessage`. The integration test (`integration-test/shared/paper/.../ArgumentTest.java:590-607`)
  sends only `hello, world!`, so it passes whether or not resolution works.
- Evidence: Plausible. I could not inspect Paper's `SignedMessageResolverImpl` visibility, because only the
  paperclip patch is on disk.
- Confidence: low-medium.
- Suggested correction: add an integration assertion with a selector (for example `@s` sent by the console or a
  player). Make the reflective fallback log once, or fail, when the server is Paper but the members are missing.

#### A4-06 — P3 — Registration depends on writing `final` fields reflectively (JEP 500)

- Component: `common/CommandNodeChildren`, used by every Bukkit and Paper registration path
- Location: `common/src/main/java/dev/marulabs/commandlib/CommandNodeChildren.java:78-87`
- Description: the copy-on-write replacement sets Brigadier's `final` `children`/`literals`/`arguments` fields through
  `Field.set`. JDK 26 (JEP 500) warns on reflective mutation of final fields by default, and a later JDK is planned to
  deny it unless `--enable-final-field-mutation` is given. Minecraft 26.x already requires Java 25, so servers that
  move to a newer JDK will print warnings and, eventually, fail registration with `IllegalAccessException`.
- Evidence: Hypothesis about future JDK defaults. The current code works.
- Confidence: medium on direction, low on timing.
- Suggested correction: track it in the roadmap. Possible alternatives: `CommandNode#addChild` on a fresh root swapped
  by reference where the server allows that, `Unsafe`/`VarHandle` with a documented flag, or a pinned fallback
  message that names the JVM flag.

### Checked and found sound (no finding)

- Version detection and NMS selection: `BukkitUtil.getMinecraftVersion` takes `x.y[.z]` from `getBukkitVersion()`, and
  `NMSClassRegistry.resolve` throws `NMSClassNotFoundException` with the reasons and a paper-artifact hint when no
  module covers the version. An unknown or newer version therefore fails at `register()` (the first
  `NMSCraftServer.create`) instead of mis-mapping. `TypedClassLinkage` checks linkage before a module is used, and
  mapping probes split the Paper Spigot-named and Mojang-named packages.
- The `TypedNmsModules` ranges match `generate.py` `MODULES`. settings.gradle.kts requires every `nms/*` directory to be
  in the table, and publishing refuses a jar built without some modules. Typed classes are compiled with
  `typedNmsRelease`: the 26.x module classes are class-file major 60 in the built jar, so Maven Shade 3.6.0 and older
  ASM can read them.
- Generator (`mojang.py`): checked the new argument wrappers per version gate. They cover: Vec3 `centerIntegers`;
  BlockPos without load checks; Item/BlockPredicate build-context and throws gating; MinMaxBounds getters before and
  after 1.20.2 and reflection for 26.x; ComponentArgument `getRawComponent` from 1.21.5; ScoreboardSlot number vs
  `DisplaySlot` at 1.20.2; Color vs TeamColor at 26.2 resolved by name; TimeArgument minimum from 1.19.4 (spigot
  `TimeArgument` re-checks the minimum for older servers); `sendSuccess(Supplier)` from 1.20. Spot-checked the
  generated spigot-1.16.5, spigot-1.19 and paper-1.21.1 classes.
- `FunctionCompilationMarker`: the system-property names survive relocation. The `#load` re-run heuristic depends on
  vanilla dropping a whole tag when a referenced function is missing, which is correct.
- Folia scheduler: work for an entity runs on the entity scheduler and runs inline when the current region owns the
  entity. Command blocks use the region scheduler, and other senders use the global region scheduler. Removed
  entities drop the task, as documented. The spigot artifact refuses Folia. `ServerFunctions` is off on Folia.
- Sender mapping: `getSender()` stays the original sender under `/execute as` on both artifacts, and `getEntity()` is
  the executor. `BukkitCommandActor` classifies remote console, console, player, entity and command block.
- Paper `unregister` is idempotent, removes only the labels it recorded, keeps going after a failed label, and on
  disable updates the trees at once with copy-on-write maps.
- Spigot and Paper argument class sets match. Paper adds only `ProfileWordArgumentType` and `SelectorResults`.

### Coverage

- In depth: spigot `CommandLib` (whole file), paper `CommandLib`, `PaperDispatcher`, `PluginRegistrationState`,
  `FoliaCommandScheduler`, both `BukkitCommandScheduler`s, both `CommandContext`s, `BukkitCommandActor`,
  `CommandPermissions`, `CommandSet`, `ServerFunctions`, `NMSClassRegistry`, `TypedNmsModules`, `NMSClass`,
  `NMSArgument`, `NMSArgumentChatFormat`, `NMSArgumentTime`, `CommandNodeChildren`, `FunctionCompilationMarker`,
  `mojang.py` diff, `generate.py` module table, settings.gradle.kts NMS inclusion, spigot build.gradle.kts packaging.
- Skimmed: `TypedClassLinkage` (header and strategy only), the spigot and paper argument diffs, `MessageArgument`
  (both), `ScoreboardDisplaySlotArgument`, `CommandLibDiagnosticsCommand`, `NMSDedicatedServer.keepPendingLoad`,
  `paper-testing` `CommandLibLifecycleTest` (names and two tests), generated classes of spigot-1.16.5, spigot-1.19
  and paper-1.21.1.
- Not examined: `spigot_legacy.py` diff, buildSrc `SpigotNmsPlugin` and `BuildToolsTask` beyond the release setting,
  paper `HelpTopics` and `RichTexts`, `FunctionLoadErrorFilter`, spigot `annotation/AnnotatedCommands`, the remaining
  ~25 nms modules, Mohist-specific paths, and the paper `MessageArgument` resolver against a real Paper jar (only the
  paperclip patch was available). No Gradle build or test was run.

---

## A5 — Production correctness: Velocity and modded (Forge / NeoForge / Fabric)

Reviewed HEAD `1b8f17dc` against baseline `origin/master` `2c8ffd7a`. Static review. I checked vanilla and loader behavior
against the Loom-cached Minecraft/Forge jars and sources (javap on `MinecraftServer` 1.16.5, Forge 1.20.1
`PermissionAPI` / `ConsoleCommandCompleter`, Fabric API `CommandManagerMixin`, Velocity's Brigadier fork). I ran no
Gradle builds.

---

### A5-01 — P1 — Forge 1.16.5: CommandLib commands disappear after `/reload` and are missing from data pack functions

- **Component:** modded, `forge-1.16.5` branch of `CommandLib`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:325-356` (the `//?} else {` branch)
- **Description.** On 1.16.5 the commands are added once to `server.getCommands().getDispatcher()` from
  `FMLServerStartedEvent`. In 1.16.5, `MinecraftServer.getCommands()` reads `this.resources` (a `ServerResources`), and
  `reloadResources` replaces that field with a freshly loaded `ServerResources`. Its constructor runs
  `new Commands(selection)`. Bytecode confirms this: `lambda$reloadResources$11` does `resources.close(); putfield
  resources`, and `ServerResources.<init>` runs `new net/minecraft/commands/Commands`. After `/reload`, the new dispatcher
  has none of the CommandLib commands, and `PlayerList.reloadResources()` resends the tree without them.
  Forge 36 already fires `net.minecraftforge.event.RegisterCommandsEvent` from every `Commands` constructor
  (`ForgeEventFactory.onCommandRegister`; its Javadoc says "whenever the Commands class is constructed"). The 1.18+
  branches already use that event.
  - Second effect: the startup function load runs before `FMLServerStartedEvent`, so data pack functions that call
    CommandLib commands fail to parse on 1.16.5.
  - Third effect: when `register` runs while a server exists (`server != null`), the branch never subscribes. A
    singleplayer world started later in the same JVM has no commands.
- **Violated contract.** Commands survive `/reload` on every other platform. `../../../docs/permissions.md` and
  `docs/compatibility.md` do not mention this limitation. The diagnostics line on this node ("the server's dispatcher
  when the server has started") hides it.
- **Trigger.** `/reload` on a Forge 1.16.5 server or singleplayer world, or a function tag / `#load` function that calls
  a CommandLib command.
- **Evidence.** Verified-by-trace (bytecode of the cached `forge-1.16.5-36.2.42` merged jar, and the Forge 1.16.5 sources
  of `RegisterCommandsEvent` and `ForgeEventFactory`).
- **Tests.** None. No modded integration case runs `/reload` or functions; `modded-testing` builds its own dispatcher.
- **Status / confidence.** Verified-by-trace, high. The code predates this range but is unchanged final code.
- **Suggested correction.** Register on `RegisterCommandsEvent` on 1.16.5 as on 1.18+, and always subscribe, whether or
  not a server is running. Keep `PermissionAPI.registerNode` at server start, or move it to the constructor. Add a
  `/reload` case to the modded integration test.

### A5-02 — P2 — Fabric production: command blocks count as the console; RCON is never reported as `REMOTE_CONSOLE` on 1.17+ runtimes

- **Component:** modded, `ForgeCommandActor`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:41-66` (`getType`), `:68-70`
  (`isConsole`). Consumer: `Command.consoleRequirer`, `modded/src/main/java/dev/marulabs/commandlib/Command.java:57-65`.
- **Description.** The source type is detected from the raw `CommandSource`'s class name:
  `contains("CommandBlock")`, `contains("RCon")`, `contains("MinecraftServer")`, with `CONSOLE` as the fallback.
  - On Fabric production, vanilla classes carry intermediary names. A command block's source is
    `net.minecraft.class_2593$1` (Mojang name `CommandBlockEntity$1`) and a command block minecart's is
    `class_1697$class_1698`. Neither contains `CommandBlock`, so both fall through to `CONSOLE`. `isConsole()` then
    returns true, and `requireConsole()` / `consoleRequirer()` accept command blocks.
  - On every Mojang-named runtime (Forge 1.17+, NeoForge, Fabric dev, and all tests) the RCON class is
    `net.minecraft.server.rcon.RconConsoleSource`. That is lowercase "con", so the case-sensitive `contains("RCon")`
    never matches, and RCON reports as `CONSOLE`. The cached 1.20.1 and 1.16.5 jars confirm the class name.
- **Violated contract.** `CommandActorType.COMMAND_BLOCK` and `REMOTE_CONSOLE` exist, and `Command.consoleRequirer`'s
  Javadoc says it accepts only "the console or a remote console". On Fabric production a command block passes a
  console-only prerequisite.
- **Trigger.** A Fabric server (intermediary names) where a command block runs a command with `requireConsole()`, or any
  code that branches on `getActor().getType()`.
- **Evidence.** Verified-by-trace. Fabric runtime names are intermediary, and `../../../docs/agents/modded.md` says Fabric
  production names are not checked on a real server.
- **Tests.** None for `COMMAND_BLOCK` or `REMOTE_CONSOLE` in `modded-testing` (no grep hit). Dev-name tests could not
  catch the Fabric case anyway.
- **Status / confidence.** Verified-by-trace, high for Fabric command blocks and for RCON.
- **Suggested correction.** Use type checks instead of names:
  - `rawSource instanceof BaseCommandBlock` for command blocks.
  - `instanceof RconConsoleSource` for RCON.
  - `instanceof MinecraftServer` for the console.
  - Anything else should be `UNKNOWN`, not `CONSOLE`.

### A5-03 — P2 — Fabric with a shaded (recommended) copy: registering after `SERVER_STARTING` adds nothing until `/reload`

- **Component:** modded, Fabric `CurrentServer` and `CommandLib`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/util/CurrentServer.java:18-29`;
  `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:214-235` (Fabric constructor)
- **Description.** On Fabric, `CurrentServer` learns of the server only through `ServerLifecycleEvents.SERVER_STARTING`.
  It registers that listener in its static initializer. With the relocated shaded copy, which `../../../docs/packaging.md`
  recommends and which leaves out `fabric.mod.json`, nothing runs that initializer until the first CommandLib call.
  - If a mod's first `CommandLib.register` happens in `SERVER_STARTING` (before CommandLib's listener) or in
    `SERVER_STARTED` or later, `CurrentServer.get()` returns null for the rest of that server's life.
  - The constructor then only adds a `CommandRegistrationCallback` listener. The commands are not in the running
    dispatcher, and players do not see them until `/reload` or the next world start.
  - Side effects: `refreshAll()` does nothing, diagnostics report "not running", and `BuiltInCommandBuildContext` uses
    the built-in registries with `DEFAULT_FLAGS`. Arguments over data-driven registries, such as enchantments on 1.21+,
    then fail.
  - Forge and NeoForge read `ServerLifecycleHooks.getCurrentServer()` and do not have this problem.
- **Violated contract.** The Fabric diagnostics line says registration happens "or the running server's dispatcher
  when registered later". Forge and NeoForge keep that promise; the shaded Fabric copy breaks it.
- **Trigger.** A shaded Fabric copy plus a first `CommandLib.register` from `SERVER_STARTED` or a later runtime hook.
- **Evidence.** Verified-by-trace.
- **Tests.** None (`modded-testing` mocks `CurrentServer`).
- **Status / confidence.** Verified-by-trace for the mechanism. How often it happens depends on where users register,
  and `onInitialize` is the norm.
- **Suggested correction.** Fall back when `server == null`. On a dedicated server, use
  `FabricLoader.getInstance().getGameInstance()`. On the client, `Minecraft.getInstance().getSingleplayerServer()` would
  need a client-only guard. Otherwise, document that the first CommandLib call must come before the server starts
  (from `onInitialize`).

### A5-04 — P2 (design, documented) — Forge / NeoForge / Fabric: every non-player source passes CommandLib permission checks, including `/execute as <non-player entity>`

- **Component:** modded, `PlatformAdapterImpl.hasPermission`
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/PlatformAdapterImpl.java:27-38`
- **Description.** When the source entity is not a `ServerPlayer`, the method returns `true`. A permission-level-2
  operator who can use `/execute` can run `execute as @e[type=pig,limit=1] run <cmd>`.
  `CommandSourceStack.withEntity` keeps their level 2, and the CommandLib command runs even when its default is
  `DefaultPermission.OP`, which is level 4 for players.
  - Vanilla level-3/4 commands (`/op`, `/stop`) check `source.hasPermission(n)` and still refuse in that case.
  - Spigot / Paper ask the Bukkit entity sender, which is not op, so they also refuse.
  - Command blocks (level 2) can likewise run level-4-default CommandLib commands.
  - `../../../docs/errors.md` ("Allowed unless the source entity is a player") and `docs/permissions.md` document this
    behavior, so this is a design concern, not a contract breach.
- **Trigger.** A level-2 operator (or a command block) runs `execute as <non-player> run <OP-default CommandLib command>`.
- **Evidence.** Verified-by-trace.
- **Tests.** Behavior is documented. I found no test that pins it.
- **Status / confidence.** Subjective on whether it should change. The trace is certain.
- **Suggested correction.** For non-player sources, fall back to `source.hasPermission(level)`, using the node's default
  (4 for `OP`, true for `ALL`, false for `NONE`), instead of returning `true`. The console (level 4) and functions keep
  working, because function permission level is configurable.

### A5-05 — P3 — Velocity `ServerArgument`: suggestions for names that need quotes are inserted unquoted and do not filter while quoting

- **Component:** velocity, `ServerArgument`
- **Location:** `velocity/src/main/java/dev/marulabs/commandlib/argument/ServerArgument.java:16-30`
- **Description.** The type is `StringArgumentType.string()`, and the Javadoc promises that any server name can be
  given as a `"quoted name"`. The suggestion provider calls `sb.suggest(name)` with the raw name. For a name with a
  space or another character not allowed unquoted (TOML keys can be quoted, so such names are possible), accepting the
  suggestion inserts unquoted text that then fails to parse. While the user types `"lob`, the latest input includes the
  quote, so `containsIgnoreCase` filters every name out. Common's suggestion code does no quoting.
- **Trigger.** A proxy with a server named, for example, `my lobby`.
- **Evidence.** Verified-by-trace.
- **Tests.** None for quoted names.
- **Status / confidence.** Verified-by-trace, low impact.
- **Suggested correction.** Suggest `"..."`-escaped text when `StringReader.isAllowedInUnquotedString` fails for any
  character, and strip a leading quote from the input before filtering.

---

### Checked, no finding

- **Velocity registration.**
  - Each label and alias is its own `BrigadierCommand` and `CommandMeta` under the plugin.
  - `unregister(CommandMeta)` removes only this registration's metas.
  - Velocity lowercases labels itself.
  - Commands run on Velocity's async executor, and `sync` runs inline, which is fine because the proxy has no main
    thread.
  - `Cooldown` and `Confirmation` use `ConcurrentHashMap`.
  - `CommandContext.proxy` is volatile.
  - `getPlayer` and `getServer` are case-insensitive in Velocity.
- **Velocity permissions.**
  - A non-`UNDEFINED` Tristate wins; otherwise only `ALL` grants. This is documented.
  - The console is `ALWAYS_TRUE`.
  - The `DEFAULTS` map is never cleared on unregister. That is harmless.
- **Velocity Brigadier fork.** Its public `CommandContext` constructor matches the Mojang 1.0.18 signature that
  `PlatformAdapter.createSourceContext` calls.
- **Velocity `RichTexts`.** All 4 `ClickAction` types are mapped. The factories work on Adventure 4 and 5.
- **Forge / NeoForge 1.18+ registration.**
  - `RegisterCommandsEvent` re-adds the commands on every `Commands` construction (`/reload`, new world).
  - `PermissionGatherEvent.Nodes` refreshes `PERMISSIONS` on every server start, so singleplayer restarts get fresh
    nodes.
  - Unregistered nodes fall back to the default resolver, which is documented.
- **Fabric registration.** Fabric API's `CommandManagerMixin` fires `CommandRegistrationCallback` from every `Commands`
  constructor, and `SERVER_STOPPED` clears `CurrentServer`.
- **`BuiltInCommandBuildContext`.**
  - 1.20.5+: the lookup is re-resolved when the server's registry identity or feature flags change, so there are no
    stale registries across singleplayer worlds.
  - 1.19.3–1.20.4: it uses the static registries, whose tags the server rebinds.
  - 1.19–1.19.2: `missingTagAccessPolicy` FAIL is evaluated at parse time, after tags load.
- **`RawWordArgumentType` on Fabric.** It finds the class map by content. Fabric API 0.92 / 0.116 / 0.119
  command-api-v2 do not filter argument types by registry id, so the unregistered `Info` serializes as vanilla
  `brigadier:string`.
- **Argument parity with Spigot.** The modded set matches, with vanilla-typed equivalents: `BlockState` for
  `BlockData`, `ResourceLocation` for `NamespacedKey`, `GameProfile` for `OfflinePlayer(s)`, `Effect` for
  `PotionEffect`.
- **Recipe / Advancement / LootTable / Objective version branches.** They match each version's vanilla API, as compile
  checks per node also confirm.
- **Async pool.** `ServerCommandScheduler` keeps a static daemon pool that survives world restarts, and the sync
  executor re-reads the source's server.

### Coverage

- **In depth:**
  - velocity: `CommandLib`, `CommandContext`, `Command`, `PlatformAdapterImpl`, `VelocityCommandActor`,
    `VelocityCommandScheduler`, `VelocityPermissions`, `PlayerArgument`, `ServerArgument`, `RichTexts`.
  - modded: `CommandLib` (every branch), `PlatformAdapterImpl`, `Command`, `ForgeCommandActor`, `CommandContext`,
    `CurrentServer`, `ServerCommandScheduler`, `BuiltInCommandBuildContext`, `RegistryEntries`, `RegistryArgument`,
    `BiomeArgument`, `RecipeArgument`, `AdvancementArgument`, `LootTableArgument`, `ObjectiveArgument`,
    `RawWordArgumentType`, `fabric.mod.json`, `../../../modded/build.gradle.kts`, `velocity` and `velocity-kotlin` build scripts.
- **Skimmed:**
  - `CommandLibDiagnosticsCommand` (both).
  - `TimeArgument`, `GameModeArgument`, `AttributeArgument`, `EntityTypeArgument`.
  - The common `CommandNodeCreator` alias and requirement paths.
- **Not examined:**
  - velocity and modded `annotation/AnnotatedCommands`.
  - Modded `UUIDArgument` / `UUIDsArgument`, `Location`, `LocationArgument`, `GameProfileArgument`,
    `Enchantment` / `Effect` / `Component` / `Message` / `Particle` / `Predicate` arguments, `util/text/RichTexts`
    (modded).
  - `velocity-testing` / `modded-testing` internals.
  - Velocity proxy internals beyond the API (`CommandGraphInjector` handling of shared alias children was not checked
    against source).
  - No builds or tests were run.
