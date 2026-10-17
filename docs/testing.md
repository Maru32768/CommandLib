# Testing Commands

CommandLib ships test artifacts that build your commands into a Brigadier dispatcher and run them without a Minecraft
server. Use them for unit tests of parsing, permissions, options, messages, and suggestions. Use a real server for
lifecycle registration, real registries, worlds, and `/execute` behavior.

## Artifacts

Pick the artifact for the CommandLib artifact your code uses, one per test source set (they share class names):

| Your code uses | Test artifact | Notes |
|---|---|---|
| `common` (platform-neutral) | `common-testing` | Commands are `TestCommand`; arguments are the `Common...Argument` classes. |
| `spigot` | `spigot-testing` | NMS argument types are replaced by mocks. Uses Mockito. |
| `paper` | `paper-testing` | Paper's `ArgumentTypes` and resolvers are stubbed. Uses Mockito. |
| `velocity` | `velocity-testing` | Senders and the `ProxyServer` are Mockito mocks; every argument parses for real. |
| `forge-<version>` | `forge-testing-<version>` | Minecraft's registries are bootstrapped; vanilla arguments parse for real. |
| `neoforge-<version>` | `neoforge-testing-<version>` | Same as Forge. |
| `fabric-<version>` | `fabric-testing-<version>` | Same as Forge. |

```kotlin
dependencies {
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:<version>")
    testImplementation("org.junit.jupiter:junit-jupiter:<version>")
}
```

The artifacts do not bring a test framework; the examples use JUnit 5 and AssertJ. `CommandLibExtension` needs JUnit
5 (`junit-jupiter-api` 5.8 or later) on the test classpath; the rest of the artifacts work with any framework.

## Async commands

