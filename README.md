# CommandLib - A Feature-Rich Minecraft Command API for Spigot / Paper

[![](https://jitpack.io/v/Maru32768/CommandLib.svg)](https://jitpack.io/#Maru32768/CommandLib)

CommandLib is an advanced, type-safe Command API for Spigot and Paper developers. It abstracts and enhances the
Minecraft 1.13+ command APIs, simplifying command implementation. With features like customizable argument suggestions,
seamless validation, improved usability, and extensibility, CommandLib empowers developers to build better commands
with ease.

#### Supported Versions

| Platform   | Artifact | Tested Versions                                            | Notes                                                         |
|------------|----------|------------------------------------------------------------|---------------------------------------------------------------|
| **Spigot** | `spigot` | `1.16.5`, `1.17.1`, `1.18`–`1.18.2`, `1.19`–`1.19.4`, `1.20`–`1.20.6`, `1.21`–`1.21.11`, `26.1`–`26.3` | Each release in the range is tested on Spigot, and `1.16.5`–`1.21.1` also on Paper. Requires Java 11+. |
| **Paper**  | `paper`  | `1.21`, `1.21.3`–`1.21.11`, `26.1.1`–`26.3` | Uses Paper official command/lifecycle API. Requires Java 21+. Use `spigot` on Paper `1.20.6` and earlier. |
| **Forge**  | `forge-<minecraft-version>` | `1.16.5`, `1.20.1`                             | Use the artifact that matches the target Minecraft version.   |
| **NeoForge** | `neoforge-<minecraft-version>` | `1.21.1`                                   | Use the artifact that matches the target Minecraft version. Requires Java 21+. |
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

## Why CommandLib

- **One command model for Spigot, Paper, Forge, and NeoForge.** The same `Command` / `argument(...)` code compiles
  against each platform artifact, from Minecraft 1.16.5 to 26.x.
- **Typed values in the executor.** `argument(new PlayerArgument("target"), new IntegerArgument("amount"))` hands a
  `Player` and an `Integer` to the lambda: no lookup by name, no casts.
- **Brigadier-native.** Commands are real Brigadier nodes, so they get client-side validation, vanilla selectors,
  `/execute` and data pack function support, and per-player command trees.
- **Permissions without boilerplate.** Every command, subcommand, and argument branch gets a registered permission
  node with a default, which also hides what a sender cannot use.
- **Options, generated help, and test utilities built in.**

| | CommandLib | CommandAPI | Paper command API |
|---|---|---|---|
| Platforms | Spigot, Paper, Forge, NeoForge | Spigot, Paper (others: see its docs) | Paper |
| Executor arguments | typed parameters | `args.get("name")` + cast | `ctx.getArgument("name", Type.class)` |
| Generated permission nodes | every command and branch | manual | manual |
| Flags and options (`-f`, `--limit 10`) | built in | — | — |
| Generated usage / help | built in | `/help` entries | `/help` entries |
| Server-free test utilities | `spigot-testing`, `paper-testing`, `forge-` / `neoforge-testing` | test toolkit (MockBukkit) | — |
| Optional arguments, result values, chat / NBT / range arguments | not yet | yes | yes |

See the [argument catalog](docs/arguments.md) for an argument-by-argument comparison and
[compatibility](docs/compatibility.md) for the tested server versions.

## Documentation

- [Arguments](docs/arguments.md): built-in arguments, comparison with CommandAPI and Paper, and gaps.
- [Suggestions](docs/suggestions.md): default, custom, asynchronous, and tooltip suggestions.
- [Permissions](docs/permissions.md): node names, defaults per platform, registration, and LuckPerms.
- [Errors](docs/errors.md): what the sender sees when a command fails.
- [Testing](docs/testing.md): `CommandTester` and `FakeSender` for plugin and mod tests.
- [Troubleshooting](docs/troubleshooting.md).
- [Migrating from CommandAPI](docs/migration/from-commandapi.md).
- [Compatibility](docs/compatibility.md): tested platforms and versions.

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
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
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
    id "com.gradleup.shadow" version "8.3.5"
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
    id("com.gradleup.shadow") version "8.3.5"
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

## Testing Commands

CommandLib provides command-level test utilities for downstream plugins.

```kotlin
dependencies {
    testImplementation("com.github.Maru32768.CommandLib:common-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:paper-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:forge-testing-1.20.1:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:neoforge-testing-1.21.1:latest.release")
}
```

Use `common-testing` for commands built against the platform-neutral `common`
API, `spigot-testing` for commands built against the `spigot` artifact,
`paper-testing` for commands built against the `paper` artifact, and
`forge-testing-<minecraft-version>` / `neoforge-testing-<minecraft-version>` for
commands built against the matching `forge-<minecraft-version>` /
`neoforge-<minecraft-version>` artifact. They provide
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

`forge-testing` and `neoforge-testing` bootstrap Minecraft's registries, so vanilla
argument types such as items, blocks, effects, and enchantments parse for real. The server is a Mockito mock:
fake players added with `CommandTester#withPlayer` (and the executing player)
are online, so player, entity, game profile, and UUID arguments resolve them by
name. Worlds, scoreboards, and other server state are not available.

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
read the value first or use `key.get(ctx)`.

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
        mutuallyExclusive(json, yaml);

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
instead to compute the default, such as `Command.playerRequirer()` to target the sender. `variadic()` takes an argument
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
            argument(new PlayerArgument("target").optional(Command.playerRequirer())).execute((target, ctx) -> {
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
Paper, and Forge / NeoForge, so code shared between platforms can send colors, decorations, hover text, and click
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
or component passed in. `RichTexts.toBungee` (spigot), `RichTexts.toAdventure` (paper), and `RichTexts.toVanilla`
(Forge / NeoForge) convert a `RichText` to the native component. For Paper-only plugins, Adventure and MiniMessage can
be used directly: `ctx.sendMessage(MiniMessage.miniMessage().deserialize("<red>Hello"))`.

### Feedback And Results

`sendSuccess` sends command feedback the way vanilla commands do: a player does not get it while the
`sendCommandFeedback` gamerule is off, and a silent source such as a function gets nothing. `sendSuccess(message, true)`
also broadcasts it to the other operators in gray italics and to the server log when `logAdminCommands` is on, like
`/gamemode`. `sendMessage`, `sendWarn`, and `sendFailure` always reach the sender.

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
                throw new CommandPrerequisiteException("No player was found");
            }
            ctx.sendSuccess("Counted " + targets.size() + " players", true);
            ctx.setResult(targets.size());
        });
    }
}
```

`/execute store result score @s count run count @a` then stores the number of players. `CommandTester#execute`
returns the result, and 0 for a failure, whose messages it sends to the `FakeSender`.

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
shows the short text, the usage and the aliases. Only top-level commands use it, and Forge / NeoForge, which have no
server help, ignore it.

