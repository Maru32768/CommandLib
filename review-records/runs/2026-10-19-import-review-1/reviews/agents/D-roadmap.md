# Agent D: roadmap verification and requirement traceability

- Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (192 commits). Two uncommitted, comment-only edits (ArgRef.java and OptionGraph.java) were ignored.
- Method: static only. I read code and tests and grepped; I ran no Gradle. "Verified" therefore means that an assertion exists which would fail if the behaviour broke. It does not mean the test was seen passing.
- Sources:
  - `git diff origin/master..HEAD -- docs/roadmap.md docs/roadmap-history.md`
  - HEAD `../../../../docs/roadmap.md` (line numbers below are HEAD lines) and `docs/roadmap-history.md`
- Paths are relative to the repo root.
- Unless a path says otherwise, test methods are in `../../../../common/src/test/java/dev/marulabs/commandlib`.

## 1. Traceability table

Abbreviations:
- **IT**: `integration-test/shared/<platform>/...` fixture run against a real server.
- **ST, PT, MT, VT**: the `spigot-testing`, `paper-testing`, `modded-testing` and `velocity-testing` artifacts.
- **RH**: `../../../../docs/roadmap-history.md`.
- **RM**: `../../../../docs/roadmap.md`.

Each claim is listed with its roadmap reference and claimed status, then the classification, the evidence and notes.

### Vanilla integration (P3, Phase 2)

**Execution result (`setResult`, failure gives `store success 0`)**
- **Roadmap:** RH:23-24, RM:25. Claimed `[x]`.
- **Classification:** Verified on Bukkit. Implemented but insufficiently tested on modded.
- **Implementation:**
  - `CommonCommandContext.java:446-457` holds the result.
  - `CommandRunner.java:175,184-190` returns `ctx.getResult()` to Brigadier.
  - Failures throw `CommandFailure` at `CommandRunner.java:338-344`.
- **Tests:**
  - IT `ScenarioTest#executeStoreReadsResultAndFailure` (spigot :102-138, paper :95-130) asserts `42,0`.
  - IT `VanillaExecutionTest#commandBlockSuccessCount` (spigot :172-191).
  - `CommandResultTest`.
  - `CommandTester.execute` returns `int` in all 5 testers.
- **Notes:**
  - Only a prerequisite failure is checked on a real server. Permission-denied giving 0 is not tested.
  - No Forge/NeoForge IT for store result.
  - A failure with no message returns 0 without throwing, so `store success` stores 1. That is documented at `docs/errors.md:48` but contradicts the blanket claim.

**Vanilla-style feedback (`sendSuccess`, gamerules, silent source)**
- **Roadmap:** RH:25-26, RM:26. Claimed `[x]`.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:**
  - spigot `CommandContext.java:122-127` uses NMS `sendSuccess`.
  - paper `CommandContext.java:139-165` calls it by reflection and **falls back to plain `sendMessage` on `NoSuchMethodException`**.
  - modded `CommandContext.java:100-107`.
  - **velocity `CommandContext.java:100-117` ignores `broadcastToOps` and just calls `sendMessage`.**
- **Tests:**
  - **No test** for `sendCommandFeedback`, `logAdminCommands` or silent sources. A grep of all tests finds nothing.
  - The PT mock goes through the fallback path.
  - IT `ScenarioTest` only checks that `sendSuccess(...,true)` does not fail.
- **Notes:**
  - "全 platform で vanilla の CommandSourceStack#sendSuccess" is false for Velocity.
  - The gamerule behaviour that is the whole point of the item is unverified. RM:26 itself lists "gamerule の tests" as missing, yet the item is marked `[x]`.

**Selector aggregate message (`sendSuccessFor`)**
- **Roadmap:** RH:96. The baseline backlog P3 item required it; RM:26 still lists it as missing.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `CommonCommandContext.java:217-234`.
- **Tests:** `CommonCommandContextTest#success_for_targets_names_a_single_target_and_counts_several` (:30).
- **Notes:** RM:26 is stale.

**Error parity (translatable resolve errors plus context line; Paper `argument.entity.notfound.*`)**
- **Roadmap:** RH:29-30. Claimed `[x]`.
- **Classification:** Partially implemented as worded.
- **Implementation:**
  - `ArgumentParseException.java:109-122` adds the context line only when the exception carries input and cursor. `docs/errors.md:137-141` says resolve errors have no input.
  - `paper/.../SelectorResults.java:10-25`.
- **Tests:**
  - PT `ArgumentFailureTest` :140-170 checks the 4 notfound keys.
  - `spigot/src/test/.../CommandSyntaxExceptionConversionTest:29`.
  - IT `CommandSyntaxExceptionTest` only checks that the conversion does not fall back.
- **Notes:**
  - The "context line" claim does not hold for resolve errors.
  - No modded empty-selector test.
  - RM:84-85 admits real-client verification is open.

