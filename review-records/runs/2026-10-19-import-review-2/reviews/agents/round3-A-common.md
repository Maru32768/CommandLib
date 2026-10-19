# Round 3 – A: common / common-kotlin (main)

Scope: `git diff 0ccd8a27..HEAD -- common/src/main common-kotlin/src/main` (round-2 fixes A-2..A-19, A2-4 single walk,
cooldown-bypass entry, suggestion-failure recovery). common-kotlin has no changes in the range.
Confirmed items were checked with a throwaway JUnit probe in `common` (deleted afterwards); probe names are given.

Checked and fine: bare runner (`bareRunner`), `CommandFailure.replayed` (restored in `finally`, no leak; async late
failures do not go through `fail()` so behaviour is unchanged there), `CommandTree.permissionConfigs` single walk and
duplicate bypass entries (all platforms replace or putIfAbsent), `hasUsableOption(owner)`, `checkDefault`/`checkBounds`,
`ParseException` wrapping, ArgumentParseException/CommandPrerequisiteException messages, `PermissionConfig` going
package-private (it did not exist in 0.17.2).

---

## R3A-1 bug, medium — reflective `sameType` rejects vanilla types that hold a parser record
`common/src/main/java/dev/marulabs/commandlib/CommandNameValidator.java:137-169` (`sameSettings` / `isValue`)

`isValue` treats every field whose class overrides `equals` as a setting. Records override `equals` but compare their
own collaborator fields by identity. From 1.21.5 vanilla `ItemPredicateArgument` extends `ParserBasedArgument`, whose
only field is `Parser<T> parser`, a record (`PackratParser(rules, top)` in yarn) built anew for each instance.
`ResourceOrIdArgument` (`RegistryEntryArgumentType`) also holds such a parser and an `Optional<registry impl>`. So two
separately created instances never compare equal, and two branches that share `<item>` fail at registration with
"Arguments named 'item' share one node ... but parse differently". Annotated overloads always create a separate
argument per parameter, so `@Execute clear(Predicate<ItemStack> item)` beside
`@Execute clear(Predicate<ItemStack> item, int count)` breaks on modded 1.21.8 and Spigot 1.21.5+. Before round 2
these merged.

Evidence: confirmed with a probe (`p5`) that uses an ArgumentType whose field is a record-like value holding
`new Object()`. The registration threw exactly that message. The fields come from `javap` on the 1.21.8 yarn jar:
`ParserBackedArgumentType.parser`, the `PackratParser` record, and `RegistryEntryArgumentType.{registry Optional, parser}`.

Fix: compare only known plain values: boxed primitives, `String`, enums, and `Class` by identity. Keep recursing into
`ArgumentType` fields and skip everything else, which keeps the old "merge unless known to differ" behaviour.

## R3A-2 regression, medium — a shared argument instance across branches only suggests to the first branch's senders
`common/src/main/java/dev/marulabs/commandlib/ArgumentCommandNodeCreator.java:63-96`

The A-10 merge deduplicates providers by argument identity (`suggesting`). Each provider is guarded by its own
`branchRequirement(branch)` and no longer by the OR'ed requirement. When two branches reuse the same `CommonArgument`
instance (a common pattern, see `CommandStructureTest.shared_argument_instance_keeps_executor_per_branch`) with
different permissions or `visibleWhen`, the second branch is skipped. Its senders then get no suggestions, even though
the node is visible to them. Which branch wins depends on build order.

Evidence: confirmed. `argument(team).permission("perm.a")` and `argument(team, member).permission("perm.b")`, where the
sender has only `perm.a`: `suggest("x ")` returned `["help"]` without `alpha` (probe `p1b`). With `perm.b` it works
(`p1`).

Fix: key a `Map<CommonArgument, Predicate<S>>` by identity and OR the requirement of each further branch that shares
the instance. Then build one provider per entry.

## R3A-3 bug, medium — a suggestion provider that throws synchronously still breaks completion and now takes sibling branches with it
`ArgumentCommandNodeCreator.java:156-183, 207-210`; `merged` at `:103-116`

e9d32a70 only recovers from asynchronous failures. A user `SuggestionProvider` that throws inside `action.accept(...)`
reaches the outer `catch (Throwable) { printStackTrace; throw e; }`. A vanilla `listSuggestions` that throws
synchronously goes the same way. Either one propagates out of `CommandDispatcher.getCompletionSuggestions`. This is the
most common failure, a bug in a lambda. Since A-10, `merged` calls each branch's provider in one loop. One throwing
provider therefore also drops the suggestions of every other branch that shares the node.

Evidence: confirmed. One branch's provider throws `IllegalStateException("boom")` and a sibling suggests `bravo`.
`runner.suggest("x ")` threw `boom` (probe `p6`).