## Permissions

CommandLib automatically generates and registers permission nodes for each command, subcommand, and argument branch.
Bukkit uses Bukkit permissions. Forge and NeoForge use their permission node APIs.

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

2. Download sources via Gradle so the skill has something to read:

```bash
./gradlew dependencies --configuration compileClasspath
```

Or in IntelliJ: **View > Tool Windows > Gradle > Download Sources**.

### Usage

```
/commandlib:commandlib <request>
```

```
/commandlib:commandlib I want a command that sends a message to a player. Args: player and string. Permission: myplugin.message
/commandlib:commandlib プレイヤーにメッセージを送るコマンドを作りたい。引数はプレイヤーと文字列、権限は myplugin.message
```

## Sample Projects

[Overview](samples)  
[Spigot](samples/spigot)  
[Paper](samples/paper)  
[Forge](samples/forge)

## License

CommandLib is licensed under the MIT License. See [LICENSE](./LICENSE).

Spigot, Paper, Bukkit, Minecraft, Brigadier, Forge, and other platform APIs or runtimes are separate projects and may
be governed by their own license terms. They are not bundled with CommandLib unless explicitly stated otherwise.

See [third-party dependencies and distribution policy](./THIRD_PARTY_NOTICES.md) for dependency licenses,
GPL platform integration, shading obligations, and Minecraft/mapping distribution restrictions.
The MIT license for CommandLib's original code does not override upstream terms for a combined distribution.