**Registry-backed args (Spigot Sound/Attribute/Biome/EntityType as `minecraft:` keys; vanilla slot and colour)**
- **Roadmap:** RH:20-22, RH:35-36. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:**
  - spigot `SoundArgument.java:15-27` and the others use `NMSArgumentNamespacedKey`.
  - `NamespacedKeyArgumentSupport.java:22`.
  - `ScoreboardDisplaySlotArgument.java:26`, `ChatColorArgument.java:16`.
  - paper `ChatColorArgument.java:14-16` returns `NamedTextColor`.
- **Tests:**
  - ST: `SoundArgumentTest:12`, `BiomeArgumentTest:26`, `ScoreboardDisplaySlotArgumentTest:40,64`, `ChatColorArgumentTest:20,32,43`.
  - IT `ArgumentTest` (spigot :237,888,904; paper :242).
- **Notes:**
  - Paper has no per-argument unit tests. Paper `ArgumentCoverageTest` only uses `assertThatCode`.
  - The "sent as a vanilla type" part is proven only by IT.

**`/execute`, command block, function, tag**
- **Roadmap:** RM:78-80. Changed `[ ]`→`[~]`.
- **Classification:** Partially implemented. The status is correct.
- **Tests:**
  - IT `VanillaExecutionTest#commandBlockRunsAsBlockSender` (:129).
  - `DataPackFunctionTest` covers function, function tag and `#minecraft:load` (:61-75,131-149).
  - `ActorTest` (modded).
- **Notes:**
  - RM:80 "残り: command block と tag の tests" is stale for Bukkit, because both are tested.
  - These remain missing on Forge/NeoForge.

**Range and Duration args (Spigot/Paper/Forge/NeoForge)**
- **Roadmap:** RH:27-28, RH:41-42. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:**
  - spigot `IntegerRangeArgument.java:18` (27 of 27 NMS impls).
  - paper `IntegerRangeArgument.java:19`.
  - modded: no fabric gate.
  - `CommonDurationArgument`.
- **Tests:**
  - ST `IntegerRangeArgumentTest:20,44`, `DurationArgumentTest:22`.
  - PT `RangeArgumentTest:29`.
  - MT `RangeArgumentTest:17`.
  - IT spigot `ArgumentTest:289,305,321`, paper :292-324, modded :100-145.
- **Notes:** ST runs on hand-written mocks, so real parsing is proven by IT only.

**BlockPosition / ItemPredicate / BlockPredicate / Location centering switch**
- **Roadmap:** RH:31-32, RH:43-44. Claimed `[x]`.
- **Classification:** Verified, with gaps.
- **Implementation:**
  - paper `BlockPredicateArgument.java:33-38` (reflection, 1.21.8+, carries a `shortcut:` comment).
  - `LocationArgument(name, boolean)` on spigot :29, paper :30 and modded :30.
- **Tests:**
  - ST `BlockPositionArgumentTest:28,44`, `BlockPredicateArgumentTest:31,43`, `ItemPredicateArgumentTest:31,43`.
  - MT `PredicateArgumentTest:42,64`, `PositionArgumentTest:16`.
  - IT `uncenteredLocationArgument` (spigot :552).
- **Notes:**
  - The centering switch has no unit test on Spigot/Paper.
  - RH:44 claims that "vanilla argument type の補完が client に届くこと" was verified, but no IT `SuggestionTest` touches these arguments.

**Component / Message / NBTCompound (incl. Fabric)**
- **Roadmap:** RH:33-34, RH:46. Claimed `[x]`.
- **Classification:** Verified on Spigot/Paper/Forge/NeoForge. Unit-tested only on Fabric.
- **Implementation:** the NMS wrappers, and modded `NBTCompoundArgument.java:12`.
- **Tests:**
  - ST, PT and MT each have `ComponentArgumentTest`, `MessageArgumentTest` and `NBTCompoundArgumentTest`.
  - IT spigot :250/593/609, paper :255/596/615, modded :92/181/185.
- **Notes:** No Fabric real server.

**RegistryArgument and TimeArgument**
- **Roadmap:** RH:35-36, RH:45. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** spigot uses `NMSArgumentTime`; paper `TimeArgument.java:25`; modded `RegistryArgument:19`.
- **Tests:**
  - ST `TimeArgumentTest:20,34`, `RegistryArgumentTest:21-44`.
  - MT `TimeArgumentTest:25,41`.
  - IT spigot :866-920, paper :876-931, modded :227-252.

**IT bug fixes: caption bundle, TeamColor rename, Mohist range link, modded sync executor**
- **Roadmap:** RH:47-48. Claimed `[x]`.
- **Classification:** Implemented. Only the sync-executor fix is Verified.
- **Implementation:**
  - `integration-test.gradle.kts:191`.
  - `NMSArgumentChatFormat.java:15,33-41`.
  - `NMSArgument.java:51-62,83-87`.
  - `ServerCommandScheduler.java:36-44`.
