package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLibExtensionTest {
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(greetCommand(),
                                                                                           "myplugin.command"));

    private CommandTester testerFromBeforeEach;

    @BeforeEach
    void captureTester(CommandTester tester) {
        testerFromBeforeEach = tester;
    }

    @Test
    void injects_the_tester_and_a_default_player(CommandTester tester, FakeSender sender) {
        tester.execute("greet Alex", sender);

        assertThat(tester).isSameAs(testerFromBeforeEach);
        assertThat(sender.getName()).isEqualTo("Player");
        assertThat(sender.isPlayer()).isTrue();
        assertThat(sender.getSentMessageTexts()).containsExactly("Hello Alex");
    }

    @Test
    void configures_senders_from_the_annotation(CommandTester tester,
                                                @Sender(value = "Alice", permissions = "myplugin.command.greet") FakeSender alice,
                                                @Sender(console = true, denyPermissions = "myplugin.command.greet.admin") FakeSender console) {
        assertThat(alice.getName()).isEqualTo("Alice");
        assertThat(alice.hasPermission("myplugin.command.greet")).isTrue();
        assertThat(alice.hasPermission("myplugin.command.greet.admin")).isFalse();
        assertThat(console.isConsole()).isTrue();
        assertThat(console.isOperator()).isTrue();
        assertThatThrownBy(() -> tester.execute("greet admin 1 spam", console)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void suggest_returns_suggestion_texts(CommandTester tester, FakeSender sender) {
        assertThat(tester.suggest("greet a", sender)).containsExactly("admin");
    }

    @Test
    void command_tree_matches_snapshot(CommandTester tester) {
        tester.assertCommandTreeMatchesSnapshot("command-tree-greet.snap");
    }

    private static TestCommand greetCommand() {
        return new TestCommand("greet") {{
            addAliases("hi");
            option(Options.flag("loud", 'l'));
            option(Options.integer("times", 't', 1, 1, 5));
            argument(new CommonStringArgument<>("name")).execute(ctx -> ctx.sendMessage("Hello "
                    + ctx.getInput("name")))
                                                        .permission(DefaultPermission.ALL);
            addChildren(new TestCommand("admin") {{
                argument(new CommonIntegerArgument<>("level"),
                         new CommonStringArgument<>("reason")).execute(ctx -> {
                         });
            }});
        }};
    }
}
