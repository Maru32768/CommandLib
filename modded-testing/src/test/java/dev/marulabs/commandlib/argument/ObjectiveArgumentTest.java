package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectiveArgumentTest {
    @Test
    void custom_scoreboard_is_resolved_on_each_parse_and_suggestion() {
        AtomicReference<Scoreboard> current = new AtomicReference<>();
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("objective") {{
            argument(new ObjectiveArgument("name").scoreboard(current::get)).execute((objective, ctx) -> {
                ctx.sendMessage(objective.getName());
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            Scoreboard scoreboard = new Scoreboard();
            addObjective(scoreboard, "kills");
            current.set(scoreboard);

            tester.execute("objective kills", sender);
            assertThat(tester.suggest("objective ki", sender)).containsExactly("kills");

            current.set(new Scoreboard());
            tester.execute("objective kills", sender);
            assertThat(tester.suggest("objective ki", sender)).isEmpty();
            messages = sender.getSentMessageTexts();
        }

        // The same message as the server scoreboard's and the other platforms'.
        assertThat(messages).containsExactly("kills", "Unknown scoreboard objective 'kills'");
    }

    @Test
    void server_scoreboard_is_the_default() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("objective") {{
            argument(new ObjectiveArgument("name")).execute((objective,
                                                             ctx) -> ctx.sendMessage(objective.getName()));
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            Objective kills = addObjective(new Scoreboard(), "kills");
            ServerScoreboard scoreboard = Mockito.mock(ServerScoreboard.class);
            Mockito.when(scoreboard.getObjective("kills"))
                   .thenReturn(kills);
            tester.withServer(server -> Mockito.when(server.getScoreboard())
                                               .thenReturn(scoreboard));

            tester.execute("objective kills", sender);
            tester.execute("objective deaths", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("kills", "Unknown scoreboard objective 'deaths'");
    }

    private static Objective addObjective(Scoreboard scoreboard, String name) {
        //? if >=1.20.3 {
        /*return scoreboard.addObjective(name,
                                       ObjectiveCriteria.DUMMY,
                                       Component.nullToEmpty(name),
                                       ObjectiveCriteria.RenderType.INTEGER,
                                       false,
                                       null);
        *///?} else {
        return scoreboard.addObjective(name,
                                       ObjectiveCriteria.DUMMY,
                                       Component.nullToEmpty(name),
                                       ObjectiveCriteria.RenderType.INTEGER);
        //?}
    }
}
