# Cadence common-testing

`common-testing` provides lightweight test utilities for Cadence's platform-neutral `common` API.
Use it for tests built on `CommonCommand`, `CommonCommandContext`, and `Common*Argument` classes.

It does not depend on Bukkit, Paper, Forge, or a running Minecraft server.

## Installation

Add the dependency to your test scope. Replace `latest.release` with a specific version.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    testImplementation "com.github.Maru32768.Cadence:common-testing:latest.release"
}
```

## Usage

Create a `TestCommand`, execute it with a `FakeSender`, and assert on the messages captured by the sender.

```java
class HelloCommandTest {
    @Test
    void hello_sends_message() {
        TestCommand command = new TestCommand("hello") {{
            argument(new CommonStringArgument<>("name")).execute(ctx -> {
                ctx.sendSuccess("Hello, " + ctx.getInput("name") + "!");
            });
        }};

        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(command, "myplugin.command")) {
            tester.execute("hello Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello, Steve!");
    }
}
```

## Notes

- Use `common-testing` when testing platform-neutral command behavior.
- Use other testing modules when testing platform-specific arguments, senders, components, or registry behavior.
- Do not put multiple testing modules on the same test runtime classpath unless you intentionally manage classpath
  ordering. They expose the same package-level testing API names, such as `CommandTester` and `FakeSender`.
