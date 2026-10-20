# Typed NMS Modules

Read this before touching `nms/*`, `buildSrc`, `tools/nms-codegen`, or the NMS wrappers in the spigot module.

## Overview

The `spigot` module talks to NMS through abstract wrappers (`util/nms`). Their only implementations live in the
`nms/*` modules, which compile against real server jars, so a renamed NMS class or member fails the build instead of a
server. They follow CommandAPI's build layout:

| Module                                       | Server jar                                                 | Names in the built jar     | Used on                                 |
|----------------------------------------------|------------------------------------------------------------|----------------------------|-----------------------------------------|
| `nms:spigot-1.16.5`                          | `org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT` (BuildTools)    | Spigot                     | Spigot / Paper 1.16.4-1.16.5            |
| `nms:spigot-<version>` (1.17 to 1.20.4)      | same artifact, `remapped-mojang` classifier (BuildTools)   | Spigot (remapped)          | Spigot / Paper `<version>`              |
| `nms:spigot-<version>` (1.20.6 to 1.21.11)   | same artifact, `remapped-mojang` classifier (BuildTools)   | Spigot (remapped)          | Spigot, see the module table            |
| `nms:spigot-26.1.2`                          | `org.spigotmc:spigot:26.1.2-R0.1-SNAPSHOT` (BuildTools)    | Mojang                     | Spigot 26.1-26.3                        |
| `nms:paper-1.20.6`, `nms:paper-1.21.1`       | paperweight userdev dev bundle `<version>-R0.1-SNAPSHOT`   | Mojang, and Spigot (reobf) | Paper 1.20.5-1.20.6, 1.21-1.21.1        |

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
  Likewise `spigot-1.21.3` covers 1.21.2, `spigot-1.21.8` 1.21.7 and `spigot-1.21.10` 1.21.9.
  BuildTools now resolves those releases, and 1.20.3, 1.20, 26.1 and 26.1.1, to the following hotfix build, so a
  target or module builds the last build of the release by its Spigot build number
  (`commandlib.integration.buildToolsRevision`, `spigotNms.buildToolsRevision`).
- From 26.1 Spigot runs with Mojang names and keeps CraftBukkit in the unversioned `org.bukkit.craftbukkit` package,
  so `spigot-26.1.2` compiles against the server jar as it is, without remapping. Mojang names do not change between
  releases the way obfuscated ones do, so the module covers every 26.x release whose target passes.
- BuildTools installs the artifacts of 1.21.11 as `1.21.11-R0.2-SNAPSHOT`, and of every other version as
  `<version>-R0.1-SNAPSHOT` (`spigotArtifactVersion` in `buildSrc`).
- The supported range is 1.16.4 to 26.3. A server outside every module's versions has no NMS implementation, and
  the first wrapper it uses throws `NMSClassNotFoundException`.
- `spigot-1.17` has no integration target: no MCProtocolLib release speaks the 1.17 protocol (755), and the 1.17.1
  bot (756) is rejected by a 1.17 server. The module is only checked by its compilation against the 1.17 server jar.
- On Paper the spigot module ends at 1.21.1, the last release with a Paper module; later Paper releases use the
  `paper` module.
- From 1.20.5 Paper runs with Mojang names. The Paper modules use paperweight userdev. Paper still remaps a plugin
  from Spigot names to Mojang names when the plugin does not declare Mojang mappings in its manifest, and CommandLib
  is shaded into plugins of both kinds. Some Spigot names are Mojang names of other classes (Spigot `MobEffect` is
  Mojang `MobEffectInstance`), so Mojang-named classes break in a remapped plugin. A Paper module therefore ships its
  Mojang-named jar and its paperweight `reobfJar`, moved to the `<package>_spigot` package. Each package has a
  `MappingProbe`, and `NMSClassRegistry` uses the package whose probe matches. The Spigot modules from 1.20.5 have a
  probe that matches only on Spigot (the Spigot name `MobEffectList` up to 1.21.11, no Paper lifecycle API from
  26.1), so the spigot jar on a later Paper throws `NMSClassNotFoundException` instead of running Spigot code. On Paper
  after 1.21.1 the message names the paper artifact; the `paper-26.3-spigot-jar` integration target checks it.

