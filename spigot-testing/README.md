# CommandLib spigot-testing

`spigot-testing` provides lightweight test utilities for CommandLib's Spigot API.
Use it for tests built on `Command`, `CommandContext`, Bukkit-backed arguments, and Spigot/Bungee components.

It does not depend on a running Minecraft server.

## Installation

Add the dependency to your test scope. Replace `latest.release` with a specific version.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    testImplementation "com.github.Maru32768.CommandLib:spigot-testing:latest.release"
}
```

## Usage

Create a `Command`, execute it with a `FakeSender`, and assert on the messages captured by the sender.

```java
class HelloCommandTest {
    @Test
    void hello_sends_message() {
        Command command = new Command("hello") {{
            argument(new StringArgument("name")).execute((name, ctx) -> {
                ctx.sendSuccess("Hello, " + name + "!");
            });
        }};

        FakeSender sender = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(command, "myplugin.command")) {
            tester.execute("hello Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello, Steve!");
        assertThat(sender.getSentMessages())
            .extracting(BaseComponent::getColor)
            .containsExactly(ChatColor.GREEN);
    }
}
```

When the command contains NMS-backed arguments such as `PlayerArgument`, create the command through a supplier so the
tester can install its NMS mocks before the argument constructors run.

```java
class HealCommandTest {
    @Test
    void heal_sends_message() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("target")).execute((target, ctx) -> {
                ctx.sendSuccess("Healed " + target.getName() + "!");
            });
        }}, "myplugin.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("heal Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve!");
    }
}
```

## Notes

- Use `spigot-testing` when testing Spigot-specific command behavior, Bukkit arguments, senders, or Bungee components.
- Use `common-testing` when testing platform-neutral command behavior.
- Use other testing modules when testing behavior for another platform.
- Do not put multiple testing modules on the same test runtime classpath unless you intentionally manage classpath
  ordering. They expose the same package-level testing API names, such as `CommandTester` and `FakeSender`.
- `FakeSender.player()` and `FakeSender.console()` grant all permissions by default. Use `permissions(...)` or Mockito
  stubbing on `asSender()` when testing permission-denied behavior.
- `FakeSender.playerBuilder(name)` and `FakeSender.consoleBuilder()` set the name, UUID, locale, operator flag, and
  granted and denied permissions in one place; denied nodes win over granted ones.
- `getSentMessages()` returns Bungee `BaseComponent` values. Use `getSentMessageTexts()` for plain-text assertions and
  `getSentMessageLegacyTexts()` when color codes matter.
- `requirePlayer()` and `requireConsole()` work with `FakeSender`; `FakeSender.player()` passes `instanceof Player`
  checks.
- Use `CommandTester.builder().mockNmsClass(...)` when a test needs an NMS mock that is not built into
  `spigot-testing` yet:
  ```java
  class XxxCommandTest {
      @Test
      void xxx_test() {
          try (CommandTester tester = CommandTester.builder()
                                                   .mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)
                                                   .command(() -> new XxxCommand())
                                                   .permissionPrefix("myplugin.command")
                                                   .build()) {
              // ...
          }
      }
  }
  ```
- Arguments that call Bukkit static APIs at parse time, such as `WorldArgument`, `OfflinePlayerArgument`, and
  `TeamArgument`, require `MockedStatic<Bukkit>` from Mockito in the same try-with-resources block as `CommandTester`.
  Declare the Bukkit static mock before `CommandTester` so `CommandTester` closes first.
