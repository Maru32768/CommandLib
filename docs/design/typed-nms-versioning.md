# Typed NMS Versioning Design

This document describes a possible migration from the current reflection-heavy
Spigot NMS bridge to a CommandAPI-like typed NMS module design.

The target is not an immediate rewrite. The practical first step is to prove
that CI can compile a typed `1.16.5` NMS implementation reliably.

## Background

The current `spigot` module compiles against `spigot-api:1.16.5` and Brigadier.
It does not compile against server NMS classes. Version-specific behavior is
handled by:

- `NMSClassRegistry`, which maps an abstract bridge class to an implementation
  class by Minecraft version range.
- `NMSReflection` and `NMSClass`, which resolve Minecraft/CraftBukkit classes,
  methods, fields, and constructors at runtime.
- Version range registrations such as `1.16.0..1.16.5`,
  `1.17.0..1.20.4`, and `1.20.5..9.9.9`.

This keeps the normal build light and avoids requiring CI to resolve Spigot NMS
artifacts. The tradeoff is that broken NMS class names, method names, and
constructor signatures are usually found at runtime.

CommandAPI uses the opposite tradeoff. It keeps version-specific modules that
compile against real NMS artifacts, remaps the built artifacts when necessary,
and chooses the correct implementation at runtime with a version handler.

## Goals

- Add a typed NMS path that can catch common NMS breakages at compile time.
- Start with Minecraft `1.16.5` / CraftBukkit `v1_16_R3`, because it is the
  current minimum supported version and uses the old package-based NMS layout.
- Keep the existing reflection bridge while typed modules are introduced.
- Avoid making normal local development depend on BuildTools unless explicitly
  requested.
- Make NMS verification suitable for CI or a manual/nightly CI job.
- Preserve the public CommandLib API.

## Non-Goals

- Do not immediately replace every reflection bridge.
- Do not make `common` depend on Bukkit, Spigot, Paper, or Minecraft classes.
- Do not move Paper 1.21+ registration back to NMS when Paper official command
  APIs are sufficient.
- Do not require downstream users to depend on version-specific artifacts
  directly.
- Do not support every historical Minecraft version with typed NMS in the first
  phase.

## Proposed Module Layout

The typed NMS design should keep the public `spigot` artifact as the user-facing
entry point.

```text
common
  ^
  |
spigot
  ^
  |
spigot-nms-api
  ^
  |
spigot-nms-v1_16_R3
```

Longer term, more implementation modules can be added:

```text
spigot-nms-v1_17_R1
spigot-nms-v1_18_R2
spigot-nms-v1_19_R3
spigot-nms-v1_20_R4
spigot-nms-v1_20_5
spigot-nms-v1_21_x
```

Exact module names should follow the server-side compatibility boundary, not
necessarily every Minecraft patch version. For example, one implementation can
cover `1.16.4` and `1.16.5` if both use `v1_16_R3` and the required APIs are
compatible.

## Bridge Contract

Add a small typed bridge API that the `spigot` module can call without knowing
which NMS implementation is loaded.

Example shape:

```java
public interface SpigotNmsBridge {
    CommandDispatcher<?> getDispatcher(Server server);

    org.bukkit.command.Command createVanillaCommandWrapper(
            Object dispatcher,
            CommandNode<?> node
    );

    ArgumentType<?> itemStackArgument();

    ArgumentType<?> entityArgument(EntitySelectorMode mode);
}
```

The bridge should start narrow. Only move code into the typed bridge when it is
both NMS-sensitive and worth compile-time checking.

Good first candidates:

- command dispatcher access
- vanilla command wrapper construction
- command source wrapper access
- item stack, entity selector, particle, enchantment, and namespaced key
  argument factories

Poor first candidates:

- pure Bukkit API code
- command tree construction in `common`
- permission registration
- Paper lifecycle registration

## Runtime Selection

Add a version selector owned by the `spigot` module or `spigot-nms-api`.

For the first phase:

```text
1.16.4, 1.16.5 -> v1_16_R3 typed bridge
otherwise      -> current reflection bridge
```

The selector should not instantiate typed implementations by direct reference
from `spigot` if that would force all NMS implementation classes onto the
compile classpath. Prefer either:

- `ServiceLoader<SpigotNmsBridgeProvider>`, or
- a small reflective lookup by implementation class name.

The provider should expose:

```java
boolean supports(String minecraftVersion, String craftBukkitPackageVersion);
SpigotNmsBridge create();
```

If no typed provider supports the current server, fall back to the existing
reflection implementation.

## Build Strategy

### Phase 1: local proof

Create `spigot-nms-v1_16_R3` and compile it against a locally available Spigot
NMS artifact:

```text
org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT
```

This artifact is normally produced by BuildTools and installed into the local
Maven repository. The implementation can directly import classes such as:

```java
net.minecraft.server.v1_16_R3.CommandDispatcher
net.minecraft.server.v1_16_R3.MinecraftServer
org.bukkit.craftbukkit.v1_16_R3.CraftServer
```

This phase does not need remapping. It can use the Spigot-mapped names that
exist in the 1.16.5 server jar.