- **Tests:**
  - MT `ServerCommandSchedulerTest:21,35`.
  - The TeamColor and Mohist fixes have **no unit regression test**; they are covered only by IT on 26.2+ and mohist-1.16.5.

**Real-server campaign of 2026-10-16 ("29 targets all passed")**
- **Roadmap:** RM:78-79, RH:42-46, `docs/compatibility.md:124-128`. Claimed `[x]`/`[~]`.
- **Classification:** Ambiguous / unverifiable.
- **Implementation:** The list at compatibility.md:124-128 gives 10 Spigot, 10 Paper (5 on the spigot module, 5 on the paper module), `paper-26.3-spigot-jar`, 2 Mohist, 4 Forge and 2 NeoForge, which is 29.
- **Tests:** No result artefact from 10-16 exists in the repo or on disk. `../../../../integration-test/shared/test-results` is git-ignored, and its newest XML (10-15) shows failures, which were fixed later.
- **Notes:**
  - `paper-26.3-spigot-jar` starts no bot (`integration-test/README.md:112-114`), so 28 targets actually ran cases.
  - Only 20 of the 61 Spigot/Paper targets were run.
  - No Fabric, Folia or Velocity target exists.

### Developer experience (P4, Phase 3)

**Options: repeatable / count / aliases / required / mutex, `OptionParseException`, help option section**
- **Roadmap:** RH:52-53, RM:47. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:**
  - `CommandOption.java:156,190,204,266`.
  - `Options.java:23,47`.
  - `CommandBuilder.java:146`.
- **Tests:** `CommandOptionTest#alias_works_like_the_long_name` :437, `#required_option_fails...` :473, `#count_option_counts_occurrences` :492, `#repeatable_value_option...` :513, `#mutually_exclusive_options_cannot_be_given_together` :544, `#help_message_includes_options` :251.
- **Notes:**
  - Pillar 4 (RM:286-287) still *requires* multi-value options, and they are absent (`OptionArgumentTest#a_variadic_argument_is_rejected`:152). The summary row is still `[x]`.
  - RM:47 and RM:97 list "annotation support" and "options after positional" as missing, but both exist. Those lines are stale.

**`Options.of`, option permission, `optionsAnywhere`, `visibleWhen`**
- **Roadmap:** RH:66-67. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** `Options.java:47,55`; `CommandBuilder.java:154,162`.
- **Tests:** `OptionArgumentTest` (9), `OptionPermissionTest` (6), `OptionsAnywhereTest` (9), `VisibilityTest`.

**Option redirects (`OptionGraph`): "491,607 → 5,490 nodes"**
- **Roadmap:** RH:68-69, RM:89-90. Claimed `[x]` (RM `[~]`).
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `OptionGraph.java:54-56,108`. The only fallback is `redirects = inheritedArguments.isEmpty()`.
- **Tests:**
  - `OptionTreeSizeTest#short_flags_do_not_multiply_the_tree_by_their_orders` :25 only asserts `< 6_000`.
  - Neither 5,490 nor 491,607 is asserted, and the old tree is never measured.
- **Notes:**
  - RM:90 says commands with "primitive 以外の value option" keep the old tree. The code does not do this, and `OptionArgumentTest#a_command_with_a_type_that_is_no_brigadier_primitive_uses_redirects`:59 asserts the opposite.
  - Velocity has not been checked on a real proxy.

**Trailing optional / variadic, with a registration-time error**
- **Roadmap:** RH:54-55, RM:21. Claimed `[x]`.
- **Classification:** Verified (common). Insufficiently tested per platform.
- **Implementation:** `CommonArgument.java:116,128,154,166`; `CommandNodeCreator.java:119-125`.
- **Tests:**
  - common `OptionalVariadicArgumentTest` has 15 tests (:193-240 placement).
  - ST has 4, PT has 1, MT none (indirect only), VT 0.
- **Notes:**
  - No real-server fixture (RM:86 admits this).
  - Variadic is capped at 16, and there is no unbounded form.

**confirm / cooldown (bypass, message, clock, remaining/reset)**
- **Roadmap:** RH:58-59, RH:72-73. Claimed `[x]`.
- **Classification:** Verified (common only).
- **Implementation:** `Confirmation.java:40-62`; `Cooldown.java:48-114`.
- **Tests:** `CommonCommandConfirmCooldownTest` (11), including `:103` (exact message), `:125` and `:160`.
- **Notes:** Cooldown persistence and per-branch confirm/cooldown are open (RM:87).

