package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalVariadicArgumentTest {
    @Test
    void an_optional_argument_gives_its_default_when_left_out() {
        CommandTester tester = new CommandTester(new Command("heal") {{
            argument(new IntegerArgument("amount").optional(20)).execute((amount, ctx) -> ctx.sendMessage("amount "
                    + amount));
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Admin");

        tester.execute("heal", sender);
        tester.execute("heal 5", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("amount 20", "amount 5");
    }

    @Test
    void a_variadic_argument_takes_every_value_and_needs_one() {
        CommandTester tester = new CommandTester(new Command("kick") {{
            argument(new PlayerArgument("targets").variadic()).execute((targets,
                                                                        ctx) -> ctx.sendMessage(names(targets)));
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Admin");
        tester.withFakePlayer(FakeSender.player("Steve"))
              .withFakePlayer(FakeSender.player("Alex"));

        tester.execute("kick Steve", sender);
        tester.execute("kick Steve Alex", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve", "Steve,Alex");
        assertThat(tester.suggest("kick Steve ", sender)).contains("Alex");
        // Without a value the command has no executor of its own, so it shows its help.
        FakeSender other = FakeSender.player("Other");
        tester.execute("kick", other);
        assertThat(other.getSentMessageTexts()).anyMatch(x -> x.contains("kick <targets...>"));
    }

    @Test
    void an_optional_variadic_argument_after_an_optional_one_may_be_left_out() {
        CommandTester tester = new CommandTester(new Command("heal") {{
            argument(new IntegerArgument("amount").optional(20),
                     new PlayerArgument("targets").variadic()
                                                  .optional(List.of())).execute((amount, targets, ctx) -> ctx.sendMessage(amount + ":" + names(targets)));
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Admin");
        tester.withFakePlayer(FakeSender.player("Steve"))
              .withFakePlayer(FakeSender.player("Alex"));

        tester.execute("heal", sender);
        tester.execute("heal 5", sender);
        tester.execute("heal 5 Steve Alex", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("20:", "5:", "5:Steve,Alex");
    }

    private static String names(List<Player> players) {
        return players.stream()
                      .map(Player::getUsername)
                      .collect(Collectors.joining(","));
    }
}
