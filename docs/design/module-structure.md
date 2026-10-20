# Module Structure

This document describes CommandLib's module / artifact layout and the rules that keep it stable. The pre-GA split of the old `bukkit` module into `spigot` and `paper` is done; this document records the result and what is still open.

## Principles

- `common` holds the platform-neutral command model. Platform modules depend on `common`; `common` depends on no platform module.
- Do not move more code into `common` unless a clearly platform-neutral new feature appears.
- `spigot` is based on Bukkit/Spigot API compatibility and does not use Paper-only APIs.
- `paper` is based on the official Paper command API, lifecycle API, and Adventure components. It does not depend on `spigot`, so Spigot-first registration, message, and NMS assumptions do not leak into it.
- Bukkit-family code that would blur platform contracts if shared stays duplicated until a truly neutral abstraction appears.
- "Bukkit" remains the name of the API family (Bukkit API compatibility); it is not a module or artifact name.

## Current Layout

| Module | Published | Role |
|---|---:|---|
| `common` | Yes | Platform-independent command model, arguments, options, permissions, help, and Brigadier tree construction. |
| `spigot` | Yes | Bukkit/Spigot implementation. Includes the typed NMS bridge (bundles the `nms:*` modules), the Bungee `BaseComponent` message path, and the legacy command registration path. |
| `paper` | Yes | Paper implementation on the official Paper command API and Adventure components. |
| `velocity` | Yes | Velocity proxy implementation on `BrigadierCommand`, Velocity's permission API, and Adventure components. |
| `modded` | Yes | Forge, NeoForge and Fabric implementation. One shared source tree built as Stonecutter nodes `forge-1.16.5`, `forge-1.18.2`, `forge-1.19.2`, `forge-1.20.1`, `neoforge-1.21.1`, `neoforge-1.21.4`, `fabric-1.20.1`, `fabric-1.21.1`, and `fabric-1.21.4` (Gradle paths `:modded:<node>`). |
| `nms:*` | No | Typed NMS modules bundled into `spigot`, one per directory under `nms/`. |
| `common-testing` | Yes | Test helpers for commands built against `common`. |
| `spigot-testing` | Yes | `CommandTester` / `FakeSender` and NMS mocks for downstream Spigot plugins. |
| `paper-testing` | Yes | Testing utilities for the Paper command API, Adventure components, and Paper registry arguments. |
| `velocity-testing` | Yes | `CommandTester` / `FakeSender` with a Mockito `ProxyServer` for downstream Velocity plugins. |
| `modded-testing` | Yes | Serverless testing utilities for downstream Forge / NeoForge / Fabric mods, built per node. |
| `common-kotlin` | Yes | The platform-neutral Kotlin DSL: argument blocks, delegated options, coroutine executors and suggestions. |
| `spigot-kotlin` / `paper-kotlin` / `velocity-kotlin` | Yes | The DSL's `command` / `subcommand` factories, compiled against each platform's `Command`. |
| `benchmark` | No | JMH benchmarks of `common` through the `common-testing` `CommandTester` (`docs/benchmarks.md`). |
| `examples` | No | The use cases of `docs/comparison.md` written in each style, with snapshot tests. |
| `integration-test` | No | Multi-platform real-server integration aggregator (Spigot, Paper, Mohist, Forge, NeoForge, Fabric, Folia, Velocity targets). |
| `samples` | No | Sample plugins / mods for Spigot, Paper (Java and Kotlin), Velocity, Forge, and Fabric; separate Gradle builds, not in `settings.gradle.kts`. |

What stays in `common`:

- `CommonCommand`, `CommonArgument`, `CommonCommandContext`
- command tree / branch model, command options
- default permission model
- common help / usage model
- exception model
- platform-neutral suggestion model

What does not go into `common`:

- Bukkit / Spigot / Paper / Forge / Fabric / Velocity types
- platform command registration and permission registration
- platform message component implementation
- NMS / reflection bridge
- Paper lifecycle API

## Artifact Names

