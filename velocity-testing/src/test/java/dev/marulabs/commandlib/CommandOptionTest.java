package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandOptionTest {
    @Test
    void an_option_takes_a_player_argument() {
        CommandTester tester = new CommandTester(new Command("heal") {{
            CommandOption<Player, CommandContext> target = option(Options.of("target",
                                                                             't',
                                                                             new PlayerArgument("target")));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new IntegerArgument("amount")).execute((ctx, amount) -> {
                ctx.sendMessage(ctx.getOption(target)
                                   .getUsername()
                        + ":" + amount + ":" + ctx.getOption(force));
            });
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Admin");
        tester.withFakePlayer(FakeSender.player("Steve"));

        tester.execute("heal -t Steve -f 5", sender);
        tester.execute("heal -f --target steve 6", sender);
        tester.execute("heal 7", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve:5:true",
                                                                 "Steve:6:true",
                                                                 "Missing required option --target.");
        assertThat(tester.suggest("heal -f -t ", sender)).contains("Steve");
    }

    @Test
    void options_follow_the_arguments_when_the_command_allows_it() {
        CommandTester tester = new CommandTester(new Command("heal") {{
            optionsAnywhere();
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> times = option(Options.integer("times", 't', 1));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((ctx, target, amount) -> {
                ctx.sendMessage(target.getUsername() + ":" + amount + ":" + ctx.getOption(force) + ":"
                        + ctx.getOption(times));
            });
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Admin");
        tester.withFakePlayer(FakeSender.player("Steve"));

        tester.execute("heal Steve -f 5", sender);
        tester.execute("heal Steve 5 -t 3 -f", sender);
        tester.execute("heal -t 2 Steve -f 5", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve:5:true:1", "Steve:5:true:3", "Steve:5:true:2");
        assertThat(tester.suggest("heal Steve -", sender)).contains("-f", "--times");
    }

    @Test
    void a_command_is_shown_and_run_only_while_its_predicate_holds() {
        java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean();
        CommandTester tester = new CommandTester(new Command("game") {{
            addChildren(new Command("start") {{
                visibleWhen(ctx -> !running.get() && ctx.getActor()
                                                        .getName()
                                                        .equals("Steve"));
                execute(ctx -> running.set(true));
            }});
        }}, "proxy.command");
        FakeSender steve = FakeSender.player("Steve");
        FakeSender alex = FakeSender.player("Alex");

        assertThatThrownBy(() -> tester.execute("game start", alex)).hasMessageContaining("Command syntax error");
        tester.execute("game start", steve);
        assertThat(running).isTrue();
        assertThatThrownBy(() -> tester.execute("game start", steve)).hasMessageContaining("Command syntax error");
    }

    @Test
    void an_option_with_a_permission_is_rejected_without_it() {
        CommandTester tester = new CommandTester(new Command("ban") {{
            CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's')
                                                                          .permission(DefaultPermission.OP));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new StringArgument("target")).execute((ctx, target) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(silent) + ":" + ctx.getOption(force));
            });
        }}, "proxy.command");
        FakeSender admin = FakeSender.player("Admin");
        FakeSender mod = FakeSender.player("Mod")
                                   .denyPermissions("proxy.command.ban.option.silent");

        tester.execute("ban -fs Steve", admin);
        tester.execute("ban -f Steve", mod);

        assertThatThrownBy(() -> tester.execute("ban -f -s Steve", mod)).hasMessageContaining("Command syntax error");
        assertThat(admin.getSentMessageTexts()).containsExactly("Steve:true:true");
        assertThat(mod.getSentMessageTexts()).containsExactly("Steve:false:true");
        assertThat(tester.commandTree()).contains("--silent, -s [proxy.command.ban.option.silent, OP]");
    }
}
