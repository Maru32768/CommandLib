package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla message parser of every node.
class MessageArgumentTest {
    @Test
    void message_takes_the_rest_of_the_input() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            tester.execute("shout 2 hello there, {world}!", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("2: hello there, {world}!");
    }

    @Test
    void selectors_are_kept_as_written_for_a_sender_that_may_not_use_them() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            tester.execute("shout 2 hello @a!", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("2: hello @a!");
    }

    @Test
    void missing_message_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("shout 2 ",
                                                    FakeSender.player("Alice"))).hasMessageContaining("Command syntax error");
        }
    }

    @Test
    void there_are_no_suggestions() {
        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            assertThat(tester.suggestions("shout 2 he", FakeSender.player("Alice"))
                             .join()
                             .getList()).isEmpty();
        }
    }

    private static Command command() {
        return new Command("shout") {{
            argument(new IntegerArgument("times"),
                     new MessageArgument("message")).execute((times, message, ctx) -> {
                         ctx.sendMessage(times + ": " + message.getString());
                     });
        }};
    }
}