**Context values, `provide`/`put`/`get`, `@Dependency`, `CommandDefaults`, `ctx.fail`, `@Condition`**
- **Roadmap:** RH:60-61, RH:72-73, RM:37-38. Claimed `[x]`.
- **Classification:** Verified. The condition registry is annotation-only.
- **Implementation:**
  - `CommonCommand.java:553,561`.
  - `CommonCommandContext.java:382-408,523-555`.
  - `CommandDefaults.java:54-74`.
  - `AnnotationParser.java:188,199`.
- **Tests:** `ContextValueTest` (8), `AnnotationParserTest` :239-282 and :436-467, `CommandResultTest:56,62`.
- **Notes:** The baseline item "reusable condition registry" was deleted. The baseline RM:39 gap "reusable condition registry, error customization" became "—", but the fluent API has no named registry.

**Middleware / interceptor**
- **Roadmap:** RH:74-76, RM:36. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** `CommandMiddleware.java:26`; `CommonCommand.java:537,584,764-776`.
- **Tests:** `CommandMiddlewareTest` (11): :51, :64, :127, :138, :175.

**Async execution and `CommandScheduler`**
- **Roadmap:** RH:74-76, RM:27. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** `CommonCommand.java:715`; `CommonCommandContext.java:467,477`; `CommandRunner.java:264,294-301`; a scheduler per platform.
- **Tests:**
  - `AsyncExecutionTest:26,53,121,141` covers thread return, the `/execute` result, timeout and cancel.
  - Every platform has `AsyncCommandTest` and a scheduler test.
- **Notes:** The Phase 3 exit criteria for timeout, cancel, exception and thread return are met for the mock tier. Folia is mock only.

**8+ args without casts; `Command.builder`; `ArgumentBuilder` removed**
- **Roadmap:** RH:77-78, RM:19,57. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:**
  - `CommonCommand.java:301,308`.
  - `CommonCommandContext.java:163`.
  - `Command.builder` in spigot :23, paper :23, modded :36 and velocity :18.
- **Tests:** `CommandStructureTest:55-70`; PT `CommandBuilderTest:34`; ST and MT `CommandBuilderTest`.
- **Notes:**
  - Velocity has no builder test.
  - `ArgumentBuilder` survives only in `.jev-lint` fixtures.

**Help / usage / error formatter, `examples`, `SYNTAX_ADVICE`**
- **Roadmap:** RH:62-63, RM:48. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** `HelpFormatter.java:17`, `UsageFormatter.java:18`, `ErrorFormatter.java:35`, `CommonCommand.java:698`.
- **Tests:** `HelpErrorFormatterTest` (6, including :76 and :160); ST and PT `HelpErrorFormatterSnapshotTest`; `CommandDocsTest:78`.
- **Notes:** RM:48 "CommandDocs への examples の出力" is stale, because it is done.

**Error taxonomy docs (`../../../../docs/errors.md`)**
- **Roadmap:** RH:37, RM:49. Claimed `[x]` (RM `[~]`).
- **Classification:** Verified (doc ↔ bundle).
- **Implementation:** `lang/commandlib_en.properties`.
- **Tests:** `CommandOptionTest`, around :483; `CaptionsTest:137`.
- **Notes:** Unknown subcommand and missing argument still have no dedicated error (RM:49).

**Localization / captions**
- **Roadmap:** RM:58 `[~]`. The baseline backlog item "localization / caption system を追加する" was **deleted with no RH entry**.
- **Classification:** Verified (mock tier).
- **Implementation:** `Caption.java`; `Captions.java:52-144`; en/ja bundles; `@CaptionBundle`.
- **Tests:**
  - `CaptionsTest` (10), including :36 (bundle parity) and :92.
  - `CaptionLocaleTest` in ST, PT, MT and VT.
- **Notes:**
  - RM:58 still lists "help / usage formatter API" as missing, which is stale.
  - Real-server locale is tested only for `en_us` (RM:92).

**Annotation API (Spigot/Paper/Velocity/modded; "fluent と同じ … snapshot で確認")**
- **Roadmap:** RH:64-65, RH:79-80, RM:55. Claimed `[x]` (RM `[~]`).
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** 30 annotations; `AnnotationParser.java:147-230`; `AnnotatedCommands` in spigot :44, paper :45, velocity :29 and modded :64.
- **Tests:**
  - `AnnotationParserTest` :158, :298, :579, :666, :810 (it compares tree, docs JSON/MD and transcript).
  - ST and PT `AnnotationStyleTest:29,51` with snapshots.
  - MT `AnnotationStyleTest:28` (arena only, no snapshot).
- **Notes:**
  - **Velocity `AnnotatedCommands` is never referenced by any test**, which contradicts the snapshot claim for Velocity.
  - `java.util.Optional<T>` is unsupported (RM:93).

