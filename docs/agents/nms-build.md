# Typed NMS Modules

Read this before touching `nms/*`, `buildSrc`, or the typed registrations in `NMSClassRegistry`.

## Overview

The `spigot` module talks to NMS through reflection (`util/nms`). The `nms/*` modules add implementations that
compile against real server jars, so a renamed NMS class or member fails the build instead of a server. They follow
CommandAPI's build layout:

| Module                                     | Server jar                                                   | Names in the built jar      | Used on                      |
|--------------------------------------------|--------------------------------------------------------------|-----------------------------|------------------------------|
| `nms:spigot-1.16.5`                        | `org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT` (BuildTools)      | Spigot                      | Spigot / Paper 1.16.4-1.16.5 |
| `nms:spigot-<version>` (1.17.1 to 1.20.4)  | same artifact, `remapped-mojang` classifier (BuildTools)     | Spigot (remapped)           | Spigot / Paper `<version>`   |
| `nms:spigot-1.20.6`, `nms:spigot-1.21.1`   | same artifact, `remapped-mojang` classifier (BuildTools)     | Spigot (remapped)           | Spigot 1.20.5-1.20.6, 1.21-1.21.1 |
| `nms:paper-1.20.6`                         | paperweight userdev dev bundle `1.20.6-R0.1-SNAPSHOT`        | Mojang, and Spigot (reobf)  | Paper 1.20.5-1.20.6          |

- Up to 1.16.5 the Spigot-mapped server jar names members readably, so the module compiles against it directly.
- From 1.17 Spigot keeps obfuscated member names. The module compiles against the Mojang-mapped jar and
  `SpigotRemapTask` remaps the built jar with SpecialSource: Mojang to obfuscated (`maps-mojang`, reversed), then
  obfuscated to Spigot (`maps-spigot`). For 1.17 a third step applies `maps-spigot-members`, because Spigot still
  renamed methods then (fields keep obfuscated names on the server).
- The module table in `tools/nms-codegen/generate.py` has the exact ranges; the generator writes it to
  `TypedNmsModules` in the spigot module. A module can also cover another release of its CraftBukkit revision when
  its classes work there and an integration target shows `TypedNmsTest` passing: `spigot-1.20.1` covers 1.20 (the
  `paper-1.20` target runs the Spigot names of 1.20), and `spigot-1.20.6` and `spigot-1.21.1` cover 1.20.5 and 1.21,
  whose mappings remap the modules' Mojang-named jars to the same classes (targets `spigot-1.20.5` and `spigot-1.21`).
  BuildTools now resolves those releases to the following hotfix build, so a target builds the last build of the
  release by its Spigot build number (`commandlib.integration.buildToolsRevision`). Spigot 1.20.3 has no module and
  uses the reflection implementations.
- Spigot support ends at 1.21.1. From 1.21.3 Bukkit's `Attribute`, `Biome` and `Sound` are registry interfaces, which
  the spigot module's enum-based arguments do not support (see `docs/roadmap.md`).
- From 1.20.5 Paper runs with Mojang names, which is what the reflection implementations for 1.20.5+ already assume.
  Those modules use paperweight userdev. Paper still remaps a plugin from Spigot names to Mojang names when the plugin
  does not declare Mojang mappings in its manifest, and CommandLib is shaded into plugins of both kinds. Some Spigot
  names are Mojang names of other classes (Spigot `MobEffect` is Mojang `MobEffectInstance`), so Mojang-named classes
  break in a remapped plugin. A Paper module therefore ships its Mojang-named jar and its paperweight `reobfJar`,
  moved to the `<package>_spigot` package. Each package has a `MappingProbe`, and `NMSClassRegistry` uses the package
  whose probe matches.

The typed classes target Java 11 up to 1.16.5 and Java 16 after (`typedNmsRelease`), the oldest releases that
compile, while the toolchain follows the server jar. A plugin's shading tool reads every class of the spigot jar, so
newer class files would require a newer ASM there.

