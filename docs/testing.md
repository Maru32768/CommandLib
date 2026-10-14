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
| `forge-<version>` | `forge-testing-<version>` | Minecraft's registries are bootstrapped; vanilla arguments parse for real. |
| `neoforge-<version>` | `neoforge-testing-<version>` | Same as Forge. |

```kotlin
dependencies {
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:<version>")
    testImplementation("org.junit.jupiter:junit-jupiter:<version>")
}
```

The artifacts do not bring a test framework; the examples use JUnit 5 and AssertJ.

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
| `CommandTester.builder().command(...).commands(...).permissionPrefix(...).build()` | Several commands, or extra setup (`mockNmsClass` in `spigot-testing`). |
| `execute(input, sender)` | Runs the input as the sender. No leading `/`. |
| `suggestions(input, sender)` | Returns Brigadier's `CompletableFuture<Suggestions>` for a partial input. |
| `close()` | Ends the tester. Use try-with-resources. |

Build commands through the `Supplier` form (`HealCommand::new`, `() -> new Command("x") {{ ... }}`). Argument
constructors call platform code: NMS lookups on Spigot, `ArgumentTypes` on Paper, vanilla argument types that need
bootstrapped registries on Forge / NeoForge. The tester makes those available only while it builds the commands.

`execute` throws a `RuntimeException` caused by Brigadier's `CommandSyntaxException` when the platform would reject
the input: unknown command or subcommand, invalid argument syntax, or a node hidden by a missing permission. Everything
CommandLib reports itself (validation, prerequisites, unexpected errors) is sent to the sender instead. See
[errors](errors.md).

`common-testing` also has `executeAndGetContext(input, sender)`, which returns the `TestCommandContext` of the run.

### Fake world state

| Artifact | Methods | Provides |
|---|---|---|
| `spigot-testing` | `withFakePlayer(Player)`, `withFakeEntity(name, Entity)`, `withFakeWorld(key, World)` | Values for player / entity selectors (names, `@s`, `@p`, `@r`, `@a`, `@e`) and `WorldArgument` |
| `paper-testing` | `withFakePlayer`, `withFakeEntity`, `withFakeWorld`, `withFakeBlockData`, `withFakeItemStack`, `withFakeEnchantment`, `withFakeParticle`, `withFakePotionEffectType`, `withFakeBiome`, `withFakeAttribute`, `withFakeEntityType`, `withFakeSound` | The value each stubbed Paper argument returns |
| `forge-testing` / `neoforge-testing` | `withPlayer(FakeSender)` | Online players for selectors, `GameProfileArgument`, `UUIDArgument` |

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
`execute` and `suggestions`.

## `FakeSender`

| Factory | Sender |
|---|---|
| `FakeSender.player(name)`, `player(name, uuid)` | A non-operator player with every permission |
| `FakeSender.console()` | The console, an operator with every permission |
| `FakeSender.playerBuilder(name)`, `consoleBuilder()` | Builders for the same senders |
| `FakeSender.unknown()` (`common-testing` only) | A sender of unknown type without permissions |

Adjust a sender with `op(boolean)`, `permissions(nodes...)` (only these nodes), `denyPermissions(nodes...)` (all but
these), `uniqueId(uuid)`, `locale(tag)` (not in `modded-testing`), and `name(name)` (not in `common-testing`). The
builders have the same methods.

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
command's default is. Test defaults on a server.

Reading messages:

| Method | `common-testing` | `spigot-testing` | `paper-testing` | `modded-testing` |
|---|---|---|---|---|
| `getSentMessages()` | `List<String>` | `List<BaseComponent>` | `List<Component>` (Adventure) | `List<Component>` (vanilla) |
| `getSentMessageTexts()` | plain text | plain text, colors stripped | plain text | plain text |
| `getSentMessageLegacyTexts()` | `§` codes kept | `§` codes kept | — | — |

Translatable messages, such as CommandLib's "Incorrect argument for command", read as English text in `spigot-testing`
and `modded-testing` and as their translation keys (`command.unknown.argument`) in `paper-testing`. Check colors on
the components, for example `getSentMessages().get(0).getColor()` in `spigot-testing`.

Other helpers: `asSender()` returns the Mockito `CommandSender` mock (`spigot-testing`, `paper-testing`), and
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
        List<String> texts = tester.suggestions("mode au", sender)
                                   .join()
                                   .getList()
                                   .stream()
                                   .map(Suggestion::getText)
                                   .collect(Collectors.toList());
        assertThat(texts).containsExactly("auto");
    }
}
```

`Suggestion` is `com.mojang.brigadier.suggestion.Suggestion`.

## What the artifacts do not cover

- Platform registration: Bukkit's command map, Paper's lifecycle events and bootstrap, Forge's events, reloads.
- `DefaultPermission` defaults, permission plugin behavior, client command trees.
- Real registries and tags (Spigot, Paper), worlds, scoreboards (except through `scoreboard(Supplier)`), selector
  filters such as `@e[type=zombie]` (Spigot, Paper).
- `/execute`, command blocks, functions, and result values.

CommandLib's own coverage of these lives in the repository's `integration-test` module, which runs real servers.
