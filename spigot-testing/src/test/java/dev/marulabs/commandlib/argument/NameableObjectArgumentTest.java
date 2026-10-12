package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NameableObjectArgumentTest {
    static class Item implements Nameable {
        private final String name;
        private final int id;

        Item(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String tabCompleteName() {
            return name;
        }
    }

    @Test
    void nameable_object_is_resolved_by_tab_complete_name() {
        FakeSender sender = FakeSender.player("Alice");
        List<Item> items = List.of(new Item("sword", 1), new Item("bow", 2));

        try (CommandTester tester = new CommandTester(new Command("equip") {{
            argument(new NameableObjectArgument<>("item", items)).execute((item, ctx) -> {
                ctx.sendMessage("equipped id=" + item.id);
            });
        }}, "test.command")) {
            tester.execute("equip bow", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("equipped id=2");
    }
}
