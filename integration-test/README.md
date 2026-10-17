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
6. Waits for the fixture to write a complete JUnit XML report.
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

Bukkit-compatible targets build their plugin in that nested `test-plugin` project. Forge, NeoForge and Fabric targets
have no nested project; see [Forge, NeoForge And Fabric Targets](#forge-neoforge-and-fabric-targets).

Shared fixture code is split by responsibility:

```text
integration-test/shared/core
integration-test/shared/spigot
integration-test/shared/paper
integration-test/shared/folia
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

Bukkit-family targets run the `spigot` module, except the `paper-<version>` targets from 1.21, which run the `paper`
module:

- `spigot-<version>`: every Spigot release from 1.16.5 to 26.3 except 1.17.
- `paper-<version>` with the `spigot` module: 1.16.5, 1.17.1, 1.18 to 1.20.2, 1.20.4 to 1.20.6 and 1.21.1.
- `paper-<version>` with the `paper` module: `paper-1.21.0` (Minecraft 1.21), 1.21.3 to 1.21.11 and 26.1.1 to 26.3.
  Paper has no 1.20.3, 1.21.2 or 26.1 build. `paper-1.21.4` and later cover the server internals that changed in
  1.21.2 and 1.21.9, such as the parameters of `ServerFunctionLibrary#reload`, which the paper module reads by
  reflection.
- `mohist-1.16.5` and `mohist-1.20.1`.
- `folia-26.3`: the `paper` module on Folia 26.3 (build 3), with the smoke fixture in `shared/folia` instead of the
  Paper suite, which schedules with the Bukkit scheduler that Folia refuses. It checks that the commands register, that
  the bot gets the messages of the bot-side checks every target runs, that `executeAsync` runs its work off the thread
  that owns the sender and its continuation on the player's region thread for the bot and on the global region thread
  for the console, and that the suggestions of a CommandLib argument are sent to the bot. Command blocks,
  `/execute as` and the `spigot` module's refusal of Folia are not covered. Folia rejects the only player as "The
  server is full!" when `max-players` is 1, so this target sets it to 2.
- `paper-26.3-spigot-jar`: the `spigot` module on Paper 26.3, which it does not support. The target sets
  `commandlib.integration.expectedEnableFailure`, so it starts no bot and passes when the plugin fails to enable with
  that text, which names the paper artifact.
- `velocity-3.4.0`: the `velocity` module in a plugin on a Velocity 3.4.0 proxy (build 566), in front of a Paper 1.21.4
  backend. `VelocityIntegrationTest` runs it instead of `IntegrationTest`, which the target selects with
  `commandlib.integration.testClass`. The target compiles the plugin from its `testPlugin` source set with `velocity`
  and `common` inside it. Its image holds the backend in `work/server` and the proxy in `work/server/proxy`. The test
  starts one container of each on a Docker network, and the bot (MCProtocolLib 1.21.4) joins through the proxy. The
  smoke test checks that the command tree the bot receives has CommandLib's nodes and leaves out a
  `DefaultPermission.OP` node, and that `ServerArgument` and `PlayerArgument` suggestions reach the bot. It checks that
  the bot receives the messages of a child command with an argument, of a `ServerArgument` and a `PlayerArgument`
  command, of a command for each other argument type (`ArgumentIntegrationCoverageTest` keeps the list complete), of an
  unknown server, of a `requireConsole` command and of an `executeAsync` command. It also checks that the `OP` command
  is refused and does not run: Velocity has no operators, and no permission plugin is installed. The plugin runs that
  command as the console at startup, so the test knows it is registered. The proxy log must not contain an exception.
  Options, permission plugins and Velocity 4.x are not covered.

From 26.1 the servers need Java 25, and from 26.2 MCProtocolLib needs Java 21 and Adventure 5, so those targets set
`commandlib.integration.botJavaVersion` to 21. A bot that brings adventure-api 5 goes before the test's own
Adventure 4 on the test classpath.

Minecraft 1.17 has no target: no MCProtocolLib release speaks its protocol (755), and a 1.17 server rejects the 1.17.1
bot (756). The `nms:spigot-1.17` module is only checked by its compilation (see `docs/agents/nms-build.md`).

Spigot targets (`spigot-<version>`) run the `spigot` module on a plain Spigot server. Spigot server jars are only
produced by BuildTools, so the target's `buildSpigotServer` task runs it (through the same BuildTools setup as the typed
NMS modules, see `docs/agents/nms-build.md`) and copies the jar to `test-plugin/server/server.jar`. BuildTools resolves
1.20, 1.20.3, 1.20.5, 1.21, 1.21.2, 1.21.7, 1.21.9, 26.1 and 26.1.1 to the build of the following hotfix release, so those
targets set the Spigot build number of the last build of their release in `commandlib.integration.buildToolsRevision`. BuildTools also installs the remapped
artifacts only for a version that has a typed NMS module. The first run of a version takes several minutes.

