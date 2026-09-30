# Forge Version Support

This document records the current design direction for expanding the Forge
module beyond Minecraft 1.16.5.

## Current State

The current `forge` module is a Forge 1.16.5 implementation:

- `forge/build.gradle.kts` compiles against
  `net.minecraftforge:forge:1.16.5-36.2.20`.
- It uses MCP snapshot mappings: `snapshot`, `20210309-1.16.5`.
- Public API classes expose Forge 1.16.5 / Minecraft 1.16.5 types such as
  `CommandSource`, `ITextComponent`, `PlayerEntity`, `ServerPlayerEntity`,
  `ServerWorld`, and `DefaultPermissionLevel`.
- Command registration is tied to `FMLServerStartedEvent` and direct mutation of
  the server command dispatcher root.
- Permission registration and lookup use the old
  `PermissionAPI.registerNode(String, DefaultPermissionLevel, String)` and
  `PermissionAPI.hasPermission(PlayerEntity, String)` API.

This is materially different from the Spigot module. The Spigot module can keep
one artifact and choose version-specific NMS bridges at runtime because its
public compile target is mostly the stable Bukkit API. The Forge module compiles
directly against Minecraft and Forge classes whose package names, class names,
method names, Java baseline, mapping channel, and permission APIs change across
Minecraft versions.

## ForgeGradle Constraints

ForgeGradle selects a single Minecraft dependency and a single mapping set for a
project. The `minecraft` dependency determines the Minecraft/Forge version to
compile against, and the `minecraft.mappings` setting determines the readable
names used by source code. ForgeGradle then re-obfuscates built jars for the
target Minecraft version.

That means one Gradle project should be treated as one remap target. Mixing
multiple Minecraft versions in one Forge source set would force incompatible
Minecraft classes into the same compile classpath and one re-obfuscation
pipeline. Even if this were forced with custom source sets, the output jars
would still need separate remap tasks and separate runtime metadata per target
version.

The key version boundary is 1.16.5 -> 1.17:

- 1.16.5 uses MCP snapshot names in the current module.
- ForgeGradle documentation notes that MCP `stable` / `snapshot` mappings no
  longer exist as of 1.17; modern Forge development uses official mappings.
- Production mappings also changed around this boundary: pre-1.17 production
  class names are SRG-style, while 1.17+ production class names use official
  mappings.

## Compatibility Risks

These surfaces are expected to require version-specific source:

| Surface | 1.16.5 shape | Modern Forge shape | Risk |
|---|---|---|---|
| Command source | `net.minecraft.command.CommandSource` | `net.minecraft.commands.CommandSourceStack` | Public API and generics differ. |
| Text components | `ITextComponent`, `StringTextComponent`, `TranslationTextComponent` | `Component`, `MutableComponent`, `Component.literal`, `Component.translatable` | Public message API differs. |
| Player/entity names | `PlayerEntity`, `ServerPlayerEntity` | `Player`, `ServerPlayer` | Public argument return types differ. |
| Command registration | Server-started hook plus dispatcher root mutation | `RegisterCommandsEvent` / server lifecycle APIs depending on version | Reload behavior and event timing differ. |
| Permissions | `DefaultPermissionLevel`, string nodes | `PermissionNode`, `PermissionTypes`, `PermissionGatherEvent.Nodes` | Public permission overloads cannot stay source-compatible. |
| Native arguments | `net.minecraft.command.arguments.*` | `net.minecraft.commands.arguments.*` | Most built-in argument wrappers differ by package and sometimes signature. |
| Registry access | 1.16 registry classes | Holder/registry access APIs in newer versions | Registry-backed arguments need per-version code. |
| Java baseline | Java 8/11-friendly | Java 17+ for 1.18+ and Java 21 for recent versions | One root toolchain is not enough. |

## Recommended Module Model

Do not try to make the current `forge` Gradle project compile multiple Minecraft
versions. Use Minecraft-version implementation modules and publish stable
artifacts for each Minecraft version.

Recommended internal layout:

```text
common
  ^
  |
  +-- forge-api
  +-- forge/1.16.5
  +-- forge/1.20.1
  +-- forge/1.21.x
  +-- neoforge/1.21.x
```

The exact Gradle project names can change, but the boundary should stay:

- `forge-api`: source-compatible CommandLib API that does not expose version
  specific Minecraft classes where avoidable.
- `forge/1.16.5`: current implementation moved mostly as-is.
- `forge/1.20.1` or another LTS-like target: modern Forge implementation on
  official mappings and Java 17.
- `forge/1.21.x`: latest Forge line if Forge remains relevant for that
  Minecraft version.
- `neoforge-*`: separate modules after the Forge/NeoForge split; do not hide
  NeoForge behind the Forge artifact unless its APIs are proven compatible.

Forge's major version is tied to the Minecraft version in practice. For example,
Minecraft 1.16.5 uses Forge 36.x.y, while Minecraft 1.20.1 uses Forge 47.x.y.
Therefore the normal module boundary should be the Minecraft version, not the
Forge major version. Forge minor and patch versions belong in the compatibility
matrix for that module.

The published artifact strategy should be explicit:

| Artifact | Meaning |
|---|---|
| `forge-1.16.5` or `forge-1_16_5` | Exact Minecraft line. Safe and clear for downstream modders. |
| `forge-1.20.1` or `forge-1_20_1` | Exact Minecraft line. |
| `forge` | Optional alias only after a policy decision. It should point to one supported line, not pretend to cover all Forge versions. |
| `neoforge-*` | Separate artifact family. |

