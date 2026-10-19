# Round 2 / A: common/src/main (parsing, options, annotation, tree, runtime, messages, help) + backward-compat lens

Base 2c8ffd7a..HEAD (master 0ccd8a27). Round-1 fix commits in common were the focus. Paths are relative to
`common/src/main/java/dev/marulabs/commandlib/`. "Confirmed" means the reviewer ran a throwaway JUnit probe against
HEAD (probes deleted; worktree clean).

## Regressions from round-1 fixes

### A-1 regression, medium: a reused, confirmed parse skips the confirmation until the timeout (da84c44b)
- `Confirmation.java:90-98`
- After a confirmation, `Pending(parse, confirmed=true)` stays in the map. Any later execution with the *same* parse
  object hits the "forked sibling" branch and returns `previous.confirmed == true`, so it runs without asking. A2-3
  showed functions (and any cached `ParseResults`) reuse the same parse on every run. Scenario: player types `/wipe`
  (asked), runs a function containing `wipe` (confirms), and every further run of that function within the timeout
  wipes without a prompt. Before da84c44b the entry was consumed and the next run asked again. The Pending javadoc
  ("a later execution asks again") is false for a reused parse.
- Confirmed: `execute("wipe")` then `parsed = parse("wipe")`, `execute(parsed)` x3 -> wiped=3, asked=1.
- Fix: bound the confirmed entry to the current dispatch (drop it when the confirming dispatch returns, or accept
  siblings only while `clock.instant()` equals the confirming instant), pending the A2-3 token decision. Add the probe
  as a test.

### A-2 regression, medium: an inherited branch's argument-error usage shows `[options]` of the wrong command (0ca0fae6)
- `CommandRunner.java:163` and `:315-323` (`hasUsableOption`), `HelpMessageAction.java:137-142` (`branchUsage`)
- `hasUsableOption` checks the runner's own `optionGraph.options()`, but `argumentsList` also holds inherited parent
  branches. When `set` in `/config <key> set` fails on the parent's `<key>`, the usage is `/config ... <key>` but the
  `[options]` tag follows whether `set` has options. Before the fix it used the branch owner's options.
- Confirmed: `config` without options, `<key>` = int with validator `x > 0`, child `set` with `Options.flag("force")`;
  `config 0 set` -> `"/config [options] <key>"` (expected `"/config <key>"`). Inverse case (only parent has options)
  drops the tag.
- Fix: compute the tag from `arguments.owner()`'s options filtered by `option.permissionName(owner, prefix)`, as
  `help()` does.

### A-3 regression, low: bare `/x` borrows another branch's executor (bc198cba)
- `ArgumentCommandNodeCreator.java:160-186`, called for the bare runner at `CommandNodeCreator.java:175-183`
- The bare runner calls `runner(allOptional.get(0).first(), ...)`. If that all-optional branch has no executor, the
  new lookup borrows the executor of any branch that runs at `<a>`, including one where `<a>` is required, so input
  ending at the literal runs it without the argument.
- Confirmed: `argument(new StrArg("a").optional("dflt"))` + `argument(new StrArg("a")).execute(...)`; `x` ->
  `[command.failed, Check the console for details.]` (before: help). `x q` still works.
- Fix: never borrow for the bare runner (the input never reaches the argument node), or reject an all-optional
  branch without an executor.

### A-4 regression, medium: a generic superclass method is read twice (1b8af338)
- `annotation/AnnotationParser.java:362-377` (`methods()`)
- Dedup key is `(name, getParameterTypes())`. `abstract class Base<P> { @Execute void run(ctx, @Name("p") P p) }` +
  `class Sub extends Base<String> { @Override @Execute void run(ctx, @Name("p") String p) }`: keys `(run,[String])`
  and `(run,[Object])` differ, the bridge in Sub is synthetic and skipped, so `Base#run(Object)` is read as another
  method and parsing fails ("parameter p has no argument type for java.lang.Object"). Same when the override has no
  annotations. Parsed fine before 1b8af338.
- Confirmed by test.
- Fix: when a class declares a bridge method (`isBridge()`) and also a non-bridge method of the same name/arity,
  reserve the bridge's erasure key so the superclass declaration counts as overridden (a pure visibility bridge must
  not mask).

## Fixes that are only partly done

### A-5 bug, medium: A1-2 fix covers only StringArgumentType; other settings-bearing types without equals still merge
- `CommandNameValidator.java:113-129`
- The rule "a type without its own equals has no settings" is wrong for vanilla `EntityArgument` (single /
  playersOnly), `ScoreHolderArgument` (multiple), `TimeArgument` (minimum), `ResourceArgument` /
  `ResourceKeyArgument` (registry key). E.g. `PlayerArgument("target")` and `EntitiesArgument("target")` branches pass
  validation and share one node, so one branch parses with the other's type.
- Confirmed with a custom type (field `max`, no equals): `<v:max2> <more>` + `<v:max10>` register; `x abcdef` is
  rejected. Vanilla EntityArgument case unconfirmed (no platform run).
- Fix: for a type without equals, compare non-static fields reflectively (at least primitives, enums, String,
  ResourceKey), or treat "no equals" as "different unless same instance". Add a platform test with player vs entities.

