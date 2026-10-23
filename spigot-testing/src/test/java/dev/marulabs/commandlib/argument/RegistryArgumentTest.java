package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Registry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryArgumentTest {
    private static Command command() {
        return new Command("spawn") {{
            argument(new RegistryArgument<>("type",
                                            Registry.ENTITY_TYPE)).execute((ctx, type) -> ctx.sendMessage(type.name()));
        }};
    }

    @Test
    void value_is_read_by_its_key_with_minecraft_as_the_default_namespace() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(RegistryArgumentTest::command, "test.command")) {
            tester.execute("spawn minecraft:zombie", sender);
            tester.execute("spawn zombie", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ZOMBIE", "ZOMBIE");
    }

    @Test
    void unknown_key_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(RegistryArgumentTest::command, "test.command")) {
            tester.execute("spawn commandlib:missing", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("commandlib:missing is not a known registry entry");
    }

    @Test
    void registry_keys_containing_the_input_are_suggested() throws Exception {
        try (CommandTester tester = new CommandTester(RegistryArgumentTest::command, "test.command")) {
            var suggestions = tester.suggestions("spawn zomb", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .contains("minecraft:zombie", "minecraft:zombie_villager")
                                   .doesNotContain("minecraft:pig");
        }
    }
}
