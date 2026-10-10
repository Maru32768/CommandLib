# Integration Test

## Purpose

This module automates the manual Test Plugin/MOD workflow.
The main goal is to verify that commands using each `Argument` can be registered, parsed, and executed on a real
Minecraft server.

This is intentionally not a normal unit test.
CommandLib depends on each platform, Brigadier, and NMS behavior that is only reliable inside an actual server runtime.

Fast tests remain in the module-level test projects such as `spigot/src/test`, `spigot-testing`, and `paper-testing`.
Use this module only for cases that need a real server, platform runtime internals, command registration, or a real
player connection.

## Flow

Each `minecraftIntegrationTest*` task does the following:

1. Builds the target fixture plugin or mod.
2. Downloads or prepares the matching server jar and server-side helper artifacts.
3. Starts the server in Docker through Testcontainers.
4. Connects a fake player named `Maru32768` through MCProtocolLib.
5. Lets the fixture execute its command test suite in game.
6. Waits for the fixture to write a JUnit XML report.
7. Fails the Gradle test if the JUnit XML report contains failures or errors.

The report is written under:

```text
integration-test/shared/test-results/
```

## Why MCProtocolLib

The fixture cases are command execution tests, not just parser unit tests.
A real player connection exercises the server command path more closely than a mocked sender.

MCProtocolLib is used because it can connect to the server without launching a full Minecraft client.
Different Minecraft versions require different MCProtocolLib artifacts, so Gradle keeps each protocol client on its own
configuration.

## Why Docker

The server process needs version-specific Minecraft and platform server classes.
Running it in Docker keeps the Java runtime and server process isolated from the Gradle test JVM.

The integration test intentionally fails when Docker is unavailable.
These tasks are opt-in and are expected to validate a real server, so a missing Docker daemon should be treated as an
infrastructure failure rather than a skipped test.

## Layout

Each target owns its fixture project:

```text
integration-test/targets/{platform}-{version}/test-plugin/
```

Bukkit-compatible targets build their plugin in that nested `test-plugin` project. Forge targets have no nested
project; see [Forge Targets](#forge-targets).

Shared fixture code is split by responsibility:

```text
integration-test/shared/core
integration-test/shared/spigot
integration-test/shared/paper
integration-test/shared/modded
```

`shared/core` is limited to platform-neutral result and report helpers so the Forge test mod can reuse it
without inheriting Bukkit lifecycle or command-dispatch assumptions. Spigot and Paper cases are intentionally separate
even when they currently look similar.

The shared Gradle script for current Bukkit-compatible plugin fixtures is:

```text
integration-test/shared/integration-test.gradle.kts
```

Current Bukkit-compatible target setup downloads PlugManX and AutoReloader into each configured server `plugins`
directory.
The AutoReloader URL can be overridden when needed:

```powershell
.\gradlew.bat -p integration-test/targets/paper-1.20.4/test-plugin downloadPluginJars -PautoReloaderJarDownloadUrl=<download-url>
```

The same URL can be supplied with `COMMANDLIB_AUTORELOADER_JAR_URL`.

## Targets

Each version has a prepare task and an integration test task.
For example:

```text
:integration-test:targets:paper-1.20.4:prepareTestPlugin
:integration-test:targets:paper-1.20.4:minecraftIntegrationTest
```

Bukkit-family targets: `paper-1.16.5`, `paper-1.17.1`, `paper-1.18`, `paper-1.18.1`, `paper-1.18.2`, `paper-1.19`,
`paper-1.19.1`, `paper-1.19.2`, `paper-1.19.3`, `paper-1.19.4`, `paper-1.20`, `paper-1.20.1`, `paper-1.20.2`,
`paper-1.20.4`, `paper-1.20.5`, `paper-1.20.6`, `mohist-1.16.5`, `mohist-1.20.1`, `spigot-1.20.4`, `spigot-1.20.5`,
`spigot-1.20.6`, `spigot-1.21` and `spigot-1.21.1` run the `spigot` module, and `paper-1.21.0` runs the `paper` module. Minecraft 1.17 has no target,
because no MCProtocolLib release speaks its protocol.

Spigot targets (`spigot-<version>`) run the `spigot` module on a plain Spigot server. Spigot server jars are only
produced by BuildTools, so the target's `buildSpigotServer` task runs it (through the same BuildTools setup as the typed
NMS modules, see `docs/agents/nms-build.md`) and copies the jar to `test-plugin/server/server.jar`. BuildTools resolves
1.20.5 and 1.21 to the build of the following hotfix release, so those targets set the Spigot build number of the last
build of their release in `commandlib.integration.buildToolsRevision`. BuildTools also installs the remapped
artifacts only for a version that has a typed NMS module. The first run of a version takes several minutes.

`minecraftIntegrationTest` is the aggregate task for all configured targets.
The aggregate task depends on per-target subprojects, so Gradle can run independent targets in parallel by default.

## Forge Targets

Forge targets (`forge-1.16.5`, `forge-1.20.1`) and the NeoForge target (`neoforge-1.21.1`) reuse the same runner, Docker image flow, and MCProtocolLib bot, but
prepare the server differently:

- The test mod lives in `integration-test/shared/modded` and is built in the main Gradle build as a Stonecutter tree
  (`:integration-test:shared:modded-fixture:<loader>-<minecraft-version>`, for example `forge-1.20.1` or
  `neoforge-1.21.1`), like the `modded` module. Conditional comments follow
  `docs/agents/modded.md`.
- `testModJar` bundles the published `:modded:<node>` jar (SRG-remapped for Forge, Mojang names for NeoForge) together with `common` and `shared/core`.
  The tests therefore run the same artifact downstream mods use, which catches bugs that only appear with production
  names.
