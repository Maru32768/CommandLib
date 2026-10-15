# Testing Notes

Use focused Gradle tasks for the modules affected by the change.

Common checks:

```bash
./gradlew :common:test
./gradlew :spigot:test
./gradlew :spigot-testing:test
```

Compile checks:

```bash
./gradlew :common:compileJava
./gradlew :spigot:compileJava
./gradlew :spigot-testing:compileJava
```

Typed NMS modules run BuildTools on the first build of each Spigot version and
set up the paperweight dev bundle (see `docs/agents/nms-build.md`):

```bash
./gradlew :nms:spigot-1.16.5:assemble :nms:spigot-1.20.4:assemble :nms:paper-1.20.6:assemble
```

Pass `-Pcommandlib.typedNms=false` to leave them out when the change does not
need the spigot jar, such as `:spigot:test` or `spigot-testing`. The jar then
has no NMS implementations, so it does not work on a server.

Forge / NeoForge / Fabric changes must compile every node, because code for
nodes other than the active one is only type-checked when that node compiles
(see `docs/agents/modded.md`):

```bash
./gradlew :modded:forge-1.16.5:compileJava :modded:forge-1.20.1:compileJava :modded:neoforge-1.21.1:compileJava :modded:fabric-1.20.1:compileJava :modded:fabric-1.21.1:compileJava
./gradlew :modded-testing:forge-1.16.5:test :modded-testing:forge-1.20.1:test :modded-testing:neoforge-1.21.1:test :modded-testing:fabric-1.20.1:test :modded-testing:fabric-1.21.1:test
```

The samples under `samples/` are separate builds, so the root build does not
compile them. Build them after a public API change, one at a time, since
parallel Loom builds share a cache:

```bash
# Spigot and Paper include the repository as a composite build. The Spigot
# sample builds the typed NMS modules it bundles.
(cd samples/spigot && ./gradlew build)
(cd samples/paper && ./gradlew build)
# ForgeGradle 6 does not run on Gradle 9, so the Forge sample reads mavenLocal.
./gradlew -Pcommandlib.typedNms=false :common:publishToMavenLocal :modded:forge-1.16.5:publishToMavenLocal
(cd samples/forge && ./gradlew build -PcommandlibLocal=true)
```

Integration tests may require Docker or a local Minecraft server environment.
Run them only when the touched behavior requires it or the user asks for it:

```bash
./gradlew :integration-test:minecraftIntegrationTest
```

When adding or fixing command behavior, prefer narrow tests around parsing,
execution, options, permissions, and suggestions. Use `spigot-testing` utilities
for command-level Bukkit tests when a full server is not needed.

## Static Analysis

Error Prone runs inside `compileJava` / `compileTestJava` of the published modules (`common`, `spigot`, `paper`,
the `modded` nodes and their `-testing` modules) and of the typed NMS modules, so the compile checks above also run it.
Its compiler is JDK 21 (JDK 25 for a module whose toolchain is 25) with `--release` set to the module's toolchain
version, or for a typed NMS module to its `typedNmsRelease`. Its warnings fail the build as well (`-Werror`). Fix what
it reports rather than suppressing it; suppress with `@SuppressWarnings("<CheckName>")` and a comment giving the reason
only at a site that does it on purpose. The checks disabled for the whole build are listed with their reasons in the
root `build.gradle.kts`. A Stonecutter node checks only the code of its own version, so compile every modded node. Fix a
finding in the typed NMS modules in `tools/nms-codegen` and regenerate.

NullAway runs with Error Prone on the main sources of the same modules and treats code in `dev.marulabs.commandlib` as
non-null unless annotated `@Nullable` (`org.jetbrains.annotations.Nullable`), and fails the build on a possible null
dereference. Annotate a field, parameter or return value `@Nullable` when null is a legitimate value, and put the
annotation of a method on the line above it, as the existing code does.

The published modules, including the modded nodes, publish a javadoc jar, and their `javadoc` task runs doclint
without the `missing` group: broken Javadoc (malformed HTML, a bad `@param` name, an unknown `{@link}`) fails it, an
undocumented member does not. Check with `./gradlew :common:javadoc :spigot:javadoc :paper:javadoc` and, for the modded
nodes, `./gradlew :modded:forge-1.16.5:javadoc :modded:forge-1.20.1:javadoc :modded:neoforge-1.21.1:javadoc
:modded:fabric-1.20.1:javadoc :modded:fabric-1.21.1:javadoc`.

`./gradlew :common:japicmp :spigot:japicmp` compares the public API with the JitPack release named in `apiBaselines`
in the root `build.gradle.kts` and writes `build/reports/japicmp/`. It lists binary-incompatible changes, and fails on
them when the project version has the baseline's major and minor version, since only a minor release may break the API
under 0.x versioning. It is not part of `check`. After a release, move the baselines to it and add the modules it
published for the first time.

[jev-lint](https://github.com/mizchi/jev-lint) asks a model whether a method body contradicts its name and whether a
comment contradicts the declaration under it (`.jev-lint.yaml`, rules in `.jev-lint/rules/java/`). It sends the judged
code to the TypeSafe API, so run it only when the user asks. `mise.toml` pins Node and jev-lint (`mise install`), and
the key goes in `mise.local.toml`, which Git ignores, as `[env]` `TYPESAFE_API_KEY = "..."`. Run it through mise, since
a non-interactive shell may not have mise activated: `mise run jev-review [base]` judges the lines changed since `base`,
by default `origin/master` for the commits not pushed yet, and `mise exec -- jev-lint check --dry-run` lists the
subjects and the price without a request. Its findings are candidates to judge, not errors. Each Java rule's cutoff is
fitted on the labelled cases in its `fixtures/` and `expect.yml`, with the reasoning in its `rule.yml` and the accepted
run in `baseline.json`; after changing a rule or its cases, refit with `mise exec -- jev-lint eval
.jev-lint/rules/java/<rule> --repeat 3` and accept with `--accept`, which needs the key. On Windows, `eval --replay` and
`--accept-last` mis-score these suites, because jev-lint compares a baseline's `/` paths with the `\` paths of the
labels; a fresh `eval` scores them correctly. Keep labels out of the fixture code, since the model reads it.