**Kotlin DSL (Spigot/Paper/Velocity, `executeSuspend`)**
- **Roadmap:** RH:81, RM:56. Claimed `[x]` (RM `[~]`).
- **Classification:** Verified.
- **Implementation:** `common-kotlin/.../Coroutines.kt:46-107`; the paper and velocity kotlin modules reuse `../../../../common-kotlin/src/platform`.
- **Tests:** `KotlinDslTest.kt:97,105,153,194`; `BukkitDslTest.kt:35`.
- **Notes:**
  - It reads the package-private `CurrentCommandContext` (`CurrentCommandContexts.kt:3-25`). This is a split package across two jars, and it would block the planned Forge/NeoForge Kotlin modules under JPMS.
  - Forge/NeoForge/Fabric versions are missing.

**Common example suite (7 use cases × 4 styles)**
- **Roadmap:** RM:189-190. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:** `../../../../examples` contains balance, ban, home, kit, score, stats and team, each as Fluent, Builder, Annotated and Kotlin.
- **Tests:** `StyleParity.assertSameInEveryStyle` :28-44; `*ExampleTest`; a snapshot for each case.
- **Notes:** The transcript includes the tree and help but not `permissionsMarkdown`, so option-level permission nodes are not compared.

**`../../../../docs/comparison.md`**
- **Roadmap:** RM:192-193. Claimed `[x]`.
- **Classification:** Verified, with caveats.
- **Notes:**
  - Competitor snippets are not compiled; the roadmap admits this.
  - `comparison.md:565-566` omits that annotated methods can return `int`.

**Benchmarks (p50/p95, tolerances, `jmhCompare`)**
- **Roadmap:** RM:194-196. Claimed `[~]`.
- **Classification:** Partially implemented.
- **Implementation:**
  - `benchmark/build.gradle.kts:51-106`; `../../../../benchmark/baseline.json`.
  - The tolerances (commit 9cf955f2) predate the baseline (1b928a3d).
- **Tests:** None. It is not run by `check` and there is no CI.
- **Notes:**
  - The baseline was taken under ~100% CPU (`docs/benchmarks.md:86-87`).
  - Benchmarks missing from the baseline are silently skipped (:92).

**Testing artifact: `CommandLibExtension` / `@Sender`, `suggest`, snapshot, int `execute`**
- **Roadmap:** RH:56-57, RM:59, RM:430. Claimed `[x]` / `[~]`.
- **Classification:** Verified.
- **Implementation:** `CommandLibExtension`, `Sender` and `CommandTester` in all 5 testing artifacts (modded-testing since c475687a).
- **Tests:** `CommandLibExtensionTest` in each artifact (MT :25,36,42,53).
- **Notes:**
  - RM:430 "modded-testing は未実装" is **stale**.
  - RM:59 omits velocity-testing.

**Dynamic refresh (`refresh` / `refreshAll`)**
- **Roadmap:** RH:82-83, RM:28. Claimed `[x]`.
- **Classification:** Verified on Spigot/Paper. Insufficiently tested on modded.
- **Implementation:**
  - spigot `CommandLib.java:690,701`.
  - paper `:370,381,387` (thread guard).
  - modded `:191,201`, which also applies to Fabric although the roadmap omits it.
- **Tests:**
  - spigot `CommandTreeRefreshTest`; PT `CommandTreeRefreshTest` (3 each).
  - IT calls `refreshAll()`, but the bot only checks that it can decode the tree.
- **Notes:** No modded unit test, and modded `refresh(player)` is never exercised.

### Platforms and tooling (P5/P6, Phase 4/5)

**Velocity support**
- **Roadmap:** RH:87-88, RM:32. Claimed `[x]` in RH, `[~]` in RM.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `velocity/.../CommandLib.java:155-159,200`; `PlayerArgument`, `ServerArgument`; `../../../../samples/velocity`.
- **Tests:** VT `CommandLibTest:63-148`, `CommandTesterTest:67-142`, `CommandLibDiagnosticsCommandTest:9` (Mockito proxy).
- **Notes:**
  - The "manual smoke on 3.4.0/4.2.0" is prose only, from commit 8898351a. It predates `diagnostics()` (2d0380b0) and 8 later velocity commits.
  - There is no integration target.
  - RM:32 "Velocity sample" missing is stale.

**Fabric support (1.20.1/1.21.1/1.21.4)**
- **Roadmap:** RH:89-91, RM:34. Claimed `[x]` in RH, `[~]` in RM.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `modded/.../CommandLib.java:213-252` (`CommandRegistrationCallback` v2 :222, permissions API :246-249).
- **Tests:** MT `FabricPermissionTest#defaults_decide_without_fabric_permissions_api` covers only the branch without the API.
- **Notes:**
  - The registration callback and the branch with the permissions API present are untested.
  - There is no real-server target.
  - RH marks this `[x]`, against the roadmap's own rule (RM:186-187) that platform completion needs real-server evidence.