The typed classes target Java 11 up to 1.16.5 and Java 16 after (`typedNmsRelease`), the oldest releases that
compile, while the toolchain follows the server jar. A plugin's shading tool reads every class of the spigot jar, so
newer class files would require a newer ASM there.

Every wrapper has a class in every module whose versions use it. The typed classes pass the server class they wrap
to `NMSClass` as a class literal, which every remapping step (SpecialSource, paperweight, Paper's and Mohist's plugin
remappers) translates along with the rest of the bytecode. Where the server keeps what a wrapper needs in a private
member, the module's class reads it by reflection, finding the member by its type rather than its obfuscated name:
`NMSArgumentTypeRegistrar` (the map of argument serializers, through the helpers of the wrapper),
`NMSDataPackResources` on Spigot 1.19 to 1.20.4 (the build context) and `NMSDedicatedServer` (the dispatcher of the
function library, through `setFunctionDispatcher`, and the function manager's boolean fields, through
`keepPendingLoad`). From 1.20.5 the build context is built from
public API (`CommandBuildContext.simple`), on Spigot and Paper alike.

## BuildTools

The `commandlib.spigot-nms` plugin in `buildSrc` adds `org.spigotmc:spigot` from the local Maven repository and an
`installSpigot` task that `compileJava` depends on. `installSpigot` runs BuildTools only when the local Maven repository
lacks the jars (and, for remapped modules, the `minecraft-server` mappings):

- BuildTools.jar and its working directory live in `<gradle user home>/caches/commandlib-buildtools`. Runs share that
  directory, so a build service serializes them within a build and a file lock (`buildtools.lock`) across Gradle
  processes. Processes with different Gradle user homes share the local Maven repository, so a run also locks
  `org/spigotmc/.commandlib-buildtools.lock` there, before the directory lock. The locks also cover checking the
  artifacts and copying a target's server jar, which BuildTools writes in place. BuildTools.jar is downloaded again
  before each run, and the cached jar is used when the download fails.
- The local Maven repository is found as Maven finds it: `-Dmaven.repo.local`, then `<localRepository>` in
  `~/.m2/settings.xml` or `$M2_HOME/conf/settings.xml`, then `~/.m2/repository`. BuildTools' own Maven finds the
  user settings and the default by itself unless the inherited `MAVEN_OPTS` names a repository; otherwise the
  repository goes through `MAVEN_OPTS`, which cannot carry a path with spaces outside Windows.
- Each run records the BuildTools revision (`revisions/spigot-<version>.txt` in the BuildTools directory). A module or
  integration target that changes the revision runs BuildTools again; artifacts installed before the record existed
  are taken to match.
- The Java toolchain follows the Minecraft version: 8 before 1.17, 16 for 1.17, 17 up to 1.20.4, 21 up to 1.21.11,
  and 25 from 26.1.
- A BuildTools run takes several minutes and needs network access. Later builds reuse the local Maven repository,
  and the BuildTools JDK is resolved only when BuildTools runs.
- `-Pcommandlib.buildToolsUrl=<url>` overrides the BuildTools download URL.
- To prepare a machine ahead of time, run `./gradlew installSpigot`, which runs the task in every module.

`-Pcommandlib.typedNms=false` leaves every `nms:*` project out of the build, so neither BuildTools nor paperweight
runs. The spigot jar then has no NMS implementations and does not work on a server, so this is only for work that
does not need the jar: `:spigot:test`, `spigot-testing` and the other modules. A comma-separated list, such as
`-Pcommandlib.typedNms=spigot-1.20.4,paper-1.20.6`, includes only those modules, for example to run the integration
targets of those versions while BuildTools has not run for the others. The spigot publish tasks fail while any module
is left out.

## Bundling And Lookup

- Each module exposes its final jar through the `nmsElements` configuration. `:spigot` bundles every `nms:*` project
  into its jar, and `:spigot:collectTypedNms` copies the same jars to `spigot/build/typed-nms` for the integration-test
  plugins, which inline the spigot sources instead of using the jar. The `spigot-testing` tests run against the
  classes of `:spigot` with mock implementations, so they do not build the modules.
