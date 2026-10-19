# Consolidated findings

Range `2c8ffd7a..1b8f17dc`. Deduplicated across A1–A5, B1–B2, C, D and the two verification passes V1 (platform) and
V2 (common). Source IDs point to the full write-ups (repro code, traces) in `production.md`, `tests.md`,
`complexity.md`, `roadmap.md` and `raw/V1-verify-platform.md`, `raw/V2-verify-common.md`.

## Legend

**Status**

- **Reproduced** — a scratch test in a disposable `git archive HEAD` copy shows the failure. Two independent agents
  reproducing it is marked **Reproduced ×2**.
- **Mutation-verified** (test findings) — a deliberate production defect in a disposable copy survives the whole suite.
- **Traced** — the code path (and, for platform behaviour, cached server/loader bytecode) was read end to end. No
  runtime repro.
- **Plausible** — strong evidence, but a key premise is not checked.
- **Hypothesis** — a stated risk that is not demonstrated.

**Severity** follows the task's P0–P3 definition. Where a verifier changed a reviewer's severity, the final value is
used and the change is noted. "Predates" means the defect is already in `origin/master` and only becomes a GA concern
because GA ships it.

No finding is P0. Nothing in the range is a security vulnerability in the usual sense. The permission findings
(GA-03, GA-13, GA-14) are authorization-correctness issues on specific platforms.

---

## P1

### GA-01 — Forge 1.16.5: commands disappear after `/reload` and are missing while data pack functions load
- **Source:** A5-01, confirmed by V1 (bytecode). **Predates the range.**
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:325-356` (the 1.16.5 `//?} else {` branch).
- **Violated expectation:** registered commands stay registered for the server's lifetime, and data pack functions can
  call them. The 1.18+ branches meet this by registering in `RegisterCommandsEvent`.
- **Trigger:**
  - Forge 1.16.5, run `/reload`. 1.16.5 builds a new `Commands` (dispatcher) inside `DataPackRegistries`, and that
    constructor fires `RegisterCommandsEvent`. CommandLib adds its nodes only once, at `FMLServerStartedEvent`, so the
    new dispatcher has none of them.
  - A function in a data pack that calls a CommandLib command fails to load, because functions load before
    `FMLServerStartedEvent`.
  - `register` called while a server is running: the next singleplayer world gets no commands.
- **Evidence:** traced by A5 and again by V1 with `javap` on the cached Forge 1.16.5 jar.
- **Tests:** none. No modded `/reload` test; IT forge targets do not run `/reload`.
- **Confidence:** high. **Severity note:** V1 says P2 is defensible because `../../../docs/compatibility.md` lists Forge as
  "best effort". It stays P1 because `/reload` is routine and the failure removes every CommandLib command silently.
- **Fix:** register through `RegisterCommandsEvent` on 1.16.5 too; that event already exists there. Add an IT step
  that runs `/reload` and then a CommandLib command.

### GA-02 — No test protects branch permissions under `optionsAnywhere()` (test deficiency)
- **Source:** B1-01 (mutation-verified); related B1-02.
- **Location:** `common/.../OptionGraph.java:280-285`. With `optionsAnywhere()`, the per-state copies made here are the
  *only* argument nodes in the tree. Runtime check: `CommandRunner.java:143-150`.
