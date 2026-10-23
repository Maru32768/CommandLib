# Testing CommandLib Usage

Use CommandLib's testing artifacts to test commands without a running Minecraft
server:

- Use `spigot-testing` for commands written against the `spigot` artifact.
- Use `paper-testing` for commands written against the `paper` artifact.

Add the dependency in test scope:

```kotlin
dependencies {
    testImplementation("com.github.Maru32768.CommandLib:spigot-testing:latest.release")
    testImplementation("com.github.Maru32768.CommandLib:paper-testing:latest.release")
}
```

```java
class MyCommandTest {
    @Test
    void basicTest() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new MyCommand(), "myplugin.command")) {
            tester.execute("mycmd arg", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("expected message");
    }
}
```

`FakeSender.player(...)` and `FakeSender.console()` grant every permission. In
`velocity-testing`, `permissions(...)` leaves the other nodes unset, so a node is then
granted only if its `DefaultPermission` is `ALL`, as on a real proxy. Use
`FakeSender.playerBuilder(name)` or `FakeSender.consoleBuilder()` to set several
attributes at once:

```java
class MyCommandTest {
    @Test
    void permissionTest() {
        FakeSender sender = FakeSender.playerBuilder("Alice")
                                      .uniqueId(UUID.randomUUID())
                                      .locale("ja_jp")
                                      .op(false)
                                      .permissions("myplugin.command.mycmd")
                                      .denyPermissions("myplugin.command.mycmd.admin")
                                      .build();
        // ...
    }
}
```

`tester.suggest(input, sender)` returns the suggestion texts as a `List<String>`.
`tester.assertCommandTreeMatchesSnapshot("name.snap")` compares the command tree
(aliases, permission nodes, options, argument branches) with
`src/test/resources/snapshots/name.snap`; the first run writes it, and
`COMMANDLIB_UPDATE_SNAPSHOTS=true` rewrites it.

With JUnit 5, `CommandLibExtension` creates and closes a tester per test and
injects it and `FakeSender` parameters (configured with `@Sender`; injected
players are registered with `withFakePlayer`):

```java
class HealCommandTest {
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(HealCommand::new, "myplugin.command"));

    @Test
    void heal(CommandTester tester, @Sender("Steve") FakeSender steve, @Sender("Admin") FakeSender admin) {
        tester.execute("heal Steve", admin);
        assertThat(admin.getSentMessageTexts()).containsExactly("healed Steve");
    }
}
```

See `docs/testing.md` in the CommandLib repository for the full guide.

## NMS-backed Arguments

This section applies to `spigot-testing`.

NMS-backed arguments such as `PlayerArgument`, `EnchantmentArgument`, and
`ItemStackArgument` call `NMSClassRegistry` in their constructors. Use the
`Supplier<Command>` constructor form so the NMS mocks are active when the
command is built.

```java
class HealCommandTest {
    @Test
    void healTest() {
        FakeSender sender = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("target")).execute((ctx, target) -> ctx.sendMessage("healed " + target.getName()));
        }}, "myplugin.command")) {
            tester.withFakePlayer((Player) sender.asSender());
            tester.execute("heal Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("healed Steve");
    }
}
```

`WorldArgument` is NMS-backed too. The mock reads the world from
`withFakeWorld(dimensionKey, world)`, not from `Bukkit.getWorld`:

```java
try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
    argument(new WorldArgument("world")).execute((ctx, world) -> ctx.sendMessage(world.getName()));
}}, "myplugin.command").withFakeWorld("minecraft:the_nether", netherWorld)) {
    tester.execute("tp minecraft:the_nether", sender);
}
```

## Bukkit Static APIs

Arguments that call Bukkit static APIs at parse time, such as
`OfflinePlayerArgument` and `UUIDArgument`, need `mockito-inline`
`MockedStatic`. Declare it before `CommandTester` in the same
try-with-resources block, so the tester closes first.

```java
class InfoCommandTest {
    @Test
    void findsOfflinePlayer() {
        FakeSender sender = FakeSender.player("Alice");
        OfflinePlayer steve = Mockito.mock(OfflinePlayer.class);
        Mockito.when(steve.getName())
               .thenReturn("Steve");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("info") {{
                 argument(new OfflinePlayerArgument("player")).execute((ctx, player) -> ctx.sendMessage(player.getName()));
             }}, "myplugin.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{steve});
            tester.execute("info steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve");
    }
}
```

In spigot-testing, arguments that read the Bukkit `Registry`, such as
`SoundArgument`, `BiomeArgument`, `AttributeArgument` and `EntityTypeArgument`,
need `Mockito.mockStatic(Bukkit.class)` declared before the `CommandTester`.

## Paper ArgumentTypes

This section applies to `paper-testing`.

Paper-backed arguments use Paper `ArgumentTypes` and resolver APIs. Use the
`Supplier<Command>` constructor form so `paper-testing` can install its
`ArgumentTypes` fakes while the command is built.

```java
class PaperCommandTest {
    @Test
    void playerArgumentTest() {
        FakeSender sender = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("target")).execute((ctx, target) -> ctx.sendMessage("healed " + target.getName()));
        }}, "myplugin.command")) {
            tester.withFakePlayer((Player) sender.asSender());
            tester.execute("heal Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("healed Steve");
    }
}
```

`paper-testing` fakes selector, position, item, block data, and some registry
argument paths for command-level tests. It does not emulate Paper lifecycle
registration; registration is covered by integration tests on real Paper
servers.

Registry arguments (`RegistryArgument`, `EnchantmentArgument`, `ParticleArgument`,
`EntityTypeArgument`, ...) resolve only the values added with
`tester.withFakeRegistryValue(RegistryKey.PARTICLE_TYPE, "minecraft:poof", Particle.POOF)`;
every other key fails while parsing, as on a server. Value classes such as
`Enchantment`, `Attribute`, `PotionEffectType` and `Biome` cannot be mocked
without Paper's `RegistryAccess`; use MockBukkit, or the `ServiceLoader` setup
in the CommandLib testing docs ("Registry values on Paper"). Verify real
registry values with integration tests.
