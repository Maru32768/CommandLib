package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.core.Registry;*/

import static org.assertj.core.api.Assertions.assertThat;

class RegistryArgumentTest {
    private static CommandTester tester() {
        return new CommandTester(() -> new Command("give") {{
            //? if >=1.19.3 {
            argument(new RegistryArgument<>("item", Registries.ITEM)).execute((item, ctx) -> {
                //?} else
                /*argument(new RegistryArgument<>("item", Registry.ITEM_REGISTRY)).execute((item, ctx) -> {*/
                ctx.sendMessage(String.valueOf(item == Items.DIAMOND));
            });
        }}, "test.command");
    }

    @Test
    void value_is_read_by_its_key_with_minecraft_as_the_default_namespace() {
        List<String> messages;

        try (CommandTester tester = tester()) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("give minecraft:diamond", sender);
            tester.execute("give diamond", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("true", "true");
    }

    @Test
    void unknown_key_sends_failure_message() {
        List<String> messages;

        try (CommandTester tester = tester()) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("give commandlib:missing", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("commandlib:missing is not a known registry entry");
    }

    @Test
    void registry_keys_containing_the_input_are_suggested() {
        List<String> suggestions;

        try (CommandTester tester = tester()) {
            suggestions = tester.suggestions("give diamond_sw", FakeSender.player("Alice"))
                                .join()
                                .getList()
                                .stream()
                                .map(Suggestion::getText)
                                .collect(Collectors.toList());
        }

        assertThat(suggestions).contains("minecraft:diamond_sword")
                               .doesNotContain("minecraft:stone");
    }
}