Fix: wrap `action.accept(suggestionBuilder)` and the `argument.type().listSuggestions(...)` call in try/catch, call
`reportSuggestionFailure`, and continue. Either remove the outer rethrow or turn it into "report and return what was
added".

## R3A-4 bug, low — merged suggestions show duplicates when they carry String tooltips
`ArgumentCommandNodeCreator.java:103-116`

`Suggestions.merge` deduplicates by `Suggestion.equals`, which includes the tooltip. `Suggestion(String, String)` wraps
the tooltip in a new `LiteralMessage`, which has no `equals`. Two branches with identical providers therefore list the
same entry twice. The A-10 commit says separate identical providers are the expected case.

Evidence: confirmed. Two `<team>` branches, each with `b.suggest("alpha", "tip")`, gave `["alpha", "alpha", "help"]`
(probe `p3`).

Fix: in `merged`, keep the first suggestion per (range, text).

## R3A-5 regression, low — a covariant-return override without annotations is rejected as "changes its parameter types"
`common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java:366-376, 428-447`

`overrideOf` also matches the bridge javac emits for a covariant return type. Example: the base declares
`@Execute CompletionStage<Void> run(ctx)` and the subclass declares `CompletableFuture<Void> run(ctx)`, which is a valid
`@Execute` return type. The subclass override has the same parameters, so `key(override) == key(bridge)`. If the
override does not repeat the annotations, the base method gets a misleading `ParseException`. A same-signature override
without covariance takes the base's annotations instead, as A1-12 intended.

Evidence: confirmed. Probe `p4` threw "AsyncBase#run(TestCommandContext) is overridden by
CovariantOverride#run(TestCommandContext), which changes its parameter types...".

Fix: record a bridge in `overrides` only when
`!Arrays.equals(bridge.getParameterTypes(), override.getParameterTypes())`.

## R3A-6 spec-question, low/medium — `getOption` throws in a parent or default middleware for children without the option
`CommonCommandContext.java:181-184`, `CommandRunner.java:45-47, 72-74`

The declared set is the running command plus its parents. A middleware on a parent, or from `CommandDefaults`, that
reads an option registered by only some children now fails with an unexpected error on the other children. Before, it
got the default. This is documented in docs/options.md:87 and was flagged as a risk in triage, so this item is a design
choice. Note that a narrower check catches the A-8 target, the unregistered `repeatable` copy, without this cost:
`if (!option.registered) throw ...`. That check also lets you delete the `declaredOptions` plumbing in
CommandRunner and CommonCommandContext.

Evidence: confirmed. A root middleware reads `verbose`, which only child `a` registers. `root b` never reached the
middleware's log and failed (probe `p2`).

Fix (if accepted): replace the declared-set check with `option.registered`.

## R3A-7 regression, low — `repeatable()` on an already registered option blocks reusing a shared option constant
`common/src/main/java/dev/marulabs/commandlib/CommandOption.java:296-302`

`option()` registers the same instance, with no copy, so option constants are meant to be shared. With
`cmdA.option(TAG)`, a later `cmdB.option(TAG.repeatable(3))` now throws. In the other order it works, so the result
depends on registration order. Reading the wrong copy is already caught by `getOption` (and would be by the check in
R3A-6).

Evidence: by code reading.

Fix: drop the guard in `repeatable`. Rely on the `getOption` check.

## R3A-8 improvement, low — interface order can pick a super-interface's declaration over a sub-interface's override
`AnnotationParser.java:397-414` (`hierarchy` / `addInterfaces`)

For `class C implements I, J` where `J extends I` overrides an annotated default method of `I`, `getInterfaces()` gives
`[I, J]`. So `I.run` is collected first and its annotations apply, not `J`'s. The same happens with
`Base implements J, Sub extends Base implements I`. Invocation still dispatches to `J`'s body. Only the annotations are
wrong.

Evidence: by code reading (unconfirmed by test).

Fix: order the collected interfaces so that a subtype comes before its supertypes. One way is to skip an interface if
any other collected interface is assignable to it, and append it after them.

## R3A-9 spec-question, low — the bypass node replaces a same-named permission declared elsewhere
`CommandTree.java:46-65` with Spigot `CommandLib.registerPermissions` / Paper `CommandPermissions.add`

Bypass nodes are user-chosen, for example a general `myplugin.admin`. Spigot and Paper now remove an existing
permission of that name, which can come from plugin.yml with its own default and children. They re-add it with `OP`
and the text "Skips the cooldown of ...", and remove it completely on disable. Velocity and modded keep the first. This
is the same mechanism as command nodes (see R2C-3), but it is more likely to collide here.

Evidence: by code reading.

Fix: decide. Either document it in docs/permissions.md#cooldown-bypass, or skip registering a bypass node that a
permission CommandLib did not register already defines.