**Forge 1.18.2/1.19.2, NeoForge 1.21.4**
- **Roadmap:** RH:91, RM:29,33. Claimed `[x]`.
- **Classification:** Ambiguous.
- **Implementation:** `settings.gradle.kts:51-62`; `../../../../integration-test/targets/forge-1.18.2`, `forge-1.19.2`, `neoforge-1.21.4`.
- **Tests:** Targets exist, but no results are saved. Fix commit e51a8297 suggests the targets ran.
- **Notes:**
  - RH:91 says "実サーバー未検証", while RM:29 and RM:33 say "検証済み". These contradict each other.
  - RM:29 puts "verified" in the "missing" column.

**Folia**
- **Roadmap:** RH:92-93, RM:31. Claimed `[x]` in RH, `[~]` in RM.
- **Classification:** Partially implemented.
- **Implementation:**
  - paper `FoliaCommandScheduler.java:29-61,119`.
  - `paper/.../CommandLib.java:146,225,511-515`.
  - spigot rejects Folia at `CommandLib.java:116-120`.
- **Tests:** PT `FoliaCommandSchedulerTest:61-128` (mock).
- **Notes:**
  - The **spigot Folia rejection has no test.**
  - No guard on register or unregister from a region thread.
  - No Folia target.
  - RH `[x]` violates the real-server rule.

**Diagnostics, `CommandDocs`, permission export**
- **Roadmap:** RH:100-101, RM:63-65. Claimed `[x]`.
- **Classification:** Verified.
- **Implementation:**
  - `diagnostics()` in spigot :734, paper :256, modded :105 and velocity :200.
  - `CommandDocs.java`, `Json.java`.
- **Tests:**
  - `CommandDocsTest:14-95` (snapshots).
  - `CommandLibDiagnosticsTest#report_matches_snapshot`.
  - `CommandLibDiagnosticsCommandTest` in ST, PT, VT and MT.
  - IT `RuntimeFeatureTest:79`.
- **Notes:** The roadmap omits Fabric and Velocity, which have it.

**Packaging doc (shade + relocate)**
- **Roadmap:** RH:102-103. Claimed `[x]`.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `../../../../docs/packaging.md`; all 6 samples relocate.
- **Tests:** Relocation is exercised only for Bukkit (`integration-test.gradle.kts:214`).
- **Notes:**
  - The modded fixture merges jars **unrelocated** (`shared/modded/build.gradle.kts:45-54`), although the doc says relocation is required for Forge 1.18+ and NeoForge.
  - A relocated Kotlin jar is never run.

**Example plugin collection (`../../../../samples`)**
- **Roadmap:** RH:104. Claimed `[x]`.
- **Classification:** Implemented but insufficiently tested.
- **Implementation:** `samples/{spigot,paper,paper-kotlin,velocity,forge,fabric}`.
- **Tests:** Not in `../../../../settings.gradle.kts` and not built by any automation. Only `samples/paper` has a test.
- **Notes:** The samples resolve `latest.release` = 0.17.2 (`net.kunmc.lab`, no paper/velocity/fabric artifacts), so they cannot build downstream until 0.18.0 ships.

**Error Prone / NullAway on NMS and modded; modded javadoc jar**
- **Roadmap:** RH:105-106, RM:136-138. Claimed `[x]`.
- **Classification:** Verified (config).
- **Implementation:** `build.gradle.kts:83-114,258-307`.
- **Tests:** n/a (build config).
- **Notes:** The integration fixture mod is excluded (RM:138 admits this).

**Documentation site**
- **Roadmap:** RM:126-127. Changed `[ ]`→`[~]`.
- **Classification:** Partially implemented.
- **Implementation:** `../../../../mkdocs.yml`.
- **Tests:** No deploy and no CI.

**Migration guides (CommandAPI / Cloud / Lamp / ACF)**
- **Roadmap:** RH:38-40, RM:62. Claimed `[x]`.
- **Classification:** Implemented, with contradictions.
- **Implementation:** `docs/migration/*.md`.
- **Tests:** n/a.
- **Notes:** See D-12.

**Mod-loader `minecraft`/`loader` split**
- **Roadmap:** RM:111-112. Changed `[ ]`→`[?]`.
- **Classification:** Re-scoped. The justification is plausible.
- **Notes:** This was a backlog item and is now "maybe not needed". Phase 4 / Pillar 11 still describe the split.

**Capability-based registration**
- **Roadmap:** RM:122. Unchanged `[ ]`.
- **Classification:** Missing.
- **Notes:** It is a Phase 4 deliverable (RM:604).

**Binary compatibility check in CI / 0.18.0 / Maven Central**
- **Roadmap:** RM:119-121, RM:134-137. `[~]`/`[ ]`.
- **Classification:** Missing.
- **Implementation:**
  - japicmp is configured only for `:common` and `:spigot` against 0.17.2 (`build.gradle.kts:309-353`), and is expected to fail on the package move.
  - There is no `.github/` and no CI.
  - There is no 0.18 tag.
