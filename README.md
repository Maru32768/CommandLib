# CommandLib - A Feature-Rich Minecraft Command API for Spigot, Paper, Velocity, Forge, NeoForge and Fabric

[![](https://jitpack.io/v/Maru32768/CommandLib.svg)](https://jitpack.io/#Maru32768/CommandLib)

CommandLib is an advanced, type-safe Command API for Spigot, Paper, Velocity, Forge, NeoForge and Fabric developers.
It abstracts and enhances the Minecraft 1.16.5+ command APIs, simplifying command implementation. With features like
customizable argument suggestions, seamless validation, improved usability, and extensibility, CommandLib empowers
developers to build better commands with ease.

#### Supported Versions

| Platform   | Artifact | Tested Versions                                            | Notes                                                         |
|------------|----------|------------------------------------------------------------|---------------------------------------------------------------|
| **Spigot** | `spigot` | `1.16.5`, `1.17.1`, `1.18`–`1.18.2`, `1.19`–`1.19.4`, `1.20`–`1.20.6`, `1.21`–`1.21.11`, `26.1`–`26.3` | Each release in the range is tested on Spigot, and `1.16.5`–`1.21.1` also on Paper. Requires Java 11+. |
| **Paper**  | `paper`  | `1.21`, `1.21.3`–`1.21.11`, `26.1.1`–`26.3` | Uses Paper official command/lifecycle API. Requires Java 21+. Use `spigot` on Paper `1.20.6` and earlier. |
| **Forge**  | `forge-<minecraft-version>` | `1.16.5`, `1.18.2`, `1.19.2`, `1.20.1` | Use the artifact that matches the target Minecraft version.   |
| **NeoForge** | `neoforge-<minecraft-version>` | `1.21.1`, `1.21.4` | Use the artifact that matches the target Minecraft version. Requires Java 21+. |
| **Fabric** | `fabric-<minecraft-version>` | `1.20.1`, `1.21.1`, `1.21.4` (`1.21.4` built and unit-tested; not yet run on a real server) | Use the artifact that matches the target Minecraft version. Requires Fabric API. |
| **Mohist** | `spigot` | `1.16.5`, `1.20.1`                                         | Works on Mohist since it's compatible with Spigot.            |
| **Folia**  | `paper`  | `26.3` (smoke test: registration, player commands, async threads, suggestions) | Detected at runtime; async commands return to the entity, region or global region thread. Declare `folia-supported: true`. See [Folia](docs/execution.md#folia). |
| **Velocity** | `velocity` | `3.4.0` (smoke test with a client through the proxy: command tree, execution, failures, permission, suggestions, async), `4.2.0` (manual proxy run, no client) | Built against Velocity API `3.4.0`. Requires Java 17+. Proxy arguments only; see [Velocity](#velocity). |

## Features

1. **Type-Safe Arguments Handling**  
   Use arguments in a type-safe manner directly within your code, reducing potential runtime errors and improving
   maintainability.
2. **No Need for Brigadier and NMS Dependency**  
   Simplify your setup and ensure compatibility across multiple Minecraft versions.
3. **Powerful Suggestion Generation**  
   Automatically generate argument suggestions with customizable options for enhanced flexibility.
4. **Typed Command Options**  
   Define command options such as `-f`, `--force`, or `--limit 10` and read them from the command context in a
   type-safe way.
5. **Automatic Usage and Help Generation**  
   Generate usage and help messages from the command tree, including arguments, subcommands, options, descriptions,
   and examples, and replace how the help, the usage lines and the errors look with formatters
   ([docs](docs/help.md)).
6. **Seamless Integration with the `/execute` command**   
   Allow your commands to be executed seamlessly from the `/execute` command, just like built-in commands.
7. **Documentation Exports and Diagnostics**  
   Export the commands and every generated permission node as Markdown or JSON, and inspect the registrations and
   the platform at runtime ([docs](docs/diagnostics.md)).
8. **Middleware and Async Execution**  
   Wrap executions with typed middlewares, and run heavy work off the server thread with `executeAsync`, coming back
   to it for messages and the platform API.
9. **Kotlin DSL**  
   Build the same commands with `command("heal") { ... }` blocks, typed argument references, options as delegated
   properties, and `suspend` executors and suggestion providers ([docs](docs/kotlin.md)).
10. **Annotated Commands**  
    Write a command as a class with `@Command`, `@Subcommand`, `@Flag` and `@Option` methods; it builds the same
    command as the fluent API, with the same help, permissions and tests ([docs](docs/annotations.md)).
11. **Localized Messages**  
    CommandLib's own messages are captions sent in each sender's locale, in English and Japanese out of the box, and a
    plugin can replace them per key and locale or load its own properties files ([docs](docs/localization.md)).
12. **Sender Mapping and Context Values**  
    Map the sender to your own type once, attach values from middlewares, and give commands their services; executors,
    annotated methods and the Kotlin DSL read them by type without casts ([docs](docs/context-values.md)).

## Why CommandLib

- **One command model for Spigot, Paper, Velocity, Forge, NeoForge, and Fabric.** The same `Command` / `argument(...)`
  code compiles against each platform artifact: Spigot from Minecraft 1.16.5 to 26.3, Paper from 1.21, Velocity 3.4+,
  Forge 1.16.5, 1.18.2, 1.19.2 and 1.20.1, NeoForge 1.21.1 and 1.21.4, and Fabric 1.20.1, 1.21.1 and 1.21.4 (see
  [Supported Versions](#supported-versions)).
- **Typed values in the executor.** `argument(new PlayerArgument("target"), new IntegerArgument("amount"))` hands a
  `Player` and an `Integer` to the lambda: no lookup by name, no casts.
- **Brigadier-native.** Commands are real Brigadier nodes, so they get client-side validation, vanilla selectors,
  `/execute` and data pack function support, and per-player command trees.
- **Permissions without boilerplate.** Every command, subcommand, and argument branch gets a permission node, which
  also hides what a sender cannot use. On Spigot, Paper, Forge, NeoForge and Fabric the node has a default. Velocity
  has no permission defaults, so a player gets a node other than `DefaultPermission.ALL` only from a permission plugin.
  `visibleWhen(ctx -> !game.isRunning())` hides a command or branch by plugin state as well
  ([permissions](docs/permissions.md#refreshing-the-command-tree)).
- **Options, generated help, and test utilities built in.**

| | CommandLib | CommandAPI | Paper command API |
|---|---|---|---|
| Platforms | Spigot, Paper, Forge, NeoForge, Fabric, Velocity | Spigot, Paper (others: see its docs) | Paper |
| Executor arguments | typed parameters | `args.get("name")` + cast | `ctx.getArgument("name", Type.class)` |
| Generated permission nodes | every command and branch | manual | manual |
| Flags and options (`-f`, `--limit 10`) | built in | — | — |
| Generated usage / help | built in | `/help` entries | `/help` entries |
| Server-free test utilities | `spigot-testing`, `paper-testing`, `velocity-testing`, `forge-` / `neoforge-` / `fabric-testing` | test toolkit (MockBukkit) | — |
| Chat / NBT arguments | text component, message, NBT compound (SNBT string on Bukkit) | yes | component, signed message; no NBT |

See the [comparison](docs/comparison.md) for seven use cases written with CommandLib, CommandAPI, Cloud, Lamp, ACF
and Paper's command API, including where the others are better, the [argument catalog](docs/arguments.md) for an
argument-by-argument comparison and [compatibility](docs/compatibility.md) for the tested server versions.

## Documentation

- [Arguments](docs/arguments.md): built-in arguments, comparison with CommandAPI and Paper, and gaps.
- [Suggestions](docs/suggestions.md): default, custom, asynchronous, and tooltip suggestions.
- [Permissions](docs/permissions.md): node names, defaults per platform, registration, and LuckPerms.
- [Errors](docs/errors.md): what the sender sees when a command fails.
- [Help, usage and error formatting](docs/help.md): the generated help, examples, and the help, usage and error
  formatters.
- [Localization](docs/localization.md): captions, the sender's locale, translations, and caption formatting.
- [Vanilla integration](docs/vanilla-integration.md): `/execute`, command blocks, functions, and tags.
- [Execution](docs/execution.md): middlewares, async executors, threads, and `/execute store` for async commands.
- [Annotated commands](docs/annotations.md): commands as annotated classes, and how they map to the fluent API.
- [Context values](docs/context-values.md): sender mapping, values from middlewares, and dependencies.
- [Testing](docs/testing.md): `CommandTester` and `FakeSender` for plugin and mod tests.
- [Kotlin DSL](docs/kotlin.md): `command { }` blocks, delegated options, and coroutines.
- [Java builder](docs/java-builder.md): `Command.builder(name)` chains, and branches of eight or more arguments.
- [Documentation exports and diagnostics](docs/diagnostics.md): Markdown / JSON of the commands and permission nodes,
  and a runtime report of the platform and the registrations.
- [Troubleshooting](docs/troubleshooting.md).
- [Javadoc](docs/javadoc.md): where to browse the Javadoc of each module.
- [Comparison](docs/comparison.md): seven use cases in CommandLib, CommandAPI, Cloud, Lamp, ACF and Paper, with the
  CommandLib side compiled and tested in [`examples`](examples).
- [Migrating from CommandAPI](docs/migration/from-commandapi.md).
- [Migrating from Cloud](docs/migration/from-cloud.md).
- [Migrating from Lamp](docs/migration/from-lamp.md).
- [Migrating from ACF](docs/migration/from-acf.md).
- [Compatibility](docs/compatibility.md): tested platforms and versions.
- [Packaging](docs/packaging.md): shading and relocating CommandLib per platform, Jar-in-Jar, and the artifacts.
- [Sample projects](samples): buildable plugins and mods for each platform.

## Installation

To ensure stability, we recommend replacing `latest.release` with a specific version such as `0.18.0`.  
You can find the latest version on the [releases page](https://github.com/Maru32768/CommandLib/releases).

Shade CommandLib into your plugin or mod and relocate `dev.marulabs.commandlib` on every platform, as below. See
[packaging](docs/packaging.md) for the reasons, Maven Shade, Kotlin, and why Jar-in-Jar and Fabric's `include()` are
not recommended while CommandLib is 0.x. The [sample projects](samples) build this way.

<details>
<summary>Spigot (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "9.4.1"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Maru32768.CommandLib:spigot:latest.release"
}

shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}.jar"
    // Avoid package conflicts
    relocate "dev.marulabs.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
}
tasks.build.dependsOn tasks.shadowJar
```

</details>

<details>
<summary>Spigot (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.CommandLib:spigot:latest.release")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

</details>

<details>
<summary>Paper (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "9.4.1"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Maru32768.CommandLib:paper:latest.release"
}

shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}.jar"
    // Avoid package conflicts
    relocate "dev.marulabs.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
}
tasks.build.dependsOn tasks.shadowJar
```

> **Note:** The Paper artifact targets Paper 1.21.0+.

</details>

<details>
<summary>Paper (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.CommandLib:paper:latest.release")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

> **Note:** The Paper artifact targets Paper 1.21.0+.

</details>

<details>
<summary>Forge (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "9.4.1"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Maru32768.CommandLib:forge-1.20.1:latest.release"
}

shadowJar {
    dependencies {
        // The platform artifact and the common artifact it depends on
        include(dependency("com.github.Maru32768.CommandLib:.*:.*"))
    }
    // Avoid package conflicts
    relocate "dev.marulabs.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
    finalizedBy("reobfShadowJar")
}

reobf {
    shadowJar {
    }
}
```

</details>

<details>
<summary>Forge (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.CommandLib:forge-1.20.1:latest.release")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        // The platform artifact and the common artifact it depends on
        include(dependency("com.github.Maru32768.CommandLib:.*:.*"))
    }
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
    finalizedBy("reobfShadowJar")
}

@Suppress("UNCHECKED_CAST")
(extensions.getByName("reobf") as NamedDomainObjectContainer<Any>).create("shadowJar")
```

</details>

<details>
<summary>NeoForge (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Maru32768.CommandLib:neoforge-1.21.1:latest.release")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    dependencies {
        // The platform artifact and the common artifact it depends on
        include(dependency("com.github.Maru32768.CommandLib:.*:.*"))
    }
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
```

> **Note:** NeoForge runs on Mojang names, so the shadow jar does not need a reobf step.

</details>

<details>
<summary>Fabric (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Fabric API is required.
    modImplementation("com.github.Maru32768.CommandLib:fabric-1.21.1:latest.release")
    // Optional: lets permission mods such as LuckPerms answer CommandLib's permission checks.
    modImplementation("me.lucko:fabric-permissions-api:0.3.3")
    include("me.lucko:fabric-permissions-api:0.3.3")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-shadow")
    dependencies {
        // The platform artifact and the common artifact it depends on. Loom puts the remapped one in the group
        // remapped.<group>.
        include { it.moduleGroup.endsWith("com.github.Maru32768.CommandLib") }
    }
    // Keep your fabric.mod.json, not CommandLib's
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
// Remap your mod and the relocated CommandLib to intermediary together
tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
```

```java
public final class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandLib.register("mymod.command", new SpawnCommand());
    }
}
```

> **Note:** Declare `fabric-command-api-v2` and `fabric-lifecycle-events-v1` (or `fabric-api`) in your
> `fabric.mod.json`. The jar is also a Fabric mod (`commandlib`) that `include()` can nest, but only the newest nested
> copy is loaded; see [packaging](docs/packaging.md#fabric). Without fabric-permissions-api, permission checks use each
> node's default and the vanilla permission level.

</details>

<details>
<summary>Velocity (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0")
    implementation("com.github.Maru32768.CommandLib:velocity:latest.release")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    // Avoid package conflicts
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

</details>

### Hiding Internal Classes In IntelliJ IDEA

CommandLib's implementation lives in `dev.marulabs.commandlib.internal` and its sub-packages, marked
`@ApiStatus.Internal`. It is not API: it changes without notice, and only the code outside it is supported. To keep it
out of completion and auto-import, open **Settings → Editor → General → Auto Import**, and under **Exclude from import
and completion** add:

```text
dev.marulabs.commandlib.internal
```

The entry covers the sub-packages too.

## Kotlin DSL

`spigot-kotlin`, `paper-kotlin`, and `velocity-kotlin` add a Kotlin DSL on top of the same command model. Use one of
them in place of the platform artifact, which they bring along:

```kotlin
dependencies {
    implementation("com.github.Maru32768.CommandLib:spigot-kotlin:latest.release")
    // Only for executeSuspend and the other coroutine functions.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
```

```kotlin
val heal = command("heal") {
    description("Heal players")
    val silent by flag("silent", 's')
    argument(IntegerArgument("amount"), PlayerArgument("targets").variadic()) { amount, targets ->
        execute { ctx ->
            targets.get().forEach { it.health = minOf(it.health + amount.get(), 20.0) }
            if (!silent) ctx.sendSuccess("Healed ${targets.get().size} players")
        }
    }
    subcommand("stats") {
        executeSuspend { ctx ->
            val count = withContext(ctx.asyncDispatcher) { database.countHeals() }
            ctx.sendSuccess("$count heals so far")
        }
    }
}
CommandLib.register(this, heal)
```

The block's receiver is the `Command`, so every fluent method (`permission`, `addPrerequisite(...)`, `cooldown`, ...) works
in it, and the tree, permissions, and help are the same as the fluent API's. See [Kotlin DSL](docs/kotlin.md).

## Java Builder

`Command.builder(name)` configures a command in one chain, without a subclass or an initializer block. `build()`
returns the same `Command` as the fluent API, with the same tree, help, and permissions:

```java
Command heal = Command.builder("heal")
                      .description("Heal a player")
                      .permission(DefaultPermission.OP)
                      .addAliases("h")
                      .addPrerequisite(Command.player())
                      .argument(new PlayerArgument("target"), new IntegerArgument("amount"),
                                branch -> branch.execute((target, amount, ctx) -> {
                                    target.setHealth(Math.min(20, target.getHealth() + amount));
                                }))
                      .addChild(Command.builder("all")
                                       .execute(ctx -> Bukkit.getOnlinePlayers().forEach(p -> p.setHealth(20))))
                      .build();
CommandLib.register(this, heal);
```

The builder has the `Command` methods under the same names; `argument(...)` and `require(...)` take a callback that
configures the branch, and `configure(...)` reaches platform methods such as `permission(PermissionDefault)`. A branch
of eight or more arguments, which the typed callbacks do not cover, reads each value with `ctx.getArgument(argument)`:
`argument(List.of(x1, y1, z1, x2, y2, z2, world, name)).execute(ctx -> ctx.getArgument(world)...)`. See
[Java builder](docs/java-builder.md).

## Testing Commands

CommandLib provides command-level test utilities for downstream plugins.

```kotlin
dependencies {
    // Pick the one line that matches your platform artifact.
    testImplementation("com.github.Maru32768.CommandLib:common-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:paper-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:velocity-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:forge-testing-1.20.1:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:neoforge-testing-1.21.1:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:fabric-testing-1.21.1:latest.release")
}
```

Use `common-testing` for commands built against the platform-neutral `common`
API, `spigot-testing` for commands built against the `spigot` artifact,
`paper-testing` for commands built against the `paper` artifact,
`velocity-testing` for commands built against the `velocity` artifact, and
`forge-testing-<minecraft-version>` / `neoforge-testing-<minecraft-version>` /
`fabric-testing-<minecraft-version>` for commands built against the matching
`forge-<minecraft-version>` / `neoforge-<minecraft-version>` /
`fabric-<minecraft-version>` artifact. They provide
`CommandTester` and `FakeSender` so commands can be executed without a running
Minecraft server. Use one testing artifact per test module; these artifacts
share package-level helper names.

```java
class Test {
    void test() {
        FakeSender sender = FakeSender.player("Steve");
        try (CommandTester tester = new CommandTester(MyCommand::new, "myplugin.command")) {
            tester.execute("mycmd arg", sender);
        }
        assertThat(sender.getSentMessageTexts()).containsExactly("expected message");
    }
}
```

Pass the command as a `Supplier` (`MyCommand::new`) so that platform argument types are created while the tester's
mocks are active. See [testing](docs/testing.md) for fake players, permissions, suggestions, and what each artifact
mocks.

`paper-testing` stubs Paper `ArgumentTypes`, selector resolvers, and several
registry-backed argument paths for command-level tests. Paper lifecycle
registration and real server registry behavior are covered by integration tests.

`forge-testing`, `neoforge-testing`, and `fabric-testing` bootstrap Minecraft's registries, so vanilla
argument types such as items, blocks, effects, and enchantments parse for real. The server is a Mockito mock:
fake players added with `CommandTester#withPlayer` (and the executing player)
are online, so player, entity, game profile, and UUID arguments resolve them by
name. Levels added with `CommandTester#withLevel` are the server's worlds; stub other server state, such as the
scoreboard or the advancements, with `CommandTester#withServer`.

## Velocity

The `velocity` artifact registers the same `Command` model on a Velocity proxy as `BrigadierCommand`s. Register from
the proxy initialize event with the plugin instance and the `ProxyServer`:

```java
@Plugin(id = "myproxy", name = "MyProxy", version = "1.0.0")
public final class MyProxyPlugin {
    private final ProxyServer proxy;

    @Inject
    public MyProxyPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        Command send = new Command("send");
        send.argument(new PlayerArgument("player"), new ServerArgument("server")).execute((player, server, ctx) -> {
            // 'player' is a com.velocitypowered.api.proxy.Player, 'server' a RegisteredServer
            player.createConnectionRequest(server)
                  .fireAndForget();
            ctx.sendSuccess("Sending " + player.getUsername() + " to " + server.getServerInfo()
                                                                              .getName());
        });
        // Permission nodes are myproxy.command.send and myproxy.command.send.player.server
        CommandLib.register(this, proxy, send);
    }
}
```

The client only knows Brigadier's own argument types through a proxy, so the Velocity artifact has the primitive,
string, duration, enum, literal, and object arguments, plus `PlayerArgument` (an online player by name) and
`ServerArgument` (a registered server by name), both suggested by the proxy. There are no world, entity, or location
arguments. The proxy has no operators, so a node whose default is not `DefaultPermission.ALL` needs a permission plugin
such as LuckPerms to grant it to players; the console has every permission. Players who are online see new commands
the next time their backend server sends the command tree, such as on a server switch.

## Code Examples

<details>
<summary>Defining Commands</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        Command message = new Command("message");
        message.argument(new PlayerArgument("target"), new StringArgument("message")).execute((target, messageText, ctx) -> {
            // 'target' is inferred as 'org.bukkit.entity.Player'. No need to cast.
            target.sendMessage(messageText);
        });
        CommandLib.register(this, message);
    }
}
```

```java
// Also you can define commands by extending Command
public final class MessageCommand extends Command {
    public MessageCommand() {
        super("message");
        argument(new PlayerArgument("target"), new StringArgument("message")).execute((target, message, ctx) -> {
            target.sendMessage(message);
        });
    }
}

public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        CommandLib.register(this, new MessageCommand());
    }
}
```

</details>

<details>
<summary>Appending Subcommands</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        Command game = new Command("game");
        Command start = new Command("start");
        start.execute(ctx -> {
            // Starts game
        });
        Command stop = new Command("stop");
        stop.execute(ctx -> {
            // Stops game
        });
        game.addChildren(start, stop);
        CommandLib.register(this, game);
    }
}
```

</details>

<details>
<summary>Appending Subcommands After Arguments</summary>

CommandLib also supports Brigadier-style command trees where an argument is followed by a literal subcommand.
Use this for vanilla-like command shapes such as `/config <key> get` or `/config <key> set <value>`.

For most plugin commands, prefer the simpler literal-first shape, such as `/config get <key>` and
`/config set <key> <value>`. Argument-first trees are useful when the target naturally comes before the action, or when
you need to match an existing command syntax.

```java
public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).description("Select a config key")
                                           .child(key -> new Command("get") {{
                                               execute(ctx -> {
                                                   String configKey = key.get();

                                                   // Get config value
                                               });
                                           }})
                                           .child(key -> new Command("set") {{
                                               argument(new StringArgument("value")).execute((value, ctx) -> {
                                                   String configKey = key.get();

                                                   // Set config value
                                               });
                                           }});
    }
}
```

The `child(...)` factory receives an `ArgRef<T>` for each parent argument. Call `key.get()` inside the child's executor
or suggestion providers to read the parsed value. In code that runs after the callback returns, such as a scheduled task,
read the value first or use `key.get(ctx)`. Argument values are kept by name, so the arguments of a child must not
repeat the name of an argument before them on the route, such as `<key>` under `<key>`; registration fails
otherwise.

Valid inputs:

```text
/config difficulty get
/config difficulty set hard
```

</details>

<details>
<summary>Defining Command Options</summary>

Command options can be placed immediately after the command name and before regular arguments. The full reference is
[docs/options.md](docs/options.md).

```java
public final class ScanCommand extends Command {
    public ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                     .description("Run even if safety checks fail."));
        CommandOption<Boolean, CommandContext> verbose = option(Options.flag("verbose", 'v')
                                                                       .description("Send detailed progress messages."));
        CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                     .description("Maximum number of targets."));

        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            boolean isForce = ctx.getOption(force);
            boolean isVerbose = ctx.getOption(verbose);
            int maxCount = ctx.getOption(limit);
            boolean limitWasSpecified = ctx.hasOption(limit);

            // Do something
        });
    }
}
```

Valid inputs:

```text
/scan Steve
/scan -f Steve
/scan --force Steve
/scan -f -v Steve
/scan -fv Steve
/scan -n 20 Steve
/scan --limit 20 Steve
/scan -fv -n 20 Steve
```

Any argument can be an option's value, with its type, suggestions and validation:
`option(Options.of("world", 'w', new WorldArgument("world")))` reads `--world world_nether` as a `World`. An option
can need its own permission node: `Options.flag("silent", 's').permission(DefaultPermission.OP)` hides `-s` from
senders without `<command node>.option.silent` ([permissions](docs/permissions.md#options)).

Options must appear before regular arguments. This is invalid:

```text
/scan Steve -f
```

unless the command calls `optionsAnywhere()`, which lets its options follow each argument too
([options](docs/options.md#options-after-the-arguments)).

For subcommands, options belong to the most specific child command and are placed after that child command name:

```java
public final class GameCommand extends Command {
    public GameCommand() {
        super("game");

        addChildren(new Command("start") {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));

            argument(new StringArgument("arena")).execute((arena, ctx) -> {
                boolean isForce = ctx.getOption(force);

                // Starts game
            });
        }});
    }
}
```

Valid:

```text
/game start -f arena1
```

Invalid:

```text
/game -f start arena1
```

Value options use the separated form, such as `--limit 20` or `-n 20`. Forms like `--limit=20` and `-n20` are not
supported.

Option descriptions are optional. When present, they are shown in the command help message.

Use `ctx.hasOption(option)` when you need to distinguish an explicitly specified option from its default value.

Options can also depend on other options:

```java
public final class ExportCommand extends Command {
    public ExportCommand() {
        super("export");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                     .description("Overwrite existing files."));
        CommandOption<String, CommandContext> reason = option(Options.string("reason", 'r', "")
                                                                     .description("Reason for forcing.")
                                                                     .requires(force));
        CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
        CommandOption<Integer, CommandContext> threads = option(Options.integer("threads", 't', 1, 1, 16)
                                                                       .requires(mode, "parallel"));

        argument(new StringArgument("target")).execute((target, ctx) -> {
            // --reason can be used only with --force.
            // --threads can be used only when ctx.getOption(mode) is parallel.
        });
    }
}
```

Valid:

```text
/export -f -r cleanup data
/export -t 4 -m parallel data
```

Invalid:

```text
/export -r cleanup data
/export -m normal -t 4 data
```

`requires(otherOption)` checks whether the other option was explicitly specified. `requires(otherOption, value)` checks
the value returned by `ctx.getOption(otherOption)`, so the other option's default value can also satisfy the condition.

Options can also have aliases, be required, be given more than once, and exclude each other:

```java
public final class TagCommand extends Command {
    public TagCommand() {
        super("tag");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                     .aliases("yes"));
        CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
        CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                         .repeatable(5)
                                                                         .required());
        CommandOption<Boolean, CommandContext> json = option(Options.flag("json", 'j'));
        CommandOption<Boolean, CommandContext> yaml = option(Options.flag("yaml", 'y'));
        mutuallyExclusiveOptions(json, yaml);

        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            boolean isForce = ctx.getOption(force);     // --force or --yes
            int verbosity = ctx.getOption(verbose);     // 2 for -vv
            List<String> tagList = ctx.getOption(tags); // [red, blue] for -t red --tag blue
        });
    }
}
```

```text
/tag --yes -vv -t red --tag blue -j Steve
```

`/tag Steve` fails with `Missing required option --tag.`, and `/tag -t red -j -y Steve` is rejected because `--json`
and `--yaml` are mutually exclusive. See [docs/options.md](docs/options.md) for every option kind, the errors, the help
output and the limits.

</details>


<details>
<summary>Suggesting Block Materials</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        CommandLib.register(this, new Command("test") {{
            argument(new EnumArgument<>("block", Material.class).validator(Material::isBlock)).execute((m, ctx) -> {
                // Do something
            });
        }});
    }
}
```

</details>

<details>
<summary>Defining Optional and Variadic Arguments</summary>

`optional(defaultValue)` lets the sender omit an argument, and the callback receives the default. Pass an extractor
instead to compute the default, such as `Command.player()` to target the sender. `variadic()` takes an argument
one or more times and gives the values as a `List`; it accepts up to 16 values, or the count given to
`variadic(maxCount)`. Make it optional with `variadic().optional(List.of())`.

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        CommandLib.register(this, new Command("give") {{
            // /give <item> [count]
            argument(new StringArgument("item"), new IntegerArgument("count").optional(1)).execute((item, count, ctx) -> {
                // count is 1 when omitted
            });
        }}, new Command("heal") {{
            // /heal [target]: heals the sender when omitted
            argument(new PlayerArgument("target").optional(Command.player())).execute((target, ctx) -> {
                target.setHealth(20);
            });
        }}, new Command("kick") {{
            // /kick <reason> <targets...>
            argument(new StringArgument("reason"), new PlayerArgument("targets").variadic()).execute((reason, targets, ctx) -> {
                targets.forEach(x -> x.kickPlayer(reason));
            });
        }});
    }
}
```

Only the last arguments of a branch can be optional, only the last one can be variadic, and such a branch cannot have
child commands; registering the command fails otherwise. A branch whose arguments are all optional also runs for the
bare command, so the command cannot have its own `execute` then. Generated help shows `[count]` and `<targets...>`.
Configure each value (validator, suggestions) before `variadic()` and the whole list after it.

</details>

## Messages

`ctx.sendMessage`, `sendSuccess` (green), `sendWarn` (yellow), and `sendFailure` (red) accept a `String`, a native
component, or a `RichText`. `RichText` is an immutable, platform-independent message that works the same on Spigot,
Paper, Velocity, and Forge / NeoForge / Fabric, so code shared between platforms can send colors, decorations, hover text, and click
actions:

```java
import dev.marulabs.commandlib.util.text.ClickAction;
import dev.marulabs.commandlib.util.text.RichText;

public final class DifficultyCommand extends Command {
    public DifficultyCommand() {
        super("difficulty");

        execute(ctx -> {
            ctx.sendMessage(RichText.text("difficulty: ")
                                    .append(RichText.text("hard")
                                                    .color(0xFF5555)
                                                    .bold()
                                                    .hover(RichText.text("Click to change"))
                                                    .click(ClickAction.suggestCommand("/difficulty "))));
            ctx.sendFailure(RichText.translatable("commands.give.failed.toomanyitems", RichText.text("64")));
        });
    }
}
```

`sendSuccess` / `sendWarn` / `sendFailure` only color a message that has no color of its own and never change the value
or component passed in. `RichTexts.toBungee` (spigot), `RichTexts.toAdventure` (paper, velocity), and `RichTexts.toVanilla`
(Forge / NeoForge / Fabric) convert a `RichText` to the native component. For Paper-only plugins, Adventure and MiniMessage can
be used directly: `ctx.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hello"))`.

### Translated Messages

CommandLib's own messages, such as "You do not have permission to execute this command." or the help headings, are
captions sent in the sender's locale, with English and Japanese built in. Replace them, and translate your own, with
`Captions`:

```java
public static final Caption HOME_SET = Caption.of("myplugin.home.set", "Home {name} set.");

Captions captions = Captions.create()
                            .set(Locale.JAPANESE, Caption.PERMISSION_DENIED, "それはできません。")
                            // lang/messages_ja.properties etc. in the plugin jar, read as UTF-8.
                            .bundle("lang/messages", getClass().getClassLoader());

public final class HomeCommand extends Command {
    public HomeCommand(Captions captions) {
        super("home");
        captions(captions);
        argument(new StringArgument("name")).execute((name, ctx) -> ctx.sendSuccess(HOME_SET, Map.of("name", name)));
    }
}
```

See [localization](docs/localization.md) for the lookup order, the keys, and `Captions#sender` to format every caption.

### Feedback And Results

`sendSuccess` sends command feedback the way vanilla commands do: a player does not get it while the
`sendCommandFeedback` gamerule is off, and a silent source such as a function gets nothing. `sendMessage`, `sendWarn`,
and `sendFailure` always reach the sender. The Velocity proxy has no gamerules, so there `sendSuccess` always reaches the
sender. A source without vanilla feedback, such as a test sender, gets `sendSuccess` as a plain message. CommandLib does
not broadcast feedback to operators; send it to them yourself when a command needs that.

`ctx.setResult(int)` sets the value that `/execute store result` stores (1 unless set). A command fails, storing 0 for
both `store result` and `store success`, when its executor throws `CommandPrerequisiteException`, its preprocess returns
false, or a permission, argument, or prerequisite check fails. Its failure messages then reach the sender the way a
vanilla command's error does, so a function or each target of `/execute as @a` does not get them.

```java
public final class CountCommand extends Command {
    public CountCommand() {
        super("count");

        argument(new PlayersArgument("targets")).execute((targets, ctx) -> {
            if (targets.isEmpty()) {
                throw ctx.fail("No player was found");
            }
            ctx.sendSuccess("Counted " + targets.size() + " players", true);
            ctx.setResult(targets.size());
        });
    }
}
```

`/execute store result score @s count run count @a` then stores the number of players. `CommandTester#execute`
returns the result, and 0 for a failure, whose messages it sends to the `FakeSender`. See
[errors](docs/errors.md#results-and-failures) for each failure.

## Confirmation and Cooldown

`confirm()` makes a command ask before it runs, and `cooldown(Duration)` keeps a sender from running it again too soon:

```java
public final class ResetCommand extends Command {
    public ResetCommand() {
        super("reset");

        // The first /reset only asks; running exactly the same input again within 10 seconds runs it.
        confirm();
        // Players wait 5 minutes between two resets.
        cooldown(Duration.ofMinutes(5));
        execute(ctx -> ctx.sendSuccess("Reset."));
    }
}
```

The first execution sends "Run the command again within 10s to confirm. [Confirm]" (a
[caption](docs/localization.md), in the sender's language), where `[Confirm]` runs the same
command when clicked. Each sender has one pending confirmation: another input, or the same one after the timeout, asks
again. A sender on cooldown gets "You must wait 4m 59s before using this command again." Both are checked after the
permissions, the arguments and the prerequisites, in this order: the registration's `CommandDefaults` middlewares, the
cooldown, the confirmation, the other middlewares and preprocesses. The cooldown starts only when all of them pass, so a sender on cooldown is not asked to confirm and an unconfirmed
execution does not start the cooldown. Both messages make the command fail like an error, see
[errors](docs/errors.md#cooldown-and-confirmation).

`Confirmation` and `Cooldown` change the timeout, the messages, the clock, and the permission that skips the cooldown:

```java
confirm(Confirmation.within(Duration.ofSeconds(30))
                    .message((ctx, command) -> ctx.sendWarn("Type " + command + " again to delete everything.")));
cooldown(Cooldown.of(Duration.ofSeconds(30))
                 .bypassPermission("myplugin.cooldown.bypass")
                 .message((ctx, remaining) -> ctx.sendFailure("Wait " + remaining.toSeconds() + " more seconds.")));
```

- The cooldown is kept per unique id, so it applies to players and entities. The console, remote consoles and command
  blocks have no unique id and are never on cooldown. Confirmations are kept per unique id too, and per sender type and
  name for senders without one, so the console has to confirm as well.
- Like a middleware, both apply to the command's argument branches and to child commands that set none of their own,
  unless the child calls `disableParentMiddleware()`. Children that inherit a cooldown share it, as do commands given
  the same `Cooldown` instance.
- Keep the `Cooldown` instance to query or end a sender's cooldown, for example from an admin command:
  `cooldown.remaining(player.getUniqueId())` is the time left (zero when not on cooldown), `cooldown.remaining(ctx)` the
  time left for the sender of a context, `reset(uuid)` ends one sender's cooldown and `resetAll()` every one.
- The state lives in memory and is lost when the server restarts.
- The bypass permission is registered with the command, with the default `OP`, so permission plugins can grant it on
  every platform ([permissions](docs/permissions.md#cooldown-bypass)). Set it before registering the command.
- In tests, pass a `Clock` with `clock(...)` instead of waiting for the time to pass.

## Middleware and Async Execution

A middleware wraps the execution of a command, its argument branches and its child commands. It runs after the
permission, argument and prerequisite checks, and returns the stage `next.proceed()` gives, which completes when the
executor is done:

```java
public final class AdminCommand extends Command {
    public AdminCommand(Database database) {
        super("admin");

        addMiddleware((ctx, next) -> {
            if (Maintenance.isActive()) {
                throw new CommandPrerequisiteException("The server is in maintenance.");
            }
            return next.proceed()
                       .whenComplete((ignored, e) -> Bukkit.getLogger().info(ctx.getSender().getName() + " ran /admin"));
        });
        addChildren(new ReloadCommand(), new StatsCommand(database));
    }
}
```

`CommandDefaults` applies middlewares and context value providers to every command of a registration. Pass it to
`CommandLib.register(this, defaults, commands)` on Spigot and Paper, `register(this, proxy, defaults, commands)` on
Velocity and `register(permissionPrefix, defaults, commands)` on Forge, NeoForge and Fabric, where `commands` is a
`Collection`, and to the testers' builder as `defaults(...)`. See
[execution](docs/execution.md#registration-wide-middlewares-and-providers).

`executeAsync` takes an executor that returns a `CompletionStage`. `ctx.supplyAsync(...)` runs the work off the server
thread and completes back on it, so the follow-up may send messages and use the platform API:

```java
argument(new OfflinePlayerArgument("player")).executeAsync((player, ctx) ->
        ctx.supplyAsync(() -> database.loadStats(player.getUniqueId()))
           .thenAccept(stats -> ctx.sendSuccess(player.getName() + " has " + stats.kills() + " kills")));
```

Exceptions of the work reach the `UncaughtExceptionHandler`s as an executor's do. Since vanilla needs the result at
once, an async command still running gives `/execute store` the result set so far (1 unless set). See
[execution](docs/execution.md) for the order of middlewares, exception mapping, threads on each platform, timeouts,
cancellation and testing.

## Sender Mapping and Context Values

A command provides typed values to its executions, such as the sender as the plugin's own type; middlewares can put
more, and every executor, prerequisite and suggestion provider of the subtree reads them by type:

```java
public final class TeamCommand extends Command {
    public TeamCommand(Games games, Accounts accounts) {
        super("team");

        provide(GamePlayer.class, ctx -> games.of(player().extract(ctx)));    // once per execution
        addMiddleware((ctx, next) -> {
            ctx.put(Account.class, accounts.load(ctx.getSender()));
            return next.proceed();
        });
        argument(new StringArgument("team")).execute((team, ctx) -> ctx.get(GamePlayer.class).join(team));
    }
}
```

The console gets "This command can only be executed by a player." from the provider. Annotated methods take the same
values as parameters (`void join(GamePlayer player, String team)`) through `registerContextValue` and
`registerDependency`, with `@Dependency` fields. See [context values](docs/context-values.md).

## Annotated Commands

A command can also be a class whose methods are annotated. `AnnotatedCommands.parse` builds an ordinary `Command`
from it through the fluent API, so help, permission nodes, options and `CommandTester` tests are the same:

```java
@Command("ban")
@Description("Ban a player")
public final class BanCommand {
    @Execute
    public void ban(CommandContext ctx,
                    @Flag(value = "silent", shortName = 's') boolean silent,
                    Player target,
                    @Greedy @Default("Banned by an operator") String reason) {
        target.kickPlayer(reason);
    }

    @Subcommand("list")
    public void list(CommandContext ctx, @Option("limit") @Range(min = 1, max = 100) @Default("10") int limit) {
    }
}

CommandLib.register(this, AnnotatedCommands.parse(new BanCommand()));
```

Parameters map by type to the built-in arguments (`Player` to `PlayerArgument`, `int` to `IntegerArgument`, ...), a
last `List<T>` parameter is variadic, `@Optional` / `@Default` make arguments optional, and a method returning a
`CompletionStage` runs as `executeAsync`. `@Subcommand("<team> add")` puts a subcommand after an argument, and
`@Middleware`, `@VisibleWhen`, `@Requires`, `@MutuallyExclusiveOptions`, `@OptionsAnywhere`, `@Examples` and
`@Description(caption = ...)` match the fluent settings of the same names. Argument names come from the parameter
names, so compile with `-parameters` (`-java-parameters` in Kotlin) or use `@Name`. See [annotated commands](docs/annotations.md) for every annotation, custom types and annotations, and
the fluent equivalent.

## Help and Error Formatting

The generated help can list examples, and three formatters replace how the help, each usage line and each failure
CommandLib reports look. Like captions, they apply to the child commands that set none of their own:

```java
public final class ArenaCommand extends Command {
    public ArenaCommand() {
        super("arena");
        examples("/arena create lobby 16");
        // The whole help, from a typed model: path, description, usage lines, options, examples.
        helpFormatter((ctx, help) -> help.usages()
                                         .forEach(usage -> ctx.sendMessage("§e" + usage.plainText())));
        // Each failure: its kind, its exception, and the default messages.
        errorFormatter((ctx, error) -> {
            ctx.sendFailure("[Arena] The command failed:");
            error.sendDefault();
        });
        // ...
    }
}
```

`errorFormatter(ErrorFormatter.SYNTAX_ADVICE)` adds the usage line of the argument branch that failed after an argument
error. See [help, usage and error formatting](docs/help.md).

## Server Help

The server's `/help` lists each Bukkit command with a short text under the plugin's index, and `/help <command>` shows
its page. `description(...)` is not used there; `serverHelp` sets the text for the server help on its own:

```java
public final class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");
        description(ctx -> "Teleports " + ctx.getSender().getName() + " to the spawn.");
        // The short text for the /help lists, and optionally the whole /help spawn page.
        serverHelp("Teleports you to the spawn.", "Teleports you to the spawn of the current world.");
    }
}
```

Without either, the command is listed as "A command by the &lt;plugin&gt; plugin.". Without the page, `/help <command>`
shows the short text, the usage and the aliases. Only top-level commands use it, and only Spigot and Paper read it:
Forge, NeoForge, Fabric and Velocity have no server help and ignore it.

## Permissions

CommandLib automatically generates and registers permission nodes for each command, subcommand, and argument branch.
Bukkit uses Bukkit permissions. Forge and NeoForge use their permission node APIs. Fabric uses fabric-permissions-api
when a mod ships it. Velocity uses the proxy's permission subjects.

### Permission Prefix

Permission nodes are generated from the registration prefix and command path.

For Bukkit, if you use `CommandLib.register(plugin, commands...)`, the prefix defaults to
`<plugin name in lowercase>.command`. For example, a plugin named `MyPlugin` generates
`myplugin.command.spawn`.

Pass a custom prefix to `CommandLib.register()` when you want to use a different namespace.

Bukkit:

```java
public final class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Generates "myplugin.command.spawn", "myplugin.command.game.start", etc.
        CommandLib.register(this, "myplugin.command", new SpawnCommand(), new GameCommand());
    }
}
```

Forge:

```java
public final class MyMod {
    public MyMod() {
        // Generates "mymod.command.spawn", "mymod.command.game.start", etc.
        CommandLib.register("mymod.command", new SpawnCommand(), new GameCommand());
    }
}
```

The prefix is applied to all commands, subcommands, and argument branches recursively.

### Custom Permission Node

To assign a specific permission node to a command instead of the auto-generated one:

```java
public final class SpawnCommand extends Command {
    public SpawnCommand() {
        super("spawn");

        permission("myplugin.admin"); // fixed node, ignores prefix

        permission("myplugin.admin", DefaultPermission.NONE); // with default
    }
}
```

### Permission Default

Control who has the permission by default with CommandLib's `DefaultPermission` enum. Root commands default to OP.
Subcommands inherit their parent command's default unless you override them.

```java
import dev.marulabs.commandlib.DefaultPermission;

public final class TestCommand extends Command {
    public TestCommand() {
        super("test");

        permission(DefaultPermission.OP);   // default - only operators

        permission(DefaultPermission.ALL);  // everyone

        permission(DefaultPermission.NONE); // no one (must be granted explicitly)
    }
}
```

Platform-specific overloads are also available when you need them:

```java
public final class TestCommand extends Command {
    public TestCommand() {
        super("test");

        permission(PermissionDefault.FALSE);      // Bukkit
        permission(DefaultPermissionLevel.NONE);  // Forge 1.16.5
    }
}
```

Argument branches also inherit the parent command's default permission unless you override the branch. A child command
attached under an argument branch inherits that argument branch's default permission.

### Argument Branch Permissions

CommandLib also generates permission nodes for argument branches. With prefix `myplugin.command`, this command
registers and checks both `myplugin.command.config` and `myplugin.command.config.key`:

```java
public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).description("Select a config key")
                                           .execute((key, ctx) -> {
                                               // Runs only when the sender has myplugin.command.config.key
                                           });
    }
}
```

Argument branch permissions are checked for command execution, tab completion, and generated help output. This is useful
when the base command is visible to many users, but only some users can execute or tab-complete a particular argument
route.

To assign a specific permission node instead of the auto-generated one:

```java
public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).permission("myplugin.command.config.key")
                                           .description("Select a config key")
                                           .execute((key, ctx) -> {
                                               // Runs only when the sender has myplugin.command.config.key
                                           });
    }
}
```

Use CommandLib's common `DefaultPermission` enum to control the default for an argument branch. If you omit it, the
argument branch inherits the parent command's default:

```java
import dev.marulabs.commandlib.DefaultPermission;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).permission(DefaultPermission.OP, "Access config keys");
    }
}
```

You can combine argument branch permissions with argument child commands:

```java
public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config");

        argument(new StringArgument("key")).permission("myplugin.command.config.key")
                                           .child(key -> new Command("get") {{
                                               execute(ctx -> {
                                                   String configKey = key.get();

                                                   // Get config value
                                               });
                                           }})
                                           .child(key -> new Command("set") {{
                                               argument(new StringArgument("value")).execute((value, ctx) -> {
                                                   String configKey = key.get();

                                                   // Set config value
                                               });
                                           }});
    }
}
```

### LuckPerms Compatibility

CommandLib delegates permission checks to the platform permission system:

- Bukkit: `CommandSender#hasPermission(permissionNode)`
- Forge / NeoForge: `PermissionAPI.getPermission(player, permissionNode)` (Forge 1.16.5:
  `PermissionAPI.hasPermission(player, permissionNode)`)
- Fabric: `Permissions.check(player, permissionNode, default)` of fabric-permissions-api
- Velocity: `PermissionSubject#getPermissionValue(permissionNode)`; an unset node is granted only when its default is
  `DefaultPermission.ALL`

LuckPerms can provide those platform permissions. Configure the same generated or custom permission nodes in LuckPerms;
CommandLib does not need additional LuckPerms-specific integration. On Forge 1.18+ and NeoForge the nodes are in the
`commandlib` namespace, so grant `commandlib.<node>` there. See [permissions](docs/permissions.md).

### Refreshing Command Trees

Players see only the commands and branches they have permission for, but the server does not resend the command tree
when a permission changes. Call `CommandLib.refresh(player)` (or `CommandLib.refreshAll()`) after granting or revoking
one, so the change shows without a rejoin. See [refreshing the command tree](docs/permissions.md#refreshing-the-command-tree).

## Claude Code Skill

A Claude Code skill is available as the `commandlib` plugin in this repository's plugin marketplace
(`plugins/commandlib/`). It loads the CommandLib API from your Gradle source cache and generates or explains code on
demand.

### Setup

1. Add this repository as a plugin marketplace and install the plugin, inside Claude Code:

```
/plugin marketplace add Maru32768/CommandLib
/plugin install commandlib@commandlib
```

Or from your shell:

```bash
claude plugin marketplace add Maru32768/CommandLib
claude plugin install commandlib@commandlib
```

Run `/plugin marketplace update commandlib` to pick up newer versions of the skill.

To pin the skill to the CommandLib version your project uses, add the marketplace at that release tag
(releases from the one after 0.17.2 onward include the plugin):

```
/plugin marketplace add Maru32768/CommandLib#<version>
```

To switch versions, run `/plugin marketplace remove commandlib` and add it again with another tag.

2. Download the CommandLib sources jars into the Gradle cache so the skill has something to read. In IntelliJ:
   **View > Tool Windows > Gradle > Download Sources**. Or with Gradle, add a task that resolves them and run
   `./gradlew downloadCommandLibSources`:

```kotlin
val commandlibSources by configurations.creating { isTransitive = false }
dependencies {
    // Your platform artifact and common, at the version you depend on.
    commandlibSources("com.github.Maru32768.CommandLib:spigot:<version>:sources")
    commandlibSources("com.github.Maru32768.CommandLib:common:<version>:sources")
}
tasks.register("downloadCommandLibSources") {
    val files: FileCollection = commandlibSources
    doLast { files.files }
}
```

### Usage

```
/commandlib:commandlib <request>
```

```
/commandlib:commandlib I want a command that sends a message to a player. Args: player and string. Permission: myplugin.message
/commandlib:commandlib プレイヤーにメッセージを送るコマンドを作りたい。引数はプレイヤーと文字列、権限は myplugin.message
```

## Sample Projects

Small buildable plugins and mods that show the main features; see the [overview](samples) for which sample shows
what.

- [Spigot](samples/spigot) and [Paper](samples/paper): every feature, in Java: optional and variadic arguments,
  options, middleware and `executeAsync`, confirmation and cooldown, the annotation style, the Java builder, and the
  diagnostics command.
- [Paper (Kotlin)](samples/paper-kotlin): the Kotlin DSL.
- [Velocity](samples/velocity): proxy player and server arguments.
- [Forge](samples/forge) (1.16.5) and [Fabric](samples/fabric) (1.21.1): the platform-neutral commands in a mod.

Each one shades and relocates CommandLib as described in [packaging](docs/packaging.md).

## License

CommandLib is licensed under the MIT License. See [LICENSE](./LICENSE).

Spigot, Paper, Bukkit, Minecraft, Brigadier, Forge, and other platform APIs or runtimes are separate projects and may
be governed by their own license terms. They are not bundled with CommandLib unless explicitly stated otherwise.

See [third-party dependencies and distribution policy](./THIRD_PARTY_NOTICES.md) for dependency licenses,
GPL platform integration, shading obligations, and Minecraft/mapping distribution restrictions.
The MIT license for CommandLib's original code does not override upstream terms for a combined distribution.
