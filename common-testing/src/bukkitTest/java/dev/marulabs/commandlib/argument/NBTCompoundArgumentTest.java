package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NBTCompoundArgumentTest {
    @Test
    void compound_is_returned_as_snbt_and_later_arguments_still_parse() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(NBTCompoundArgumentTest::command, "test.command")) {
            tester.execute("data {CustomName:\"Bob }\",Tags:[a,b]} 3", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("{CustomName:\"Bob }\",Tags:[a,b]} x3");
    }

    @Test
    void input_that_is_not_a_compound_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(NBTCompoundArgumentTest::command, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            assertThatThrownBy(() -> tester.execute("data abc 3",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("data {a:1 3",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }
    }

    @Test
    void there_are_no_suggestions() {
        try (CommandTester tester = new CommandTester(NBTCompoundArgumentTest::command, "test.command")) {
            assertThat(tester.suggest("data {", FakeSender.player("Alice"))).isEmpty();
        }
    }

    private static Command command() {
        return new Command("data") {{
            argument(new NBTCompoundArgument("nbt"), new IntegerArgument("count")).execute((ctx, nbt, count) -> {
                ctx.sendMessage(nbt + " x" + count);
            });
        }};
    }
}
