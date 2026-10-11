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
            argument(new PlayerArgument("target")).execute((target, ctx) -> ctx.sendMessage("healed " + target.getName()));
        }}, "myplugin.command")) {
            tester.withFakePlayer((Player) sender.asSender());
            tester.execute("heal Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("healed Steve");
    }
}
```

## Bukkit Static APIs

Arguments that call Bukkit static APIs at parse time, such as `WorldArgument`,
`OfflinePlayerArgument`, and `TeamArgument`, need `mockito-inline`
`MockedStatic` in the same try-with-resources block as `CommandTester`.

```java
class TpCommandTest {
    @Test
    void tpTest() {
        FakeSender sender = FakeSender.player("Alice");
        World mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getName())
               .thenReturn("nether");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); CommandTester tester = new CommandTester(
                new Command("tp") {{
                    argument(new WorldArgument("world")).execute((world, ctx) -> ctx.sendMessage(world.getName()));
                }},
                "myplugin.command")) {
            bukkit.when(() -> Bukkit.getWorld("nether"))
                  .thenReturn(mockWorld);
            tester.execute("tp nether", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("nether");
    }
}
```

Arguments backed by non-Bukkit static registries, such as `EnchantmentArgument`
or `PotionEffectArgument`, need `MockedStatic` for their respective classes.

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
            argument(new PlayerArgument("target")).execute((target, ctx) -> ctx.sendMessage("healed " + target.getName()));
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

`EnchantmentArgument` and `PotionEffectArgument` can be wired into commands in
`paper-testing`, but command-level execution with concrete registry values needs
Paper's server-side `RegistryAccess` bootstrap. Verify their real registry values
with integration tests.
