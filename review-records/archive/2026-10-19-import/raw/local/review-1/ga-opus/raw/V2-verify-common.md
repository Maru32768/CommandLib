# V2: adversarial verification of the common findings

Reviewed HEAD 1b8f17dc, baseline origin/master 2c8ffd7a. All experiments ran in a `git archive HEAD` copy
(`C:/Users/maru3/AppData/Local/Temp/clv2`, deleted afterwards) with one scratch test,
`common/src/test/java/dev/marulabs/commandlib/V2ScratchTest.java`, run by
`./gradlew --configure-on-demand :common:test --tests dev.marulabs.commandlib.V2ScratchTest -i`. The repros were
written from scratch. They use the existing `TestCommandRunner` (`redirectToRoot`, `root()`) and the
`OptionTreeSizeTest.nodes/flags` helpers.

The main working tree has uncommitted one-line edits to `OptionGraph.java` and `ArgRef.java`. They were not
examined; every result below is for HEAD.

| ID | Verdict | Severity (claimed → mine) | In baseline? | Existing test |
|---|---|---|---|---|
| A1-01 | CONFIRMED (corrected: `--force` between the arguments works) | P1 → P2 | No (OptionGraph is new in the range) | No |
| A1-06 | CONFIRMED-WITH-CORRECTIONS (documented) | P2 → P3 | No | Bounds for 4 and 6 flags only |
| C-01 | CONFIRMED-WITH-CORRECTIONS (the bug predates the range) | P2 → P3 | Yes (the nested rule) | No |
| A2-01 | CONFIRMED | P2 → P3 | Yes | No |
| A2-02 | CONFIRMED-WITH-CORRECTIONS (partly documented, platform part from trace only) | P2 → P3 | No (Confirmation is new) | No |
| A2-03 | CONFIRMED | P2 | No (Cooldown is new) | No |
| A2-04 | CONFIRMED | P2 (borderline P3) | Yes (help node unchanged) | No |
| A3-02 | CONFIRMED | P2 → P3 | No (AnnotationParser is new) | No |

## A1-01: CONFIRMED, P2

Repro: `cfg` with `optionsAnywhere()`, a flag `force`/`f`, and `argument(Word a, Word b).execute(...).child((a, b) ->
go)`. `go` reads `a.get(ctx)`.

```
[cfg x y go]          -> 1 [go xy]
[cfg --force x y go]  -> 0 [command.failed, Check the console for details.]
[cfg -f x y go]       -> 0 [command.failed, ...]
[cfg x -f y go]       -> 0 [command.failed, ...]
[cfg x --force y go]  -> 1 [go xy]          <- works: the long name before the last argument is the hub
[cfg --force x y]     -> 1 [own xytrue]      (the parent runner recovers)
control, one argument:      [one -f x go] -> [go x]
control, no optionsAnywhere: [cfg2 -f x y go] -> [go xy]
stderr: IllegalArgumentException: No such argument 'a' exists on this command
        at CommonCommandContext.getArgument(CommonCommandContext.java:141) ... ArgRef.get(ArgRef.java:57)
```

The mechanism matches the report. The trigger is narrower: a long name given between the arguments reaches the
hub and works. Any short name fails, and so does a long name given before the first argument. Nothing in
docs/options.md says this case is unsupported. "Options After the Arguments" allows options before an argument
child. Severity: the opt-in `optionsAnywhere()` must meet a branch of two or more arguments that has an argument
child. The failure is loud (unexpected error), not wrong data. So P2, not release-blocking. P1 is defensible only if
`optionsAnywhere` with argument children is a headline GA feature. `OptionsAnywhereTest.an_argument_child_keeps_its_parent_options_after_the_argument` covers only a one-argument branch.

## A1-06: CONFIRMED-WITH-CORRECTIONS, P3

```
flags 4 nodes=187     36 ms
flags 6 nodes=5427   165 ms
flags 7 nodes=37523  360 ms
flags 8 nodes=298627 2096 ms
8 long-only flags: nodes=1027
```

The numbers match the report (±1 for the root). There is no registration guard. docs/options.md:262-270 ("Limits")
states the growth plainly: "six short flags already make about 5,500 nodes", and it tells users to keep the options
few. The growth is in the short-flag permutations. Long-only flags stay linear-ish (1,027 for 8). The behavior is
documented and avoidable, so it is a hardening request (a fail-fast budget), P3.

## C-01: CONFIRMED-WITH-CORRECTIONS, P3

Tree: `/config <key> mode <value>`. `<value>` executes and also has the child `reset`.

```
help [config]            -> Usage: /config <key> mode <value> reset            (the <value> usage is missing)
help [config k mode help] -> Usage: /config <key> mode <value> reset, /config <key> mode <value>
exec [config k mode v]   -> [mode v]                                          (a valid input)
CommandDocs "/config <key> mode" table -> `/config <key> mode <value>`         (listed)
```