Directory layout, Gradle path, and artifact names do not need to be identical.
The preferred shape is:

```text
source layout: forge/1.20.1
Gradle path:   :forge:1.20.1
artifactId:    forge-1.20.1
```

For modded platforms, exact-line artifacts are preferable to one broad
`forge` artifact. Downstream Forge mods are already tied to a Minecraft and
ForgeGradle line, so selecting a versioned CommandLib artifact is normal and
reduces accidental binary incompatibility.

### Forge Minor And Patch Versions

Each Minecraft-version module must record:

- compile target Forge version,
- tested Forge versions,
- supported Forge minor/patch range,
- known unsupported Forge versions,
- known behavior differences inside the same Minecraft version.

Example:

| Artifact | Compile target | Supported Forge range | Notes |
|---|---|---|---|
| `forge-1.16.5` | `1.16.5-36.2.20` | `36.2.x` | `36.0.x` and `36.1.x` are not promised unless tested. |
| `forge-1.20.1` | `1.20.1-47.3.x` | `47.3.x` | Exact lower bound should be set by integration tests. |

Do not split a module just because Forge has a different patch version. Split
below the Minecraft-version level only when the same artifact cannot honestly
support the affected Forge range. Examples:

- a Forge minor or patch version changes a compile-time API used by CommandLib,
- the same compiled artifact hits a linkage error on a Forge minor/patch line,
- command registration, permissions, or registry behavior differs enough that
  one implementation would need separate source,
- the supported range cannot be explained clearly in the compatibility matrix.

If that happens, introduce a sub-line only for the broken range:

```text
forge/
  1.20.1/
    47.2/
    47.3/
```

The default remains:

```text
module boundary = Minecraft version
compatibility matrix = Forge minor/patch range
```

## Public API Direction

Keep the high-level CommandLib model consistent, but avoid promising one Java
source file can compile unchanged against all Forge versions when it mentions
Minecraft-native types.

Recommended split:

- Keep platform-neutral command building, branching, options, validation, help,
  and default permission model in `common`.
- Keep Forge registration, sender, message component, permission, location, and
  native argument classes in Minecraft-version-specific Forge modules.
- For public native argument return types, allow version-specific artifacts to
  expose their native Minecraft type. For example, 1.16.5 may return
  `ServerPlayerEntity`, while a modern module may return `ServerPlayer`.
- Where a type can be made CommandLib-owned without losing practical value,
  prefer that for cross-version consistency. `Location` is a candidate, but
  player/entity/item/block native types are less suitable.
- Deprecate or avoid new overloads that expose Forge's old
  `DefaultPermissionLevel` in any modern API. Use CommandLib's
  `DefaultPermission` in common-facing APIs, and adapt to Forge-specific
  permission systems internally.

## Migration Path

1. Freeze the current `forge` module as the 1.16.5 implementation.
2. Introduce an internal Minecraft-version module for 1.16.5, either by moving
   `forge` to `forge/1.16.5` or by creating `forge/1.16.5` and keeping `forge`
   as a temporary compatibility alias.
3. Extract only genuinely shared, non-Minecraft-specific Forge code. Most code
   should stay duplicated until a second implementation proves the abstraction.
4. Add one modern Forge target first, preferably a widely used stable line such
   as 1.20.1, before chasing the newest Minecraft release.
5. Implement modern command registration and permissions using that version's
   Forge APIs.
6. Add focused compile verification per Minecraft-version module.
7. Add sample mods and integration fixtures per supported Forge line.
8. Decide whether `forge` remains a moving alias, a 1.16.5 legacy alias, or is
   removed before GA.

## Testing Strategy

Use compile checks as the first gate:

```text
./gradlew :forge:1.16.5:compileJava
./gradlew :forge:1.20.1:compileJava
```

Then add fixture-level checks:

- `sample/forge-1.16.5`
- `sample/forge-1.20.1`
- future `integration-test/fixtures/{version}-forge`

For each fixture, test the compile target first and then add selected
minor/patch versions inside the supported Forge range. When a Forge patch is
known to break, document it before adding a new module.

Runtime integration should be added after the first modern module compiles.
Forge server startup is heavier than command-level unit tests, so start with:

- command registration smoke test,
- permission registration / lookup smoke test,
- execution through Brigadier dispatcher,
- argument parse checks for native arguments.

## Open Questions

- Should the old `forge` artifact stay as an alias for 1.16.5, or should every
  Forge artifact be versioned before GA?
- Which modern Forge line should be first: 1.20.1 for ecosystem stability, or
  the newest stable Minecraft line for forward-looking support?
- What is the minimum supported Forge minor/patch for each Minecraft-version
  artifact?
- Should modern Forge expose native `Component` APIs directly, or should
  CommandLib-owned text builders remain the primary user-facing path?
- How much source compatibility do we want between `forge-1.16.5` and modern
  Forge modules when native Minecraft types necessarily differ?
- Should `forge-testing` be one artifact per version line, or a test helper
  layer that depends on the selected Forge implementation?

## Recommendation

Use Minecraft-version Forge implementation modules. Treat `forge` as a
convenience alias only if the release policy can explain exactly what it
targets. Track Forge minor and patch compatibility in the matrix, and split
below the Minecraft-version level only when a real Forge minor/patch
incompatibility is proven. This matches ForgeGradle's one-remap-target model
and avoids hiding incompatible Minecraft and Forge APIs behind a single
artifact.
