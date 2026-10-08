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
| `nms:paper-1.20.6`                         | paperweight userdev dev bundle `1.20.6-R0.1-SNAPSHOT`        | Mojang, and Spigot (reobf)  | Paper 1.20.5-1.20.6          |

- Up to 1.16.5 the Spigot-mapped server jar names members readably, so the module compiles against it directly.
- From 1.17 Spigot keeps obfuscated member names. The module compiles against the Mojang-mapped jar and
  `SpigotRemapTask` remaps the built jar with SpecialSource: Mojang to obfuscated (`maps-mojang`, reversed), then
  obfuscated to Spigot (`maps-spigot`). For 1.17 a third step applies `maps-spigot-members`, because Spigot still
  renamed methods then (fields keep obfuscated names on the server).
- From 1.20.5 Paper runs with Mojang names, which is what the reflection implementations for 1.20.5+ already assume.
  Those modules use paperweight userdev. Paper still remaps a plugin from Spigot names to Mojang names when the plugin
  does not declare Mojang mappings in its manifest, and CommandLib is shaded into plugins of both kinds. Some Spigot
  names are Mojang names of other classes (Spigot `MobEffect` is Mojang `MobEffectInstance`), so Mojang-named classes
  break in a remapped plugin. A Paper module therefore ships its Mojang-named jar and its paperweight `reobfJar`,
  moved to the `<package>_spigot` package. Each package has a `MappingProbe`, and `NMSClassRegistry` uses the package
  whose probe matches.

Typed classes cover every wrapper that calls NMS members. Wrappers that only hold a handle (`NMSItemStack`,
`NMSEnchantment`, `NMSParticle`, ...) and `NMSArgumentTypeRegistrar`, which edits private registry maps, stay on
reflection. On Spigot, `NMSDataPackResources` also stays on reflection because the build context field is private.

## BuildTools

The `commandlib.spigot-nms` plugin in `buildSrc` adds `org.spigotmc:spigot` from the local Maven repository and an
`installSpigot` task that `compileJava` depends on. `installSpigot` runs BuildTools only when the local Maven repository
lacks the jars (and, for remapped modules, the `minecraft-server` mappings):

- BuildTools.jar and its working directory live in `<gradle user home>/caches/commandlib-buildtools`. Runs are
  serialized because they share that directory. BuildTools.jar is downloaded again before each run, and the cached
  jar is used when the download fails.
- The local Maven repository is found as Maven finds it: `-Dmaven.repo.local`, then `<localRepository>` in
  `~/.m2/settings.xml` or `$M2_HOME/conf/settings.xml`, then `~/.m2/repository`. BuildTools gets it through
  `MAVEN_OPTS`.
- The Java toolchain follows the Minecraft version: 8 before 1.17, 16 for 1.17, 17 up to 1.20.4, and 21 after.
- A BuildTools run takes several minutes and needs network access. Later builds reuse the local Maven repository,
  and the BuildTools JDK is resolved only when BuildTools runs.
- `-Pcommandlib.buildToolsUrl=<url>` overrides the BuildTools download URL.
- To prepare a machine ahead of time, run `./gradlew installSpigot`, which runs the task in every module.

`-Pcommandlib.typedNms=false` leaves every `nms:*` project out of the build. The spigot jar then contains only the
reflection implementations, which is enough for work that does not touch NMS.

## Bundling And Lookup

- Each module exposes its final jar through the `nmsElements` configuration. `:spigot` bundles every `nms:*` project
  into its jar, and `:spigot:collectTypedNms` copies the same jars to `spigot/build/typed-nms` for the integration-test
  plugins, which inline the spigot sources instead of using the jar.
- `:spigot` never references typed classes directly. `NMSClassRegistry.TYPED_MODULES` lists each module's package
  and the versions its server jar matches, and the wrapper registers its typed classes next to the reflection
  registrations:

  ```java
  NMSClassRegistry.registerTyped(NMSArgumentPlayer.class);
  ```

  That registers `net.kunmc.lab.commandlib.nms.<module>.NMSArgumentPlayer_<module>` for every module. Pass a version
  range to leave some versions on reflection, such as `registerTyped(NMSDataPackResources.class, "1.20.6", "9.9.9")`.
- Obfuscated names differ between releases, so a Spigot-remapped module covers its own release only. Paper modules
  keep Mojang names and cover the releases their reflection counterparts cover.
- `findClass` loads the class only on a server inside the range, because the class may target a newer Java release
  than older servers run. It prefers the typed class and falls back to the reflection implementation when:
  - the class is missing, as in `:spigot:test` and `spigot-testing`, or for a wrapper the module does not implement;
  - its package has a `MappingProbe` whose `matches()` returns false or throws a `LinkageError`;
  - `TypedClassLinkage` finds a class, field or method in its constant pool, or in the classes it uses from its own
    package, that the server lacks. This covers servers that differ from the compiled jar, such as Spigot 1.20.5-1.20.6
    for the Mojang-named Paper classes, forks, and hybrid servers.
- Typed classes extend the abstract `NMS*` wrapper, so the reflection implementations and mocks keep working.

## Adding A Module Or Implementation

1. For a new version, add `nms/<platform>-<version>/build.gradle.kts` like the existing modules and add the name to
   `typedNmsModules` in `settings.gradle.kts`. Use `spigotNms { remapped.set(true) }` from 1.17 and paperweight from
   1.20.5. Set the Java toolchain the server jar needs.
2. Generate the classes with `tools/nms-codegen` (see its README): add the module to `MODULES` in `generate.py`, and
   for a Mojang-named Spigot module its CraftBukkit package to `SPIGOT_TARGETS` in `mojang.py`, then run
   `uv run generate.py <module>`. Classes go to `net.kunmc.lab.commandlib.nms.<module>`, named `<NMSWrapper>_<module>`
   with dots replaced by underscores, such as `NMSArgumentPlayer_spigot_1_20_4`. Where the Minecraft API changed,
   add a version flag in `mojang.py` rather than editing the generated classes, which a regeneration overwrites.
3. Add the module to `TYPED_MODULES` in `NMSClassRegistry`. For a Paper module, also add the `<package>_spigot`
   package and give the module a `MappingProbe` like `nms/paper-1.20.6`. A new wrapper calls `registerTyped` in its
   static block.
4. Verify with `./gradlew :nms:<module>:assemble :spigot:jar :spigot:test` and, when the behavior needs a server, the
   matching `:integration-test:targets:<target>:minecraftIntegrationTest`.

Keep the reflection implementation for a version until the typed one is covered by the integration test for it.