- **Notes:** Phase 4 and Phase 5 exit criteria.

## 2. Notable gaps

- **D-01 (P0): No CI, no binary-compatibility gate, no release, no Maven Central.**
  - There is no `.github/` or other CI. japicmp exists only for common and spigot against a `net.kunmc.lab` baseline, and it is not wired anywhere.
  - 0.18.0 is unreleased and there is no Maven Central publishing.
  - This leaves the following GA exit criteria unmet: Phase 4 RM:613 (binary compatibility in CI) and Phase 5 RM:631 (Maven Central). The roadmap's own step 1 (RM:183) puts these first.
- **D-02 (P0): No release notes or changelog for breaking changes.**
  - No CHANGELOG or release-notes file exists, although the range contains breaking changes: the package move, `AbstractCommandContext` and `ArgumentBuilder` removed, and the `bukkit` artifact removed.
  - This leaves Success Metric RM:652 unmet.
- **D-03 (P1): Platform "support" items are marked `[x]` in roadmap-history without real-server evidence.**
  - Velocity (RH:87), Fabric (RH:89), the new modded matrix (RH:91) and Folia (RH:92) are all marked `[x]`.
  - This breaks the completion rule at RM:186-187.
  - The Phase 4 exit criteria RM:609-610 (Folia, Velocity and Fabric verified in the target environment) are unmet: there is no Folia, Velocity or Fabric target.
  - Untested paths:
    - Fabric's `CommandRegistrationCallback`.
    - Fabric with fabric-permissions-api present.
    - The spigot Folia rejection.
    - A region-thread registration guard, which is also missing.
  - The Velocity smoke test is undocumented prose and predates the current code.
- **D-04 (P1): The real-server campaign of 2026-10-16 has no saved evidence.**
  - "29 targets all passed" (RM:78-79, RH:42-46) cannot be reproduced from the repo. The only local results are from 10-15 and show failures.
  - 28 of the 29 ran cases; `paper-26.3-spigot-jar` starts no bot.
  - Only 20 of the 61 Spigot/Paper targets were run, although Phase 2 exit RM:567 asks for coverage of the main supported versions.
  - RH:44 claims client-side completion checks that no IT test performs.
- **D-05 (P1): Vanilla-style feedback is marked `[x]` with its defining behaviour untested.**
  - No test anywhere covers `sendCommandFeedback`, `logAdminCommands` or silent sources.
  - Paper falls back silently to `sendMessage` on `NoSuchMethodException` (`paper/.../CommandContext.java:139-165`), and the PT mock runs that fallback.
  - Velocity ignores `broadcastToOps` (`velocity/.../CommandContext.java:100-117`), contradicting "全 platform" (RM:26).
- **D-06 (P2): The `OptionGraph` size claim is not asserted, and the roadmap misstates the fallback.**
  - "491,607 → 5,490" (RH:68) is only bounded by `< 6_000` (`OptionTreeSizeTest:25`).
  - RM:90's fallback description ("primitive 以外の value option keeps the old tree") is contradicted by the code and by `OptionArgumentTest:59`.
  - No test asserts that an argument child gets no redirects.
- **D-07 (P2): The annotation API on Velocity is untested, although it is claimed as snapshot-verified.**
  - Velocity `AnnotatedCommands` is never referenced by a test.
  - modded-testing has only the arena case and no snapshot.
- **D-08 (P2): The error-parity claim is overstated.**
  - RH:29 promises a context line on resolve errors, but `ArgumentParseException.java:109-122` and `docs/errors.md:137-141` show there is none.
  - IT `CommandSyntaxExceptionTest` checks only that the conversion does not fall back.
  - There is no modded empty-selector test.
- **D-09 (P2): Optional/variadic coverage is thin per platform, with nothing on a real server.**
  - PT has 1 test, VT 0, and MT none directly.
  - There is no IT fixture (RM:86).
  - The tree-size impact of `maxCount` × options is unmeasured.
- **D-10 (P2): Requirements were silently weakened or re-scoped.**
  - Pillar 4 "multi-value options" (RM:287) is still required and unimplemented, yet the options summary row is `[x]` (RM:47).
  - The "reusable condition registry" became annotation-only `@Condition`, and the gap was erased ("—", RM:39). The fluent API has no registry.
  - The baseline backlog items for localization and the Velocity policy were deleted without an RH entry.
  - The `minecraft`/`loader` split went from `[ ]` to `[?]`.
  - The selector aggregate message was folded into `[x]` while RM:26 still lists it as missing.
- **D-11 (P2): Packaging and samples are not validated.**
  - Modded relocation is never exercised: the IT fixture is unrelocated, although `../../../../docs/packaging.md` calls relocation required.
  - Samples are not built anywhere, and they cannot build downstream until 0.18.0 exists.
  - The Kotlin split-package trick (`CurrentCommandContexts.kt`) is a latent JPMS blocker for Forge and NeoForge.
