# Typed NMS Modules

Read this before touching `nms/*`, `buildSrc`, or the typed registrations in `NMSClassRegistry`.

## Overview

The `spigot` module talks to NMS through reflection (`util/nms`). The `nms/*` modules add implementations that
compile against real server jars, so a renamed NMS class or member fails the build instead of a server. They follow
CommandAPI's build layout:

| Module               | Server jar                                              | Names in the built jar | Used on                     |
|----------------------|---------------------------------------------------------|------------------------|-----------------------------|
| `nms:spigot-1.16.5`  | `org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT` (BuildTools) | Spigot                 | Spigot / Paper 1.16.4-1.16.5 |
| `nms:spigot-1.20.4`  | same, `remapped-mojang` classifier (BuildTools)         | Spigot (remapped)      | Spigot / Paper 1.20.3-1.20.4 |
| `nms:paper-1.20.6`   | paperweight userdev dev bundle `1.20.6-R0.1-SNAPSHOT`   | Mojang                 | Paper 1.20.5-1.20.6         |

- Up to 1.16.5 the Spigot-mapped server jar names members readably, so the module compiles against it directly.
- From 1.17 Spigot keeps obfuscated member names. The module compiles against the Mojang-mapped jar and
  `SpigotRemapTask` remaps the built jar with SpecialSource: Mojang to obfuscated (`maps-mojang`, reversed), then
  obfuscated to Spigot (`maps-spigot`).
- From 1.20.5 Paper runs with Mojang names, which is what the reflection implementations for 1.20.5+ already assume.
  Those modules use paperweight userdev and need no remapping.

## BuildTools

The `commandlib.spigot-nms` plugin in `buildSrc` adds `org.spigotmc:spigot` from the local Maven repository and an
`installSpigot` task that `compileJava` depends on. `installSpigot` runs BuildTools only when the local Maven repository
lacks the jars (and, for remapped modules, the `minecraft-server` mappings):

- BuildTools.jar and its working directory live in `<gradle user home>/caches/commandlib-buildtools`. Runs are
  serialized because they share that directory.
- The Java toolchain follows the Minecraft version: 8 before 1.17, 16 for 1.17, 17 up to 1.20.4, and 21 after.
- A BuildTools run takes several minutes and needs network access. Later builds reuse `~/.m2`.
- `-Pcommandlib.buildToolsUrl=<url>` overrides the BuildTools download URL.
- To prepare a machine ahead of time, run `./gradlew :nms:spigot-1.16.5:installSpigot :nms:spigot-1.20.4:installSpigot`.

`-Pcommandlib.typedNms=false` leaves every `nms:*` project out of the build. The spigot jar then contains only the
reflection implementations, which is enough for work that does not touch NMS.

## Bundling And Lookup

- Each module exposes its final jar through the `nmsElements` configuration. `:spigot` bundles every `nms:*` project
  into its jar, and `:spigot:collectTypedNms` copies the same jars to `spigot/build/typed-nms` for the integration-test
  plugins, which inline the spigot sources instead of using the jar.
- `:spigot` never references typed classes directly. Register them by class name next to the reflection registrations:

  ```java
  NMSClassRegistry.registerTyped(NMSArgumentPlayer.class,
                                 "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentPlayer_spigot_1_20_4",
                                 "1.20.3",
                                 "1.20.4");
  ```

  `findClass` loads the class only on a server inside the range, because the class may target a newer Java release
  than older servers run. It prefers the typed class and falls back to the reflection implementation when the class
  is missing, as in `:spigot:test` and `spigot-testing`.
- Register a typed class only for the versions its server jar matches. Obfuscated names differ between releases, so a
  Spigot-remapped module covers one CraftBukkit revision (for example `v1_20_R3` is 1.20.3-1.20.4).
- Typed classes extend the abstract `NMS*` wrapper, so the reflection implementations and mocks keep working.

## Adding A Module Or Implementation

1. For a new version, add `nms/<platform>-<version>/build.gradle.kts` like the existing modules and add the name to
   `typedNmsModules` in `settings.gradle.kts`. Use `spigotNms { remapped.set(true) }` from 1.17 and paperweight from
   1.20.5. Set the Java toolchain the server jar needs.
2. Put classes in `net.kunmc.lab.commandlib.nms.<module>`, named `<NMSWrapper>_<module>` with dots replaced by
   underscores, such as `NMSArgumentPlayer_spigot_1_20_4`.
3. Register the class with `registerTyped` in the wrapper's static block.
4. Verify with `./gradlew :nms:<module>:assemble :spigot:jar :spigot:test` and, when the behavior needs a server, the
   matching `:integration-test:targets:<target>:minecraftIntegrationTest`.

Keep the reflection implementation for a version until the typed one is covered by the integration test for it.
