# CommandLib paper-testing

`paper-testing` provides lightweight test utilities for CommandLib's Paper API.
Use it for tests built on `Command`, `CommandContext`, Paper-backed arguments, and Adventure components.

It does not depend on a running Paper server.

## Installation

Add the dependency to your test scope. Replace `latest.release` with a specific version.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    testImplementation "com.github.Maru32768.CommandLib:paper-testing:latest.release"
}
```

## Usage

Create a `Command`, execute it with a `FakeSender`, and assert on the messages captured by the sender.

```java
class HelloCommandTest {
    @Test
    void hello_sends_message() {
        Command command = new Command("hello") {{
            argument(new StringArgument("name")).execute((ctx, name) -> {
                ctx.sendSuccess("Hello, " + name + "!");
            });
        }};

        FakeSender sender = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(command, "myplugin.command")) {
            tester.execute("hello Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello, Steve!");
        assertThat(sender.getSentMessages())
            .extracting(Component::color)
            .containsExactly(NamedTextColor.GREEN);
    }
}
```

## Notes

- Use `paper-testing` when testing Paper-specific command behavior, Paper argument providers, senders, or Adventure
  components.
- Use `common-testing` when testing platform-neutral command behavior.
- Use other testing modules when testing behavior for another platform.
- Do not put multiple testing modules on the same test runtime classpath unless you intentionally manage classpath
  ordering. They expose the same package-level testing API names, such as `CommandTester` and `FakeSender`.
- `FakeSender.player()` and `FakeSender.console()` grant all permissions by default. Use `permissions(...)` or Mockito
  stubbing on `asSender()` when testing permission-denied behavior.
- `FakeSender.playerBuilder(name)` and `FakeSender.consoleBuilder()` set the name, UUID, locale, operator flag, and
  granted and denied permissions in one place; denied nodes win over granted ones.
- `getSentMessages()` returns Adventure `Component` values. Use `getSentMessageTexts()` for plain-text assertions.
- `CommandTester` exposes Brigadier suggestions so tooltip behavior can be asserted.
- `paper-testing` stubs Paper's vanilla argument provider, selectors, registry-backed arguments, and resolver-backed
  argument parsing for command-level tests. Real registration and server behavior remain covered by integration tests.
- `EnchantmentArgument` and `PotionEffectArgument` can be wired into commands, but command-level execution with concrete
  registry values requires Paper's server-side `RegistryAccess` bootstrap. Verify their real registry values in
  integration tests.
