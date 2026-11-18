package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayersArgumentTest {
    @Test
    void resolves_fake_player_as_singleton_list() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("kick") {{
            argument(new PlayersArgument("targets")).execute((ctx, targets) -> {
                targets.forEach(p -> ctx.sendMessage("Kicked " + p.getName()));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("kick Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Kicked Steve");
    }
}