### A-6 bug, low: A1-6 fix left the most common paths with a null getMessage()
- `exception/ArgumentParseException.java:95-99` (`ofIncorrectInput`, used by every `validator(...)` failure),
  `:109-122` (`ofCommandSyntaxException`), the public Consumer constructor `:141`;
  `exception/ArgumentValidationException.ofIncorrectInput`; `exception/CommandPrerequisiteException.java:12-29`
  (no-arg and Caption constructors). `OptionParseException(Caption, ..)` sets a message, `CommandPrerequisiteException`
  does not: inconsistent.
- Unconfirmed (code reading). Error formatters / handlers reading `CommandError.exception().getMessage()` see null.
- Fix: give each a message (input + argument name; `e.getMessage()` + cause for CommandSyntaxException; caption key +
  placeholders).

### A-7 bug, low: A1-16 fix still names `@Flag`/`@Option` parameters `argN`
- `annotation/AnnotationParser.java:1379-1383` (`error(Method, Parameter, ...)`)
- Option parameters may not carry `@Name` (rejected at `:1023`), so without `-parameters` an error reads
  `parameter arg1 has @Flag, which needs a boolean or int parameter`.
- Confirmed by test.
- Fix: fall back to non-empty `Flag.value()` / `Option.value()` before `parameter.getName()`.

## New bugs

### A-8 bug, medium: `option(...).repeatable(n)` returns an unregistered option, and getOption silently returns its default
- `CommandOption.java:293-320`, `CommonCommandContext.java:171-179`
- `option()` returns the registered instance and `aliases/requires/description` mutate it, but `repeatable()` returns
  a copy. The natural chain `var tags = option(Options.string("tag","")).repeatable(3)` keeps the single option in the
  tree and `tags` is never registered; `getOption(tags)` returns `[]` with no error.
- Confirmed: `x --tag a` -> `tags=[]`; `x --tag a --tag b` -> CommandSyntaxException.
- Fix: make `repeatable()` throw once the option is registered, and make `getOption`/`hasOption` throw for an option
  the running command does not declare (the runner has `optionGraph.options()`).

### A-9 bug, low-medium: interface default methods with `@Execute`/`@Subcommand` are silently skipped
- `annotation/AnnotationParser.java:364` (walks only `getSuperclass()`)
- `interface I { @Execute default void run(ctx) }` + `@Command class X implements I {}` parses, but the command has
  no executor (running it shows usage). Same silent skip A1-12 reported, now for interfaces.
- Confirmed by test.
- Fix: walk interfaces too, or throw when an interface in the hierarchy declares command methods.

