# ponytail review 1 — A: common parsing / tree / options / arguments / annotation

Range: `2c8ffd7a..HEAD` (master 16926819), paths: OptionGraph, OptionNode, Options, CommandOption, CommandNodeCreator,
ArgumentCommandNodeCreator, CommandNodeChildren, Arguments, ArgRef, CommonArgument, VariadicArgument,
ArgumentSuggestionProvider, CommandTree, CommandTreeRenderer, CommandBuilder, CommandNameValidator, packages
argument/, annotation/, suggestion/, branch/, exception/.
Load assumed: one server registers commands once at startup; parsing and suggesting run per player command, many players.

What this change does: it builds the Brigadier tree for commands. That covers their options (a state graph with
redirects, plus "options anywhere"), argument nodes shared across branches, variadic arguments, the annotation front
end, and the snapshot/permission walk (CommandTree). Earlier rounds fixed most issues; the items below are new.

## Should fix

1. **P1-A1: Too many short flags hang or crash the server at startup instead of giving the "too many nodes" error**
   - category: load / bug, severity: medium
   - file: `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java:217-227` (the check) and `:390-420` (`tokens` / `permutations`)
   - What this is: `children()` counts option tokens and throws a clear `IllegalStateException` past `MAX_NODES` (50,000).
   - Problem: the check runs only after `tokens(state.counts)` has generated every order of the state's short flags.
     With k short flags that is sum k!/(k-s)! tokens, each holding a String and a List: 9 flags is about 1M, 10 about
     10M, 11 about 108M.
   - Evidence (confirmed by a throwaway probe, now deleted): `OptionTreeSizeTest.flags(n)` built with
     `CommandNodeCreator` in the common test JVM (536 MB heap):
     - n=9: the friendly error, after 2.7 s.
     - n=10: `OutOfMemoryError` after 46 s.
     - n=11: `OutOfMemoryError`.

     A plugin with 10 or more short flags (rsync-style) stalls or crashes at startup, and nothing says why.
   - Fix: stop generating once over budget. Pass the budget into `tokens`. At the top of `permutations`, before the leaf
     check so inner calls stop too, add `if (tokenCount + dest.size() > MAX_NODES) return;`. The existing check then
     throws as before. About 3 lines. Extend `a_command_whose_options_make_too_many_nodes_is_rejected_at_registration`
     with `flags(12)`: today it runs out of memory, after the fix it fails fast.
   - If we skip it: an ordinary-looking command crashes the server at enable time with an OOM stack trace.

2. **P1-A2: A required or repeatable `@Range` int option cannot be declared** (found by the annotation sub-review)
   - category: bug, severity: medium
   - file: `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java:1238-1250` (`valueOption`)
   - What this is: the A-11 fix checks that an option's default, explicit or the implicit 0, lies inside `@Range`.
   - Problem: `@Option(value = "n", required = true) @Range(min = 1, max = 10) int n` fails with "parameter n defaults
     to 0, outside its @Range; give @Default a value within it".
     - The advice cannot be followed: `@Default` is rejected on a required option (line ~1196) and on a repeatable one
       (line ~1214).
     - `@Option(repeatable = 3) @Range(min = 1, max = 10) List<Integer>` takes the same path.
     - The implicit 0 is never used in either case.
     - `Options.integer(...)` also calls `checkBounds(0, min, max)`, so skipping `checkDefault` alone is not enough.
   - Evidence: the required case was confirmed by a throwaway probe (now deleted). The repeatable case was only read
     (unconfirmed).
   - Fix: when there is no `@Default` and the option is required or repeatable:
     - skip `checkDefault`;
     - pass a value clamped into the range: `Math.max(range.min(), Math.min(range.max(), 0))`. Only Integer reaches
       this path.

     About 4 lines, plus one test next to `OptionOutsideRange`.
   - If we skip it: a natural declaration ("required level 1..10") cannot be written with annotations.

## Nice to have

3. **P1-A3: Names starting with `-` collide with option tokens without an error**
   - category: bug (registration validation), severity: low
   - file: `common/src/main/java/dev/marulabs/commandlib/CommandNameValidator.java:78-108` (and the skip in
     `CommonCommandContext.java:77`)
   - What this is: `CommandNameValidator` exists to reject node-name collisions that Brigadier would merge silently.
   - Problem: option tokens (`-f`, `--force`) are literals under the same node as child commands, but no check covers
     them. Confirmed by probe (now deleted):
     - Command `x` with `Options.flag("force", 'f')` and a child named `-f`: `x -f` runs the child. The flag can never
       be given, and registration does not complain.
     - An argument named `-name`: `CommonCommandContext.collectInputs` drops every node whose name starts with `-`. The
       argument looks missing, and `y Alex` ends in "command.failed / Check the console for details".
   - Fix: in `CommandNameValidator`, reject command names, aliases and argument names that start with `-` or with
     `CommandOption.INTERNAL_NAME_PREFIX`. That is one `if` in `addLiteral` and one in `addArgument`, which also covers
     top-level commands. About 6 lines, plus one test in `CommandStructureTest`.
   - If we skip it: a rare naming choice gives either a flag that silently never works or a runtime "unexpected error".

## Checked, no finding

- OptionGraph redirect recovery (`recover` / `findRegion` / `step`) matches Brigadier on each case checked:
  - Brigadier's literal-first choice of token, and its rule about the space after a value.
  - Quoted values that contain the command name.
  - `execute ... run`, namespaced labels and aliases.
  - Types that parse with the source (`parse(reader, source)`) are parsed again more leniently. Brigadier has already
    accepted that input, and selector permission is checked again when the selector is resolved.
- Shared argument nodes:
  - requirements and suggestions are merged over the branches that share a node;
  - the runner always picks the same branch with an executor;
  - `runsAt` rejects two executors for the same end of input.
- Also checked:
  - VariadicArgument parsing each value again;
  - `CommonArgument.parseNode` for option values;
  - the message and caret changes in ArgumentParseException;
  - CommonDurationArgument (overflow and huge numbers return null);
  - QuotableSuggestions.
- Branch classes: every arity has `executeAsync` and `visibleWhen` overrides. The overloads take lambdas with
  different numbers of parameters, so calls are not ambiguous.
- CommandTree, CommandTreeRenderer, CommandBuilder: thin, and no dead API found beyond the Pending A-20 and A-21.
- annotation/: checked overridden, bridge and interface methods, placeholders, shared options, the order of
  `@Requires` / `@MutuallyExclusive`, injection, `@Suggest` and `@Inherited` containers. No further issue.

Verdict: fix 1 and 2 first.
Not checked:
- An `@Optional` placeholder in the middle of a path, such as `@Subcommand("<team> add")`.
- Two methods that name one placeholder, one with `@Name` and one through the compiled parameter name. Common tests
  compile without `-parameters`.
