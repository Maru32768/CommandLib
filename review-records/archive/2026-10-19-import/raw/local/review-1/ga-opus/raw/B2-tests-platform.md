# B2 — Platform test modules and integration tests (GA audit)

Reviewer: Agent B2. Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (uncommitted comment-only edits ignored).
Scope: spigot-testing, paper-testing, velocity-testing, modded-testing (static), spigot/paper/velocity `src/test`,
integration-test (runner + shared spigot/paper/modded fixtures, static only), samples tests.

Method: read harnesses, fixtures, runner and the production code they stand in for; baseline run of
`:spigot-testing:test :paper-testing:test :velocity-testing:test :spigot:test :paper:test :velocity:test`
(`-Pcommandlib.typedNms=false`) in a disposable `git archive` copy — all green (spigot-testing 213, paper-testing 151,
velocity-testing 28, spigot 130, paper 7, velocity 5 tests); then single-site mutations of production code, one at a
time, each re-running the module's unit tests plus its testing module. Modded and Docker targets were reviewed
statically only (not built, not run).

## Findings

### B2-01 — P1 — Forge/NeoForge player permission denial is verified nowhere; the integration check passes vacuously

- Runner: `integration-test/src/test/java/dev/marulabs/commandlib/integration/IntegrationTest.java:193-203` sends
  `/commandlibtest deniedProbe` for **every** platform (it is outside the `isBukkitPlatform` guard) and accepts
  `Incorrect argument` / `command.unknown.argument` / `Unknown or incomplete command`.
- Modded fixture: `../../../../integration-test/shared/modded/src/main/java/dev/marulabs/testmod` defines **no** `deniedProbe`
  (only the Bukkit fixtures do, `shared/spigot/.../ScenarioTest.java:226-236`). On Forge/NeoForge the bot therefore
  gets Brigadier's "unknown argument" for a sub-command that does not exist, which satisfies the predicate, and
  `noneMatch("deniedProbe executed")` is trivially true. The modded fixture uses only `DefaultPermission.ALL`
  (grep), so no modded integration case exercises a denied node at all.
- Unit harness: `modded-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:249-256` replaces
  `CommandLib.hasPermission(source, ServerPlayer, node)` with a static mock answered by `FakeSender`, and `<1.18`
  mocks `PermissionAPI.hasPermission`. The production Forge/NeoForge resolution in
  `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:313-324` (the `"commandlib." + node` key, the
  `PermissionAPI.getRegisteredNodes()` check, the default-resolver fallback, level 4 for an unknown node) is never
  executed by any test. `FabricPermissionTest` covers only the Fabric branch (`:242-252`).
- Claim vs reality: `../../../../docs/compatibility.md` records "2026-10-16 に 4 target とも全 case が通った" for Forge and both
  NeoForge targets, and the roadmap marks permission-aware execution `[x]`. The only player-side permission check on
  those targets is the vacuous one.
- Defect it would miss: `hasPermission` returning `true` (or looking up the node without the `commandlib.` prefix,
  so every lookup falls into `source.hasPermission(4)` and an op bot passes while non-ops are locked out of
  `DefaultPermission.ALL` commands) — every OP/NONE command usable by every player on a real Forge/NeoForge server,
  with all unit and integration tests green. Not mutated (modded builds are off-limits); confidence high from source.
- Fix: add `deniedProbe` (DefaultPermission.NONE) to the modded fixture, and either guard the bot check by platform
  or assert that the sub-command exists (e.g. a control sub-command with the same parent that must reply). Add a
  Forge/NeoForge unit test of `CommandLib.hasPermission` like `FabricPermissionTest` (registered node, unregistered
  node with default resolver, unknown node → level 4).

### B2-02 — P2 — The testers never run the platform's failure-message path; joining/rendering bugs survive

- `CommandFailure` (`common/.../CommandFailure.java:12-64`) carries two things: `message` (the server's
  `toFailureMessage` component, read via `getRawMessage()` by the platform) and `sends` (replayed by testers).
  Every tester catches it and calls `e.sendMessages()` (spigot-testing `CommandTester.java:187-189`, paper-testing
  `:150-152`, velocity-testing `:118-120`, modded-testing `execute`). No test in the platform/testing modules calls
  `getRawMessage()` / `toFailureMessage`.
- Production paths untested: `spigot/.../CommandContext.java:162-172`, `paper/.../CommandContext.java:201-204`,
  `velocity/.../CommandContext.java:154-158`, `modded/.../CommandContext.java:197`.
