# CommandLib Sample Projects

This directory contains small, buildable sample projects for each supported
platform.

- [Spigot](./spigot)
- [Paper](./paper)
- [Forge](./forge)

When the Spigot and Paper samples are built from this repository, Gradle uses
`includeBuild` and dependency substitution so the samples compile against the
local CommandLib source. When copied outside the repository, they resolve
`com.github.Maru32768.CommandLib:*:latest.release` from the configured
repositories.

The Forge sample uses ForgeGradle 6, which does not run on the Gradle 9 that
the repository build needs, so it cannot include the repository build. To
build it against the local source, publish the artifacts it uses at the
repository root and pass `-PcommandlibLocal=true`:

```bash
./gradlew -Pcommandlib.typedNms=false :common:publishToMavenLocal :modded:forge-1.16.5:publishToMavenLocal
cd samples/forge
./gradlew build -PcommandlibLocal=true
```

Without the property it resolves the jitpack release like a downstream mod.

## Commands

Each platform sample registers the same set of example commands.

| Command                                                      | Demonstrates                                                                             |
|--------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `/ping`                                                      | Minimal command with no arguments.                                                       |
| `/hello <players>`                                           | Platform player argument and typed argument execution.                                   |
| `/config get`                                                | Literal child command.                                                                   |
| `/config set <number>`                                       | Literal child command with an integer argument.                                          |
| `/admincheck`                                                | Command-level permission metadata.                                                       |
| `/topic <name>`                                              | Custom argument suggestions.                                                             |
| `/broadcast [-s\|--silent] [-r\|--repeat <count>] <message>` | Command options, flag options, value options, short names, long names, and option reads. |
| `/preprocesscommand`                                         | Preprocess checks before command execution.                                              |
| `/preprocesscommand sub`                                     | Disabling inherited preprocess behavior on a child command.                              |
| `/increment <target>`                                        | Variable-length command shape with a default count of 1.                                 |
| `/increment <target> <count>`                                | Variable-length command shape with an explicit count.                                    |
| `/math add <left> <right>`                                   | Multi-argument execution.                                                                |
| `/math multiply <left> <right>`                              | Multi-argument execution with a child command shape.                                     |

## Notes

- The sample commands are intentionally small and are meant to show public API
  usage rather than complete plugin behavior.
- Keep examples focused and buildable. Behavioral coverage belongs in the test
  modules.
- Prefer literal-first command shapes for ordinary examples, such as
  `/config set <value>`, unless the sample specifically demonstrates an
  argument-first tree.
