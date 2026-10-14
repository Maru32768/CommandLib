package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.ArgumentValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the behavior docs/errors.md and docs/testing.md describe, with the examples they use.
 */
class DocumentedErrorBehaviorTest {
    @Test
    void missing_argument_sends_the_generated_help() {
        FakeSender sender = FakeSender.player("Alice");
        try (CommandTester tester = new CommandTester(() -> new Command("msg") {{
            argument(new StringArgument("target", StringArgument.Type.WORD),
                     new StringArgument("text")).execute((target, text, ctx) -> ctx.sendMessage("sent"));
        }}, "test.command")) {
            tester.execute("msg Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).contains("Usage:")
                                                .doesNotContain("sent");
    }

    @Test
    void custom_validator_message_is_sent_as_failure() {
        FakeSender sender = FakeSender.player("Alice");
        try (CommandTester tester = new CommandTester(() -> new Command("give") {{
            argument(new IntegerArgument("amount").validator((amount, ctx) -> {
                if (amount % 2 != 0) {
                    throw new ArgumentValidationException("The amount must be even.");
                }
            })).execute((amount, ctx) -> ctx.sendSuccess("ok " + amount));
        }}, "test.command")) {
            tester.execute("give 3", sender);
            tester.execute("give 4", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("The amount must be even.", "ok 4");
    }

    @Test
    void denied_argument_branch_is_hidden_from_brigadier() {
        FakeSender sender = FakeSender.player("Alice")
                                      .denyPermissions("myplugin.command.config.key");
        try (CommandTester tester = new CommandTester(() -> new Command("config") {{
            argument(new StringArgument("key")).execute((key, ctx) -> ctx.sendMessage("config " + key));
        }}, "myplugin.command")) {
            assertThatThrownBy(() -> tester.execute("config difficulty", sender)).isInstanceOf(RuntimeException.class);
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }
}
