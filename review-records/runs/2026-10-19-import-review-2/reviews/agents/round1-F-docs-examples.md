# Round 1 / F: docs, README, CHANGELOG, examples, samples, plugin skill, tools

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..6b898637` (master HEAD). Scope: `docs/**` (except `docs/agents`,
`docs/roadmap*.md`), `README.md`, `CHANGELOG.md`, `examples/**`, `samples/**`, `plugins/**`, `tools/**`, `mkdocs.yml`,
`.jev-lint.yaml`, `THIRD_PARTY_NOTICES.md`.

## How the review was done

- `examples`: built and tested. `./gradlew --configure-on-demand -Pcommandlib.typedNms=false :examples:compileTestJava :examples:test`
  passed. All four styles of each example produce the same snapshot.
- `samples`: every source file and build script was read and each CommandLib call checked against the source. No Gradle
  build was run on them. No compile errors were found. Dependency coordinates, composite substitution, `plugin.yml`,
  `fabric.mod.json`, `mods.toml` and the Velocity `@Plugin` metadata are correct.
- Docs, README, CHANGELOG and the skill: split into seven slices. Each claim was checked against `common/`, `spigot/`,
  `paper/`, `modded/`, `velocity/`, `*-kotlin/`, `*-testing/`, `settings.gradle.kts`, `integration-test/` and the 0.17.2
  tag. I re-checked every high or medium finding against the source myself.
- Checked and correct: every README Java and Kotlin snippet, every snippet in the migration pages and comparison.md, the
  caption keys and texts, the mkdocs nav, which covers every included page, the anchors, the test-helper APIs, the
  benchmark task names, the line-count table in comparison.md, and the CHANGELOG lists of renames and removals.
- Many API misstatements below repeat across several pages, so each finding lists every location.

## Findings

### F-1 doc-wrong, high: the docs say Forge, NeoForge and Fabric have no player or console requirers

- **Locations:**
  - `docs/context-values.md:75`
  - `docs/migration/from-acf.md:175`
  - `docs/migration/from-cloud.md:170`
  - `docs/migration/from-lamp.md:170`
  - `docs/migration/from-commandapi.md:158`
  - `docs/errors.md:210-211`, which marks the four methods "(Spigot, Paper)"
- **What is wrong:**
  - context-values.md:75 says "Forge, NeoForge and Fabric have no `playerRequirer()`; read the player from
    `ctx.getHandle().getSource()`...".
  - from-commandapi.md:158 says "Forge / NeoForge have neither, nor `requirePlayer()`".
  - A reader is told to write their own extractor for an API that already exists.
- **Evidence:**
  - `modded/src/main/java/dev/marulabs/commandlib/Command.java`:
    - `:44` `public static Extractor<CommandContext, ServerPlayer> playerRequirer()`
    - `:57` `consoleRequirer()`
    - `:67` `requirePlayer()`
    - `:71` `requireConsole()`
  - None of these are behind a Stonecutter guard. They were added in 7cb8c037.
  - Velocity has them too: `velocity/.../Command.java:22,31,40,44`.
- **Fix:**
  - Delete the one-line notes in the three migration pages.
  - Rewrite context-values.md:75 and from-commandapi.md:158: the requirers exist on every platform. On modded,
    `playerRequirer()` gives a `ServerPlayer`.
  - Drop "(Spigot, Paper)" in errors.md.

### F-2 doc-wrong, high: the example version `0.17.2` has none of the documented modules or packages

- **Locations:**
  - `README.md:122`: "replacing `latest.release` with a specific version such as `0.17.2`". This range changed it from
    0.16.0.
  - `docs/javadoc.md:16-27`: the Javadoc links for `spigot`, `paper`, `velocity` and the `*-testing` modules at `0.17.2`.
- **What is wrong:**
  - Tag 0.17.2 has only the `bukkit`, `forge` and `common` modules.
  - Its package is `net.kunmc.lab.commandlib`.
  - So `spigot:0.17.2`, `paper:0.17.2` and the rest do not exist, and 7 of the 8 Javadoc links return 404.
  - The `common` link shows the old API.
- **Evidence:**
  - `git ls-tree 0.17.2` lists `bukkit common forge`.
  - `git show 0.17.2:settings.gradle` contains `include "forge", "bukkit"`.
  - `CHANGELOG.md:12-18` says the packages move and the `bukkit` artifact is removed in 0.18.0.
  - The root `build.gradle.kts` `apiBaselines` comment says "0.17.2 published this module as bukkit".
- **Fix:** Use a `<version>` placeholder, as `docs/packaging.md:84` does, or 0.18.0.

### F-3 doc-wrong, high: the skill's "Bukkit Static APIs" test example cannot work

- **Location:** `plugins/commandlib/skills/commandlib/references/testing.md:105-133`
- **What is wrong:**
  - The example tests `WorldArgument` by stubbing `Bukkit.getWorld("nether")`, and builds the command eagerly with
    `new CommandTester(new Command("tp"){{...}}, ...)`. This fails for two reasons.
  - First, `WorldArgument` is backed by NMS. Its constructor calls `NMSArgumentDimension.create()`
    (`spigot/.../argument/WorldArgument.java:10-13`). `CommandTester` installs the NMS mock only in its own constructor
    (`spigot-testing/.../CommandTester.java:131-133`), so the eager construction hits the real registry. The skill's own
    NMS section at `testing.md:78-85` says to use the `Supplier` form.
  - Second, parsing never calls `Bukkit.getWorld`. The mock dimension reads `CommandTester.getFakeWorld(key)` instead.
- **Evidence:**
  - `docs/testing.md:79` names `withFakeWorld(key, World)` as the way to feed `WorldArgument`.
  - `docs/testing.md:89-111` uses `OfflinePlayerArgument` as the static-mock example.
- **Fix:**
  - Replace the example with the `OfflinePlayerArgument` one from docs/testing.md.
  - Show `WorldArgument` with the `Supplier` constructor and `tester.withFakeWorld("minecraft:the_nether", world)`.
  - Drop `WorldArgument` from the list at `testing.md:107-108`.

### F-4 doc-wrong, medium: the modded integration fixture does check command blocks

- **Locations:**
  - `docs/vanilla-integration.md:71-72`: "The Forge and NeoForge fixture does not check command blocks".
  - `docs/compatibility.md:124`: the modded `ActorTest` is described as only `execute as` and `positioned ... rotated`.
  - `docs/compatibility.md:133`: "Forge / NeoForge の command block と function は未確認" (command blocks and functions on
    Forge/NeoForge are unconfirmed).
  - This contradicts `compatibility.md:41`, which says Fabric runs the same suite and checks command blocks.
- **Evidence:**
  - `integration-test/shared/modded/src/main/java/dev/marulabs/testmod/ActorTest.java:20-45`: `commandBlockActorType()`
    places a command block and asserts `COMMAND_BLOCK,false`.
  - `PermissionTest.java:54` uses `ActorTest.commandBlock(...)`.
  - Commit 41bc1954.
- **Fix:** Say that the modded fixture checks the command block's actor type and permission level, but not
  `SuccessCount`, `LastOutput` or functions. Do this in all three places.

### F-5 doc-wrong, medium: vanilla-integration.md says a Paper 1.20.5 command block runs in a player-loaded chunk

- **Location:** `docs/vanilla-integration.md:64-65`: "runs only in a chunk a player keeps loaded or one added with
  `/forceload`".
- **Evidence:**
  - `integration-test/shared/{paper,spigot}/.../VanillaExecutionTest.java:31-32` says Paper 1.20.5 "does not tick the
    spawn chunk next to the bot".
  - Line 164 then force-loads it.
  - Also, `forceload add blockX blockZ` loads only one chunk, while the blocks at `blockX+2` and `blockX+4` can fall in the
    next chunk. That is a fixture bug, not part of this scope, but the doc is built on that fixture.
- **Fix:** Say "runs only in a force-loaded chunk, or one within simulation distance". Do not say a nearby player is
  enough.

### F-6 doc-wrong, medium: compatibility.md contradicts its own summary on the 89b9503f run

- **Locations:**
  - `docs/compatibility.md:41`, `:52`, `:124`
- **What is wrong:**
  - The summary says all 78 targets passed at 89b9503f, but the Fabric, Folia and Velocity rows still show older dates or
    no commit.
  - `:52` says 1.19.x is "Tested（Forge は Supported）" (Tested, with Forge only Supported), while line 39 says
    `forge-1.19.2` passed at 89b9503f.
- **Fix:** Update the Fabric, Folia and Velocity rows to 89b9503f, and change the 1.19.x Forge entry to Tested.

### F-7 doc-wrong, medium: CommandDefaults registration is described with the Spigot form "on every platform"

- **Location:** `README.md:1110-1111`: "passed as `CommandLib.register(this, defaults, commands)` on every platform".
- **What is wrong:**
  - Modded has no plugin argument. Velocity needs the proxy.
  - On every platform `commands` is a `Collection`, not varargs.
- **Evidence:**
  - `modded/.../CommandLib.java:83` `register(String permissionPrefix, CommandDefaults<CommandContext>, Collection<? extends Command>)`
  - `velocity/.../CommandLib.java:97` `register(Object plugin, ProxyServer, CommandDefaults<CommandContext>, Collection<...>)`
  - `docs/execution.md:97-100` lists the forms correctly.
- **Fix:** List the three forms, or link to execution.md.

### F-8 doc-wrong, medium: the middleware order leaves out the registration's CommandDefaults middlewares

- **Locations:**
  - `README.md:1059-1061`
  - `docs/errors.md:28` (step 6) and `:260` ("`cooldown(...)` and `confirm(...)` are the first middlewares")
  - `docs/design/execution-pipeline.md:32`: `[cooldown check] [confirmation] [inherited and own middlewares] [cooldown start]`
- **Evidence:**
  - `common/.../CommonCommand.java:780-803`: `middlewares()` starts with `defaults.middlewares()`, then adds the cooldown
    and the confirmation.
  - The cooldown step now reserves and releases (`cooldown::reserve` / `release`, commit 1aa854f4).
  - `docs/execution.md:13-15` has the right order.
- **Fix:** Put "registration (`CommandDefaults`) middlewares" first in all three places. In the design doc, describe the
  cooldown step as reserve/release.

### F-9 doc-wrong, medium: options.md says `repeatable()` drops everything but the aliases and the description

- **Location:** `docs/options.md:86`: "Call it before `requires(...)`, since the returned option only keeps the aliases
  and the description."
- **Evidence:**
  - `common/.../CommandOption.java:262-296`: the javadoc says it "keeps the aliases, the description, the permission,
    required() and the requires(...) dependencies".
  - The code copies `permissionNode`, `defaultPermission`, `restricted`, `required`, `validators` and `requiredOptions`.
- **Fix:** Delete the sentence.

### F-10 doc-wrong, medium: arguments.md says Fabric has no integration target

- **Location:** `docs/arguments.md:31-34`: "Fabric has no integration target yet, so its arguments are checked only by
  `fabric-testing`".
- **Evidence:**
  - `integration-test/targets/fabric-1.20.1` and `fabric-1.21.1` exist, with platform `fabric` (commit 69559659).
  - The shared modded `ArgumentTest` covers every modded argument.
  - `docs/compatibility.md:41` says all 65 cases pass.
- **Fix:** Add Fabric 1.20.1 and 1.21.1 to the real-server list. Keep the caveat only for fabric-1.21.4.

### F-11 bug, medium: the docs/kotlin.md option snippet should not compile

- **Location:** `docs/kotlin.md:180`: `option(Options.integer("limit", 'n', 10, 1, 100).description("..."))`
- **What is wrong:**
  - `Options.integer` is `<C extends CommonCommandContext<?, ?>> CommandOption<Integer, C>` (`common/.../Options.java:102`).
  - When it is the receiver of `.description(...)`, Kotlin has no expected type to infer `C` from.
- **Evidence:**
  - The repo's own Kotlin code gives the type argument whenever it chains a call:
    - `examples/.../KotlinBanCommand.kt:19` `Options.string<CommandContext>(...).description(...)`
    - `common-kotlin/src/platformTest/.../KotlinDslTest.kt:62`
  - I did not compile this snippet.
- **Fix:** `Options.integer<CommandContext>("limit", 'n', 10, 1, 100).description(...)`

### F-12 doc-wrong, medium: diagnostics.md says options have no permission nodes

- **Location:** `docs/diagnostics.md:93`: "Options have no permission nodes of their own."
- **Evidence:**
  - `common/.../CommandTree.java:119-123` adds `option.permission` to the export.
  - `CommandOption.java:239-246` builds `<prefix>.<path>.option.<name>`.
  - The same page at `:30` lists `kind` = `option`, and `docs/permissions.md:57-82` describes these nodes.
- **Fix:** "...and one per option that calls `permission(...)`."

### F-13 doc-wrong, medium: localization.md's MiniMessage `CaptionSender` sends raw keys and colors everything red

- **Location:** `docs/localization.md:105-113`
- **What is wrong:**
  - It deserializes `message.plainText()`.
  - For a caption with no text, such as `Caption.UNEXPECTED_ERROR` by default, the message is
    `RichText.translatable("command.failed")`. Its `plainText()` is the key, so the sender sees `command.failed`.
  - It also calls `sendFailure` for every caption, so a `ctx.sendSuccess(HOME_SET)` is shown red.
- **Evidence:**
  - `Captions.java` `send(...)` creates `RichText.translatable(...)`.
  - `RichText.java:200-207`: "A translatable text contributes its key".
- **Fix:** Pass translatable messages to `send` unchanged, and choose success or failure per caption. Or say the snippet
  is only for failure captions with literal text.

### F-14 doc-wrong, medium: the migration guides say the roadmap tracks items it does not contain

- **Locations:**
  - `docs/migration/from-acf.md:326-328`: catch-unknown, hidden commands, root aliases of subcommands, global
    contexts/completions.
  - `docs/migration/from-lamp.md:388-390`: registration hooks, response handlers, global parameter types and suggestion
    providers.
  - `docs/migration/from-commandapi.md:315`: per-sender executor variants.
- **Evidence:** `docs/roadmap.md` has none of these. A grep for hidden, secret, response handler, registration hook and
  per-sender finds only "per-sender locale" at :349. The roadmap does have the conditions registry at :51/:104 and missing
  arguments at :38.
- **Fix:** Add the items to the roadmap, or reword these sections to "Not available", without "Tracked in the roadmap".

### F-15 doc-wrong, medium: the plugin skill claims Forge/NeoForge support but teaches only Spigot/Paper

- **Locations:**
  - `plugins/commandlib/skills/commandlib/SKILL.md:3`
  - `references/user-guide.md:3-15`
  - `plugin.json:10`
- **What is wrong:**
  - The description promises Forge and NeoForge.
  - The artifact table lists only `spigot` and `paper`, and every registration example is `CommandLib.register(this, ...)`.
  - Code generated from this skill for a mod would not compile. Modded uses `register(String permissionPrefix, ...)`
    (`modded/.../CommandLib.java:65-83`, `samples/forge/.../SampleMod.java:10`).
  - Fabric and Velocity are not covered at all.
- **Fix:** Add artifact rows and registration snippets for `forge-*`, `neoforge-*`, `fabric-*` and `velocity`. Optionally
  add short pointers for the annotations, the Kotlin DSL and `executeAsync`.

### F-16 doc-wrong, medium: the design docs on testing are stale

- **Locations:**
  - `docs/design/testing-strategy.md:227`, `:248` and `:250`: only common, spigot and paper testing have
    `CommandLibExtension` and `@Sender`, and "`modded-testing` does not have these helpers yet".
  - `docs/design/testing-module-structure.md:185-187`: "Remaining Work" still asks for Fabric, real Folia and real
    Velocity targets.
- **Evidence:**
  - `modded-testing/.../CommandLibExtension.java` and `Sender.java`, and the same files in `velocity-testing/` (commits
    c475687a and 508adede).
  - `integration-test/targets/fabric-1.20.1`, `fabric-1.21.1`, `folia-26.3` and `velocity-3.4.0` exist.
  - `CHANGELOG.md:180` already says the extension is available everywhere.
- **Fix:** List all five testing artifacts, delete line 250, and replace the Remaining Work bullet with what is still
  open: fabric-1.21.4.

### F-17 doc-wrong, low: execution-pipeline.md describes the modded sync executor as it was before the fix

- **Location:** `docs/design/execution-pipeline.md:143-146`: "Sync is the source's `MinecraftServer`, whose `execute`
  runs inline on the server thread".
- **Evidence:**
  - `modded/.../ServerCommandScheduler.java:40-43` checks `server.isSameThread()` itself.
  - Its javadoc says that `execute` alone queues while the server runs a task. This was fixed in 37f4f909.
  - The heading also leaves out Fabric.
- **Fix:** "Forge / NeoForge / Fabric: sync runs inline when `server.isSameThread()`, else `server.execute(task)`."

### F-18 doc-wrong, low: the Fabric version pin in packaging.md can never match

- **Location:** `docs/packaging.md:36-39` and `:261`: `"commandlib": ">=0.17.0 <0.18.0"`.
- **Evidence:** 0.17.x has no Fabric artifact and no `commandlib` mod id (see F-2). The modded artifact naming is in
  `CHANGELOG.md` under 0.18.0, Unreleased.
- **Fix:** `">=0.18.0 <0.19.0"` or a placeholder.

### F-19 doc-wrong, low: the Paper range of the `spigot` artifact is given as "up to 1.20.6"

- **Locations:**
  - `docs/packaging.md:89`
  - `plugins/commandlib/skills/commandlib/references/user-guide.md:10,15`. Line 10 says "Bukkit-compatible Paper", which
    reads as unlimited.
- **Evidence:**
  - `integration-test/targets/paper-1.21.1/test-plugin/build.gradle.kts:4` sets `commandlibModule = "spigot"`.
  - `TypedNmsModules.java:38-39` has `paper_1_21_1`.
  - `CHANGELOG.md` says "`spigot` for Spigot, Mohist, and Paper up to 1.21.1".
- **Fix:** "Paper up to 1.21.1; newer Paper needs `paper`."

### F-20 doc-wrong, low: two pages say options must come before the positional arguments

- **Locations:**
  - `plugins/commandlib/skills/commandlib/references/patterns.md:221-222`
  - `docs/migration/from-cloud.md:231-232`
- **Evidence:**
  - `common/.../CommonCommand.java:276-286` has `optionsAnywhere()`, and there is an `@OptionsAnywhere` annotation.
  - `docs/options.md:135-139` and `from-lamp.md:214` describe it.
- **Fix:** Add "unless the command calls `optionsAnywhere()`".

### F-21 doc-wrong, low: comparison.md uses a different permission prefix from the snapshots it cites

- **Location:** `docs/comparison.md:62`, `:99` and `:262` say `myplugin.command`, `myplugin.command.ban` and
  `myplugin.command.team.add`.
- **Evidence:**
  - `examples/src/test/resources/snapshots/ban.snap:2` `ban [example.command.ban, OP]`
  - `team.snap:4`
  - `StyleParity.java:23` `PREFIX = "example.command"`
- **Fix:** Use `example.command`, or say that the snapshot prefix differs.

### F-22 doc-wrong, low: comparison.md says the untyped executor sets the result

- **Location:** `docs/comparison.md:565-566`: the result "needs `setResult` or the untyped executor in Java".
- **Evidence:** `common/.../command/CommandExecutor.java:8` `accept` is `void`. `FluentScoreCommand` still calls
  `ctx.setResult`.
- **Fix:** "needs `ctx.setResult(n)`, or an annotated method returning `int`."

### F-23 doc-minor, low: comparison.md's home snippet is labelled with a single file but mixes two

- **Location:** `docs/comparison.md:469-474`
- **What is wrong:**
  - The snippet is labelled `FluentHomeCommand.java`, but its `Caption SET = ...` and `captions(Captions.create().bundle(...))`
    lines come from `HomeCaptions.java`.
  - It declares `SET` but then uses `HomeCaptions.SET`.
- **Fix:** Label it with both files, or show `captions(HomeCaptions.create())`.

### F-24 doc-minor, low: errors and help pages leave out option permission and option argument errors

- **Locations:**
  - `docs/errors.md:24` and `:185`, and `docs/help.md:128`: `PERMISSION_DENIED` is described only for "the command or an
    argument branch". `CommandRunner.java:120-129` also checks the permission of each given option.
  - `docs/help.md:123` and `:164`: `usage()` and syntax advice are said to apply to `ARGUMENT` / `VALIDATION` errors. For
    an option value's error the usage is null (`CommandRunner.java:134-141`).
  - `docs/errors.md:127-128`: `additionalParser` is said to get "the raw input first". It is a fallback that runs after
    `parseImpl` fails (`CommonArgument.java:276-289`).
- **Fix:** Correct each sentence as above.

### F-25 doc-minor, low: ObjectiveArgument errors differ between modded and Spigot/Paper, but the docs call them the same

- **Locations:**
  - `docs/errors.md:124`: "The message is the same on every platform."
  - `docs/localization.md:133`
- **Evidence:**
  - `modded/.../argument/ObjectiveArgument.java:67-68` throws `ofIncorrectInput`.
  - `spigot/.../argument/ObjectiveArgument.java:56` sends `arguments.objective.notFound`.
- **Fix:** Make modded throw the same translatable (preferred, for parity), or document the difference.

### F-26 doc-minor, low: docs/README omit Fabric and Velocity from several "every platform" lists

- **Locations:**
  - `README.md:937-940` and `:963-965` (RichText, `toVanilla`)
  - `README.md:1227-1228`: `serverHelp` is said to be ignored by "Forge / NeoForge". Fabric and Velocity ignore it too;
    only Spigot and Paper read it.
  - `README.md:1232-1234` and `:1405-1411`: permission backends
  - `docs/vanilla-integration.md:41-45`, `:80-81` and `:110-119`
  - `docs/localization.md:39-41`: locale source. Fabric 1.20.2+ reads `clientInformation().language()`
    (`modded/.../CommandContext.java:61-70`).
  - `docs/arguments.md:77-79`: Forge 1.18.2 and 1.19.2 also lack the vanilla time minimum (`//? if >=1.19.4` in
    `modded/.../TimeArgument.java:28-46`).
- **Fix:** Add Fabric and Velocity, or the missing versions, to each list.

### F-27 doc-minor, low: the README overstates the modded version range and understates the platforms

- **Locations:**
  - `README.md:62-64`: "Forge, NeoForge and Fabric from 1.16.5 to 1.21.4". Only these specific nodes exist: Forge
    1.16.5, 1.18.2, 1.19.2 and 1.20.1; NeoForge 1.21.1 and 1.21.4; Fabric 1.20.1, 1.21.1 and 1.21.4
    (`settings.gradle.kts:55-66`).
  - `README.md:1` and `:5-6`: the title and intro still say "for Spigot / Paper" and "1.13+". The minimum supported
    version is 1.16.5 (`docs/compatibility.md`).
- **Fix:** List the ranges per loader, and update the title and intro.

### F-28 doc-minor, low: the README test dependency snippet contradicts its own advice

- **Location:** `README.md:511-518` against `:530-531`.
- **What is wrong:** The snippet lists all seven testing artifacts in one `dependencies` block. The text right after says
  to use one per module, because they all define `dev.marulabs.commandlib.CommandTester` and `FakeSender`.
- **Fix:** Present the lines as alternatives.

### F-29 doc-wrong, low: the README "download sources" command does not download sources

- **Location:** `README.md:1455-1459` (this predates the range).
- **What is wrong:** `./gradlew dependencies --configuration compileClasspath` only prints the dependency tree. The skill's
  `scripts/extract-sources.sh:7-10` looks for `-sources.jar` files, which this never fetches.
- **Fix:** Use IDE Download Sources, or `idea { module { downloadSources = true } }`, or a resolve task for the `sources`
  classifier.

### F-30 bug, low: the skill's source-extraction script picks versions by text sort and misreports a missing jar

- **Locations:**
  - `plugins/commandlib/skills/commandlib/scripts/extract-sources.sh:9-11`
  - `SKILL.md:35`
- **What is wrong:**
  - `latest()` uses `sort | tail -1`, so `0.9.0` sorts after `0.18.0`. It also ignores the version the project depends
    on.
  - When no jar is found, the script prints `LOCAL_BUILD`, and the skill then says the CommandLib sources are at the
    project root. That is false for a downstream project.
- **Fix:**
  - Use `sort -V`, or take a version argument.
  - Print `NOT_FOUND`, and use the project root only if `common/src/main/java/dev/marulabs/commandlib` exists there.

### F-31 doc-minor, low: the skill says Paper differs in "registration timing" but never shows how

- **Locations:**
  - `plugins/commandlib/skills/commandlib/references/user-guide.md:34-52`
  - `patterns.md:3-6`
- **What is wrong:** The Paper example registers in `onEnable`, just like Spigot. `CommandLib.bootstrap(BootstrapContext, ...)`
  (`paper/.../CommandLib.java:173-225`) is never mentioned.
- **Fix:** Show `bootstrap` and say when to use it, or drop the claim.

### F-32 backcompat, low: the CHANGELOG leaves out behaviour changes that break 0.17.2 code

- **Location:** `CHANGELOG.md`, Breaking changes
- **What is left out:**
  - `CommandLib.register` and `unregister` now throw `IllegalStateException` off the main thread on spigot and paper
    (ba86162e).
  - Registration now rejects these trees (ca1bd61a, d565e717, c78b4411):
    - two argument branches that both match the same input
    - same-named shared first arguments of different types
    - a command added under a second parent
    - a greedy string argument that does not end its branch
  - Spigot `Command#permissionName()` became `permissionName(String prefix)`.
- **Fix:** Add a "Registration" group and a line for `permissionName(prefix)`.

### F-33 doc-minor, low: stale lists in module-structure.md and forge-version-support.md

- **Locations:**
  - `docs/design/module-structure.md:31-32`:
    - The integration-test row leaves out the Fabric, Folia and Velocity targets.
    - The samples row says "Spigot, Paper, and Forge", but `fabric`, `velocity` and `paper-kotlin` also exist.
    - `benchmark` and `examples` are missing.
  - `docs/design/module-structure.md:177-182`: "Not Yet Implemented" is an empty table under "These platforms have no
    module".
  - `docs/design/forge-version-support.md:270`: the `>=1.18` row lists "`RegisterCommandsEvent` registration", but 1.16.5
    also registers through it. The same doc says so at `:57-59`, and so does `modded/.../CommandLib.java` in the 1.16.5
    branch.
- **Fix:** Update the rows, remove the empty table, and drop that item from the `>=1.18` row.

### F-34 doc-minor, low: THIRD_PARTY_NOTICES.md leaves out declared versions and repo-only tools

- **Locations:**
  - `THIRD_PARTY_NOTICES.md:15-19`: these versions declared in `modded/versions/*/gradle.properties` are missing: Forge
    1.18.2 (40.3.0) and 1.19.2 (43.5.0), NeoForge 1.21.4 (21.4.161), Fabric API 0.119.4+1.21.4, and Minecraft 1.18.2,
    1.19.2 and 1.21.4.
  - `:30-38`: JMH 1.37 (`benchmark/`, GPL-2.0 with Classpath Exception) and MCProtocolLib (integration-test, MIT) are
    missing.
- **Fix:** Add them, or point to the gradle.properties files.

### F-35 spec-question, low: `latest.release` resolves to the old 0.17.2 API until 0.18.0 is tagged

- **Locations:**
  - `README.md:142-517`, which has every install snippet
  - `samples/README.md:25-26`
  - `samples/*/build.gradle.kts`, for example `samples/fabric/build.gradle.kts:51` and `samples/forge/build.gradle.kts:94`
- **What is wrong:**
  - The newest tag is 0.17.2. Until 0.18.0 is released, a copied sample or a README snippet resolves to nothing, or to the
    `net.kunmc` API.
  - `docs/packaging.md:84-85` also warns against `latest.release`, which the README and samples use.
- **Question:** Is this intended until the release? If not, add an "unreleased, use a JitPack commit hash" note, or use
  `<version>` in the README.

### F-36 improvement, low: samples/README doesn't say the spigot composite build compiles every typed NMS module

- **Location:** `samples/README.md:17-23`
- **What is wrong:** A composite build of `samples/spigot` builds the included `:spigot` jar, which bundles every
  `:nms:*` module unless `-Pcommandlib.typedNms` is set. Each module needs BuildTools or a dev bundle.
- **Fix:** Document `./gradlew build -Pcommandlib.typedNms=false`, after checking that the property reaches the included
  build.
- This is reasoned from the build scripts and not run.

### F-37 bug, low: tools/docs-site/hooks.py treats excluded docs as built pages

- **Location:** `tools/docs-site/hooks.py:48`
- **What is wrong:** `files.get_file_from_path(...) is not None` also matches files listed in `exclude_docs`. In MkDocs
  1.6 those stay in `Files` with `inclusion=EXCLUDED`. So a link to `agents/` or `roadmap*.md` would stay a broken site
  link instead of going to GitHub, as the docstring promises.
- **Impact:** None today, because no built page links to those files.
- **Fix:** Also require `f.inclusion.is_included()`.

## Examples module

The examples compile and pass their tests. All four styles produce identical snapshots. I found no backward-compatibility
code in the examples. Their only doc-facing mismatches are F-21 and F-23 in comparison.md.