Typed classes cover every wrapper that calls NMS members. Wrappers that only hold a handle (`NMSItemStack`,
`NMSEnchantment`, `NMSParticle`, ...) and `NMSArgumentTypeRegistrar`, which edits private registry maps, stay on
reflection. On Spigot up to 1.20.4, `NMSDataPackResources` also stays on reflection because the build context field is
private.

The reflection implementations for 1.20.5 and later assume Paper's Mojang names, so on Spigot 1.20.5+ the typed
modules are the only working path. Those modules also cover `NMSDataPackResources` (built from public API) and the
handle wrappers whose Spigot class names differ (`NMSMobEffectList`, `NMSIBlockData`, `NMSParticle`). Their
`MappingProbe` matches only on Spigot: Paper shares the version range and its remapper translates Spigot names in
bytecode, but not the class names these wrappers look up by string.

## BuildTools

The `commandlib.spigot-nms` plugin in `buildSrc` adds `org.spigotmc:spigot` from the local Maven repository and an
`installSpigot` task that `compileJava` depends on. `installSpigot` runs BuildTools only when the local Maven repository
lacks the jars (and, for remapped modules, the `minecraft-server` mappings):

- BuildTools.jar and its working directory live in `<gradle user home>/caches/commandlib-buildtools`. Runs share that
  directory, so a build service serializes them within a build and a file lock (`buildtools.lock`) across Gradle
  processes. The lock also covers checking the artifacts and copying a target's server jar, which BuildTools writes
  in place. BuildTools.jar is downloaded again before each run, and the cached jar is used when the download fails.
- The local Maven repository is found as Maven finds it: `-Dmaven.repo.local`, then `<localRepository>` in
  `~/.m2/settings.xml` or `$M2_HOME/conf/settings.xml`, then `~/.m2/repository`. BuildTools' own Maven finds the
  user settings and the default by itself unless the inherited `MAVEN_OPTS` names a repository; otherwise the
  repository goes through `MAVEN_OPTS`, which cannot carry a path with spaces outside Windows.
- Each run records the BuildTools revision (`revisions/spigot-<version>.txt` in the BuildTools directory). An
  integration target that changes `commandlib.integration.buildToolsRevision` runs BuildTools again; artifacts
  installed before the record existed are taken to match.
- The Java toolchain follows the Minecraft version: 8 before 1.17, 16 for 1.17, 17 up to 1.20.4, and 21 after.
- A BuildTools run takes several minutes and needs network access. Later builds reuse the local Maven repository,
  and the BuildTools JDK is resolved only when BuildTools runs.
- `-Pcommandlib.buildToolsUrl=<url>` overrides the BuildTools download URL.
- To prepare a machine ahead of time, run `./gradlew installSpigot`, which runs the task in every module.

`-Pcommandlib.typedNms=false` leaves every `nms:*` project out of the build. The spigot jar then contains only the
reflection implementations, which is enough for work that does not touch NMS. A comma-separated list, such as
`-Pcommandlib.typedNms=spigot-1.20.4,paper-1.20.6`, includes only those modules, for example while BuildTools has not
run for the others.

## Bundling And Lookup

- Each module exposes its final jar through the `nmsElements` configuration. `:spigot` bundles every `nms:*` project
  into its jar, and `:spigot:collectTypedNms` copies the same jars to `spigot/build/typed-nms` for the integration-test
  plugins, which inline the spigot sources instead of using the jar. The `spigot-testing` tests run against the
  classes of `:spigot`, so they do not build the modules.
- `:spigot` never references typed classes directly. `TypedNmsModules` lists each module's package and the versions
  its server jar matches, and the wrapper registers its typed classes next to the reflection registrations:

  ```java
  NMSClassRegistry.registerTyped(NMSArgumentPlayer.class);
  ```

  That registers `net.kunmc.lab.commandlib.nms.<module>.NMSArgumentPlayer_<module>` for every module, for the
  module's versions. When one module's class needs a later release than the module's lower version, narrow it in the
  module table (`wrapper_versions`): the Paper module's `NMSDataPackResources` uses `PaperCommands`, which Paper
  1.20.5 lacks, so it runs on 1.20.6 only.
