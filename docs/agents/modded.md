# Modded (Forge / NeoForge) Module Notes

## Layout

Every supported loader (Forge and NeoForge) and Minecraft version is built from
one shared source tree with Stonecutter and Architectury Loom:

| Path                                               | Role                                                                |
|----------------------------------------------------|---------------------------------------------------------------------|
| `modded/src/main/java`                             | The only Forge / NeoForge source tree. Edit code here.              |
| `modded/build.gradle.kts`                          | Version template, evaluated once per node.                          |
| `modded/stonecutter.gradle.kts`                    | Stonecutter controller. Holds the active node.                      |
| `modded/versions/<node>/gradle.properties`         | Per-node settings (`loom.platform`, `deps.forge` / `deps.neoforge`). |
| `modded/versions/<node>/build/generated/...`       | Generated per-node sources. Never edit these.                       |

Nodes are named `<loader>-<minecraft-version>` (for example `forge-1.20.1`,
`neoforge-1.21.1`). Gradle projects are `:modded:<node>` and publish
`<loader>-<minecraft-version>`, so `:modded:forge-1.20.1` publishes
`forge-1.20.1` and `:modded:neoforge-1.21.1` publishes `neoforge-1.21.1`. The
node list lives in `settings.gradle.kts`. Design background is in
`docs/design/forge-version-support.md`.

All versions, including 1.16.5, are written against official Mojang mappings.
Loom remaps the built Forge jars to SRG. NeoForge runs on Mojang names, so its
jars keep them.

## Conditional Comments

Version-specific code uses Stonecutter comments:

```java
//? if >=1.19 {
MutableComponent component = Component.literal(message);
//?} else
/*BaseComponent component = new TextComponent(message);*/
```

- `modded/src` is committed in the `vcsVersion` state (currently
  `forge-1.20.1`). Code for other nodes is commented out in the shared source,
  so the IDE does not type-check it. Only compiling that node checks it.
- Do not put `/* */` comments or Javadoc inside a conditional block. Java does not
  allow nested block comments, so the block breaks when it is commented out.
  Use `//` comments there.
- Use the Minecraft version where the API actually changed as the boundary
  (for example `>=1.19` for `Component.literal`, `>=1.18` for Forge permission
  nodes). Only the nodes in `settings.gradle.kts` are built, so boundaries
  for versions in between are unverified until such a version is added.
- Loader-specific code uses the `forge` and `neoforge` constants, defined from
  `loom.platform` in each build script. Most differences are package renames
  (`net.minecraftforge` -> `net.neoforged.neoforge`, `net.neoforged.bus` for
  `SubscribeEvent`, `NeoForge.EVENT_BUS`). Put the loader condition first and
  fall back to version conditions with `elif`:

  ```java
  //? if neoforge {
  /*import net.neoforged.neoforge.server.ServerLifecycleHooks;
  *///?} elif >=1.17 {
  import net.minecraftforge.server.ServerLifecycleHooks;
  //?} else
  /*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/
  ```

- Prefer code that needs no condition when the same call works on every
  version.
- If the active node was switched in the IDE, switch back to the
  `vcsVersion` before committing.

## Verification

Compile every node after any change under `modded/`, not only the node you
were working on, and run the `modded-testing` tests:

```bash
./gradlew :modded:forge-1.16.5:compileJava :modded:forge-1.20.1:compileJava :modded:neoforge-1.21.1:compileJava
./gradlew :modded-testing:forge-1.16.5:test :modded-testing:forge-1.20.1:test :modded-testing:neoforge-1.21.1:test
```

`modded-testing` holds the public testing artifacts (`forge-testing-<version>` and `neoforge-testing-<version>`:
`CommandTester`, `FakeSender`). It is a Stonecutter tree like `modded`, with the same nodes and conditional comment
rules. It bootstraps Minecraft's registries and mocks the server, Forge's `NetworkHooks` during bootstrap (NeoForge:
`FeatureFlagLoader`, which needs FML's mod list), and the permission lookup while a command executes. On 1.20.5+ the
mocked server's `registryAccess()` is built with `VanillaRegistries.createLookup()`, so data-driven registries such as
enchantments resolve.

When a change can affect the public API or published jars, also run
`./gradlew :modded:forge-1.16.5:build :modded:forge-1.20.1:build :modded:neoforge-1.21.1:build` and inspect the jars
in `modded/versions/<node>/build/libs`.

The build needs JDK 21 for the Gradle daemon (`gradle/gradle-daemon-jvm.properties`).

Behavior that depends on a real server, such as command registration, argument parsing against registries, or
production (SRG) names, is covered by the Forge / NeoForge integration targets. They need Docker:

```bash
./gradlew :integration-test:targets:forge-1.16.5:minecraftIntegrationTest
./gradlew :integration-test:targets:forge-1.20.1:minecraftIntegrationTest
./gradlew :integration-test:targets:neoforge-1.21.1:minecraftIntegrationTest
```

The test mod is in `integration-test/shared/modded` (see `integration-test/README.md`). When adding or changing a
public argument class, add a case to its `ArgumentTest`; `:integration-test:test` fails when one is missing.

## Adding A Node

1. Add `version("<loader>-<version>", "<version>")` to each of the three
   Stonecutter trees in `settings.gradle.kts` (`modded`, `modded-testing`, and
   the test mod tree).
2. Create `modded/versions/<node>/gradle.properties` with
   `loom.platform=forge` and `deps.forge=<forge build>`, or
   `loom.platform=neoforge` and `deps.neoforge=<neoforge build>`.
3. Add the project to `publishedArtifactIds` and `publishedProjectPaths` in the
   root `build.gradle.kts`.
4. Check the Java version selection in `modded/build.gradle.kts`.
5. Compile every node and fix errors with conditional comments, adjusting
   existing boundaries when the new version shows they were wrong.
6. Create `modded-testing/versions/<node>/gradle.properties` and add the project
   to the root publication lists.
7. Create `integration-test/shared/modded/versions/<node>/gradle.properties`
   (also with `deps.forgeLoader`, the mod loader version range lower bound), and
   add an `integration-test/targets/<node>` target registered in the
   `minecraftIntegrationTest` aggregate task.
8. Update the supported versions in `README.md` and
   `docs/design/forge-version-support.md`.
