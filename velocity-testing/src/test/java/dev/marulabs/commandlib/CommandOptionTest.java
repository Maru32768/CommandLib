package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandOptionTest {
    @Test
    void an_option_takes_a_player_argument() {
        CommandTester tester = new CommandTester(new Command("heal") {{
            CommandOption<Player, CommandContext> target = option(Options.of("target",
                                                                             't',
                                                                             new PlayerArgument("target")));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new IntegerArgument("amount")).execute((amount, ctx) -> {
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
}