The testers run `executeAsync` work at once by default, so the command has finished when `execute` returns. Give the
builder a scheduler with `scheduler(CommandScheduler.of(Runnable::run, queue::add))` to run the async work yourself and
check the state in between; see [execution](execution.md#testing).

## `CommandTester`

```java
class HealCommandTest {
    @Test
    void heals_the_target() {
        FakeSender sender = FakeSender.player("Steve");
        try (CommandTester tester = new CommandTester(HealCommand::new, "myplugin.command")) {
            tester.execute("heal", sender);
        }
        assertThat(sender.getSentMessageTexts()).containsExactly("Healed");
    }
}
```

| Member | Purpose |
|---|---|
| `new CommandTester(Supplier<Command>, prefix)` | Builds the command inside the tester. **Prefer this form.** |
| `new CommandTester(Command, prefix)`, `new CommandTester(Collection<Command>, prefix)` | For commands without platform argument types. |
| `CommandTester.builder().command(...).commands(...).permissionPrefix(...).build()` | Several commands, or extra setup (`mockNmsClass` in `spigot-testing`, `defaults(...)` for the registration's [middlewares and providers](execution.md#registration-wide-middlewares-and-providers)). |
| `execute(input, sender)` | Runs the input as the sender and returns the result `/execute store result` would store (`0` for a failure). No leading `/`. |
| `suggestions(input, sender)` | Returns Brigadier's `CompletableFuture<Suggestions>` for a partial input. |
| `suggest(input, sender)` | Returns the suggestion texts for a partial input as a `List<String>`. |
| `commandTree()`, `assertCommandTreeMatchesSnapshot(name)` | Renders the registered commands as text and compares it with a snapshot file. See [command tree snapshots](#command-tree-snapshots). |
| `close()` | Ends the tester. Use try-with-resources. |

Build commands through the `Supplier` form (`HealCommand::new`, `() -> new Command("x") {{ ... }}`). Argument
constructors call platform code: NMS lookups on Spigot, `ArgumentTypes` on Paper, vanilla argument types that need
bootstrapped registries on Forge / NeoForge / Fabric. The tester makes those available only while it builds the commands.

`execute` throws a `RuntimeException` caused by Brigadier's `CommandSyntaxException` when the platform would reject
the input: unknown command or subcommand, invalid argument syntax, or a node hidden by a missing permission. Everything
CommandLib reports itself (validation, prerequisites, unexpected errors) is sent to the sender instead. See
[errors](errors.md). An `AssertionError` that a command throws, such as a failed assertion in an executor, is sent as
an unexpected error and then rethrown by `execute`, so the test fails.

`common-testing` also has `executeAndGetContext(input, sender)`, which returns the `TestCommandContext` of the run.

### Fake world state

| Artifact | Methods | Provides |
|---|---|---|
| `spigot-testing` | `withFakePlayer(Player)`, `withFakeEntity(name, Entity)`, `withFakeWorld(key, World)` | Values for player / entity selectors (names, `@s`, `@p`, `@r`, `@a`, `@e`) and `WorldArgument` |
| `paper-testing` | `withFakePlayer`, `withFakeEntity`, `withFakeWorld(key, World)`, `withFakeBlockData`, `withFakeItemStack`, `withFakeEnchantment`, `withFakeParticle`, `withFakePotionEffectType`, `withFakeBiome`, `withFakeAttribute`, `withFakeEntityType`, `withFakeSound` | The value each stubbed Paper argument returns |
| `forge-testing` / `neoforge-testing` / `fabric-testing` | `withPlayer(FakeSender)`, `withLevel(ServerLevel)`, `withServer(Consumer<MinecraftServer>)` | Online players for selectors, `GameProfileArgument`, `UUIDArgument`; levels for `WorldArgument`; any other stub of the server mock, such as `getScoreboard()`, `getAdvancements()`, `getRecipeManager()` or the loot tables |
| `velocity-testing` | `withFakePlayer(FakeSender)`, `withFakeServer(name)`, `proxy()` | Players and servers of the fake `ProxyServer` for `PlayerArgument` and `ServerArgument` |

The [argument catalog](arguments.md#testing-each-argument) lists, argument by argument, what runs for real, what is
mocked, and what needs Bukkit static mocks.

### Bukkit static APIs

Arguments that call `Bukkit` statics (`OfflinePlayerArgument`, `UUIDArgument`, the Spigot registry arguments,
`AdvancementArgument`, `ObjectiveArgument`, `TeamArgument` on Paper, ...) need them stubbed. Declare the static mock
before the tester in the same try-with-resources, so the tester closes first:

```java
@Test
void finds_offline_player() {
    FakeSender sender = FakeSender.player("Alice");
    OfflinePlayer steve = Mockito.mock(OfflinePlayer.class);
    Mockito.when(steve.getName())
           .thenReturn("Steve");

    try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
         CommandTester tester = new CommandTester(() -> new Command("info") {{
             argument(new OfflinePlayerArgument("player")).execute((player, ctx) -> ctx.sendMessage(player.getName()));
         }}, "test.command")) {
        bukkit.when(Bukkit::getOfflinePlayers)
              .thenReturn(new OfflinePlayer[]{steve});
        tester.execute("info steve", sender);
    }

    assertThat(sender.getSentMessageTexts()).containsExactly("Steve");
}
```

`spigot-testing` and `paper-testing` also accept plain Bukkit `CommandSender`s, such as MockBukkit's `PlayerMock`, in
`execute`, `suggestions` and `suggest`.

## JUnit 5 extension

Every testing artifact has `CommandLibExtension`. Register it on a field with the
tester to create; it creates one tester per test, closes it after the test, and injects `CommandTester` and
`FakeSender` parameters into test methods and `@BeforeEach` / `@AfterEach` methods. A lifecycle method and the test
it runs around get the same tester.

```java
class HealCommandTest {
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(HealCommand::new,
                                                                                          "myplugin.command"));

    @Test
    void heals_the_target(CommandTester tester, @Sender("Steve") FakeSender steve, @Sender("Admin") FakeSender admin) {
        tester.execute("heal Steve", admin);
        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve");
    }

    @Test
    void guests_cannot_heal(CommandTester tester,
                            @Sender(value = "Guest", permissions = "myplugin.command.heal") FakeSender guest) {
        assertThatThrownBy(() -> tester.execute("heal Guest", guest)).isInstanceOf(RuntimeException.class);
    }
}
```

The factory is any `Supplier<CommandTester>`, so it can use `CommandTester.builder()` for several commands or
`mockNmsClass`.

`@Sender` configures an injected sender:

| Element | Default | Effect |
|---|---|---|
| `value` | `"Player"` | The player's name |
| `console` | `false` | Injects the console, an operator, instead of a player |
| `op` | `false` | Makes a player an operator (not in `velocity-testing`, since Velocity has no operators) |
| `permissions` | `{}` (every permission) | Grants only these nodes |
| `denyPermissions` | `{}` | Denies these nodes, even to operators |

A `FakeSender` parameter without `@Sender` is a non-operator player named `Player` with every permission. In
`spigot-testing`, `paper-testing` and `velocity-testing`, every injected player is also registered with `withFakePlayer`
(`withPlayer` in the Forge / NeoForge / Fabric artifacts), so player and entity arguments resolve it by name. Build senders that need more, such as a UUID or a locale, with
`FakeSender.playerBuilder(name)` (`FakeSender.player(name, uuid)` in `velocity-testing`) in the test.

## `FakeSender`

| Factory | Sender |
|---|---|
| `FakeSender.player(name)`, `player(name, uuid)` | A non-operator player with every permission |
| `FakeSender.console()` | The console, an operator with every permission |
| `FakeSender.playerBuilder(name)`, `consoleBuilder()` | Builders for the same senders |
| `FakeSender.unknown()` (`common-testing` only) | A sender of unknown type without permissions |

Adjust a sender with `op(boolean)`, `permissions(nodes...)` (only these nodes), `denyPermissions(nodes...)` (all but
these), `uniqueId(uuid)`, `locale(tag)` (a `Locale` in `velocity-testing`), and `name(name)` (not in
`common-testing`). The builders have the same methods. `velocity-testing` has no builders and only `permissions`,
`denyPermissions` and `locale`. A player's locale decides the language of CommandLib's messages
and of your [captions](localization.md); the console uses the JVM's default locale.

Keep the returned sender: in `common-testing` these methods return a new `FakeSender`, in the other artifacts they
change the sender and return it.

```java
FakeSender admin = FakeSender.player("Alice")
                             .op(true);
FakeSender guest = FakeSender.playerBuilder("Bob")
                             .permissions("myplugin.command.spawn")
                             .build();
```

The artifacts do not apply `DefaultPermission` defaults: a fake sender has the nodes you give it, whatever the
command's default is. Test defaults on a server. `velocity-testing` is the exception, since on Velocity CommandLib
applies the defaults itself: as on a real proxy, a node the sender leaves unset is granted only if its default is
`ALL`. There `permissions(nodes...)` grants these nodes and leaves the others unset, and `permissions()` with no nodes
is a player on a proxy without a permission plugin.

Reading messages:

| Method | `common-testing` | `spigot-testing` | `paper-testing` | `modded-testing` |
|---|---|---|---|---|
| `getSentMessages()` | `List<String>` | `List<BaseComponent>` | `List<Component>` (Adventure) | `List<Component>` (vanilla) |
| `getSentMessageTexts()` | plain text | plain text, colors stripped | plain text | plain text |
| `getSentMessageLegacyTexts()` | `§` codes kept | `§` codes kept | — | — |

Translatable messages, such as CommandLib's "Incorrect argument for command", read as English text in `spigot-testing`
and `modded-testing` and as their translation keys (`command.unknown.argument`) in `paper-testing`. Check colors on
the components, for example `getSentMessages().get(0).getColor()` in `spigot-testing`.

`velocity-testing` reads messages like `paper-testing` (Adventure `Component`s, plain texts).

Other helpers: `asSender()` returns the Mockito `CommandSender` mock (`spigot-testing`, `paper-testing`) or
`CommandSource` mock (`velocity-testing`), and
`asPlayer()` returns the `ServerPlayer` mock (`modded-testing`).

## Examples

### Permissions

```java
@Test
void key_branch_needs_its_own_node() {
    FakeSender sender = FakeSender.player("Alice")
                                  .denyPermissions("myplugin.command.config.key");
    try (CommandTester tester = new CommandTester(ConfigCommand::new, "myplugin.command")) {
        assertThatThrownBy(() -> tester.execute("config difficulty", sender)).isInstanceOf(RuntimeException.class);
    }
}
```

The denied branch is hidden, so Brigadier rejects the input before CommandLib runs.

### Validation errors

```java
@Test
void rejects_unknown_mode() {
    FakeSender sender = FakeSender.player("Alice");
    try (CommandTester tester = new CommandTester(() -> new Command("mode") {{
        argument(new LiteralArgument("mode", List.of("auto", "manual"))).execute((mode, ctx) -> ctx.sendMessage(mode));
    }}, "test.command")) {
        tester.execute("mode turbo", sender);
    }
    assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command", "mode turbo<--[HERE]");
}
```

This is `spigot-testing` output; `paper-testing` gives `command.unknown.argument` and `mode turbocommand.context.here`.

### Suggestions

```java
@Test
void suggests_modes() {
    FakeSender sender = FakeSender.player("Alice");
    try (CommandTester tester = new CommandTester(() -> new Command("mode") {{
        argument(new LiteralArgument("mode", List.of("auto", "manual"))).execute((mode, ctx) -> ctx.sendMessage(mode));
    }}, "test.command")) {
        assertThat(tester.suggest("mode au", sender)).containsExactly("auto");
    }
}
```

`suggest` waits for the suggestions and returns their texts. For tooltips or ranges, read the `Suggestions` that
`suggestions(input, sender).join()` returns.

### Command tree snapshots

`commandTree()` renders the registered commands: each command with its aliases, permission node and
`DefaultPermission`, then its options (long names and aliases, short name, value name, and whether it is required or
repeatable), argument branches (argument names and classes, `[optional]` and `<variadic...>` markers, and the branch's
permission) and child commands, indented by depth. The generated `help` children are left out. It is the same text as
[`CommandDocs#tree()`](diagnostics.md#command-documentation).

```text
heal (aliases: h) [myplugin.command.heal, OP]
  --silent, -s
  <target: PlayerArgument> <amount: IntegerArgument> [myplugin.command.heal.target.amount, OP]
```

`assertCommandTreeMatchesSnapshot(name)` compares the tree with `src/test/resources/snapshots/<name>`, relative to the
working directory (the project directory under Gradle and Maven), so a change to the tree, a permission node, or an
alias fails a test:

```java
@Test
void command_tree_is_unchanged() {
    try (CommandTester tester = new CommandTester(HealCommand::new, "myplugin.command")) {
        tester.assertCommandTreeMatchesSnapshot("heal.snap");
    }
}
```

The first run writes the missing snapshot and fails; review the file, commit it, and run the test again. After an
intended change, run the tests once with the environment variable `COMMANDLIB_UPDATE_SNAPSHOTS=true` (or the system
property `commandlib.updateSnapshots=true` in the test JVM) to rewrite the snapshots, and review the diff. Gradle does
not pass its own `-D` options to the test JVM, so use the environment variable there:

```bash
COMMANDLIB_UPDATE_SNAPSHOTS=true ./gradlew test
```

## What the artifacts do not cover

- Platform registration: Bukkit's command map, Paper's lifecycle events and bootstrap, Forge's events, reloads.
- `DefaultPermission` defaults (except in `velocity-testing`), permission plugin behavior, client command trees.
- Real registries and tags (Spigot, Paper), worlds, scoreboards (except through `scoreboard(Supplier)`), selector
  filters such as `@e[type=zombie]` (Spigot, Paper).
- `/execute`, command blocks, and functions. `execute` returns the result, but gamerules, silent sources, and the
  operator broadcast of `sendSuccess(message, true)` are not modeled.

CommandLib's own coverage of these lives in the repository's `integration-test` module, which runs real servers.
