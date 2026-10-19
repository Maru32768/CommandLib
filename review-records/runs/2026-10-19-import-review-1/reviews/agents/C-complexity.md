# Agent C: complexity and maintainability review

Range: `origin/master` 2c8ffd7a .. HEAD 1b8f17dc (192 commits, 1666 files, +65,536/-3,167). Working-tree edits ignored.
Method: `ponytail:ponytail-review` (loaded through the Skill tool), done by hand and read-only. Facts come from `git diff`,
`git grep`, `diff` and line counts. Nothing was built or run.

What this change does: it carries out docs/roadmap.md. It adds options (with an option graph that uses Brigadier
redirects), an annotation parser, a Java builder and a Kotlin DSL, help, usage and error formatters, captions,
middlewares, context values, docs and diagnostics, five testing modules, examples, samples, benchmarks and many more
real-server integration targets.

Assumed load: a library shaded into plugins. Maintainability cost is paid by one maintainer plus agents, so
copies that must change together count for more than raw line count.

## Findings

### C-01 [P2, a real bug that duplication hides] Help: the rule for which branch is a usage exists three times and the copies disagree
- Component: common / help
- Files: `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java:59-66` (top level),
  `HelpMessageAction.java:97-108` (nested, inside `addArgumentChildUsages`), `CommandDocs.java:288-300` (docs).
- What is duplicated: three places decide whether an argument branch is a usage of its own:
  - top-level help: `children().isEmpty() || isExecutable()`
  - nested help: `children().isEmpty()` only
  - CommandDocs: `branch.executable || branch.children.isEmpty()`
- Evidence: commit 59baaa24 ("leave argument branches that only lead to child commands out of the help usages") added
  `isExecutable()` to the top-level copy only (`git log -S'isExecutable()'`). Concrete case: a child command
  `mode` under `/config <key>` has an argument branch `<value>` that both executes and has a child `reset`. The help of
  `/config` lists `/config <key> mode <value> reset` but leaves out `/config <key> mode <value>`, which is a valid
  input. The help at top level and CommandDocs both list such a branch.
- Simpler: add one predicate, say `Arguments.endsUsage()` (`children.isEmpty() || isExecutable()`). Use it in both
  HelpMessageAction places, and let `CommandTree.BranchEntry` take it for CommandDocs. Add a nested case to
  `HelpErrorFormatterTest`/the help tests. The diff is a few lines net.
- Risk: help snapshot files that contain the nested case change. That change is the fix.
- Confidence: medium-high, from reading the code. No test was run to show it.

### C-02 [P2] Every platform's `AnnotatedCommands` repeats the annotation mapping for common value types
- Component: spigot / paper / velocity / modded annotation entry points
- Files: `{spigot,paper,velocity,modded}/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java`
  (velocity:42-90 is the example; the others match it).
- What is duplicated: about 50 lines that register String (with `@Greedy` → PHRASE), Integer, Long, Float and Double
  (with `@Range`), Boolean, Enum and Duration. The float clamping to `±Float.MAX_VALUE` appears in all 4 files
  (`git grep -c Float.MAX_VALUE`). Spigot and paper differ in 1 line (`ChatColor` vs `NamedTextColor`).
- Evidence: the platform classes `IntegerArgument`, `StringArgument` and the others have 0 overrides. They are thin
  subclasses of the concrete `Common*Argument` classes. So the meaning of `@Range` and `@Greedy` is platform-neutral
  logic, kept in 4 copies.
- Simpler: give `AnnotationParser` a method that registers these defaults from constructor references, for example
  `registerStandardArguments(StringArgument::new, IntegerArgument::new, ...)`. Or have it register them by default
  with `Common*Argument`.
- Risk: `CommandTree.typeName` prints the argument's class simple name. Registering `Common*Argument` directly would
  rename the types in tree snapshots and docs (`IntegerArgument` → `CommonIntegerArgument`). The constructor-reference
  form avoids that and still saves about 35 lines per platform (about 140 total). It also gives Range/Greedy one home.
- Confidence: high.

### C-03 [P3] `CommandBuilder` copies 45 `CommonCommand` methods, and nothing checks that the copy stays complete
- Component: common
- Files: `../../../../common/src/main/java/dev/marulabs/commandlib/CommandBuilder.java` (445 lines, 51 public members).
- What is duplicated: each configuration method of `CommonCommand` has a one-line forwarder here. Only
  `argument(..., Consumer<branch>)` ×8 and `require` add real value, because `CommonCommand.argument` returns a branch
  and breaks the chain. `configure(Consumer<T>)` already reaches every other method.
