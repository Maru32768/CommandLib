package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.EnumArgument;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.util.text.RichText;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class HelpErrorFormatterSnapshotTest {
    private static final List<String> INPUTS = List.of("arena",
                                                       "arena create",
                                                       "arena mode foo 3",
                                                       "arena mode solo 3");

    @Test
    void custom_formatters_match_snapshot() {
        FakeSender sender = FakeSender.player("Alice");
        List<String> lines = new ArrayList<>();
        try (CommandTester tester = new CommandTester(() -> new Command("arena") {{
            description("Manage arenas");
            examples("/arena create lobby 16");
            helpFormatter((ctx, help) -> {
                ctx.sendMessage(RichText.text("[Arena] " + help.description())
                                        .color(0xFFAA00));
                help.usages()
                    .forEach(x -> ctx.sendMessage(x.line()));
                help.examples()
                    .forEach(x -> ctx.sendMessage("e.g. " + x));
            });
            usageFormatter((ctx, usage) -> usage.plainText() + (usage.description()
                                                                     .isEmpty() ? ""
                                                                             : " - " + usage.description()));
            errorFormatter(ErrorFormatter.SYNTAX_ADVICE);
            addChildren(new Command("create") {{
                description("Create an arena");
                argument(new StringArgument("name"),
                         new IntegerArgument("size").optional(16)).execute((name, size, ctx) -> ctx.sendSuccess("Created " + name));
            }}, new Command("mode") {{
                argument(new EnumArgument<>("mode", Mode.class),
                         new IntegerArgument("rounds")).execute((mode, rounds, ctx) -> ctx.sendSuccess("Mode " + mode));
            }});
        }}, "test.command")) {
            for (String input : INPUTS) {
                int before = sender.getSentMessageTexts()
                                   .size();
                int result = tester.execute(input, sender);
                lines.add("/" + input + " -> " + result);
                sender.getSentMessageTexts()
                      .subList(before,
                               sender.getSentMessageTexts()
                                     .size())
                      .forEach(x -> lines.add("    " + x));
            }
        }

        Snapshots.assertMatches("help-error-formatter.snap", String.join("\n", lines));
    }

    private enum Mode {
        SOLO
    }
}
