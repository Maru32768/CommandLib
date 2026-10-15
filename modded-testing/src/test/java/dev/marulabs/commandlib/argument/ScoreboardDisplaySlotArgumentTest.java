package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

//? if <1.20.2
import net.minecraft.world.scores.Scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreboardDisplaySlotArgumentTest {
    @Test
    void slot_is_read_by_its_vanilla_name() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("slot") {{
            argument(new ScoreboardDisplaySlotArgument("slot")).execute((slot, ctx) -> {
                //? if >=1.20.2 {
                /*ctx.sendMessage(slot.getSerializedName());
                *///?} else
                ctx.sendMessage(Scoreboard.getDisplaySlotName(slot));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("slot sidebar", sender);
            tester.execute("slot sidebar.team.red", sender);
            assertThatThrownBy(() -> tester.execute("slot nowhere", sender)).isInstanceOf(RuntimeException.class);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("sidebar", "sidebar.team.red");
    }
}
