# A1 — Production correctness: command tree construction, arguments and options

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

## Summary

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

## A1-01 — P1 — optionsAnywhere + an argument child after a multi-argument branch loses the inherited arguments

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
- **Violated contract:** `../../../../docs/options.md` ("Options After the Arguments"): `/config -f <key> set` must work, since
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

## A1-02 — P2 — A shared-prefix branch node takes the longest branch's requirement, visibility and suggestions

- **Component:** `CommandNodeCreator#toCommandNode`, `ArgumentCommandNodeCreator#buildArgument`
- **Location:** `CommandNodeCreator.java:158-181` (the branches are sorted by size descending, then
  `argumentNodes.addChild(...)`), `ArgumentCommandNodeCreator.java:269-278` (the per-branch `requires`/`suggests`).
- **Description:** `CommandNameValidator.addArgument` (lines 92-99) deliberately allows argument nodes with the same
  name at the same position, as "the shared prefix of variable-length branches". Brigadier's `CommandNode.addChild`
  merges a same-named child: it keeps the existing node's **requirement, type and suggestion provider** and replaces
  only `command`. The longest branch is added first. The shared node therefore requires the longest branch's
  permission node (`<cmd>.<a>.<b>`), and its `visibleWhen` too, instead of the shorter branch's `<cmd>.<a>`.
- **Violated contract:** `../../../../docs/permissions.md`: "CommandLib gives every command, subcommand, and argument branch its
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

## A1-03 — P2 — Same-named branch arguments of different types are accepted and fail at runtime

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

## A1-04 — P3 — Two branches executable for the same input are accepted

- **Location:** `CommandNodeCreator.java:119-126` checks only the bare-command case. `Arguments.validateTrailingArguments`
  checks a single branch.
- **Description:** `../../../../docs/design/optional-variadic-arguments.md` rejects two actions for the bare command "since each
  would handle the bare command". The same ambiguity at a deeper depth is not rejected. In
  `argument(w)` + `argument(w, n.optional(7))`, the input `a x` runs the shorter branch. The longer branch's default
  is dead code, and the order of definition decides the result without any error. The same happens for two identical
  branches: the later one's executor silently replaces the earlier one's.
- **Evidence (scratch `ambiguousExecutable`):** `[a x] -> [short x]`, `[a x 1] -> [long x1]`.
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Extend the bare-command check to every shared path. Reject two branches that are
  executable at the same merged node.

## A1-05 — P3 — A first argument named `help` merges with the generated help literal

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

## A1-06 — P2 — Exponential option-tree growth with no fail-fast guard

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

## A1-07 — P3 — `repeatable(max)` silently drops modifiers

- **Location:** `CommandOption.java:266-288`
- **Description:** The returned option copies only the aliases, the description, the permission and the suggestion
  argument. `required()` called before `repeatable` is lost, and so are `requires(...)` validators. For `Options.of`,
  the argument's required-ness is lost too, because `option.argument` is not set. The Javadoc mentions the limit, but
  `../../../../docs/options.md` shows help output `(required, up to 5 times)`. The user-facing docs say only "call it before
  `requires(...)`", which reads as if `required()` survives.
- **Evidence (scratch `repeatableRequired`):** `Options.string("tag",'t',"").required().repeatable(3)` →
  `isRequired() == false`. `/r` runs with `tags=[]` instead of failing with "Missing required option".
- **Confidence:** High. Verified by repro.
- **Suggested correction:** Copy `required` and the validators, or throw in `repeatable` when modifiers that it would
  drop are already set.

## A1-08 — P3 — An argument child after a parent option redirect sees a truncated context

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

## A1-09 — P3 — Minor validation gaps (verified by trace)

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
   tokens that follow it. `../../../../docs/options.md` mentions only variadic arguments as the place where options cannot follow.

## Areas checked and found correct

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

## Coverage

- **In depth:** `OptionGraph.java`, `OptionNode.java`, `CommandOption.java`, `CommandNodeCreator.java`,
  `ArgumentCommandNodeCreator.java`, `Arguments.java`, `CommandNameValidator.java`, `CommonCommand.java`
  (registration and tree parts), `CommandRunner.java` (run, option parsing and validation paths),
  `ChainedCommandContext.java`, `VariadicArgument.java`, `CommonArgument.java`, `CommonDurationArgument.java`,
  `ArgRef.java`.
- **Skimmed:** `Options.java`, `CommandTree.java`, `CommandTreeRenderer.java`, `CommandNodeChildren.java`,
  `CommandBuilder.java`, `branch/UnaryArgumentBranch.java`, `branch/ArgumentBranch.java`,
  `branch/RequiredArgumentBranch.java`, `CommonEnumArgument.java`, `CommonCommandContext.java` (input collection only),
  `HelpMessageAction.java` (route only), and the tests `OptionsAnywhereTest`, `OptionRedirectTest`,
  `OptionTreeSizeTest` and `CommandStructureTest` (shared-prefix tests), plus `../../../../docs/options.md`,
  `docs/design/command-options.md`, `docs/design/optional-variadic-arguments.md` and `docs/permissions.md` (checks).
- **Not examined:**
  - The other typed branches (`Bi`..`Hept`, `Required*`) beyond the pattern.
  - The remaining `argument/*` classes (numeric, boolean, literal, object, nameable).
  - `ArgumentSuggestionProvider` beyond its use.
  - Platform conversions: whether Paper's API-mirror argument types parse in `OptionGraph.parseValue` the same way the
    vanilla dispatcher does after a redirect. This is a hypothesis-level risk for `recover` on Paper, not tested.
  - The vanilla `fillUsableCommands` hashing cost on real servers.
  - Re-registration and unregistration in the platform modules.