- `integration-test/gradle/forge-integration-target.gradle.kts` registers `prepareTestPlugin`. It downloads the Forge
  or NeoForge installer (selected by `commandlib.integration.platform`), installs the server into
  `targets/<target>/work/server`, and copies the test mod into `mods`. The NeoForge test mod ships
  `META-INF/neoforge.mods.toml` instead of `mods.toml`.
- The loader build is read from `modded/versions/<node>/gradle.properties`, so the server runs the build the
  CommandLib jars are compiled against. The server is started with arguments derived from it (an
  `@libraries/.../unix_args.txt` file for 1.17+ and NeoForge, `-jar forge-<version>.jar` for 1.16.5).
- Test commands are performed through `Commands#performPrefixedCommand`, like typed commands. On 1.20.3+ commands
  performed during another command are queued, so the report is written by a `reportTests` command queued after
  them.

Test cases run from the server console source while the bot is online, and cover every public Forge argument
(enforced by `ArgumentIntegrationCoverageTest`) plus `CommandActor` behavior.

Notes:

- The 1.16.5 target runs Forge 36.2.42, the recommended build, which the library also compiles against. Older 36.2.x
  builds such as 36.2.20 crash on current Java 11 updates with `NoSuchMethodError` in `ManifestEntryVerifier`.
- NeoForge pings a connecting client during the configuration phase and waits for the pong. MCProtocolLib does not
  answer pings, so the bot replies to `ClientboundPingPacket` itself.
- From Minecraft 1.20.3, a command performed while another command runs is queued until that command finishes. The
  test mod therefore executes the test commands through Brigadier directly on 1.20.3+, so their results exist when
  `runTests` collects them.
- MCProtocolLib 1.16.5 cannot decode `brigadier:long`, so the `LongArgument` case is not registered on 1.16.5, the
  same as the Spigot fixture.

## Typed NMS Jars

Targets that run the `spigot` module bundle the typed NMS jars that `:spigot:collectTypedNms` writes to
`spigot/build/typed-nms`, as the published spigot jar does. `prepareTestPlugin` depends on that task, so the first run
of a Bukkit-family target may run BuildTools (see `docs/agents/nms-build.md`).

The `spigot` module's NMS resolver test loads the Spigot 1.16.5 server jar that BuildTools installs into the local Maven
repository:

```text
~/.m2/repository/org/spigotmc/spigot/1.16.5-R0.1-SNAPSHOT/spigot-1.16.5-R0.1-SNAPSHOT.jar
```

`COMMANDLIB_NMS_TEST_JAR_1_16_5` can override that path. The test is skipped when the jar is missing.

## Mohist Constraint

Mohist targets require a first startup before the integration test server is launched in Docker.
That startup generates Mohist libraries, mappings, and server-side files in the target fixture's `server` directory.

Gradle handles this with per-target `bootstrapMohist` tasks.
The task checks for generated marker paths such as `libraries` and `world`.
If they are missing, it starts Mohist once with the target Java toolchain, waits until the server reaches the ready
state, sends `stop`, and then lets the Docker-based integration test continue.
The bootstrap startup leaves Mohist library checks enabled; the Docker test startup disables those checks after
bootstrap
so repeated test runs avoid unnecessary Mohist library validation.

Example:

```powershell
.\gradlew.bat :integration-test:targets:mohist-1.20.1:prepareTestPlugin
.\gradlew.bat :integration-test:targets:mohist-1.20.1:bootstrapMohist
```

The versioned integration test task depends on this bootstrap task, so manual execution is only needed when diagnosing
Mohist startup itself.

## Running

Run one target:

```powershell
.\gradlew.bat :integration-test:targets:paper-1.20.4:minecraftIntegrationTest
```

Run all configured targets:

```powershell
.\gradlew.bat :integration-test:minecraftIntegrationTest
```

Each target builds a reusable Docker image for its server base before running the Minecraft integration test. The image
contains the server jar, generated libraries, helper artifacts, base configuration, and pre-generated overworld data. The
current fixture artifact is copied into the container at test startup, so normal code changes do not require recopying the
whole server directory. Gradle marks the image task up-to-date while the staged server base is unchanged.

Gradle project parallelism is enabled by default, but Minecraft server tests are throttled to avoid starting every
Docker server at once. The default server-test concurrency is 4.

For smaller Docker Desktop environments, reduce it:

```powershell
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.minecraftIntegrationMaxParallel=1
```

Mohist bootstrap is throttled separately and defaults to 4. Each Mohist target writes a target-specific bootstrap
`server-port`, so bootstrap tasks can run in parallel when the local machine has enough CPU and memory.

To reduce Mohist bootstrap concurrency:

```powershell
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.mohistBootstrapMaxParallel=1
```

Each Dockerized Minecraft server is also CPU-limited by default so one server startup cannot consume the whole Docker
Desktop CPU budget. The default is 2 CPUs per container.

To tune or disable the limit:

```powershell
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.containerCpuLimit=1.5
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.containerCpuLimit=0
```

The normal `test` task runs lightweight integration coverage checks only. Real server tests run from the target
subprojects' `minecraftIntegrationTest` tasks.

## Caveats

- Docker availability depends on the local Docker Desktop environment.
- Docker-based runs can be environment-sensitive even when normal `test` passes.
- MCProtocolLib compatibility should be validated on the real target environment when adding or upgrading server
  versions.
- Mohist startup is intentionally split into `bootstrapMohist` because Mohist generates libraries, mappings, and
  server-side files during startup.