`minecraftIntegrationTest` is the aggregate task for all configured targets.
The aggregate task depends on per-target subprojects, so Gradle can run independent targets in parallel by default.

## Forge, NeoForge And Fabric Targets

Forge targets (`forge-1.16.5`, `forge-1.18.2`, `forge-1.19.2`, `forge-1.20.1`), the NeoForge targets
(`neoforge-1.21.1`, `neoforge-1.21.4`) and the Fabric targets (`fabric-1.20.1`, `fabric-1.21.1`) reuse the same runner,
Docker image flow, and MCProtocolLib bot, but prepare the server differently:

- The test mod lives in `integration-test/shared/modded` and is built in the main Gradle build as a Stonecutter tree
  (`:integration-test:shared:modded-fixture:<loader>-<minecraft-version>`, for example `forge-1.20.1`,
  `neoforge-1.21.1` or `fabric-1.21.1`), like the `modded` module. Conditional comments follow
  `docs/agents/modded.md`.
- `testModJar` bundles the published `:modded:<node>` jar (SRG-remapped for Forge, Mojang names for NeoForge) together with `common` and `shared/core`.
  On Fabric it nests the published, intermediary-remapped `:modded:<node>` jar and `common` as jar-in-jar (Loom
  `include`), so Fabric Loader loads CommandLib as its own mod with its `fabric.mod.json` and entrypoint, and merges
  only `shared/core`. The Fabric test mod's entrypoint is a `ModInitializer` declared in its `fabric.mod.json`.
  The tests therefore run the same artifact downstream mods use, which catches bugs that only appear with production
  names.
- `integration-test/gradle/forge-integration-target.gradle.kts` registers `prepareTestPlugin`. It downloads the Forge,
  NeoForge or Fabric installer (selected by `commandlib.integration.platform`), installs the server into
  `targets/<target>/work/server`, and copies the test mod into `mods`. The NeoForge test mod ships
  `META-INF/neoforge.mods.toml` instead of `mods.toml`. On Fabric the official Fabric installer installs Fabric Loader
  and downloads the vanilla server jar, and the Fabric API jar of the node (`deps.fabricApi`, from maven.fabricmc.net)
  is copied into `mods` next to the test mod.
- The loader build is read from `modded/versions/<node>/gradle.properties`, so the server runs the build the
  CommandLib jars are compiled against. The server is started with arguments derived from it (an
  `@libraries/.../unix_args.txt` file for 1.17+ and NeoForge, `-jar forge-<version>.jar` for 1.16.5,
  `-jar fabric-server-launch.jar` for Fabric).
- Test commands are performed through `Commands#performPrefixedCommand`, like typed commands. On 1.20.3+ commands
  performed during another command are queued, so the report is written by a `reportTests` command queued after
  them. It writes the report two seconds later, after the asynchronous commands have completed.

Test cases run from the server console source while the bot is online, and cover every public Forge argument
(enforced by `ArgumentIntegrationCoverageTest`) plus `CommandActor` behavior, options, `visibleWhen`, `executeAsync`
with a middleware, the diagnostics, a command after `/reload` and a sub command that no player may use. The Fabric
targets run the same suite on Fabric Loader 0.16.14 with Fabric API, so they check registration through
`CommandRegistrationCallback`, the commands after `/reload`, and the production (intermediary) names, such as the raw
command source that `unwrap` reads by reflection. They do not cover RCON, fabric-permissions-api (no
permission mod is installed), or CommandLib shaded into a mod instead of nested.