- Mutations: M2 (Paper joins only the first message) **survived**; M4 (Velocity, same) **survived**; M3 (Spigot drops
  the newline between messages) **survived**. Integration checks only single-message failures (`captionProbe`), not
  on Velocity (no target) and not on mohist-1.20.1 (B2-03).
- Real defect missed: a multi-line failure (validator message + usage from `ErrorFormatter.SYNTAX_ADVICE`, or two
  `sendFailure` calls in a prerequisite) reaches real players truncated or run together, while every tester shows
  the right lines.
- Fix: a platform unit test per module that builds a failure with ≥2 messages and asserts the `getRawMessage()`
  component (Paper/Velocity: Adventure join with newlines; Spigot: through a mock `NMSCommandListenerWrapper`); or an
  opt-in tester mode that renders `getRawMessage()` instead of replaying `sends`.

### B2-03 — P2 — mohist-1.20.1 bot assertions accept "Unknown command", turning four checks into no-ops

- `IntegrationTest.java:175-221` (commit ad060861): for `mohist-1.20.1` the incorrect-argument, converted-CSE,
  option-permission and caption checks also pass on `"Unknown command. Type"`.
- What still holds: `optionMessages.noneMatch("secret=true")` still catches a wrongly accepted option, because the
  probe's executor uses `sendFailure` (a plain red message, not a `CommandFailure`), which Mohist delivers.
- What is lost on that target: any verification that CommandLib's own error path (incorrect-argument message,
  `ArgumentParseException` conversion, prerequisite captions and the player-locale caption lookup) works at all. A
  regression that, e.g., throws inside the caption lookup or inside CSE conversion shows up only as "Unknown command"
  and passes. The underlying user-visible defect (no CommandLib failure message ever reaches a Mohist 1.20.1 player) is
  documented (`docs/compatibility.md:37`, roadmap `[ ]` item), yet the same row says "2026-10-16 に両方で全 case が
  通った".
- Assessment: the relaxation reflects a real Mohist behaviour (the commit message cites vanilla `/gamemode
  not_a_mode`), so it is not masking a CommandLib bug by itself; the problem is that it accepts *either* outcome, so
  it can neither detect a CommandLib regression there nor notice when the Mohist limitation goes away.
- Fix: for mohist-1.20.1 require the Unknown-command reply exactly (known-failure expectation) and verify the
  CommandLib side through a channel Mohist does not swallow (the console-driven report, e.g. a probe that records the
  caption text resolved for a player-locale context); state in compatibility.md that these four checks are
  expectation-inverted on that target rather than "全 case が通った".

### B2-04 — P2 — paper-testing's WorldArgument mock takes the world *name*; real Paper takes the dimension *key*

- `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java:291-294` keys fake worlds by
  `world.getName()` and `:324-325` resolves the token against that map.
- `paper-testing/src/test/.../argument/WorldArgumentTest.java:14-27` asserts that `tp world` resolves — input that a
  real Paper server rejects (Paper's `ArgumentTypes.world()` parses a dimension resource location; `world` becomes
  `minecraft:world`). The Paper integration case uses `minecraft:overworld` (`shared/paper/.../ArgumentTest.java:1020`),
  which the mock rejects with "Unknown value". spigot-testing keys by dimension key (`withFakeWorld(key, World)`), and
  `docs/arguments.md:354` says paper-testing is "by key" — the doc and the implementation disagree.
- Probe in the disposable copy: see "Harness probes" below.
- Defect it hides: a plugin's tests (and suggestions/help text written from them) use world names; on the server every
  such input fails.
- Fix: key by `world.getKey().toString()` (or accept both, namespaced first), add a `withFakeWorld(key, world)`
  overload like Spigot's, and change `WorldArgumentTest` to the server's syntax.

### B2-05 — P2 — paper-testing mocks accept or fail differently from the server (game mode, entities `@a`)

