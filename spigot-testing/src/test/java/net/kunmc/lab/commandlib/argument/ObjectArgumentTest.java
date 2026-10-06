package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectArgumentTest {
    @Test
    void object_is_resolved_by_name() {
        FakeSender sender = FakeSender.player("Alice");
        Map<String, Integer> items = Map.of("sword", 1, "bow", 2);

        try (CommandTester tester = new CommandTester(new Command("give") {{
            argument(new ObjectArgument<>("item", items)).execute((item, ctx) -> {
                ctx.sendMessage("item=" + item);
            });
        }}, "test.command")) {
            tester.execute("give sword", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("item=1");
    }

    @Test
    void unknown_name_causes_failure() {
        FakeSender sender = FakeSender.player("Alice");
        Map<String, Integer> items = Map.of("sword", 1);

        try (CommandTester tester = new CommandTester(new Command("give") {{
            argument(new ObjectArgument<>("item", items)).execute((item, ctx) -> {
                ctx.sendMessage("item=" + item);
            });
        }}, "test.command")) {
            tester.execute("give axe", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "give axe<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()
                         .getColor()).isEqualTo(new java.awt.Color(0xFF5555));
    }
}
