package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void optionsFollowTheArgumentsWhenTheCommandAllowsIt() {
        CommandTester tester = new CommandTester(() -> new Command("heal") {{
            optionsAnywhere();
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> times = option(Options.integer("times", 't', 1));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((target, amount, ctx) -> {
                ctx.sendMessage(target.getName() + ":" + amount + ":" + ctx.getOption(force) + ":"
                        + ctx.getOption(times));
            });
        }}, "test.command").withFakePlayer(player("Steve"));
        FakeSender sender = FakeSender.player("Admin");

        tester.execute("heal Steve -f 5", sender);
        tester.execute("heal Steve 5 -t 3 -f", sender);
        tester.execute("heal -t 2 Steve -f 5", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve:5:true:1", "Steve:5:true:3", "Steve:5:true:2");
    }

    @Test
    void anOptionWithAPermissionIsRejectedWithoutIt() {
        CommandTester tester = new CommandTester(() -> new Command("ban") {{
            CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's')
                                                                          .permission(DefaultPermission.OP));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(silent) + ":" + ctx.getOption(force));
            });
        }}, "test.command");
        FakeSender admin = FakeSender.player("Admin");
        FakeSender mod = FakeSender.player("Mod")
                                   .denyPermissions("test.command.ban.option.silent");

        tester.execute("ban -fs Steve", admin);
        tester.execute("ban -f Steve", mod);

        assertThatThrownBy(() -> tester.execute("ban -f -s Steve", mod)).hasMessageContaining("Command syntax error");
        assertThat(admin.getSentMessageTexts()).containsExactly("Steve:true:true");
        assertThat(mod.getSentMessageTexts()).containsExactly("Steve:false:true");
        assertThat(tester.commandTree()).contains("--silent, -s [test.command.ban.option.silent, OP]");
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
