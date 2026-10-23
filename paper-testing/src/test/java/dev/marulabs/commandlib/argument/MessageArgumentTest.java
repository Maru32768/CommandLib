package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void missing_message_is_not_executed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("shout 2 ",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(sender.getSentMessageTexts()).noneMatch(x -> x.startsWith("2:"));
    }

    @Test
    void there_are_no_suggestions() {
        try (CommandTester tester = new CommandTester(MessageArgumentTest::command, "test.command")) {
            assertThat(tester.suggest("shout 2 he", FakeSender.player("Alice"))).isEmpty();
        }
    }

    private static Command command() {
        return new Command("shout") {{
            argument(new IntegerArgument("times"),
                     new MessageArgument("message")).execute((ctx, times, message) -> {
                         ctx.sendMessage(times + ": " + PlainTextComponentSerializer.plainText()
                                                                                    .serialize(message));
                     });
        }};
    }
}