- `CommandTester.java:316-317`: `GameMode.valueOf(token.toUpperCase(Locale.ROOT))` accepts `SURVIVAL` / `Creative`
  (vanilla's `gamemode` argument accepts only the lower-case names). An unknown token does end as a
  `CommandSyntaxException` (probe), so only the over-acceptance is wrong. Spigot-testing runs the real parser.
- `:466-475`: `entities` with `@a` returns every fake entity (zombies included); vanilla `@a` selects players only.
  Probe: with only a zombie registered, `ents @a` executes with 1 entity; a server would select no one.
- Severity is P2 only because these mocks are the documented way to test Paper plugins; each is a case where a
  user's test passes for input the server treats differently.
- Fix: accept only the lower-case names; filter `@a` to players for `entities`.

### B2-06 — P2 — Paper parity: ten Paper arguments are never executed in paper-testing, and their failure paths are untested anywhere

- Arguments with no execution test in paper-testing (only constructed by `ArgumentCoverageTest.java:146-161`, which
  asserts "does not throw" while building a tester): `AdvancementArgument`, `AttributeArgument`, `BiomeArgument`,
  `EnchantmentArgument`, `EntityTypeArgument`, `LootTableArgument`, `ObjectiveArgument`, `PotionEffectArgument`,
  `RecipeArgument`, `SoundArgument`. Spigot-testing has a `*Test` per argument and enforces it
  (`spigot-testing/.../ArgumentCoverageTest.java:46-70`); paper-testing has no such rule.
- These are not thin wrappers: e.g. `paper/.../argument/ObjectiveArgument.java:25-60` has its own suggestion filter
  (`containsIgnoreCase`) and not-found failure; `AdvancementArgument.java:15-35` has Bukkit lookup + translatable
  failure. Integration (`shared/paper/.../ArgumentTest.java`) covers only the happy path.
- Mutations: M7 (Paper advancement not-found key) **survived**; M1b (Paper objective not-found key) **survived**.
  The same mutations on the Spigot classes (M9/M10) are killed by `KeyedArgumentFailureTest`.
- Fix: add a paper-testing `*Test` per argument (happy path, not-found failure message, suggestions), mirroring the
  Spigot tests, and give paper-testing the same "every argument has a test" rule.

### B2-07 — P2 — Spigot lifecycle (register / unregister / plugin disable) has no unit tests; plugin-disable is untested anywhere

- `../../../../spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java` (~800 lines) — `register`, `unregister(boolean)`
  (`:715`), `onPluginDisable` (`:650-680`, including the function recompilation) — has no test in `spigot/src/test`
  or `spigot-testing`. Paper has 20 lifecycle unit tests (`paper-testing/.../CommandLibLifecycleTest.java`).
- Integration covers register → run → unregister on Spigot/Paper from the console
  (`shared/spigot/.../ScenarioTest.java:137-200`) and late registration surviving reload (Spigot fixture only;
  the Paper fixture lacks `lateRegisteredCommandSurvivesReload`), but no fixture disables a plugin, so the
  `onPluginDisable` path (removal of every instance, function recompilation, filter uninstall) runs nowhere.
- Defect it would miss: commands of a disabled/reloaded plugin left in the dispatcher (stale classloader,
  `NoClassDefFoundError` on use) or a recompile on a stopping server.
- Fix: a fixture case that registers through a second registration and fires/forces the disable path, or a unit test
  with the NMS wrappers mocked as `CommandTreeRefreshTest` does; port `lateRegisteredCommandSurvivesReload` to the
  Paper fixture.

### B2-08 — P3 — Folia: detection and the real scheduler are untested; the tests check routing only

- `FoliaCommandSchedulerTest.java:60-138` verifies which mocked scheduler receives a task. The detection branch
  (`FoliaCommandScheduler.java:30-36,54-66,115-127`) is only checked negatively (`the_test_server_is_not_folia`).
  M6b (wrong `RegionizedServer` class name → Folia never detected → `BukkitCommandScheduler`, which throws on Folia)
  **survived**. There is no Folia integration target (documented as best effort, roadmap `[ ]`).
- Fix: a test that loads a stub `io.papermc.paper.threadedregions.RegionizedServer` in an isolated class loader and
  asserts `forServer` returns the Folia scheduler; the Folia target already on the roadmap.

### B2-09 — P3 — Harness permission semantics differ from every server (documented, but no realistic mode)

- All `FakeSender`s grant every node by default and ignore `PermissionDefault` / `DefaultPermission`
  (spigot-testing `FakeSender.java:86-89`, paper-testing `:80-83`, velocity-testing `:54`, modded `:48`). A command
  left at the default OP permission, or `PermissionDefault.FALSE`, runs for a "non-operator player" in tests. On
  Velocity a real player gets `Tristate.UNDEFINED` → only `DefaultPermission.ALL` (`VelocityPermissions.java:28-34`).
  Documented in docs/testing.md:153-166, so not a bug, but no test helper reproduces the server's defaults.
- Integration does not fill the gap: the bot is op level 4 (`IntegrationTest.java:384-387`), and the only denial
  probe uses `FALSE`; "OP default denies a non-op player" is not tested on any real server.
- Fix: a `FakeSender` mode that answers from the registration's permission defaults and `isOp`; a non-op bot case.

### B2-10 — P3 — velocity-testing's proxy is a static swapped only during `execute`

- `velocity-testing/.../CommandTester.java:113-126,138-151` sets the static `CommandContext.proxy`
  (`velocity/.../CommandContext.java:22-23`) only for the call. A queued async follow-up (the pattern docs/testing.md
  recommends) that calls `ctx.getProxy()` after `execute` returns throws `IllegalStateException` in tests although it
  works on a proxy; two testers on parallel threads overwrite each other. No test covers it.
- Fix: keep the proxy set for the tester's lifetime (restore in `close`) or bind it to the context.

### B2-11 — P3 — Integration coverage guard checks only that `new XArgument(` appears in the fixture source

- `ArgumentIntegrationCoverageTest.java:60-106` passes if the class name is instantiated anywhere in `ArgumentTest.java`
  (a comment-free regex), not that the case is dispatched, has `putCommandNotExecutedResult`, or asserts a value. The
  runner (`IntegrationTest.java:331-342`) only requires `tests > 0` and zero failures, so a fixture case that silently
  stops being built drops out of the report without failing. The modded fixture passes this guard although
  `deniedProbe` is absent (B2-01).
- Most of the fixture cases run as the console (`shared/spigot/.../TestMain.java:125-131`), while
  integration-test/README.md says "A real player connection exercises the server command path"; only the seven bot
  checks run as a player.
- Fix: assert an expected minimum/explicit list of report keys per platform; note the console/player split in the
  README.

### B2-12 — P3 — Spigot source location/rotation mapping is integration-only

- spigot-testing's `MockNMSCommandListenerWrapper.getBukkitLocation` (`:69-74`) overrides the production
  `NMSCommandListenerWrapper.getBukkitLocation` (`spigot/.../NMSCommandListenerWrapper.java:62-73`), so the
  yaw/pitch mapping has no unit test. M8 (yaw ← `rotation.x()`) **survived** the unit suites; the integration case
  `execute positioned 10.25 70.5 -20.75 rotated 90 45` (`shared/spigot/.../VanillaExecutionTest.java:99,204`) would
  catch it. Acceptable, recorded for completeness.

## Things checked and found sound

- Spigot-testing selector mock rejects `@a`/`@e` for single arguments and `@e` for player arguments at parse time,
  and fails loudly for an unregistered name (documented); `MockArgumentTypes` range/time/resource parsing mirrors
  vanilla closely enough for its documented scope.
- Fixture result integrity: `putCommandNotExecutedResult` pre-fails each case; rejected-input cases use a control
  input plus an `AtomicBoolean`, so "never executed" cannot pass (`shared/spigot/.../OptionTest.java:152-200`).
- Report parsing waits for a parseable file, fails on Docker absence, and checks the log for CME and for function
  load errors after the reload marker (`IntegrationTest.java:113-167`).
- Bukkit/Paper/Velocity/modded scheduler unit tests verify the thread hand-off with mocks
  (`BukkitCommandSchedulerTest`, `ServerCommandSchedulerTest`, `VelocityCommandSchedulerTest`), and the real Bukkit
  thread hand-off is asserted in the fixture (`RuntimeFeatureTest.executeAsyncAndMiddleware`).
- Paper bootstrap permission defaults are unit tested (`CommandLibBootstrapTest.java:74-110`).
- No `@Disabled`, sleeps, timeouts or swallowed results in the unit suites; only the two coverage tests rely on
  "does not throw".
- The 29-target count in the roadmap matches the list in `../../../../docs/compatibility.md` (10+10+1+2+4+2). The run
  reports themselves are not stored in the repository, so the claim is not reproducible from it.
- Spigot and Paper snapshot files are identical except `help-error-formatter.snap`, where Paper's flattened
  translatable keys (`command.unknown.argument`, `...foocommand.context.here`) are an artefact of the plain-text
  serializer, not wrong output.

## Harness probes (disposable copy)

A throw-away JUnit test added to the copy's paper-testing (one tester with `GameModeArgument`, `WorldArgument`
with a fake world named `world`, `EntitiesArgument` with one fake zombie `z`, sender a FakeSender player) printed:

