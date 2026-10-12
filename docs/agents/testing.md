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

Forge / NeoForge changes must compile every node, because code for nodes
other than the active one is only type-checked when that node compiles (see
`docs/agents/modded.md`):

```bash
./gradlew :modded:forge-1.16.5:compileJava :modded:forge-1.20.1:compileJava :modded:neoforge-1.21.1:compileJava
./gradlew :modded-testing:forge-1.16.5:test :modded-testing:forge-1.20.1:test :modded-testing:neoforge-1.21.1:test
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

Error Prone runs inside `compileJava` / `compileTestJava` of `common`, `spigot`, `paper` and their `-testing`
modules, so the compile checks above also run it. Its compiler is JDK 21 with `--release` set to the module's toolchain
version. Its warnings fail the build as well (`-Werror`). Fix what it reports rather than suppressing it; suppress with
`@SuppressWarnings("<CheckName>")` and a comment giving the reason only at a site that does it on purpose. The checks
disabled for the whole build are listed with their reasons in the root `build.gradle.kts`.

NullAway runs with Error Prone on the main sources of the same modules and treats code in `dev.marulabs.commandlib` as
non-null unless annotated `@Nullable` (`org.jetbrains.annotations.Nullable`), and fails the build on a possible null
dereference. Annotate a field, parameter or return value `@Nullable` when null is a legitimate value, and put the
annotation of a method on the line above it, as the existing code does.

The same modules publish a javadoc jar, and their `javadoc` task runs doclint without the `missing` group: broken
Javadoc (malformed HTML, a bad `@param` name, an unknown `{@link}`) fails it, an undocumented member does not. Check
with `./gradlew :common:javadoc :spigot:javadoc :paper:javadoc`.

`./gradlew :common:japicmp :spigot:japicmp` compares the public API with the JitPack release named in `apiBaselines`
in the root `build.gradle.kts` and writes `build/reports/japicmp/`. It lists binary-incompatible changes, and fails on
them when the project version has the baseline's major and minor version, since only a minor release may break the API
under 0.x versioning. It is not part of `check`. After a release, move the baselines to it and add the modules it
published for the first time.

[jev-lint](https://github.com/mizchi/jev-lint) asks a model whether a method body contradicts its name and whether a
comment contradicts the declaration under it (`.jev-lint.yaml`, rules in `.jev-lint/rules/java/`). It sends the judged
code to the TypeSafe API and needs `TYPESAFE_API_KEY` and Node 24+, so run it only when the user asks:
`npx -y -p node@24 -p jev-lint jev-lint review --base master`. `--dry-run` lists the subjects and the price without a
request. Its findings are candidates to judge, not errors, and the Java rules' cutoffs are not calibrated yet: each rule
has labelled cases in its `fixtures/` and `expect.yml`, and `jev-lint eval .jev-lint/rules/java/<rule> --repeat 3`
fits the cutoff on them, which also needs the key. Keep labels out of the fixture code, since the model reads it.