- **D-12 (P3): Docs contradict themselves or the code.**
  - `docs/migration/from-acf.md:326` lists "reusable named conditions" as not available, but :216 documents `@Condition`.
  - `docs/migration/from-commandapi.md:315` lists "requirement-based visibility" as not available, but :211 maps it to `visibleWhen`.
  - `docs/migration/from-cloud.md:102` says `BlockPredicateParser` and `ItemStackPredicateParser` are not available, but `BlockPredicateArgument` and `ItemPredicateArgument` were added in this range.
  - README "Why CommandLib" claims 1.16.5–26.x for Forge/NeoForge/Fabric, but modded stops at 1.21.4.
  - The README says every command gets a registered permission default, which is false on Velocity (`docs/compatibility.md:38`).
  - The "real selectors" line in `docs/arguments.md:348-349` is unproven for modded-testing (null level; RM:83).
- **D-13 (P3): The roadmap's own text is stale or contradicts itself.**
  - RM:26: the aggregate message exists.
  - RM:47: annotation support for options exists.
  - RM:48: `CommandDocs` examples are done.
  - RM:58: the formatter API is done.
  - RM:59: velocity-testing is omitted.
  - RM:80: command block and tag tests exist for Bukkit.
  - RM:97: options after positional arguments are done.
  - RM:430: modded-testing does have `CommandLibExtension`.
  - RM:32: the Velocity sample exists.
  - RM:29 vs RH:91: "verified" vs "未検証".
  - The competitor table RM:164-168 still says async, pipeline, annotation and Kotlin are not implemented.
  - RM:26 claims "全 platform".
- **D-14 (P2): Benchmark regression detection is not operational.**
  - `jmhCompare` is manual and not in `check`.
  - The baseline was taken under load, and missing benchmarks are skipped silently.
  - Success Metric RM:656 is not met.
- **D-15 (P3): Bug fixes have no unit regression test.**
  - The Spigot 26.2+ `TeamColorArgument` fix and the Mohist 1.16.5 range link fix are covered only by IT.
  - The Spigot/Paper `LocationArgument` centering switch has no unit test.
- **D-16 (P3): Result semantics are inconsistent with the claim.**
  - A failure with no message returns 0 instead of throwing, so `store success` is 1. This contradicts RH:23's "失敗を store success 0 に".
  - A permission-denied result has no IT.

## 3. GA prerequisites still open

- **Phase 2 exit (RM:562-568):**
  - Command block and function are unverified on Forge/NeoForge.
  - Paper translatable errors are unverified on a real client (RM:84).
  - Only 20 of the 61 Bukkit targets were run in the latest campaign.
- **Phase 3 exit (RM:587-593):**
  - Largely met by `../../../../examples`, `AsyncExecutionTest`, middleware and the migration guides.
  - Weak spots: the annotation API on Velocity, and option permissions in the style-parity transcript.
- **Phase 4 exit (RM:607-614):** unmet. It lacks:
  - Folia, Velocity and Fabric verified in their environments.
  - A real Folia thread check.
  - Binary compatibility in CI.
  - Capability-based registration (RM:122).
- **Phase 5 exit (RM:627-632):** unmet. It lacks:
  - Maven Central.
  - A published documentation site.
  - A downstream compatibility test kit.
  - Reproducible benchmark results; the baseline is not idle.
- **実装順と完了判定 (RM:199-202):** open items are parity per major competitor area with one demonstrated advantage, and a published list of platform constraints and out-of-scope uses.
- **Success Metrics (RM:645-656):** release notes (D-02), latest-Paper follow-up tracking and benchmark regression detection (D-14) are unmet.

## 4. Coverage

- **Verified:**
  - Every RH line added in this range (P3, P4, P5 and P6 completed sections).
  - Every summary-table row whose status or text changed (RM:19-65).
  - The new and changed backlog items (RM:70-138).
  - The completion-criteria checklist (RM:189-202).
  - Release Phase exit criteria, Success Metrics, and the Pillar 9 line change (RM:430).
- **Docs spot-checked:** README, `../../../../docs/comparison.md`, `docs/migration/*`, `docs/errors.md`, `docs/arguments.md`, `docs/compatibility.md`, `docs/testing.md` and `docs/packaging.md`.
- **Not verified:**
  - Whether any test passes (no Gradle run).
  - Product Pillars 1-11 line by line (only the requirements related to changed items).
  - `docs/design/*` content.
  - `../../../../docs/suggestions.md`, `docs/permissions.md`, `docs/troubleshooting.md` and `docs/vanilla-integration.md` in detail.
  - Whether Shadow actually rewrites the `"dev.marulabs.commandlib.nms."` literal.
  - `mkdocs build --strict`.