| Gradle project | Artifact ID |
|---|---|
| `:common` | `common` |
| `:spigot` | `spigot` |
| `:paper` | `paper` |
| `:velocity` | `velocity` |
| `:modded:forge-1.16.5` | `forge-1.16.5` |
| `:modded:forge-1.18.2` | `forge-1.18.2` |
| `:modded:forge-1.19.2` | `forge-1.19.2` |
| `:modded:forge-1.20.1` | `forge-1.20.1` |
| `:modded:neoforge-1.21.1` | `neoforge-1.21.1` |
| `:modded:neoforge-1.21.4` | `neoforge-1.21.4` |
| `:modded:fabric-1.20.1` | `fabric-1.20.1` |
| `:modded:fabric-1.21.1` | `fabric-1.21.1` |
| `:modded:fabric-1.21.4` | `fabric-1.21.4` |
| `:common-testing` | `common-testing` |
| `:spigot-testing` | `spigot-testing` |
| `:paper-testing` | `paper-testing` |
| `:velocity-testing` | `velocity-testing` |
| `:modded-testing:forge-1.16.5` | `forge-testing-1.16.5` |
| `:modded-testing:forge-1.18.2` | `forge-testing-1.18.2` |
| `:modded-testing:forge-1.19.2` | `forge-testing-1.19.2` |
| `:modded-testing:forge-1.20.1` | `forge-testing-1.20.1` |
| `:modded-testing:neoforge-1.21.1` | `neoforge-testing-1.21.1` |
| `:modded-testing:neoforge-1.21.4` | `neoforge-testing-1.21.4` |
| `:modded-testing:fabric-1.20.1` | `fabric-testing-1.20.1` |
| `:modded-testing:fabric-1.21.1` | `fabric-testing-1.21.1` |
| `:modded-testing:fabric-1.21.4` | `fabric-testing-1.21.4` |
| `:common-kotlin` | `common-kotlin` |
| `:spigot-kotlin` | `spigot-kotlin` |
| `:paper-kotlin` | `paper-kotlin` |
| `:velocity-kotlin` | `velocity-kotlin` |

No `bukkit` artifact exists. `integration-test` is not published.

## Dependency Direction

```text
common
  ^
  |
  +-- spigot
  +-- paper
  +-- velocity
  +-- modded (forge-* / neoforge-* / fabric-* nodes)

common-testing   -> common
spigot-testing   -> spigot
paper-testing    -> paper
velocity-testing -> velocity
modded-testing   -> modded
common-kotlin    -> common
spigot-kotlin    -> spigot, common-kotlin
paper-kotlin     -> paper, common-kotlin
velocity-kotlin  -> velocity, common-kotlin
integration-test -> platform modules
```

## Packages And Visibility

Everything outside `dev.marulabs.commandlib.internal` is API. Implementation that another package or module must reach
is public in `internal` or one of its sub-packages, each annotated `@ApiStatus.Internal` in its `package-info.java`
(and each public class there too), so a user who excludes `dev.marulabs.commandlib.internal` from completion, as the
README describes, sees only API. The rule is to hide by package, not to make the internals unreachable: Java has no
visibility between "package" and "public" that spans packages, and the JPMS cannot be used because CommandLib is shaded
into plugins and mods.

- `common`: `internal` holds `PlatformAdapter` and its holder, `CurrentCommandContext`, `ArgumentBranchDelegate` and
  `StringUtil`. `spigot`: the NMS bridge (`internal.nms`, typed modules in `internal.nms.typed.<module>`), reflection
  and Bukkit helpers. `paper`: `internal.bukkit`, `internal.brigadier`. `modded`: `internal.CurrentServer`.
  `spigot-testing`: the NMS mocks (`internal.nms.mock`).
- Each platform's `PlatformAdapterImpl` stays package-private in the root package, since it uses package-private
  helpers there. `PlatformAdapterHolder` loads it by its fixed name and calls its declared constructor; both are in
  the same jar after shading, so the access check passes. There is no `ServiceLoader`, which failed on plugin reloads
  (PlugMan).
- The tree generation (`CommandNodeCreator`, `CommandRunner`, `OptionGraph`, `CommandTree`, ...) stays package-private
  in the root package. It reads package-private members of `CommonCommand`, `CommonArgument` and `CommandOption`;
  moving it out would make those members public on classes every user completes against, which no package exclusion
  hides.
- The testing artifacts' `CommandTester` is API in the root package and drives that tree generation, so it keeps
  reaching it from the same package (see `testing-strategy.md`). That only needs one class loader for both jars,
  which a test classpath always has.

## Public API Shape

### Spigot