- `:spigot` never references typed classes directly, because the modules depend on it. `TypedNmsModules` lists each
  module's package and the versions its server jar matches, and each module generates `<package>.TypedNmsClasses`,
  which lists its classes as class literals. `NMSClassRegistry` maps each listed class to the wrapper it extends, so
  the wrappers register nothing.
- Obfuscated names usually differ between releases, so a Spigot-remapped module covers its own release, plus the
  other releases of its CraftBukkit revision whose mappings remap it to the same classes and whose integration target
  passes `TypedNmsTest` (see Overview).
- `findClass` loads a module's classes only on a server inside its versions, because they may target a newer Java
  release than older servers run. Of the modules for the version, in table order, it takes the first one where:
  - the package has no `MappingProbe`, or its `matches()` returns true;
  - `TypedNmsClasses` lists a class extending the wrapper;
  - `TypedClassLinkage` finds that the class, and the classes it uses from its own package, link on the server.
    It links each class without initializing it, so HotSpot's verifier rejects a server class that is no longer a
    subclass of the class the bytecode assigns it to. The verifier treats interfaces as `Object`, and a JVM without
    verification checks no supertypes, so a server class that dropped an interface still fails at command time.
    It then resolves each class the instructions use, and each field and method reference through a
    `MethodHandles.Lookup` of the class with the kind of the instruction that uses it (instance or static, virtual,
    special or interface; a write also rejects a final field of another class). A lookup checks access and resolves
    only that member. Members of JDK classes are not resolved: the class's Java release has them, and from Java 17 a
    lookup cannot resolve caller-sensitive methods such as `Class.getDeclaredFields`. Servers that remap the plugin
    rewrite its `Lookup` calls to map member names as Spigot names, which Mohist's class files, read with Spigot names,
    need, and Paper's, already remapped, do not. So each lookup goes through a method handle, which no server
    rewrites, and on failure through the plain call. This keeps the Mojang-named Paper classes off Spigot and checks
    forks and hybrid servers.
  When no module qualifies, `findClass` throws `NMSClassNotFoundException` with the reason of each module.
- `findClass` caches the class it resolves for each wrapper and server version.
- Typed classes extend the abstract `NMS*` wrapper, so the spigot-testing mocks keep working.

## Adding A Module Or Implementation

1. For a new version, add `nms/<platform>-<version>/build.gradle.kts` like the existing modules; `settings.gradle.kts`
   includes every directory under `nms` with a build script. Use `spigotNms { remapped.set(true) }` from 1.17, with
   `buildToolsRevision` for a release that BuildTools resolves to a later build, and paperweight from 1.20.5. Set the
   Java toolchain the server jar needs.
2. Generate the classes with `tools/nms-codegen` (see its README): add the module, its versions and, for a Spigot
   module, its CraftBukkit package to `MODULES` in `generate.py`, then run `uv run generate.py <module>`. That also
   writes the module table to `TypedNmsModules`, and `settings.gradle.kts` fails for a module directory missing from
   it. Classes go to `dev.marulabs.commandlib.nms.<module>`, named `<NMSWrapper>_<module>` with dots replaced by
   underscores, such as `NMSArgumentPlayer_spigot_1_20_4`, and are listed in `TypedNmsClasses`. Where the Minecraft
   API changed, add a version flag in `mojang.py` rather than editing the generated classes, which a regeneration
   overwrites. A new wrapper needs a class in every module whose versions use it.
3. For a Paper module, give the module a `MappingProbe` like `nms/paper-1.20.6`; the table already lists its
   `<package>_spigot` package.
4. Verify with `./gradlew :nms:<module>:assemble :spigot:jar :spigot:test` and the matching
   `:integration-test:targets:<target>:minecraftIntegrationTest`.
5. Run `uv run generate.py --check` in `tools/nms-codegen`. It regenerates every module and `TypedNmsModules` into a
   temporary directory, lists each committed source that is changed, missing or no longer generated, and exits with
   status 1 when there is any, so it catches generated classes edited by hand or scripts changed without regenerating.

The Spigot suite's `TypedNmsTest` resolves every class of the modules for the server version
(`NMSClassRegistry.unavailableClasses()`), so a class that does not link fails there even when no other test uses its
wrapper.
