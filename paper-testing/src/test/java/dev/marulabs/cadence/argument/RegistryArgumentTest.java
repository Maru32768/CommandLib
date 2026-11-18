package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class RegistryArgumentTest {
    private static CommandTester tester() {
        return new CommandTester(() -> new Command("spawn") {{
            argument(new RegistryArgument<>("type",
                                            RegistryKey.ENTITY_TYPE)).execute((ctx, type) -> ctx.sendMessage(type.name()));
        }}, "test.command").withFakeRegistryValue(RegistryKey.ENTITY_TYPE, "zombie", EntityType.ZOMBIE);
    }

    @Test
    void value_is_read_by_its_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester()) {
            tester.execute("spawn minecraft:zombie", sender);
            tester.execute("spawn zombie", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ZOMBIE", "ZOMBIE");
    }

    @Test
    void unknown_key_is_a_syntax_error() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester()) {
            assertThatThrownBy(() -> tester.execute("spawn minecraft:pig",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }
}
