# Forge Module Notes

## Layout

Every supported Minecraft version is built from one shared source tree with
Stonecutter and Architectury Loom:

| Path                                               | Role                                                                |
|----------------------------------------------------|---------------------------------------------------------------------|
| `forge/src/main/java`                              | The only Forge source tree. Edit code here.                         |
| `forge/build.gradle.kts`                           | Version template, evaluated once per Minecraft version.             |
| `forge/stonecutter.gradle.kts`                     | Stonecutter controller. Holds the active version.                   |
| `forge/versions/<version>/gradle.properties`       | Per-version settings (`loom.platform`, `deps.forge`).               |
| `forge/versions/<version>/build/generated/...`     | Generated per-version sources. Never edit these.                    |

Gradle projects are `:forge:<minecraft-version>` (for example `:forge:1.16.5`,
`:forge:1.20.1`) and publish `forge-<minecraft-version>`. The version list lives
in `settings.gradle.kts`. Design background is in
`docs/design/forge-version-support.md`.

All versions, including 1.16.5, are written against official Mojang mappings.
Loom remaps the built jars to SRG.

## Conditional Comments

Version-specific code uses Stonecutter comments:

```java
//? if >=1.19 {
MutableComponent component = Component.literal(message);
//?} else
/*BaseComponent component = new TextComponent(message);*/
```

- `forge/src` is committed in the `vcsVersion` state (currently 1.20.1). Code
  for other versions is commented out in the shared source, so the IDE does not
  type-check it. Only compiling that version checks it.
- Do not put `/* */` comments or Javadoc inside a conditional block. Java does not
  allow nested block comments, so the block breaks when it is commented out.
  Use `//` comments there.
- Use the Minecraft version where the API actually changed as the boundary
  (for example `>=1.19` for `Component.literal`, `>=1.18` for Forge permission
  nodes). Only the versions in `settings.gradle.kts` are built, so boundaries
  for versions in between are unverified until such a version is added.
- Prefer code that needs no condition when the same call works on every
  version.
- If the active version was switched in the IDE, switch back to the
  `vcsVersion` before committing.

## Verification

Compile every Forge version after any change under `forge/`, not only the
version you were working on:

```bash
./gradlew :forge:1.16.5:compileJava :forge:1.20.1:compileJava
```

When a change can affect the public API or published jars, also run
`./gradlew :forge:1.16.5:build :forge:1.20.1:build` and inspect the remapped jars
in `forge/versions/<version>/build/libs`.

The build needs JDK 21 for the Gradle daemon (`gradle/gradle-daemon-jvm.properties`).

Behavior that depends on a real server, such as command registration, argument parsing against registries, or
production (SRG) names, is covered by the Forge integration targets. They need Docker:

```bash
./gradlew :integration-test:targets:forge-1.16.5:minecraftIntegrationTest
./gradlew :integration-test:targets:forge-1.20.1:minecraftIntegrationTest
```

The test mod is in `integration-test/shared/forge` (see `integration-test/README.md`). When adding or changing a
public argument class, add a case to its `ArgumentTest`; `:integration-test:test` fails when one is missing.

## Adding A Minecraft Version

1. Add the version to `versions(...)` in the `stonecutter` block of
   `settings.gradle.kts`.
2. Create `forge/versions/<version>/gradle.properties` with
   `loom.platform=forge` and `deps.forge=<forge build>`.
3. Add the project to `publishedArtifactIds` and `publishedProjectPaths` in the
   root `build.gradle.kts`.
4. Check the Java version selection in `forge/build.gradle.kts`.
5. Compile every version and fix errors with conditional comments, adjusting
   existing boundaries when the new version shows they were wrong.
6. Add the version to the test mod tree in `settings.gradle.kts`, create
   `integration-test/shared/forge/versions/<version>/gradle.properties`, and add an
   `integration-test/targets/forge-<version>` target registered in the `minecraftIntegrationTest` aggregate task.
7. Update the supported versions in `README.md` and
   `docs/design/forge-version-support.md`.
