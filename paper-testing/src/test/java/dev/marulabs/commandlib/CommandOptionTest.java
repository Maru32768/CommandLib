package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CommandOptionTest {
    @Test
    void executesCommandWithOptions() {
        CommandTester tester = new CommandTester(new Command("scan") {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force) + ":" + ctx.getOption(limit));
            });
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("scan -f -n 20 Alex", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Alex:true:20");
    }

    @Test
    void anOptionTakesAPlayerArgument() {
        CommandTester tester = new CommandTester(() -> new Command("heal") {{
            CommandOption<Player, CommandContext> target = option(Options.of("target",
                                                                             't',
                                                                             new PlayerArgument("target")));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new IntegerArgument("amount")).execute((amount, ctx) -> {
                ctx.sendMessage(ctx.getOption(target)
                                   .getName()
                        + ":" + amount + ":" + ctx.getOption(force));
            });
        }}, "test.command").withFakePlayer(player("Steve"));
        FakeSender sender = FakeSender.player("Admin");

        tester.execute("heal -t Steve -f 5", sender);
        tester.execute("heal -f --target Steve 6", sender);
        tester.execute("heal 7", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve:5:true",
                                                                 "Steve:6:true",
                                                                 "Missing required option --target.");
    }

    private static Player player(String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName())
               .thenReturn(name);
        return player;
    }

    @Test
    void helpMessageIncludesOptions() {
        CommandTester tester = new CommandTester(new Command("scan") {{
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));

            argument(new StringArgument("target")).execute((target, ctx) -> {
            });
        }}, "test.command");
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
