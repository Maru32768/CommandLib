package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayersArgumentTest {
    @Test
    void resolves_fake_player_as_singleton_list() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("kick") {{
            argument(new PlayersArgument("targets")).execute((targets, ctx) -> {
                targets.forEach(p -> ctx.sendMessage("Kicked " + p.getName()));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("kick Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Kicked Steve");
    }
}
