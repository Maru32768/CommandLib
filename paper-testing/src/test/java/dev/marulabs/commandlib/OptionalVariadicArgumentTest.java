package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalVariadicArgumentTest {
    @Test
    void optional_and_variadic_arguments_parse_paper_argument_types() {
        CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new IntegerArgument("amount").optional(20),
                     new PlayerArgument("targets").variadic()
                                                  .optional(List.of())).execute((amount, targets, ctx) -> {
                                                      ctx.sendMessage(amount + ":" + targets.stream()
                                                                                            .map(Player::getName)
                                                                                            .collect(Collectors.joining(",")));
                                                  });
        }}, "test.command").withFakePlayer(player("Steve"))
                           .withFakePlayer(player("Alex"));
        FakeSender sender = FakeSender.player("Admin");

        tester.execute("heal", sender);
        tester.execute("heal 5", sender);
        tester.execute("heal 5 Steve Alex", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("20:", "5:", "5:Steve,Alex");
    }

    private static Player player(String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName())
               .thenReturn(name);
        return player;
    }
}
