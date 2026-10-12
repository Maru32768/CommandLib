package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerArgumentTest {
    @Test
    void resolves_fake_player_by_name() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage("Healed " + target.getName() + "!");
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("heal Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve!");
    }

    @Test
    void get_player_returns_resolved_player() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("info") {{
            requirePlayer();
            argument(new PlayerArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(ctx.getPlayer()
                                   .getName() + " targeted " + target.getName());
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("info Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Admin targeted Steve");
    }
}
