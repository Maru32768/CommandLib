package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TeamArgumentTest {
    @Test
    void custom_scoreboard_is_resolved_on_each_parse_and_suggestion() {
        AtomicReference<Scoreboard> current = new AtomicReference<>();
        List<String> messages;

        // The mocked server has no scoreboard, so the server scoreboard must not be touched.
        try (CommandTester tester = new CommandTester(() -> new Command("team") {{
            argument(new TeamArgument("name").scoreboard(current::get)).execute((team, ctx) -> {
                ctx.sendMessage(team.getName());
            });
        }}, "test.command")) {
            // Minecraft classes are only usable once the tester has bootstrapped the registries.
            FakeSender sender = FakeSender.player("Alice");
            Scoreboard first = new Scoreboard();
            first.addPlayerTeam("arena_red");
            Scoreboard second = new Scoreboard();
            current.set(first);

            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .join()
                             .getList()).extracting(Suggestion::getText)
                                        .containsExactly("arena_red");

            current.set(second);
            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .join()
                             .getList()).isEmpty();

            current.set(null);
            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .join()
                             .getList()).isEmpty();
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("arena_red",
                                             "Incorrect argument for command",
                                             "§7team §c§narena_red§r<--[HERE]",
                                             "Incorrect argument for command",
                                             "§7team §c§narena_red§r<--[HERE]");
    }
}
