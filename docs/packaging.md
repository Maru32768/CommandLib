# Packaging Cadence in a Plugin or Mod

Cadence is a library, not a plugin or a mod that server owners install. Every plugin or mod that uses it ships its
own copy, so two of them can run different Cadence versions side by side. This page explains how to ship that copy
on each platform and why. The [sample projects](../samples) build with the setup below.

## Recommendation

| Platform | Ship Cadence with | Why |
|---|---|---|
| Spigot, Paper, Folia | Shadow (or Maven Shade) + relocate | Bukkit has no library mechanism for Cadence, and plugin class loaders also find classes in other plugins, so two plugins with the same unrelocated packages can run against each other's copy. |
| Velocity | Shadow + relocate | Same as Bukkit: Velocity's plugin class loaders also look up classes in other plugins. |
| Forge 1.16.5 | Shadow + relocate, then `reobfShadowJar` | Forge 1.16.5 has no Jar-in-Jar. |
| Forge 1.18.2, 1.19.2 and 1.20.1, NeoForge | Shadow + relocate | Mods are Java modules there, and two mods that contain the same package fail to load, so relocation is required. The `forge-*` / `neoforge-*` jars are plain libraries (no `mods.toml`, no `FMLModType` manifest entry) and are not tested as Jar-in-Jar entries. See [version conflicts](#jar-in-jar-and-version-conflicts). |
| Fabric | Shadow + relocate, then `remapJar` | `include()` works (Loom generates a `fabric.mod.json` for the nested jar), but only the newest nested Cadence is loaded. See [version conflicts](#jar-in-jar-and-version-conflicts). |

In short: **shade and relocate on every platform.** Cadence is designed for it: several relocated copies in one
server are expected. They share data pack function compilation through system properties whose names relocation
leaves alone, and on Forge, NeoForge and Fabric each copy registers its own argument type class.

## Jar-in-Jar and version conflicts

Forge and NeoForge's Jar-in-Jar (`jarJar`) and Fabric's `include()` nest a library jar inside the mod jar instead of
copying its classes. When several mods nest the same library, the loader picks **one** version, the newest, and every
mod runs against it. A mod compiled against an older Cadence then runs on a newer one, and a method or class that
changed between them fails at runtime with `NoSuchMethodError`, `NoClassDefFoundError` or `AbstractMethodError`,
often only when a command runs.

Cadence is still 0.x. A minor release (0.17 to 0.18) may break the binary API; only patch releases keep it. Until
1.0 promises binary compatibility, the version picked under Jar-in-Jar can be one your mod was not built for.
A relocated copy cannot be replaced by another mod's: each mod runs the version it was built and tested with.

If you still use `include()` on Fabric, your `fabric.mod.json` cannot pin the nested Cadence well. Cadence's
jars are not mods, so the only ids to refer to are the ones Loom generates for the nested jars, one for the platform
jar and one for `common`. Loom builds an id from the jar's group, name and classifier, replacing `.` with `_` and
converting the result to lower case, such as `com_github_maru32768_cadence_fabric-1_21_1` and
`com_github_maru32768_cadence_common`. A `depends` on both ids pins the version in your built jar, but the ids exist
only there: Loom's development runs load Cadence as a library, so the `depends` stops them with a missing-mod error.
A `breaks` does not fail at startup: Fabric Loader avoids the listed versions by loading another nested copy, or none,
without a message. Shade Cadence if your mod must run the version it was built with.

## What to shade and relocate

Relocate the one package `dev.marulabs.cadence` to a package of your own, such as
`com.example.myplugin.libs.cadence`. It covers every Cadence artifact: `common`, the platform artifact, the
typed NMS classes bundled in the `spigot` jar, the annotation API, the Kotlin DSL, and the implementation under
`dev.marulabs.cadence.internal`.

```kotlin
tasks.named<ShadowJar>("shadowJar") {
    relocate("dev.marulabs.cadence", "com.example.myplugin.libs.cadence")
}
```

- **Shade only Cadence.** Brigadier, the platform APIs and JetBrains annotations are compile-only dependencies of
  Cadence and not in its runtime dependencies, so Shadow does not pick them up (the Kotlin modules add the Kotlin
  standard library, see below). Do not add Brigadier as an `implementation` dependency or
  relocate `com.mojang.brigadier`: Cadence builds the server's own command nodes, so it must use the Brigadier the
  server provides (on Velocity, Velocity's fork). Paper and Velocity provide Adventure as well.
- **Do not use Shadow's `minimize()`.** The `spigot` jar loads the typed NMS class for the running server by name, so
  `minimize()` drops classes that are needed at runtime. Relocation is fine: it rewrites those names too.
- **Kotlin DSL.** `common-kotlin` reads the current command context through an internal class of `common`, so relocate
  both under the same package, which the one `relocate` above does. Ship the Kotlin standard library as any Kotlin plugin does (shade it unrelocated, or on
  Spigot 1.17+ and Paper list it under `libraries` in `plugin.yml`), and `kotlinx-coroutines-core` if you call the
  coroutine functions.
- **Check the result** with the [diagnostics command](diagnostics.md#diagnostics-command): its `Cadence package`
  line shows the relocated package, and `jar tf` should list no `dev/marulabs/cadence` entries.

## Artifacts

Cadence is published on [JitPack](https://jitpack.io/#Maru32768/Cadence) today:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.Cadence:<artifact>:<version>")
}
```

Use a release tag from the [releases page](https://github.com/Maru32768/Cadence/releases) as the version.
`latest.release` resolves to whatever is newest at build time, so a later build can pick up a minor release that
breaks your code.

| Artifact | Platform |
|---|---|
| `spigot` | Spigot 1.16.5+, and Paper up to 1.21.1 (also Mohist); newer Paper needs `paper` |
| `paper` | Paper 1.21+ and Folia |
| `velocity` | Velocity 3.4+ |
| `spigot-kotlin`, `paper-kotlin`, `velocity-kotlin` | Kotlin DSL; brings the platform artifact and `common-kotlin` |
| `forge-1.16.5`, `forge-1.18.2`, `forge-1.19.2`, `forge-1.20.1` | Forge |
| `neoforge-1.21.1`, `neoforge-1.21.4` | NeoForge |
| `fabric-1.20.1`, `fabric-1.21.1`, `fabric-1.21.4` | Fabric (needs Fabric API) |
| `common-testing`, `spigot-testing`, `paper-testing`, `velocity-testing`, `forge-testing-<version>`, `neoforge-testing-<version>`, `fabric-testing-<version>` | Test utilities, `testImplementation` only; never shade them |

Every platform artifact depends on `common`, which Gradle adds transitively. Publishing to Maven Central is planned;
the group and the repository will change then, and this page will list the new coordinates.

## Spigot and Paper

Gradle with [Shadow](https://gradleup.com/shadow/):

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("com.github.Maru32768.Cadence:paper:<version>")
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("dev.marulabs.cadence", "com.example.myplugin.libs.cadence")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

Maven with the Shade plugin:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.6.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <relocations>
          <relocation>
            <pattern>dev.marulabs.cadence</pattern>
            <shadedPattern>com.example.myplugin.libs.cadence</shadedPattern>
          </relocation>
        </relocations>
      </configuration>
    </execution>
  </executions>
</plugin>
```

with the JitPack repository and the dependency in the default `compile` scope:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.github.Maru32768.Cadence</groupId>
    <artifactId>paper</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```

Paper notes:

- **`paper-plugin.yml`.** Paper plugins have isolated class loaders, which is one more reason to shade: a Cadence
  in another plugin is not visible to them. Shading works the same with `plugin.yml` and `paper-plugin.yml`;
  register from `onEnable` or, for bootstrap commands, with `Cadence.bootstrap(context, ...)` (see
  [permissions](permissions.md)).
- **`libraries` in `plugin.yml`** resolves from Maven Central only, where Cadence is not published yet.
- **paperweight-userdev.** The `paper` artifact uses only the Paper API, so it works in any plugin jar. The `spigot`
  artifact is tested in a plain Shadow jar, which Paper treats as Spigot-mapped (no
  `paperweight-mappings-namespace: mojang` manifest entry). A Mojang-mapped plugin jar (paperweight's
  `MOJANG_PRODUCTION`) with the `spigot` artifact, and running it through `reobfJar`, are not tested.
- The `paper` artifact needs Paper 1.21+. Use `spigot` for a plugin that also runs on Paper 1.20.6 or earlier.

## Velocity

The same Shadow setup, with the Velocity API as `compileOnly` and `annotationProcessor`:

```kotlin
dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0")
    implementation("com.github.Maru32768.Cadence:velocity:<version>")
}
```

## Forge and NeoForge

Shade with a dependency filter, since the runtime classpath also holds Minecraft and the loader:

```kotlin
tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        // The platform artifact and the common artifact it depends on
        include(dependency("com.github.Maru32768.Cadence:.*:.*"))
    }
    relocate("dev.marulabs.cadence", "com.example.mymod.libs.cadence")
}
```

- **Forge (ForgeGradle).** The jar has to be reobfuscated after shading: add a `reobf` entry for `shadowJar` and
  finalize `shadowJar` with `reobfShadowJar`, as in the [Forge sample](../samples/forge/build.gradle.kts). The
  Cadence jar itself is already in SRG names.
- **NeoForge.** NeoForge runs on Mojang names, so the shadow jar needs no reobfuscation step.
- **Jar-in-Jar** is not recommended, see above.

## Fabric

Loom remaps a `modImplementation` dependency to the development names, so shade Cadence from the runtime classpath,
then remap the shadow jar to intermediary together with your mod.

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    modImplementation("net.fabricmc.fabric-api:fabric-api:<fabric api version>")
    modImplementation("com.github.Maru32768.Cadence:fabric-1.21.1:<version>")
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-shadow")
    dependencies {
        // Loom puts the remapped artifact in the group remapped.<group>
        include { it.moduleGroup.endsWith("com.github.Maru32768.Cadence") }
    }
    relocate("dev.marulabs.cadence", "com.example.mymod.libs.cadence")
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
```

Declare `fabric-command-api-v2` and `fabric-lifecycle-events-v1` (or `fabric-api`) in your `depends`, since
Cadence uses them and its jar declares no dependencies, shaded or nested. The [Fabric sample](../samples/fabric)
builds this way.

The alternative, `include()`, nests the Cadence jar and `common`:

```kotlin
dependencies {
    modImplementation("com.github.Maru32768.Cadence:fabric-1.21.1:<version>")
    include("com.github.Maru32768.Cadence:fabric-1.21.1:<version>")
    include("com.github.Maru32768.Cadence:common:<version>")
}
```

Fabric Loader then picks which nested copy runs, as described in [version conflicts](#jar-in-jar-and-version-conflicts).

To let permission mods such as LuckPerms answer Cadence's permission checks, nest
[fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) with `include()` as usual. It is a separate
mod that Cadence looks up at runtime, so it is not relocated:

```kotlin
dependencies {
    modImplementation("me.lucko:fabric-permissions-api:0.3.3")
    include("me.lucko:fabric-permissions-api:0.3.3")
}
```