- Evidence: 5 of the 6 commits that touch the file only add a forwarder for a new `CommonCommand` feature (28465b3f,
  03c2c57e, ab600a49, 11f4294f, fd559a9f). No test checks that the method sets match. The builder is tested only in
  platform modules (`spigot-testing` and `paper-testing` have byte-identical 152-line `CommandBuilderTest`s, and
  `modded-testing` has 114 lines). Common and velocity have none.
- Simpler, pick one:
  - (a) A reflection test in common that fails when a public `CommonCommand` configurer has no builder method of the
    same name. This is about 20 lines and keeps the API.
  - (b) Cut the builder down to `argument`/`require`/`configure`/`build` and a few common setters. The user allows
    breaking changes. About 250 lines go.
- Risk: (b) breaks the API and needs the 7 `examples/*/Builder*Command.java` files and the docs rewritten. (a) has no
  risk.
- Confidence: high on the facts. Which option to take is a product decision.

### C-04 [P3] `CommandLibDiagnosticsCommand` has 4 copies with identical code
- Files: `{spigot,paper,velocity,modded}/src/main/java/dev/marulabs/commandlib/CommandLibDiagnosticsCommand.java`
  (46 lines each).
- Evidence: `diff` shows differences only in the Javadoc example lines. The constructor, `section` and `send` are
  identical. Only `CommandLib.diagnostics()` and the `Command`/`CommandContext` types are per platform.
- Simpler: a generic static helper in common, such as `CommandLibDiagnostics.configure(T root, Function<String,T>
  factory, Supplier<CommandLibDiagnostics>)`. Each platform class becomes a constructor that calls it. That is about
  -90 lines and keeps one copy of the "No registrations." behavior.
- Risk: low. The public class names stay.
- Confidence: high.

### C-05 [P3] Spigot and paper keep byte-identical copies of Bukkit-API code, and one copy has already drifted
- Files: 11 of 63 same-named main files are identical (298 lines). 9 more differ in 6 lines or fewer. For example:
  - `Command.java` differs only in the exception message at line 90 (the copies have already drifted)
  - `annotation/AnnotatedCommands.java` differs in 1 line
  - `BukkitCommandScheduler.java` differs only in a `Supplier<Plugin>`
  - in testing, `Sender.java` (41 lines) and `CommandLibExtension.java` (98 lines) are identical
- Why it matters: the memory rule about parity calls for the same argument set, not the same files. These are
  Bukkit-API classes with no Paper-specific content.
- Simpler: a shared `bukkit-shared/src/main/java` directory added through `sourceSets.main.java.srcDir` in both
  builds, for the identical files only.
- Risk: medium-low. The paper module compiles against paper-api and spigot against spigot-api 1.16.5. A shared file
  must compile against both, which today's identical files already do. IDE navigation gets slightly harder.
- Confidence: high on the counts. The value is moderate.

### C-06 [P3] The five testing modules each re-implement the same JUnit and snapshot plumbing
- Files: `*-testing/src/main/java/dev/marulabs/commandlib/{CommandLibExtension,Sender,CommandTester}.java`, and
  `../../../../common/src/main/java/dev/marulabs/commandlib/Snapshots.java`.
- Evidence:
  - `CommandLibExtension` has 5 copies of 92-98 lines. The store and resolve lifecycle is the same; only the sender
    building differs.
  - `CommandTester` repeats its 3 constructors, the nested `Builder` (`command/commands/permissionPrefix/scheduler/
    defaults`), `commandTree()` and `assertCommandTreeMatchesSnapshot` in all 5.
  - The root cause is that `common-testing` ships its own `dev.marulabs.commandlib.PlatformAdapterImpl`. A platform
    testing module therefore cannot depend on it, because the adapter is found by `Class.forName` and the two would
    clash.
  - So the shared `Snapshots` helper lives in common main code and ships in every plugin's shaded runtime jar. It reads
    `COMMANDLIB_UPDATE_SNAPSHOTS` and `src/test/resources`.
  - The names have also drifted. Modded uses `withPlayer/withLevel/withServer`, while the others use
    `withFakePlayer/withFakeWorld/withFakeServer`.
- Simpler: move `PlatformAdapterImpl`/`TestCommand*` in common-testing out of the published main source set. Then the
  platform testing modules can `api(project(":common-testing"))` and share `Snapshots` plus an abstract extension
  base. Separately, pick one naming scheme for the `with*` methods.