- Obfuscated names usually differ between releases, so a Spigot-remapped module covers its own release, plus the
  other releases of its CraftBukkit revision whose mappings remap it to the same classes and whose integration target
  passes `TypedNmsTest` (see Overview). Paper modules keep Mojang names and cover the releases their reflection
  counterparts cover.
- `findClass` loads the class only on a server inside the range, because the class may target a newer Java release
  than older servers run. It prefers the typed class and falls back to the reflection implementation when:
  - the class is missing, as in `:spigot:test` and `spigot-testing`, or for a wrapper the module does not implement;
  - its package has a `MappingProbe` whose `matches()` returns false or throws a `LinkageError`;
  - `TypedClassLinkage` finds that the class, or a class it uses from its own package, does not link on the server.
    It links each class without initializing it, so HotSpot's verifier rejects a server class that is no longer a
    subclass of the class the bytecode assigns it to. The verifier treats interfaces as `Object`, and a JVM without
    verification checks no supertypes, so a server class that dropped an interface still fails at command time.
    It then resolves each class the instructions use, and each field and method reference through a
    `MethodHandles.Lookup` of the class with the kind of the instruction that uses it (instance or static, virtual,
    special or interface; a write also rejects a final field of another class). A lookup checks access and resolves
    only that member. Servers that remap the plugin rewrite its `Lookup` calls to map member names as Spigot names,
    which Mohist's class files, read with Spigot names, need, and Paper's, already remapped, do not. So each lookup
    goes through a method handle, which no server rewrites, and on failure through the plain call. This covers servers
    that differ from the compiled jar, such as Spigot 1.20.5-1.20.6 for the Mojang-named Paper classes, forks, and
    hybrid servers.
- `findClass` caches the class it resolves for each look-up class and server version.
- Typed classes extend the abstract `NMS*` wrapper, so the reflection implementations and mocks keep working.

## Adding A Module Or Implementation

1. For a new version, add `nms/<platform>-<version>/build.gradle.kts` like the existing modules; `settings.gradle.kts`
   includes every directory under `nms` with a build script. Use `spigotNms { remapped.set(true) }` from 1.17 and
   paperweight from 1.20.5. Set the Java toolchain the server jar needs.
2. Generate the classes with `tools/nms-codegen` (see its README): add the module, its versions and, for a Spigot
   module, its CraftBukkit package to `MODULES` in `generate.py`, then run `uv run generate.py <module>`. That also
   writes the module table to `TypedNmsModules`, and `settings.gradle.kts` fails for a module directory missing from
   it. Classes go to `net.kunmc.lab.commandlib.nms.<module>`, named `<NMSWrapper>_<module>`
   with dots replaced by underscores, such as `NMSArgumentPlayer_spigot_1_20_4`. Where the Minecraft API changed,
   add a version flag in `mojang.py` rather than editing the generated classes, which a regeneration overwrites.
3. For a Paper module, give the module a `MappingProbe` like `nms/paper-1.20.6`; the table already lists its
   `<package>_spigot` package. A new wrapper calls `registerTyped` in its static block.
4. Verify with `./gradlew :nms:<module>:assemble :spigot:jar :spigot:test` and, when the behavior needs a server, the
   matching `:integration-test:targets:<target>:minecraftIntegrationTest`.

Keep the reflection implementation for a version until the typed one is covered by the integration test for it. The
Spigot suite's `TypedNmsTest` fails when a wrapper has a typed class for the server version, in a package whose probe
matches, but `findClass` falls back to reflection (`NMSClassRegistry.typedFallbacks()`), so a silently rejected typed class
does not pass unnoticed. A typed class that fails to load, such as one built for a newer Java release than the server
runs, counts too. Only wrappers initialized so far are checked, so the test also reads the command source's location.