The disagreement is real. The nested copy (`HelpMessageAction.java:102-108`, `children().isEmpty()` only) drops an
executable branch that has children. The top-level copy (:61-63) and `CommandDocs.java:288-300` keep it. A
correction to the report: the nested rule is unchanged from origin/master (`createArgumentChildUsages`), so this
omission predates the range. 59baaa24 (in the range) only added `isExecutable()` to the top-level copy, so it did not
cause the bug and did not fix it either. The impact is one usage line missing from a rare nested shape, so P3.

## A2-01: CONFIRMED, P3

```
direct          [game]          -> /game start, /game <map> <mode>
redirect        [run game]      -> /run start,  /run <map> <mode>
redirect help   [run game help] -> /run start,  /run <map> <mode>
```

`HelpMessageAction.path` (:164-173) takes the first token of `getHandle().getInput()`. Brigadier keeps the full
input in each context of a redirect chain, so on a server the help shows `/execute ...`. The logic is identical in
origin/master (lines 212-215), so the bug predates the range. Severity is P3: it needs generated help or
`SYNTAX_ADVICE` run through `/execute ... run`, usually from a function or by an operator. Help output is cosmetic.
Clicking a usage only fills or runs vanilla `/execute`, which then fails.

## A2-02: CONFIRMED-WITH-CORRECTIONS, P3

Repro: a vanilla-style `literal("each").fork(root, ctx -> nCopies(4, source))` added through `runner.root()`,
then `dispatch("each wipe")` with a `confirm(...)` command.

```
fork result=4 ran=[wiped, wiped] asked=[/each wipe, /each wipe]
```

Forks 1 and 3 store a request, and forks 2 and 4 consume it and execute. Faithfulness:

- `CommandTester` cannot reproduce this. It registers only CommandLib nodes and has no hook for redirect or fork
  nodes. The repro needs the common-test `TestCommandRunner.root()` on raw Brigadier 1.0.18. That matches the
  servers before 1.20.3.
- On a real Spigot or Paper server, `getActor()` wraps `getSender()`, the original sender. See
  docs/vanilla-integration.md:39-50 (the table at :110-111) and `spigot/.../CommandContext.java:57`,
  `paper/.../CommandContext.java:65`. So every fork has the same key. The console has no UUID, so its key is
  `CONSOLE:CONSOLE`, and every fork shares it too.
- Vanilla 1.20.3+ runs forks through `ExecutionContext`/`ContextChain` rather than `dispatcher.execute`. From my
  knowledge of the code, it still runs each source's leaf in turn and suppresses failure messages in forked mode.
  I did not run this on a server.

Correction: docs/errors.md:262-263 already says "each target of a forked `/execute as @a ... run` does not get them
[the confirmation request]". So the missing prompt is documented. The alternating execution is not. It needs
`/execute` access (an operator, a function, or a command block), so P3.

## A2-03: CONFIRMED, P2

Repro: `cooldown(5m)` and a middleware that defers `next.proceed()` to a later point. Two runs go in, then the
deferred work runs.

```
first -> 1 []   second -> 1 []   ran=[healed, healed]   third -> 0 [You must wait 5m ...]
```

`CommonCommand.middlewares()` (:760-780) runs the check first and the start last. docs/errors.md:265 ("The cooldown
starts only when the confirmation and the preprocesses pass") makes the order intended, but not the race.
docs/execution.md names async middlewares as a supported case. On a real server, two command packets from one
player run one after the other on the main thread. A middleware that waits on async I/O lets the second one through.
P2 holds for `/kit`-style rewards.

## A2-04: CONFIRMED, P2 (borderline P3)

```
prereq bare [game]      -> 0 [players only]
prereq help [game help] -> 1 [Usage: /game start]
fmt bare    [fmt]       -> 0 [command.failed, ...] handled=[IllegalStateException: formatter broke]
fmt help    [fmt help]  -> 0 [] handled=[]
```

`CommandNodeCreator.createHelpNode` (:72-89) calls `helpAction` directly and catches with `printStackTrace`. That
contradicts docs/help.md:12 ("after the permission, option, argument and prerequisite checks"),
docs/design/execution-pipeline.md:27-28, and help.md:134 (`UNEXPECTED` covers "the help"). The bypass is unchanged
from origin/master, so it predates the range, but the docs promise the checks. The impact is a help-text leak past
prerequisites and silent formatter failures. The command permission is still checked through `requires`.

## A3-02: CONFIRMED, P3

A fluent `argument(PHRASE reason, Int n)` gives `[g hello 5] -> Usage: /g <reason> <n>`. The command never runs.
`AnnotationParser.java:772-774` is the only `@Greedy` check (its type), and no core check exists (`grep -i greedy`).
That contradicts docs/design/annotation-api.md:39. The mistake shows up on the first manual test and does no harm at
runtime, so P3.