| Input | paper-testing result | Real Paper server |
|---|---|---|
| `gm SURVIVAL` | executes, `SURVIVAL` | rejected (lower-case names only) |
| `gm Creative` | executes, `CREATIVE` | rejected |
| `gm foo` | `RuntimeException` caused by CSE | rejected (CSE) — consistent |
| `tp world` | executes, world `world` | rejected (`minecraft:world` is not a dimension) |
| `tp minecraft:overworld` | `RuntimeException` caused by CSE | executes (the integration fixture's input) |
| `ents @a` | executes with 1 entity (the zombie) | `@a` selects players only |

## Mutation table

Each mutation was applied alone in the disposable copy and the listed tasks were run (`-Pcommandlib.typedNms=false`).

| ID | Site | Change | Tasks | Result |
|---|---|---|---|---|
| M1 | `paper/.../argument/ObjectiveArgument.java:55` | `if (objective == null)` → `if (false)` | paper, paper-testing | killed at compile (NullAway) — replaced by M1b |
| M1b | same file `:56` | translation key `arguments.objective.notFound` → `arguments.objective.missing` | paper, paper-testing | **survived** |
| M2 | `paper/.../CommandContext.java:203` | `Component.join(newlines(), messages)` → `messages.get(0)` | paper, paper-testing | **survived** |
| M3 | `spigot/.../CommandContext.java:167` | drop `joined.addExtra("\n")` | spigot, spigot-testing | **survived** |
| M4 | `velocity/.../CommandContext.java:157` | join → `messages.get(0)` | velocity, velocity-testing | **survived** |
| M5 | `paper/.../CommandPermissions.java:38-39` | remove `|| Bukkit.getPluginManager().getPermission(name) != null` (a permission registered with the plugin manager no longer overrides the bootstrap default) | paper, paper-testing | **survived** (first attempt aborted by an external `gradle --stop`; re-run clean) |
| M6 | `paper/.../FoliaCommandScheduler.java:56` | `if (isGlobalTickThread == null)` → `if (true)` | paper, paper-testing | killed at compile (NullAway) — replaced by M6b |
| M6b | same file `:119` | class name `...RegionizedServer` → `...RegionizedServerX` (Folia never detected) | paper, paper-testing | **survived** |
| M7 | `paper/.../argument/AdvancementArgument.java:31` | key `advancement.advancementNotFound` → `advancement.notFound` | paper, paper-testing | **survived** |
| M8 | `spigot/.../util/nms/command/NMSCommandListenerWrapper.java:70` | yaw from `rotation.x()` | spigot, spigot-testing | **survived** (integration `VanillaExecutionTest` would catch) |
| M9 | `spigot/.../argument/ObjectiveArgument.java:56` | same key change as M1b | spigot, spigot-testing | killed (`KeyedArgumentFailureTest`) |
| M10 | `spigot/.../argument/AdvancementArgument.java:33` | same key change as M7 | spigot, spigot-testing | killed (`KeyedArgumentFailureTest`) |

M9/M10 against M1b/M7 is the parity gap of B2-06 in one line: the same mutation is caught on Spigot and survives on
Paper. M5 adds a Paper bootstrap-permission branch to the list of untested paths (`CommandLibBootstrapTest` never
registers a `Permission` with the mocked plugin manager).

## Coverage summary

| Area | Unit (harness) | Real server | Gap |
|---|---|---|---|
| Spigot arguments | per-argument tests, enforced | 10 Spigot + 5 spigot-module Paper + Mohist targets | none found (M9/M10 killed) |
| Paper arguments | 17 with tests; 10 only constructed | 5 paper-module targets, happy path only | B2-06 |
| Failure message rendering | replayed, never rendered | single-message only, not Mohist 1.20.1 | B2-02, B2-03 |
| Permissions | FakeSender grants all; modded mocks production check | op bot; FALSE probe on Bukkit only | B2-01, B2-09 |
| Lifecycle | Paper thorough; Spigot none | register/unregister/reload from console | plugin disable (B2-07) |
| Folia | mocked routing | none | B2-08 |
| Velocity | tester + scheduler | none (manual 2026-10-15) | B2-02, B2-10 |
| Fabric | modded-testing + Fabric permission unit test | none | — |
