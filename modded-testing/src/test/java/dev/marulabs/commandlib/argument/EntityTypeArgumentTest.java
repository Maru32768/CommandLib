package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeArgumentTest {
    @Test
    void every_entity_type_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("type") {{
            argument(new EntityTypeArgument("type")).execute((ctx, type) -> {
                ctx.sendMessage(String.valueOf(EntityType.getKey(type)));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("type minecraft:zombie", sender);
            // Players cannot be summoned, but they are an entity type.
            tester.execute("type player", sender);
            String unknown = RegistryArgumentTestSupport.failure(tester, "type commandlib:missing", sender);
            suggestions = tester.suggest("type zombie_villa", sender);
            messages = sender.getSentMessageTexts();
            assertThat(unknown).contains("commandlib:missing");
        }

        assertThat(messages).startsWith("minecraft:zombie", "minecraft:player");
        assertThat(suggestions).containsExactly("minecraft:zombie_villager");
    }
}
