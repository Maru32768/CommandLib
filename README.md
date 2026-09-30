# CommandLib - A Feature-Rich Minecraft Command API for Spigot / Paper

[![](https://jitpack.io/v/Maru32768/CommandLib.svg)](https://jitpack.io/#Maru32768/CommandLib)

CommandLib is an advanced, type-safe Command API for Spigot and Paper developers. It abstracts and enhances the
Minecraft 1.13+ command APIs, simplifying command implementation. With features like customizable argument suggestions,
seamless validation, improved usability, and extensibility, CommandLib empowers developers to build better commands
with ease.

#### Supported Versions

| Platform   | Artifact | Tested Versions                                            | Notes                                                         |
|------------|----------|------------------------------------------------------------|---------------------------------------------------------------|
| **Spigot** | `spigot` | `1.16.5`, `1.19.4`, `1.20.1`, `1.20.4`, `1.20.6`, `1.21.0` | Expected to work on intermediate versions. Requires Java 11+. |
| **Paper**  | `paper`  | `1.21.0`                                                   | Uses Paper official command/lifecycle API. Requires Java 21+. |
| **Forge**  | `forge-<minecraft-version>` | `1.16.5`, `1.20.1`                             | Use the artifact that matches the target Minecraft version.   |
| **Mohist** | `spigot` | `1.16.5`, `1.20.1`                                         | Works on Mohist since it's compatible with Spigot.            |

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
   Generate usage and help messages from the command tree, including arguments, subcommands, options, and descriptions.
6. **Seamless Integration with the `/execute` command**   
   Allow your commands to be executed seamlessly from the `/execute` command, just like built-in commands.

## Installation

To ensure stability, we recommend replacing `latest.release` with a specific version such as `0.16.0`.  
You can find the latest version on the [releases page](https://github.com/Maru32768/CommandLib/releases).

<details>
<summary>Spigot (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "8.3.5"
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
    relocate "net.kunmc.lab.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
}
tasks.build.dependsOn tasks.shadowJar
```

</details>

<details>
<summary>Spigot (Kotlin DSL)</summary>

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "8.3.5"
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
    relocate("net.kunmc.lab.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

</details>

<details>
<summary>Paper (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "8.3.5"
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
    relocate "net.kunmc.lab.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
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
    id("com.gradleup.shadow") version "8.3.5"
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
    relocate("net.kunmc.lab.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
```

> **Note:** The Paper artifact targets Paper 1.21.0+.

</details>

<details>
<summary>Forge (Groovy DSL)</summary>

```groovy
plugins {
    id "com.gradleup.shadow" version "8.3.5"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Maru32768.CommandLib:forge-1.20.1:latest.release"
}

shadowJar {
    archiveFileName = "${rootProject.name}-${project.version}.jar"
    dependencies {
        include(dependency("com.github.Maru32768.CommandLib:forge-1.20.1:.*"))
    }
    // Avoid package conflicts
    relocate "net.kunmc.lab.commandlib", "${project.group}.${project.name.toLowerCase()}.commandlib"
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
    id("com.gradleup.shadow") version "8.3.5"
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
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    dependencies {
        include(dependency("com.github.Maru32768.CommandLib:forge-1.20.1:.*"))
    }
    // Avoid package conflicts
    relocate("net.kunmc.lab.commandlib", "$projectGroup.$projectNameLower.commandlib")
    finalizedBy("reobfShadowJar")
}

@Suppress("UNCHECKED_CAST")
(extensions.getByName("reobf") as NamedDomainObjectContainer<Any>).create("shadowJar")
```

</details>

## Testing Commands

CommandLib provides command-level test utilities for downstream plugins.

```kotlin
dependencies {
    testImplementation("com.github.Maru32768.CommandLib:common-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:paper-testing:latest.release")
}
```

Use `common-testing` for commands built against the platform-neutral `common`
API, `spigot-testing` for commands built against the `spigot` artifact, and
`paper-testing` for commands built against the `paper` artifact. They provide
`CommandTester` and `FakeSender` so commands can be executed without a running
Minecraft server. Use one testing artifact per test module; these artifacts
share package-level helper names.

```java
class Test {
    void test() {
        FakeSender sender = FakeSender.player("Steve");
        try (CommandTester tester = new CommandTester(new MyCommand(), "myplugin.command")) {
            tester.execute("mycmd arg", sender);
        }
        assertThat(sender.getSentMessageTexts()).containsExactly("expected message");
    }
}
```

`paper-testing` stubs Paper `ArgumentTypes`, selector resolvers, and several
registry-backed argument paths for command-level tests. Paper lifecycle
registration and real server registry behavior are covered by integration tests.

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
                                           .child(keyArg -> new Command("get") {{
                                               execute(ctx -> {
                                                   String key = ctx.getArgument(keyArg);

                                                   // Get config value
                                               });
                                           }})
                                           .child(keyArg -> new Command("set") {{
                                               argument(new StringArgument("value")).execute((valueValue, ctx) -> {
                                                   String key = ctx.getArgument(keyArg);

                                                   // Set config value
                                               });
                                           }});
    }
}
```

Valid inputs:

```text
/config difficulty get
/config difficulty set hard
```

</details>

<details>
<summary>Defining Command Options</summary>

Command options can be placed immediately after the command name and before regular arguments.

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

Options must appear before regular arguments. This is invalid:

```text
/scan Steve -f
```

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
<summary>Defining Variable Length Arguments</summary>

```java
public final class TestPlugin extends JavaPlugin {
    public void onEnable() {
        CommandLib.register(this, new Command("test") {{
            argument(new PlayerArgument("player")).execute((player, ctx) -> {
                // Do something
            });
            argument(new PlayerArgument("player"), new StringArgument("message")).execute((player, message, ctx) -> {
                // Do something
            });
        }});
    }
}
```

</details>

## Permissions

CommandLib automatically generates and registers permission nodes for each command, subcommand, and argument branch.
Bukkit uses Bukkit permissions. Forge uses Forge permission nodes.

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
import net.kunmc.lab.commandlib.DefaultPermission;

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
        permission(DefaultPermissionLevel.NONE);  // Forge
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
import net.kunmc.lab.commandlib.DefaultPermission;

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
                                           .child(keyArg -> new Command("get") {{
                                               execute(ctx -> {
                                                   String key = ctx.getArgument(keyArg);

                                                   // Get config value
                                               });
                                           }})
                                           .child(keyArg -> new Command("set") {{
                                               argument(new StringArgument("value")).execute((value, ctx) -> {
                                                   String key = ctx.getArgument(keyArg);

                                                   // Set config value
                                               });
                                           }});
    }
}
```

### LuckPerms Compatibility

CommandLib delegates permission checks to the platform permission system:

- Bukkit: `CommandSender#hasPermission(permissionNode)`
- Forge: `PermissionAPI.hasPermission(player, permissionNode)`

LuckPerms can provide those platform permissions. Configure the same generated or custom permission nodes in LuckPerms;
CommandLib does not need additional LuckPerms-specific integration.

## Claude Code Skill

A Claude Code skill is available at `.claude/skills/commandlib/`. It loads the CommandLib API from your Gradle source
cache and generates or explains code on demand.

### Setup

1. Copy the `.claude/skills/commandlib/` directory to your project's `.claude/skills/` directory.

2. Download sources via Gradle so the skill has something to read:

```bash
./gradlew dependencies --configuration compileClasspath
```

Or in IntelliJ: **View > Tool Windows > Gradle > Download Sources**.

### Usage

```
/commandlib <request>
```

```
/commandlib I want a command that sends a message to a player. Args: player and string. Permission: myplugin.message
/commandlib プレイヤーにメッセージを送るコマンドを作りたい。引数はプレイヤーと文字列、権限は myplugin.message
```

## Sample Projects

[Overview](samples)  
[Spigot](samples/spigot)  
[Paper](samples/paper)  
[Forge](samples/forge)

## License

CommandLib is licensed under the MIT License. See [LICENSE](./LICENSE).

Spigot, Paper, Bukkit, Minecraft, Brigadier, Forge, and other platform APIs or runtimes are separate projects and may
be governed by their own license terms. They are not bundled with ConfigLib unless explicitly stated otherwise.
