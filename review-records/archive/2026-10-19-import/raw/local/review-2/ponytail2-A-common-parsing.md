# ponytail review 2 — A: common parsing / tree / options / arguments / annotation

Range: `2c8ffd7a..HEAD` (master 02fbf5f9), with focus on `16926819..HEAD`. Paths: OptionGraph, OptionNode, Options,
CommandOption, CommandNodeCreator, ArgumentCommandNodeCreator, CommandNodeChildren, Arguments, ArgRef, CommonArgument,
VariadicArgument, ArgumentSuggestionProvider, CommandTree, CommandTreeRenderer, CommandBuilder, CommandNameValidator,
packages argument/, annotation/, suggestion/, branch/, exception/.
Load assumed: one server registers commands at startup (sometimes at runtime); parsing, suggestions and Paper's
async command-tree builds run per player.

What this change does: since the first ponytail pass, three commits touch these paths. a8f6574c stops making short-flag
orders once the node budget is used up (P1A-1). 36426eca lets a required or repeatable `@Range` int option be declared
without 0 in its range (P1A-2). 334695ec rejects command, alias and argument names that start with `-` or the internal
option prefix (P1A-3). All three are correct. The only gap found is a missing CHANGELOG line.

## Nice to have

1. **P2A-1: The CHANGELOG does not list the new name rejection**
   - category: spec-question (docs), severity: low
   - file: `CHANGELOG.md:100-106` (the "Registration rejects trees it used to accept" list). The check is in
     `common/src/main/java/dev/marulabs/commandlib/CommandNameValidator.java:112-118`.
   - What this is: 334695ec makes `addChildren`, `addAliases`, `argument(...)` and top-level registration throw
     `IllegalArgumentException` for names such as `-f`, `--force` or `-name`. 0.17.2 accepted them. Triage itself calls
     this a breaking change.
   - Problem: the CHANGELOG list of trees that registration now rejects has no line for it. Someone upgrading with a
     `-x` child or alias gets a new startup exception that the upgrade notes do not mention.
   - Evidence: read (confirmed by grep: there is no entry for it in CHANGELOG.md or docs/).
   - Fix: add one bullet under that list: "a command name, alias or argument name that starts with `-`, since option
     tokens use those names (`IllegalArgumentException`)". +1 line.
   - If we skip it: upgrade notes are incomplete for a rare naming choice.

## Checked, no finding

- **a8f6574c (P1A-1).** `budget = MAX_NODES - tokenCount` is never negative, because the check after each state throws
  before it could be. `dest` stops at `budget + 1`, so the existing `tokenCount > MAX_NODES` check still throws with
  the same message. Once over budget, each recursion level returns at once, so the extra work is O(k·depth). The
  single-name tokens are counted in `dest` before the permutations start, which is the right count.
  `OptionTreeSizeTest` (including `flags(12)`), `CommandStructureTest` and `AnnotationParserTest` all pass. The whole
  OptionTreeSizeTest class runs in 0.8 s.
- **36426eca (P1A-2).** The clamp `(int) Math.max(min, Math.min(max, 0))` is only reached for Integer: `@Range` on any
  other type is rejected just above, and `checkRange` rejects fractional bounds. The made-up default is never visible:
  - a required option is always given;
  - `@Requires(equalTo)` on a repeatable option is rejected;
  - help and usage do not print option defaults (`defaultValue()` is read only in
    `CommonCommandContext.java:184`).

  An int `@Range` outside int bounds (for example `min = 3e9`) still gives a confusing "defaults to 2147483647"
  message. That was true before this change, it is a rare case, and it is not reported.
- **334695ec (P1A-3).** Every path that adds a name goes through the validator:
  - `addChildren`, `addAliases` (when there is a parent), `addArguments` and `addArgumentChildren`;
  - `validateTopLevel` in `CommandNodeCreator` and Paper's `CommandSet`.

  Annotation paths (`@Subcommand("-x")`, `@Name("-x")`) run inside `at(...)`, so the error names the class and method.
  The `-` skip in `CommonCommandContext.collectInputs` is still needed for real option tokens.
- **The CME the first pass found at runtime on Paper** (triage asked this pass to investigate it). I read Paper's
  sources from the paperweight dev bundle in `~/.gradle/caches/paperweight-userdev`: `Commands.sendAsync` and
  `fillUsableCommands`, `ApiMirrorRootNode.unwrapNode` / `convertFromPureBrigNode`, `PluginCommandNode`, and Paper's
  patched `CommandNode`. Results:
  - No CommandLib path changes a live children map in place on the `paper` artifact.
    - `CommandSet.buildNodes()` builds fresh nodes for each registration.
    - `PluginCommandNode` copies children into a new node.
    - `unwrapNode` adds children only to the converted nodes it has just created. Its `unwrappedCached` is set only
      on those fresh pure nodes, so later registrations never get a cache hit on a live node.
    - The only writes to the live vanilla root go through `CommandNodeChildren.replace`, and on the main thread only.
  - `fillUsableCommands` iterates the children of every live node, not only the root's. So the CME needs some node's
    map changed in place, or an iteration inside a requirement predicate (it runs inside `canUse`).
  - Candidates I could not confirm without the full stack trace:
    - Bukkit's command map writing into the dispatcher root in place (`BukkitBrigForwardingMap.put` /
      `root.removeCommand`), from Paper or another plugin;
    - a CommandLib predicate iterating mutable command state: `isVisibleTo` → `providers()` copies the `providers`
      LinkedHashMaps. A command object would have to be changed after registration for that to happen.
  - No finding. Next step: get the top frames of the logged stack trace. A `LinkedHashIterator` frame directly under
    `fillUsableCommands` (line 527 or 570 in 26.x) means a node map was changed in place. A CommandLib lambda frame
    means the cause is a predicate.
- Re-checked the first pass's "Not checked" items:
  - An `@Optional` placeholder in the middle of a path: `Arguments` rejects an optional or variadic argument with
    children, so it fails at startup.
  - `@Name("team")` on one method and the compiled name `team` on another: `branchAt` refuses this with "declares the
    arguments of '<team>' differently". That is strict and the message is vague, but it fails at startup and is easy
    to fix, so it is not reported.

Verdict: Ship. (P2A-1 is a docs line.)
Not checked:
- The Paper CME root cause. No Docker or server here, and the stack trace was not available.
- Modded builds were not run.