### A-10 bug, low: suggestion providers of a shared argument node come only from the first branch
- `ArgumentCommandNodeCreator.java:65` with the merge at `CommandNodeCreator.java:192-212` (Brigadier `addChild`
  keeps the first node's suggestions)
- Requirements are OR-merged across sharing branches, suggestions are not. With the A1-1 pattern
  (`@Execute(@Name("team"))` + `@Subcommand("<team> add")`), a `@Suggest` on one of them is dropped depending on
  order.
- Confirmed: branch A `<team>` suggests `alpha`, branch B `<team>` suggests `bravo`; `x ` -> `[alpha, help]`.
- Fix: merge the providers of all sharing branches (dedup by identity), or reject sharing branches whose providers
  differ.

### A-11 bug, low: defaults are not checked against `@Range`
- `annotation/AnnotationParser.java:905-927` (arguments), `:1155-1162` (options); `Options.integer(name, default,
  min, max)` does not check either.
- `@Range(min=1,max=10) @Default("0") int n`: `r` runs with n=0, `r 0` is rejected. `@Option("n") @Range(min=1,
  max=10) int n` without `@Default` yields 0. Same for `@Optional` numerics (empty value 0).
- Confirmed by test (both).
- Fix: reject a default outside the range in `parseDefault` and in `Options.integer`; require `@Default` when the
  range excludes the implicit 0.

### A-12 bug, low: `List<? extends T>` parameters are rejected
- `annotation/AnnotationParser.java:1298-1312` (`elementType`)
- A `WildcardType` element fails with "needs a List of a class". Kotlin emits `List<? extends Player>` for a
  `List<Player>` parameter of a non-final type, so Kotlin annotated classes with a variadic/repeatable `List<Player>`
  cannot be parsed. Java behaviour from code; Kotlin effect unconfirmed.
- Fix: resolve a wildcard to its single upper bound.

## Doc / message issues

### A-13 doc-wrong / improvement, low: a silent ErrorFormatter plus an AssertionError now sends an empty failure line (27eacadf)
- `CommandRunner.java:354-361`; `ErrorFormatter.java:9`, `docs/errors.md:47`
- `fail` throws a message-less `CommandFailure` whenever an AssertionError was recorded, which on a real server is
  an empty red line (production `assert` under `-ea`, assertion libraries). Docs still say "a failure for which it
  sends nothing only returns 0". Unconfirmed (code reading).
- Fix: let the testers read the recorded AssertionError from the context without throwing a failure, or update both
  docs.

### A-14 doc-wrong, low: CHANGELOG advice for removed `sendComponent` misleads generic code (2b3561dd)
- `CHANGELOG.md:63`; `CommonCommandContext.java:186-190`
- Code generic over `CommonCommandContext<S, C>` that called `sendComponent(C)` compiles against
  `sendMessage(@Nullable Object)` and sends `toString()`. Unconfirmed (overload read).
- Fix: "on a platform `CommandContext`, pass it to `sendMessage`; in common code, use `RichText`".

### A-15 doc-wrong, low: `CommandError#usage()` promises a usage for every ARGUMENT/VALIDATION error
- `CommandError.java:59-63` vs `CommandRunner.java:134-141`: an option value (Options.of) parse failure gives
  ARGUMENT/VALIDATION with `usage == null`. Unconfirmed.
- Fix: document null for option-value errors, or pass the usage.

### A-16 improvement, low: builder errors from annotations do not name the class/method
- `annotation/AnnotationParser.java:713,716`, `finishOption`, `Options.*`, `argument.variadic(max)`
- `@Cooldown(0)` -> `duration must be positive: PT0S`, `@Confirm(seconds=0)` -> `timeout must be positive`, invalid
  option names/aliases, `@Variadic(max=0)` - none say where. The class javadoc promises errors that name the method
  and parameter.
- Fix: wrap those builder calls and rethrow `IllegalArgumentException(source + ": " + msg, e)`.

## Spec questions

### A-17 spec-question, low: option arguments are parsed before positional arguments
- `CommandRunner.java:119` (`parseOptions`) runs before the argument loop (`:147`). An `Options.of` argument's
  validator/transformer/default that calls `ctx.getArgument("target")` throws `IllegalArgumentException: No such
  argument` -> "unexpected error". Confirmed (`x --o a b`).
- Fix: parse branch arguments first, or document that option arguments cannot read positional arguments.

### A-18 spec-question, low: parent options before an argument child are accepted and dropped
- `OptionGraph.java:206-212`; `docs/options.md:166-167` shows `/config -f <key> set` as working, `:254-260` says a
  child does not inherit options (and `/game -f start` is rejected). In `/config -f difficulty set` only `set` runs
  and `-f` is ignored; `CommandOptionTest.argument_child_does_not_count_the_options_of_its_parent` pins
  `force=false`.
- Fix: choose one: reject the input, or expose the parent's options to the child; align docs.

### A-19 spec-question, low: class-level annotations on a superclass are ignored
- `annotation/AnnotationParser.java:331,345`; none of the annotations is `@Inherited`. Superclass methods are read
  since 1b8af338, but `@Description`, `@Permission`, `@Cooldown`, `@Condition`, `@Middleware`, `@MutuallyExclusive`,
  `@CaptionBundle` on an abstract base are silently dropped.
- Fix: walk superclasses with "most derived wins", or throw when a superclass carries command annotations.

### A-20 backcompat / spec-question, low: two ways to build value options with different default semantics
- `Options.java:61-190` (`bool/integer/longValue/floatValue/doubleValue/string`) vs `Options.of(name, argument)`
- Round 1 kept the typed factories because routing them through `Options.of` changes default/required behaviour.
  Pre-GA this leaves two overlapping APIs: the typed ones take a mandatory default and bypass argument
  validators/suggestions; `Options.of` derives required/default from the argument. `@Range` is honoured on long/
  float/double arguments but rejected on options of those types (`AnnotationParser.java:1156`) because only
  `Options.integer` has bounds.
- Suggest: decide whether the typed factories stay; if yes, add bounded long/float/double or route `@Range` options
  through `Options.of`; if no, delete them.

### A-21 backcompat / spec-question, low: `addPreprocess(Predicate)` overlaps `addMiddleware` and `addPrerequisite`
- `CommonCommand.java:509-518`, `CommandBuilder.java:291-295`
- Kept from 0.17 as a middleware wrapper. Three gate mechanisms (prerequisite throws, preprocess returns false with
  buffered messages, middleware) for the same job. If the buffered-message semantics are not wanted as a separate
  API, delete it pre-GA and document `addMiddleware`.

## Not re-reported
- A1-17 `@Optional` + `@Default` on arguments is still accepted (triage: skipped as merely redundant).
- A1-18 duplication still present.

## Checked and fine
- bc198cba (normal case, both orders, permission/visibility of the running branch, inherited arguments), c836b171,
  2132823d (also the help node's graph), 4afd26e5, 5c9c0fec, 710a3709, 0b45173f, 01096424, ad5c45ab, 6395c5f4 (no
  Consumer mirror in builder/Kotlin; samples return boolean).
- en/ja caption properties match `Caption` keys (ja intentionally omits `unexpected_error`), placeholders consistent.
- FunctionLoadErrorFilter tag-message handling, PlatformAdapterHolder caching.
- No `@Deprecated` or compat shims in common/src/main.

## Not checked
- Real-server behaviour of A-1, A-13; platform `deliver` wiring; vanilla EntityArgument case of A-5; Kotlin runtime
  of A-12; optionsAnywhere combined with the bc198cba borrowing.
