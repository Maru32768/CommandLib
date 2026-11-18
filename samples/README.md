# Cadence Sample Projects

Small, buildable plugins and mods, one per platform. Each one shades and relocates Cadence as described in
[packaging](../docs/packaging.md).

| Sample | Platform | Language | Build |
|---|---|---|---|
| [spigot](./spigot) | Spigot 1.16.5+ | Java | composite build |
| [paper](./paper) | Paper 1.21+ | Java | composite build |
| [paper-kotlin](./paper-kotlin) | Paper 1.21+ | Kotlin DSL | composite build |
| [velocity](./velocity) | Velocity 3.4+ | Java | composite build |
| [forge](./forge) | Forge 1.16.5 (ForgeGradle 6) | Java | `-PcadenceLocal=true` |
| [fabric](./fabric) | Fabric 1.21.1 (Loom) | Java | `-PcadenceLocal=true` |

## Building

The composite-build samples include the repository build with `includeBuild` and dependency substitution, so they
compile against the local Cadence source:

```bash
cd samples/paper
./gradlew build
```

The spigot sample builds the included `:spigot` jar, which bundles every typed NMS module under `nms/`, and each of those
needs BuildTools or a paperweight dev bundle. To build it without them, pass the repository's property; it reaches the
included build:

```bash
cd samples/spigot
./gradlew build -Pcadence.typedNms=false
```

That jar has no NMS implementations and throws `NMSClassNotFoundException` on a server, so use it to compile only.

Copied outside the repository, they resolve `com.github.Maru32768.Cadence:*:latest.release` from JitPack like a
downstream project. Replace `latest.release` with a release tag in your own project.

The Forge and Fabric samples do not include the repository build: ForgeGradle 6 does not run on the Gradle 9 the
repository needs, and Loom remaps mod dependencies from a Maven artifact. To build them against the local source,
publish the artifacts they use at the repository root and pass `-PcadenceLocal=true`:

```bash
./gradlew -Pcadence.typedNms=false :common:publishToMavenLocal :modded:forge-1.16.5:publishToMavenLocal
(cd samples/forge && ./gradlew build -PcadenceLocal=true)

./gradlew -Pcadence.typedNms=false :common:publishToMavenLocal :modded:fabric-1.21.1:publishToMavenLocal
(cd samples/fabric && ./gradlew build -PcadenceLocal=true)
```

Without the property they resolve the JitPack release like a downstream mod.

## Commands

| Command | Demonstrates | Samples |
|---|---|---|
| `/ping` | Minimal command with no arguments. | spigot, paper, forge, fabric |
| `/hello <players>` | Platform player selector argument and typed argument execution. | spigot, paper, forge |
| `/config get`, `/config set <number>` | Literal child commands, an integer argument. | spigot, paper, forge |
| `/admincheck` | Command-level permission metadata. | spigot, paper, forge |
| `/topic <name>` | Custom argument suggestions. | spigot, paper, forge |
| `/broadcast [-s\|--silent] [-r\|--repeat <count>] <message>` | Flag and value options, short and long names. | spigot, paper, forge |
| `/preprocesscommand`, `/preprocesscommand sub` | Preprocess checks, and a child that disables the parent's. | spigot, paper, forge |
| `/increment <target> [count]` | An optional argument with a default value. | spigot, paper, forge, fabric |
| `/math add <left> <right>`, `/math multiply ...` | Multi-argument execution in child commands. | spigot, paper, forge, fabric |
| `/math sum <numbers...>` | A variadic argument. | spigot, paper, forge, fabric |
| `/reset` | `confirm()` and `cooldown(...)`. | spigot, paper, forge, fabric |
| `/lookup <name>` | A middleware, and `executeAsync` with `ctx.supplyAsync`. | spigot, paper |
| `/arena create\|delete\|list` | The annotation style (`AnnotatedCommands.parse`). | spigot, paper |
| `/feed [target]` | The Java builder (`Command.builder`) and a player argument defaulting to the sender. | spigot, paper |
| `/feed [-s] <targets...>`, `/notes add\|list [page]` | The Kotlin DSL: delegated flags, typed references, optional arguments. | paper-kotlin |
| `/send <player> <server>`, `/find <players...>` | Velocity's proxy `PlayerArgument` and `ServerArgument`. | velocity |
| `/<sample>-diagnostics` | `CadenceDiagnosticsCommand`: the platform, the relocated package and the registrations. | all |

## Notes

- The sample commands are intentionally small and are meant to show public API usage rather than complete plugin
  behavior. Behavioral coverage belongs in the test modules.
- Prefer literal-first command shapes for ordinary examples, such as `/config set <value>`, unless the sample
  specifically demonstrates an argument-first tree.