- Risk: package-split classes on the test classpath. Renaming the `with*` methods is breaking, which is allowed.
- Confidence: medium. The amount saved is about 300 lines, and the restructure needs care.

### C-07 [P3] Spigot-testing and paper-testing have 10 byte-identical new test files that test common logic
- Files: `{spigot,paper}-testing/src/test/java/dev/marulabs/commandlib/{AnnotatedArenaCommand, AnnotatedGuildCommand,
  AnnotationStyleTest, AsyncCommandTest, CaptionLocaleTest, CommandBuilderTest, FluentArenaCommand, FluentGuildCommand,
  HelpErrorFormatterSnapshotTest}.java`, `argument/NBTCompoundArgumentTest.java`. That is 764 lines per copy.
- Evidence: every one of them was added in this range (`--diff-filter=A`). Apart from the NBT test, they test behavior
  of common or the annotation parser.
- Simpler: keep one copy, either in common tests through `common-testing` or in one platform module. This fits the
  memory rule about test placement, because these files are new in this range rather than older tests being moved.
- Risk: the second platform loses some of its wiring checks. Keep one platform smoke test if needed.
- Confidence: high.

### C-08 [P3] The spigot and paper integration-test plugins share 13 identical files (about 1,370 lines) and are edited in lockstep
- Files: `integration-test/shared/{spigot,paper}/src/main/java/dev/marulabs/testplugin/`. Identical:
  - TestBase, RuntimeFeatureTest, CommandSyntaxExceptionTest, RuntimePermissionTest
  - VanillaExecutionTest (217), OptionTest (383)
  - SuggestionTest, CommandDispatch*, HelpMessageTest, MainCommand, ReloadWatcher
  Nearly identical: `ArgumentTest` (1,032 lines, 39 differing), ScenarioTest, DataPackFunctionTest, HelpTopicTest,
  TestMain.
- Evidence: 8 of the 10 commits in range that touch `shared/spigot/src` also touch `shared/paper/src`.
- Simpler: move the identical files to a shared source directory, next to the existing `shared/core`, and add it
  through `srcDir` in both plugin builds.
- Risk: low. Both plugins already compile these files unchanged.
- Confidence: high.

### C-09 [P3] `../../../../benchmark/baseline.json` is 7,576 lines, and the compare task reads about 5% of them
- Files: `../../../../benchmark/baseline.json`, `benchmark/build.gradle.kts:33-47` (jmhBaseline), `:59-110` (jmhCompare).
- Evidence: 34 entries at about 220 lines each. `jmhCompare` reads only these fields:
  - `benchmark`, `mode`, `params`
  - `primaryMetric.score` and `primaryMetric.scorePercentiles.{50.0,95.0}`
  - `secondaryMetrics."gc.alloc.rate.norm".score`
  The file also stores every gc.* metric, scoreConfidence, all percentiles and the developer's absolute JDK path
  (`C:\Users\maru3\.jdks\...`). The first entry has scoreError 0.20 against a score of 0.048. That noise is far above
  the 10% thrpt tolerance, so a compare against that entry means nothing.
- Simpler: have `jmhBaseline` write only the fields listed above, about 15 lines per entry and roughly 500 lines in
  total. That is about -7,000 lines of churn on each re-baseline, and the local path is no longer committed.
- Risk: none for `jmhCompare`. Anyone reading the raw file by hand would lose the extra fields.
- Confidence: high.

### C-10 [P3] 1,452 generated NMS Java files are committed with no check that they match the generator, and some modules are identical
- Files: `nms/*/src/**`, `tools/nms-codegen/*.py`.
- Evidence:
  - The README says "generated sources are committed; edit the scripts and regenerate". Nothing in the build checks
    for drift: `generate.py` is referenced only in a build.gradle.kts comment, and there is no CI (by plan).
  - Hand edits therefore go unnoticed. a8077449 ("regenerate ... so their imports match the generator") shows drift
    has already happened once.
  - `spigot-1.20.3` and `spigot-1.20.4` (both `v1_20_R3`) differ in 0 lines besides the package name. Each module
    is about 1.4k lines plus its own BuildTools run.
- Simpler: add a `generate.py --check` mode that writes to a temp directory and diffs, about 10 lines, run from the
  `check` task or by hand. Separately, try whether `spigot-1.20.4` can cover 1.20.3. The doc
  (`docs/agents/nms-build.md:25-29`) already allows a module to cover another release of its revision, as
  1.20.1→1.20 and 1.21.8→1.21.7 do. The same goes for 1.17/1.17.1, 1.18/1.18.1 and 1.19/1.19.1/1.19.2.