The `spigot` artifact exposes `dev.marulabs.commandlib.Command`, `CommandContext`, `CommandLib`, and the Bukkit/Spigot argument classes.

Responsibilities:

- Bukkit/Spigot command registration and permission registration.
- Bukkit sender/context bridge.
- NMS fallback where no official API exists (Spigot `1.16.5` to `1.21.1`).
- Legacy text / Bungee `BaseComponent` compatibility.
- Old Paper compatibility as long as it matches Spigot-oriented behavior.

### Paper

The `paper` artifact provides the same high-level command model without being constrained by Spigot internals.

Responsibilities:

- Paper official command API / lifecycle registration.
- Adventure component message path.
- Paper registry-backed arguments, predicates, and Adventure arguments.
- Paper-native suggestion behavior.
- NMS fallback only where Paper official APIs do not cover the required behavior.

### Velocity

The `velocity` artifact exposes the same `Command` / `CommandContext` / `CommandLib` shape on a proxy. Velocity's
`BrigadierCommand` takes Brigadier nodes, so `CommandNodeCreator` builds them unchanged, and every top-level node
(command or alias) is registered as its own `BrigadierCommand` of the plugin.

Responsibilities:

- Registration through `CommandManager` and removal by `CommandMeta`.
- Sender bridge for `CommandSource`, `Player`, and `ConsoleCommandSource`.
- Permission checks through `PermissionSubject#getPermissionValue`, applying CommandLib's defaults to unset nodes,
  since Velocity has no permission registry and no operators.
- Adventure messages. Paper and Velocity both compile the `RichText` converter from `common/src/adventure`. It is
  written against the click-event factories so that it links against Adventure 4 (Velocity 3) and Adventure 5
  (Velocity 4).
- Proxy arguments as string arguments with proxy-side parsing and suggestions (`PlayerArgument`, `ServerArgument`),
  since the client knows only Brigadier's own argument types through a proxy. Spigot / Paper argument parity does not
  apply.

The module compiles against Velocity API 3.4.0 (Java 17). The API it uses is unchanged up to 4.2.0, and the jar was
run on Velocity 3.4.0 and 4.2.0 proxies.

### Kotlin DSL

The DSL is extension functions over the fluent model (`CommonCommand`, `ArgumentBranch`, `CommandOption`), so it
adds no command semantics of its own. Platform types stay out of `common-kotlin`; the only platform-specific code is
`command` / `subcommand`, which must create the platform's `Command`. Every platform names that class
`dev.marulabs.commandlib.Command`, so the platform modules compile one shared source directory
(`common-kotlin/src/platform`) and one shared test directory (`common-kotlin/src/platformTest`) instead of each having
its own copy. Forge / NeoForge / Fabric have no Kotlin module yet, since a Loom build per node would be needed for three
functions.

`common-kotlin` reads the current command context through `internal.CurrentCommandContext`, which keeps the core API
unchanged without requiring `common` and `common-kotlin` to share a class loader. kotlinx-coroutines is `compileOnly`, and the coroutine functions are in their own file
class, so the rest of the DSL loads without it.

### Forge / NeoForge / Fabric

`modded` exposes the same command model where practical, using platform-native registration, permission, message, and registry APIs. Public API exposes each Minecraft version's native types. See `forge-version-support.md`.

## Platforms Without a Module

A new platform module should follow the rules above (platform module depends on `common`, no code moved into `common`).

Mohist has no module of its own. It is handled through `spigot` compatibility and the Mohist integration targets.

Folia has no module of its own either: Folia keeps Paper's command and lifecycle API, so the `paper` artifact detects
Folia at runtime and schedules async commands on its region and entity threads (`docs/execution.md#folia`). The
`spigot` artifact refuses Folia, since the Bukkit scheduler throws there and its command map and NMS paths assume one
main thread.

## Open Questions

- Should `paper` share any source with `spigot`, or should duplication be accepted until a true neutral abstraction appears?
- Should `mohist` stay tested under `spigot`, or become its own artifact if its differences grow?
- Should message APIs expose CommandLib's own abstraction, Adventure, or platform-native components?
- Should `modded` be split into a Minecraft-version module and loader modules (see `docs/roadmap.md`)? Fabric was added as
  a loader of the same tree without the split: the loader-specific code is command registration, permissions, the
  running server lookup (`internal.CurrentServer`) and the `fabric.mod.json` metadata.
