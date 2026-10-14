package dev.marulabs.commandlib;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void command_tester_executes_repeatable_required_and_exclusive_options() {
        try (CommandTester tester = new CommandTester(new Command("tag") {{
            CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                             .repeatable(3)
                                                                             .required());
            CommandOption<Boolean, CommandContext> json = option(Options.flag("json", 'j')
                                                                        .aliases("machine"));
            CommandOption<Boolean, CommandContext> yaml = option(Options.flag("yaml", 'y'));
            mutuallyExclusive(json, yaml);
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(verbose) + ":" + ctx.getOption(tags) + ":"
                        + ctx.getOption(json));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("tag -vv -t red --machine --tag blue Alex", sender);
            tester.execute("tag Alex", sender);
            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:2:[red, blue]:true",
                                                                     "Missing required option --tag.");
            assertThat(tester.suggestions("tag -j -", sender)
                             .join()
                             .getList()).extracting(Suggestion::getText)
                                        .doesNotContain("-y", "--yaml")
                                        .contains("-t", "--tag");
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
                                                    .anyMatch(x -> x.contains("/scan") && x.contains("options")
                                                            && x.contains("target"))
                                                    .anyMatch(x -> x.contains("Options:"))
                                                    .anyMatch(x -> x.contains("-f") && x.contains("--force")
                                                            && x.contains("Force execution"))
                                                    .anyMatch(x -> x.contains("-n") && x.contains("--limit")
                                                            && x.contains("Maximum count"));
        }
    }
}