- Risk: collapsing modules is safe only if the obfuscated mappings match and the integration targets pass
  `TypedNmsTest`. That was not checked here, so confidence on the collapse is low. Confidence on the drift check is
  high.

### C-11 [P3] Minor duplication inside AnnotationParser
- Files: `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java:881-896` and `:1131-1148`.
- Two copies of the reflective `invoke` with InvocationTargetException unwrapping (RuntimeException, Error, then wrap).
  One private `invoke(Method, Object[])` helper would save about 12 lines. The executor would keep its own
  `CommandPrerequisiteException` and checked-exception branch.
- No other finding for this class. It is 1,400 lines, but it is one parse session with methods grouped by job
  (registration, path/branch resolution, arguments, options, executor). There are no parallel duplicate paths, and an
  856-line test covers it. Splitting it by line count would not help.
- Confidence: high. Value is low.

### C-12 [P3] Dependency versions are hard-coded in each module, with mixed JUnit versions
- Files: `*/build.gradle.kts`.
- Evidence:
  - `com.mojang:brigadier:1.0.18` appears in 13 build files.
  - JUnit is 5.8.1 in common, common-testing, spigot and spigot-testing, and 5.10.2 in paper, velocity, the other
    testing modules, examples and the Kotlin modules (root `build.gradle.kts:209`).
  - The `compileOnly junit-jupiter-api` versions that the testing modules expose to users also differ (5.8.1 vs 5.10.2).
- Simpler: a `gradle/libs.versions.toml` with one line per library. Each bump then touches one file instead of up to 13.
- Risk: low.
- Confidence: high.

## Looked at and judged justified (not findings)
- `OptionGraph` redirects and the `recover` re-parse (~700 lines): commit c31bc162 measured 491,607 → 5,490 nodes
  for six flags. Brigadier's leaf context has no link back to the contexts before a redirect, and the platform owns
  `S`, so stashing state there is not possible. `OptionTreeSizeTest` and `OptionRedirectTest` guard it. The two follow-up
  fixes (89cdaaff, 86e93411) are the expected cost and do not show a wrong abstraction. Keeping a non-redirect mode
  only for argument children is documented and necessary.
- `ChainedCommandContext`: this is the smallest way to read arguments across redirect parts while suggesting.
- The help, docs, tree and diagnostics outputs are all built from one `CommandTree` model, so there is no parallel
  traversal apart from C-01. `HelpFormatter`/`UsageFormatter`/`ErrorFormatter`/`CaptionSender` are single-method
  hooks with tests (`HelpErrorFormatterTest`).
- `Json.java` (106 lines, writer only): it avoids a JSON dependency in a library that gets shaded. That is fine.
- The annotation parser, `CommandBuilder` and the Kotlin DSL all configure `CommonCommand`. None of them
  re-implements execution. The 7 commands × 4 styles in `examples` back an equivalence check.
- Arity classes (Unary..Hept, Required*) and Octo/Nona functions: these follow the existing pattern. The range added
  only `executeAsync` to each.
- Samples' copies of gradlew and the wrapper: each sample must build on its own once copied out. Forge uses Gradle
  8.14.3 and the others 9.5.0.
- `CommonCommand.addPreprocess` now runs as a middleware, so it uses the same mechanism as middlewares.

## Coverage
Examined:
- common: AnnotationParser, OptionGraph (structure and commit history), CommandBuilder, the help, docs and tree
  classes, CommonCommandContext surface, Captions/CaptionSender, Json, Snapshots, branch/function arity
- platform `AnnotatedCommands`, `CommandLibDiagnosticsCommand`, `Command`, `BukkitCommandScheduler`
- the five testing modules (extension, sender, tester API)
- spigot and paper test duplication, the integration-test spigot/paper split
- nms codegen layout and the module table, a sample of module pairs
- benchmark baseline and tasks, samples/examples, build dependency declarations

Not examined in depth:
- the inner logic of OptionGraph's parse (`step/advance/parseValue`)
- CommandRunner and CommonCommand line by line
- modded's Stonecutter variants
- velocity/modded CommandTester internals
- the buildSrc BuildTools tasks (260 lines)
- mojang.py and spigot_legacy.py
- docs/ content and mkdocs config
- paper CommandLib (719 lines) and spigot CommandLib (968 lines) internals

No build or test was run.
