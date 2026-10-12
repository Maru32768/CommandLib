package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandOptionTest {
    @Test
    void command_tester_executes_command_with_options() {
        try (CommandTester tester = new CommandTester(new Command("scan") {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force) + ":" + ctx.getOption(limit));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("scan -f -n 20 Alex", sender);
            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:true:20");
        }
    }

    @Test
    void help_message_includes_options() {
        try (CommandTester tester = new CommandTester(new Command("scan") {{
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));

            argument(new StringArgument("target")).execute((target, ctx) -> {
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("scan", sender);
            assertThat(sender.getSentMessageTexts()).anyMatch(x -> x.contains("Usage:"))
                                                    .anyMatch(x -> x.contains("/scan") && x.contains("options") && x.contains("target"))
                                                    .anyMatch(x -> x.contains("Options:"))
                                                    .anyMatch(x -> x.contains("-f") && x.contains("--force") && x.contains("Force execution"))
                                                    .anyMatch(x -> x.contains("-n") && x.contains("--limit") && x.contains("Maximum count"));
        }
    }
}