- **What is missing:** HEAD behaves correctly (B1's probe test passes). But removing the `requires` on the copies (M3)
  survives the whole suite, so the branch shows in the tree and in suggestions. Removing the runtime branch check as
  well (M15) also survives, so a sender without the branch permission can execute the branch.
- **Why P1:** this is the only line of defence for an authorization rule. The tree copying in `OptionGraph` is the code
  most likely to be refactored next (see GA-08 and A1-06), and no test would notice a regression.
- **Fix:** add a test with `optionsAnywhere()` and a branch with `.permission(...)`. Assert that the tree hides the
  branch, that suggestions leave it out, and that execution is refused for a sender without the node. Add the
  revoke-after-parse cases from B1-02 (M2, M4, M8 survived).

### GA-03 — Forge/NeoForge player permission denial is verified nowhere (test deficiency)
- **Source:** B2-01 (traced). Related to GA-13 (A5-04).
- **Location:**
  - `integration-test/src/test/java/dev/marulabs/commandlib/integration/IntegrationTest.java:193-203` sends `deniedProbe`
    on every platform, but the modded fixture never defines that command. The "unknown or incomplete command" error
    satisfies the denial assertion.
  - modded-testing replaces `CommandLib.hasPermission` with a mock, so the production permission path
    (`modded/.../CommandLib.java:313-324`, `PlatformAdapterImpl.java:27-38`) runs in no test.
- **Failure scenario:** a regression that lets every player through on Forge/NeoForge would pass both the unit suites
  and the IT campaign.
- **Fix:** define `deniedProbe` in the modded fixture. Assert the specific permission-failure text, not just "some
  error". Add one modded-testing case that goes through the real `hasPermission`.

### GA-04 — No changelog or release notes for a release with breaking changes (release process)
- **Source:** D-02. Rated P0 by D, set to P1 here: it is a release artefact, not a code defect, and it is cheap to fix.
- **Facts:** no CHANGELOG or release-notes file exists. The range contains `feat!` / `fix(paper)!` commits, removed
  `ArgumentBuilder` / `AbstractCommandContext`, a package move, a removed `bukkit` artifact, and caption-based messages
  (`1458ede5`) that change user-visible output. Success Metric `docs/roadmap.md:652` requires release notes.
- **Fix:** write migration-oriented release notes from the `!` commits and the deleted public types (`git diff
  --diff-filter=D origin/master..HEAD -- '*/src/main/**'`).

### GA-05 — GA exit criteria for platform support are marked done without real-server evidence (release evidence)
- **Source:** D-03 (traced against `docs/roadmap.md:186-187`, `:609-610`).
- **Facts:**
  - Velocity, Fabric, the new modded versions (forge-1.18.2, forge-1.19.2, neoforge-1.21.4, fabric-1.21.4) and Folia are
    `[x]` in `docs/roadmap-history.md:87-92`.
  - No integration target exists for Velocity, Fabric or Folia. The roadmap's own completion rule requires
    target-environment verification.
  - Code paths that run in no test: Fabric `CommandRegistrationCallback`, Fabric with fabric-permissions-api, the Spigot
    Folia refusal (B2-08), and Folia region-thread behaviour.
- **Why it matters:** the GA claim "supports Velocity/Fabric/Folia" rests on unit harnesses only. GA-01, GA-12 and
  GA-14 show that modded behaviour differs from harness behaviour.
- **Fix:** either add one smoke target per new platform, or mark those platforms preview/experimental for GA.

### GA-06 — CI, binary-compatibility gate and Maven Central publishing are absent (release gate; maintainer decision)
- **Source:** D-01. D rated it P0.
- **Facts:**
  - There is no `.github/` directory and no other CI.
  - japicmp is configured only for `:common` and `:spigot` against 0.17.2 (`build.gradle.kts:309-353`) and runs in no
    pipeline.
  - 0.18.0 is not released. Maven Central publishing is not set up.
  - Phase 4 (`roadmap.md:613`) and Phase 5 (`:631`) list these as exit criteria.
- **Context:** project memory records the maintainer's decision (2026-10-08) to defer CI and keep it as a roadmap item
  only, and to move from jitpack to Maven. That makes this a known, intentional gap, not an oversight. It is listed so
  that the GA decision is made consciously: either GA waits for these, or the GA criteria in the roadmap are changed.

---

## P2

### GA-07 — `optionsAnywhere()`: an argument child under a branch of two or more arguments fails when an option precedes the last argument
- **Source:** A1-01 (P1), Reproduced ×2 (A1, V2). V2 lowered it to P2: it fails loudly, never with wrong data, and the
  trigger is narrower than first stated.
- **Location:** `common/.../OptionGraph.java:266-296` (copied argument node is a redirect), `:403-406` (the child's
  graph never recovers the input before it).
- **Trigger:**
  - `cfg -f x y go`, `cfg --force x y go` and `cfg x -f y go` all end in an unexpected error (`No such argument 'k1'`).
  - `cfg x --force y go` and any one-argument branch work.
- **Tests:** the existing option-redirect tests cover only a one-argument branch.
- **Fix:** recover the parent inputs for argument children in the same way as for literal children, or reject this
  shape at registration until it is supported. Add the three failing inputs as tests.

### GA-08 — Kotlin coroutine dispatchers run blocks in place: `yield()` overflows the stack and never yields the server thread
- **Source:** A3-01, Reproduced (A3). The test gap is B1-04.
- **Location:** `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/Coroutines.kt:24-34` (`dispatch` runs
  inline when already on the sync thread), `:112`.
- **Violated contract:** `CoroutineDispatcher.dispatch` must not run the block in place; kotlinx-coroutines 1.8
  documents this. The common pattern of spreading work across ticks with `yield()` / `delay` on the main thread breaks.
- **Trigger:** `executeSuspend { repeat(200_000) { yield() } }` gives `StackOverflowError`. On Spigot/Paper `yield()`
  never gives the server thread back, so the tick is blocked.
- **Tests:** KotlinDslTest "executeSuspend finishes on the sync thread" uses `Runnable::run` as the sync executor, so it
  cannot see this (B1-04).
- **Fix:** always schedule (`scheduler.runSync(block)`); override `isDispatchNeeded` for the fast path. Test it with an
  executor that queues tasks.

### GA-09 — Cooldown is checked at the start of the middleware chain but started at its end
- **Source:** A2-03, Reproduced ×2 (A2, V2).
- **Location:** `common/.../CommonCommand.java:760-781` (check at `:766-768`, start after `proceed`).
- **Trigger:** a user middleware that proceeds asynchronously. `../../../docs/execution.md` recommends this for lookups. Two
  runs started while the first is still waiting both pass the check and both execute; only the third is blocked.
- **Tests:** cooldown tests are synchronous only.
- **Fix:** reserve the cooldown at check time and release it if the run does not reach the executor. Or document that
  the cooldown starts on completion and guard re-entry.

### GA-10 — The generated `/<cmd> help` node bypasses the `CommandRunner` pipeline
- **Source:** A2-04, Reproduced ×2. **Predates the range** in part (the help node existed; the prerequisite/handler
  contract is new).
- **Location:** `common/.../CommandNodeCreator.java:72-89`.
- **Violated contract:**
  - `docs/help.md:12,134`: prerequisites run before help.
  - Error handlers receive failures.
- **Trigger:** `game help` skips the command's prerequisite. A `helpFormatter` that throws sends no message, calls no
  handler, and returns 0. The command permission is still checked.
- **Fix:** run help through `CommandRunner` as a built-in executor.

### GA-11 — Spigot `CommandLib.unregister()` from `onDisable()` throws `IllegalPluginAccessException`
- **Source:** A4-01, confirmed by V1 (bytecode of Spigot 1.16.5 and 1.21.8, and Paper). **Predates the range.**
- **Location:** `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:715-717`, `:784-802`, `:845-853`.
- **Mechanism:**
  - `PluginDisableEvent` fires before `setEnabled(false)`. CommandLib's listener therefore already unregisters
    everything.
  - When the user then calls `unregister()` in `onDisable()`, the plugin already counts as disabled, so the scheduled
    `runTask` throws.
  - Every statement after that call in `onDisable()` is skipped, which can include saving data.
- **Contract:** the Paper artifact makes the same call a no-op (`paper/.../CommandLib.java:528-537`). Spigot/Paper
  parity is a project rule.
- **Tests:** none (B2-07: Spigot register/unregister and the disable cleanup have no unit tests).
- **Fix:** make `unregister` idempotent (return early when nothing is registered), and do not schedule tasks when
  `!plugin.isEnabled()`.

### GA-12 — Fabric production: command blocks count as the console (`requireConsole()` passes)
- **Source:** A5-02, confirmed with corrections by V1. V1's correction: on Forge 1.16.5 the `RCon` string is right, and
  RCON is treated as console either way; only `getType()` reports the wrong type.
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:41-66`. The source type is
  decided by matching class-name strings.
- **Trigger:** on a real Fabric server class names are intermediary-mapped (`class_…`), so `CommandBlock` never matches.
  A command block passes `requireConsole()` and `getType()` returns `CONSOLE`.
- **Tests:** none. The dev environment uses named mappings, so tests there would pass.
- **Fix:** use `instanceof` against the mapped types (`BaseCommandBlock`, `MinecraftServer`, `RconConsoleSource`).

### GA-13 — Modded: every non-player source passes CommandLib permission checks
- **Source:** A5-04, confirmed by V1. **Documented design. Predates the range.**
- **Location:** `modded/src/main/java/dev/marulabs/commandlib/PlatformAdapterImpl.java:27-38`.
- **Effect:**
  - A level-2 operator can use `execute as <non-player entity> run …`, and a command block can run, commands whose
    default is level 4 / OP. Vanilla, Spigot and Paper refuse both.
  - Commands that default to `NONE` are reachable as well (V1).
  - `../../../docs/errors.md` has no Fabric row.
- **Decision needed:** keep the behaviour (and document it on every platform page) or use the source's permission level.
  Paired with GA-03, nothing tests the modded permission path at all.

### GA-14 — Branches sharing a first argument name merge into one node with the longest branch's permission, visibility and suggestions
- **Source:** A1-02, Reproduced (A1). **Predates the range.**
- **Location:** `common/.../CommandNodeCreator.java:158-181`.
- **Trigger:** `/t <name>` with permission `a` and `/t <name> <count>` with permission `b`. A sender who has only `a`
  gets "Incorrect argument" for `/t x`.
- **Fix:** OR the requirements of merged branches and check each branch's own requirement at execution (the runtime
  check in `CommandRunner.java:143-150` already exists). Or reject such definitions at registration.

### GA-15 — Same-named branch arguments with different types are accepted and fail at runtime
- **Source:** A1-03, Reproduced (A1). **Predates the range.**
- **Location:** `CommandNameValidator.java:92-99`, `CommandNodeCreator.java:158-179`.
- **Trigger:** two branches with `<n>` as an int in one and a string in the other. `/t 5` throws
  `IllegalArgumentException` at execution.
- **Fix:** reject the definition at registration (the validator already sees both).

### GA-16 — Integration/test-harness assertions that cannot fail on the cases they claim (test deficiencies)
- **Source:** B2-03, B2-02, B1-03, B2-11.
- **Items:**
  - **B2-03:** on `mohist-1.20.1` (commit `ad060861`), four player-side checks also accept "Unknown command"
    (`IntegrationTest.java:175-221`). They can no longer detect a CommandLib regression on that target, yet
    `../../../docs/compatibility.md` still reports every case passing.
  - **B2-02:** every tester replays `CommandFailure.sendMessages()` instead of the server's message path. Mutations to
    how failure lines are joined (M2–M4) survive on Spigot, Paper and Velocity.
  - **B1-03:** an `AssertionError` thrown inside an executor becomes `command.failed` plus "Check the console", and
    `CommandTester.execute` returns 0. A test whose executor asserts and fails still passes unless it checks the result.
    This is documented, but it is a trap for every library user.
  - **B2-11:** the IT coverage guard only greps for `new XArgument(`, and the report check accepts "≥1 test, 0
    failures".
- **Fix:**
  - B2-03: make the Mohist allowance case-specific and record it in `compatibility.md`.
  - B1-03: rethrow `AssertionError` (and `Error`) from the tester.
  - B2-02: assert the joined failure output in one platform test per tester.

### GA-17 — paper-testing mocks accept input that real Paper rejects, and the reverse
- **Source:** B2-04, B2-05 (probe-verified), B2-06 (mutation-verified).
- **Items:**
  - **B2-04:** worlds are looked up by name (`paper-testing/.../CommandTester.java:291-294`). Real Paper takes the
    dimension key: `tp world` passes in the tester and `tp minecraft:overworld` fails there.
  - **B2-05:** the tester accepts `SURVIVAL` / `Creative` as game modes (`:316-317`), and `entities @a` returns
    non-player entities.
  - **B2-06:** ten Paper arguments are never executed in paper-testing. The same key mutation survives on Paper (M1b,
    M7) and is caught on Spigot (M9, M10). This breaks test parity between Spigot and Paper.
- **Fix:** match vanilla's parsing in the mocks (keys, lowercase game modes, `@a` players only), and port Spigot's
  argument-execution coverage rule to paper-testing.

### GA-18 — Lifecycle tests are missing for Spigot register / unregister / disable and for late registration on Paper
- **Source:** B2-07. It overlaps GA-11 and A4-02/GA-27.
- **Fix:** unit tests for register → unregister → register, unregister twice, and unregister from `onDisable`.

### GA-19 — Documentation and roadmap claims that the code does not support
- **Source:** D-05, D-06, D-07, D-08, D-10, D-12, D-13, A3-05. D-05 was P1 in D's report and is P2 here: the claim is
  wrong only on some platforms.
- **Items (each with file:line in `roadmap.md`):**
  - **Vanilla-style feedback marked `[x]` "on all platforms":** no test covers `sendCommandFeedback`,
    `logAdminCommands` or silent sources. Paper falls back silently to `sendMessage` on `NoSuchMethodException`
    (`paper/.../CommandContext.java:139-165`). Velocity ignores `broadcastToOps` (`velocity/.../CommandContext.java:100-117`).
  - **Velocity annotation API "snapshot-verified":** nothing references Velocity `AnnotatedCommands` in a test. The docs
    list `UUID` as a general type, but Velocity has no `UUID` argument.
  - **Resolve-error "context line":** promised in `roadmap-history.md:29`, absent in `ArgumentParseException.java:109-122`.
  - **Silently weakened requirements:** "multi-value options" (roadmap.md:287) is still open while the summary row is
    `[x]`, and the "reusable condition registry" became annotation-only `@Condition`.
  - **Migration guides and README contradict the code:** `from-acf.md:326` vs `:216`, `from-commandapi.md:315` vs
    `:211`, `from-cloud.md:102`, and README's claim of 1.16.5–26.x for modded (modded stops at 1.21.4).
- **Fix:** correct the docs before GA. They are the contract users will read.

### GA-20 — The 2026-10-16 real-server campaign cannot be reproduced from the repo
- **Source:** D-04. P1 in D's report, P2 here.
- **Facts:**
  - The newest `../../../integration-test/build/test-results` in this checkout is from 10-15 (checked), and those results include
    failures.
  - The 10-16 runs most likely happened in deleted worktrees, so this is missing evidence, not proof of a false claim.
  - Only 20 of the 61 Spigot/Paper targets were in the campaign. `paper-26.3-spigot-jar` starts no bot.
- **Fix:** rerun the campaign at the GA commit and keep the reports (attach them to the release).

### GA-21 — Per-platform `AnnotatedCommands` duplicate the common type mapping (maintainability)
- **Source:** C-02.
- **Facts:** about 50 lines that map String, number, Boolean, Enum and Duration (with `@Range` / `@Greedy`) are repeated
  in 4 platform files. A fix to one copy has to be made four times. This is the parity rule's risk area.
- **Fix:** move the mapping into `AnnotationParser` with constructor references (about −140 lines).

### GA-22 — Benchmark regression detection is not operational
- **Source:** D-14, B1-09.
- **Facts:**
  - `jmhCompare` is manual.
  - The time metrics in `../../../benchmark/baseline.json` have errors up to about 4× the score, against tolerances of 10–20%.
    Only the allocation numbers are usable.
  - `ExecuteBenchmark` never checks that its inputs succeed, so it could be timing failures.
  - The baseline contains a local JDK path (C-09).
- **Fix:** gate on allocations only, assert that inputs succeed, and strip the baseline to the fields that are compared.

---

## P3

| ID | Source | Location | Finding | Status |
|---|---|---|---|---|
| GA-23 | A1-06 (V2: P2→P3) | `OptionGraph.java:333-393` | The option tree grows as Π(max+1): 6 short flags make 5.4k nodes, 7 make 37.5k, 8 make 298k (2.1 s). There is no registration guard. `docs/options.md:262-270` warns about the growth. Add a node-count limit with a clear error. | Reproduced ×2 |
| GA-24 | C-01 (V2: P2→P3, predates) | `HelpMessageAction.java:59-66` vs `:97-108`, `CommandDocs.java:288-300` | The rule for which branch is a usage exists three times and the copies disagree. A nested branch that both executes and has children is missing from root help but listed in docs. Use one shared predicate. | Reproduced (V2) |
| GA-25 | A2-01 (V2: P2→P3, predates) | `HelpMessageAction.java:164-173` | Help and syntax advice take the first word of the whole input as the root, so through `/execute run game …` they show `/execute start` or `/run start`. | Reproduced ×2 |
| GA-26 | A2-02 (V2: P2→P3) | `Confirmation.java:70-91` | Forks of one input by the same sender confirm each other: with 4 forks, 2 ran without confirmation. `docs/errors.md:262-263` covers forked prompts only partly. Spigot/Paper actor identity is traced, not run. | Reproduced ×2 (raw Brigadier) |
| GA-27 | A4-02 (V1: P2→P3, predates) | `paper/.../CommandLib.java:52`, `:327-348`, `:573-633` | With a shaded and relocated copy, a first `register()` after `onEnable` always throws. The Javadoc says to register at initialization, but the `Mode.Direct` fallback, its diagnostics text and test `fallback_registration_is_restored_after_reload` only cover an unrelocated shared copy, which does not happen in production. Delete or fix the fallback. | Traced ×2 |
| GA-28 | A3-02 (V2: P2→P3) | `AnnotationParser.java:772-774`; core validation | `@Greedy` that is not last (or a greedy placeholder followed by more) parses and registers but can never run. `@Greedy List<String>` gives a one-element list. | Reproduced ×2 |
| GA-29 | A5-03 (V1: P2→P3) | `modded/.../util/CurrentServer.java:18-29` | Fabric with a shaded copy that is first touched after `SERVER_STARTING` never learns the server. The README path (`onInitialize`) works. | Traced ×2 |
| GA-30 | A4-03 (predates) | `spigot/.../CommandLib.java:381-396` | Unregister removes labels by name without checking ownership, and a displaced command is never restored. | Traced ×2 |
| GA-31 | A4-04 | spigot `:115-147`, paper `:138-168` | `register` / `unregister` do not enforce the main-thread contract that `refresh` does. | Traced |
| GA-32 | A4-05 | `paper/.../argument/MessageArgument.java:55-86` | Selector resolution in `MessageArgument` falls back to raw text silently if reflection fails, and the IT sends no selector. | Plausible |
| GA-33 | A4-06 | `CommandNodeChildren.java:78-87` | Writes Brigadier's `final` fields by reflection. JDK 26 warns (JEP 500), and a future JDK may refuse. | Hypothesis |
| GA-34 | A3-03 | `Coroutines.kt:49-50`, `CommandRunner.java:294` | A `withTimeout` timeout (recommended by `docs/kotlin.md:228`) is treated as a cancellation: no message, no log. | Reproduced |
| GA-35 | A3-04 | `AnnotationParser.java:1177-1186` | Modded `List<Holder<X>>` is rejected before `holderArgument` can handle it, so that branch is dead. | Traced |
| GA-36 | A1-04, A1-05, A1-07, A1-08, A1-09 | see `production.md` | Validation gaps: two branches executable for the same input (the shorter wins silently); a first argument named `help`; `repeatable()` drops `required` / `requires`; truncated `getInputs()` after a redirect; short name `-`; a command added under two parents; greedy arguments swallow options. | Reproduced (A1-09 traced) |
| GA-37 | A2-05 … A2-11 | see `production.md` | Execution edge cases: `visibleWhen` contexts without providers throw out of `requires`; a throwing ErrorFormatter hides the original error; preprocess messages are lost on `fail`; stack traces print twice; all command blocks share one confirmation; `[options]` is shown when none are permitted; `CommandPrerequisiteException(String)` has a null `getMessage()`. | Reproduced (A2-10, A2-11 traced) |
| GA-38 | A5-05 | `velocity/.../argument/ServerArgument.java:16-30` | Velocity server names that need quotes are suggested unquoted. | Traced |
| GA-39 | B1-05 … B1-08, B2-08 … B2-12 | see `tests.md` | Weaker test gaps: combined-flag permission (M9 survived); rejection tests that don't assert which error; "some redirect exists" assertions; FakeSender ignores `DefaultPermission`; Folia detection (M6b survived); the integration bot is always an operator; velocity-testing proxy available only during `execute`; Spigot yaw/pitch (M8 survived). | Mutation-verified / traced |
| GA-40 | C-03 … C-12 | see `complexity.md` | Duplication and maintenance cost: `CommandBuilder`'s 45 forwarders; 4 identical diagnostics commands; 11 identical Spigot/Paper files (one has already drifted); 5 copies of the testing plumbing; 10 identical test files; 13 identical IT files (about 1.4k lines); 1,452 committed generated NMS files with no up-to-date check; no version catalog (mixed JUnit 5.8.1 / 5.10.2). | Measured |
| GA-41 | D-09, D-11, D-15, D-16 | see `roadmap.md` | Optional/variadic barely tested per platform; modded relocation never exercised; samples unbuildable until 0.18.0; bug fixes with IT-only coverage; `store success` semantics for message-less failures inconsistent with the roadmap text. | Traced / plausible |

---

## Claims checked and not upheld (or narrowed)

- **A1-01 as P1:** narrowed. `cfg x --force y go` and one-argument branches work, and the failure is loud (V2).
- **A5-02 "RCon string is wrong":** corrected. Forge 1.16.5's class is `RCon…`; only Fabric's intermediary names and
  `getType()` are affected (V1).
- **A4-02 "always throws, undocumented":** narrowed. The Javadoc and the exception message already tell users to
  register during initialization. What remains is a dead fallback and misleading diagnostics (V1).
- **D-01 / D-02 as P0:** neither is a code defect. CI is deferred by recorded maintainer decision (GA-06); release
  notes are a release task (GA-04).
- **D-04 "no evidence, so not run":** narrowed. Missing local artefacts are expected with deleted parallel worktrees.
  The finding is that the evidence is not reproducible (GA-20).
- **A1 / A2 / A3 "areas checked and found sound":** option re-parse with quoted values and trailing spaces, option
  permissions, optional+variadic with options, cooldown/confirm basics (mutations M5–M7 and M14 killed), help filtering
  (M11–M13 killed), the reflection paths of the annotation parser. NMS version detection fails fast on unknown versions
  (A4). Those reviews found no issues there.

## Process notes

- The `code-review` skill was not invoked by A1–A5; they applied its correctness methodology by hand on local paths, as
  allowed by the brief (no PR exists and none was created). C loaded `ponytail:ponytail-review` and applied it by hand.
- `pstack:interrogate` (multi-model review) was not used. Every P1/P2 code finding was either reproduced by two
  independent agents or confirmed against platform bytecode by a fresh verifier, and no finding had competing
  interpretations left unresolved. Agreement between agents was not taken as proof: V1 and V2 were told to refute, and
  they corrected four claims.