Notes:

- The 1.16.5 target runs Forge 36.2.42, the recommended build, which the library also compiles against. Older 36.2.x
  builds such as 36.2.20 crash on current Java 11 updates with `NoSuchMethodError` in `ManifestEntryVerifier`.
- NeoForge pings a connecting client during the configuration phase and waits for the pong. MCProtocolLib does not
  answer pings, so the bot replies to `ClientboundPingPacket` itself.
- Fabric API on 1.20.1 sends login queries and waits for the answers. MCProtocolLib does not answer them, so the bot
  answers each `ClientboundCustomQueryPacket` as not understood, as a vanilla client does.
- MCProtocolLib 1.16.5 cannot decode `brigadier:long`, so the `LongArgument` case is not registered on 1.16.5, the
  same as the Spigot fixture.

## Vanilla Command Integration

The Bukkit-family fixtures (`VanillaExecutionTest`, `DataPackFunctionTest`) run commands the way vanilla content
does: through `/execute as|at|positioned|rotated ... run`, from two command blocks placed over the world spawn and
powered by redstone blocks (`server.properties` enables command blocks), from a data pack function, from a function tag
and from `#minecraft:load`. Command blocks and `#minecraft:load` run on a later tick, so their checks are dispatched after
`/minecraft:reload`, through `TestBase#afterReload`, once every test reports through `TestBase#readyAfterReload` that
what it checks has run, or after 30 seconds. The Forge and NeoForge
fixture checks `/execute as` and `/execute positioned ... rotated` in `ActorTest`. The verified behavior is described in
[`docs/vanilla-integration.md`](../docs/vanilla-integration.md).

## Typed NMS Jars

Targets that run the `spigot` module bundle the typed NMS jars that `:spigot:collectTypedNms` writes to
`spigot/build/typed-nms`, as the published spigot jar does. They are the only NMS implementations, so
`prepareTestPlugin` depends on that task, and the first run of a Bukkit-family target may run BuildTools (see
`docs/agents/nms-build.md`). The Spigot suite's `TypedNmsTest` checks that every typed class for the server version
links.

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

Mohist 1.16.5 cannot start with the library check: it puts the Guava 20 it checks before the Guava 21 of the
Minecraft server, which fails in `GameRules` with `NoSuchMethodError` (`ImmutableMap.toImmutableMap`). Its target sets
`commandlib.integration.mohistInstallArgs` to `-noserver`, so the bootstrap first runs Mohist with the check and that
argument, which installs the libraries and exits, and then starts the server without the check, as the Docker run does.

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
whole server directory. The image is tagged `commandlib-it-<target>:<hash>` with a hash of the staged server base, and
the image task is skipped while Docker has that tag. Checkouts and worktrees with the same server base therefore share
one image, and different server bases never replace each other's image. Images of older server bases stay in Docker
until removed, for example with `docker image rm` or `docker image prune -a`.

The image build starts the server once (without the fixture) and stops it after `Done`. That run generates the world
and, on Paper, downloads and patches the server jar, remaps it, and downloads plugin libraries, so a test run no longer
repeats that work or depends on those downloads. Docker caches the step until the staged server base
changes; rebuilding the image therefore takes about one extra server start.

The nested `test-plugin` builds use the same Gradle user home as the outer build, so they reuse its Gradle
distributions, dependencies and toolchains. Pass `-Pcommandlib.integrationGradleUserHome=<dir>` to give them a separate
one.

Gradle project parallelism is enabled by default, but Minecraft server tests are throttled to avoid starting every
Docker server at once. The default server-test concurrency is 4.

The server JVM sets no `-Xmx`, so its heap limit is the JVM default of a quarter of the Docker VM memory (about 1.9 GiB
in an 8 GiB VM). Containers used about 1.1 GiB each after startup in measurements on paper-1.21.0 and forge-1.20.1, so
four servers at once need roughly 4.5 GiB of Docker memory. A fixed lower limit was not added because no modded target
was measured under heavy load.

For smaller Docker Desktop environments, reduce the concurrency:

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