### Phase 2: CI compile job

Add a dedicated CI job that prepares the NMS artifact before running the typed
NMS compile task.

Possible CI flow:

```text
restore BuildTools / Maven cache
run BuildTools for 1.16.5 if cache miss
run ./gradlew :spigot-nms-v1_16_R3:compileJava
```

This job should be allowed to run separately from the normal unit-test job. A
manual or nightly job is acceptable until the NMS artifact setup is stable.

### Phase 3: normal build integration

Once CI is reliable, decide whether typed NMS compile should be part of the
default `build`.

Conservative default:

- `./gradlew build` keeps using the existing modules.
- `./gradlew typedNmsCheck` compiles all typed NMS modules.
- release CI runs `typedNmsCheck`.

Stricter default:

- all typed NMS modules are included in `check`.
- CI fails whenever the NMS artifact cannot be prepared.

The conservative default is recommended until BuildTools cache behavior is
proven stable.

## Packaging Strategy

Downstream users should continue depending on the public `spigot` artifact.

There are two packaging options:

### Option A: shade typed implementations into `spigot`

The published `spigot` jar contains:

- the current public API
- the reflection fallback
- typed NMS provider classes

This is simple for users. The risk is that version-specific classes may fail to
load on unsupported servers if referenced too eagerly. The selector must avoid
loading implementation classes until it knows the server version is compatible.

### Option B: publish an aggregate runtime artifact

Publish:

```text
spigot
spigot-nms-v1_16_R3
spigot-nms-runtime
```

The `spigot-nms-runtime` artifact depends on all typed NMS implementations.
This is closer to CommandAPI's dependency aggregation model, but it is more
complex for users unless `spigot` depends on the aggregate.

Option A is recommended for CommandLib unless jar size or class loading becomes
a problem.

## Relationship To Paper

The `paper` module should not adopt this design for command registration when
Paper official APIs are enough.

Paper-specific policy:

- Use Paper lifecycle command registration for modern Paper.
- Use Paper/Adventure APIs for Paper-native behavior.
- Add typed NMS only for features that Paper APIs cannot expose.

This keeps Paper support from inheriting Spigot's legacy NMS constraints.

## Migration Plan

### Step 1: introduce bridge interface

- Add `spigot-nms-api`.
- Define a small `SpigotNmsBridge` contract.
- Add an adapter that wraps the current reflection implementation.
- Keep behavior unchanged.

### Step 2: add typed `v1_16_R3` implementation

- Add `spigot-nms-v1_16_R3`.
- Implement only command registration primitives first.
- Compile against local `org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT`.
- Add focused tests that verify provider selection.

### Step 3: route 1.16.5 through typed bridge

- Update the selector to prefer the typed provider for `v1_16_R3`.
- Keep reflection fallback available.
- Verify with `:spigot:compileJava`, `:spigot:test`, and the `1.16.5`
  integration target when available.

### Step 4: move selected argument factories

- Move high-risk NMS argument factories to the typed bridge one at a time.
- Keep existing `NMSArgument*` reflection classes until each typed replacement
  is proven.
- Add integration coverage for every moved argument family.

### Step 5: decide future version boundaries

After `v1_16_R3` works, choose the next boundary based on maintenance pain:

- `1.17.0..1.20.4`: package layout changed, but many Brigadier/NMS APIs are
  still stable enough to group.
- `1.20.5+`: registry and command build context changes are more likely to
  justify a separate typed module.
- latest Paper-only behavior should remain in the `paper` module when possible.

## CI Risks

- BuildTools can be slow and network-sensitive.
- BuildTools output is installed into local Maven, so cache invalidation must be
  explicit.
- CI runners may need a specific Java version for old Minecraft builds.
- Spigot artifacts are not as stable as normal Maven Central dependencies.
- Typed NMS compile jobs can fail for infrastructure reasons unrelated to
  CommandLib source changes.

Mitigations:

- Cache BuildTools output and Gradle/Maven caches.
- Keep typed NMS compile in a separate CI job at first.
- Use a manual/nightly workflow before making it a pull-request gate.
- Document the local setup command for maintainers.

## Open Questions

- Should typed NMS providers be loaded through `ServiceLoader` or explicit
  reflection?
- Should typed implementations be shaded into `spigot`, or published as
  separate runtime artifacts?
- Should BuildTools be run by Gradle, by CI shell steps, or by a separate
  repository cache?
- Should failures in typed NMS compile block normal pull requests immediately,
  or only release builds at first?
- Which bridge methods are valuable enough to move before argument factories?
- How much fallback behavior should remain after a typed implementation exists
  for a version?

## Recommendation

Use a hybrid model:

- Keep the current reflection bridge as the default fallback.
- Add typed NMS modules gradually, starting with `spigot-nms-v1_16_R3`.
- Verify typed modules in a dedicated CI job.
- Keep Paper's modern command path separate.
- Move NMS-sensitive behavior into typed adapters only when compile-time
  checking clearly pays for the added module and CI complexity.

This gets the main benefit of the CommandAPI approach without forcing a full
multi-version NMS rewrite before the build infrastructure is proven.
