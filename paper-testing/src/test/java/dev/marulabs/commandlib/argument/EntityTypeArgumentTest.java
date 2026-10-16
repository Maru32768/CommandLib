package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class EntityTypeArgumentTest {
    private static CommandTester tester(AtomicReference<EntityType> received, EntityType value) {
        return new CommandTester(() -> new Command("summon") {{
            argument(new EntityTypeArgument("value")).execute((parsed, ctx) -> received.set(parsed));
        }}, "test.command").withFakeRegistryValue(RegistryKey.ENTITY_TYPE, "minecraft:pig", value);
    }

    @Test
    void value_is_read_from_the_entity_type_registry_by_its_key() {
        AtomicReference<EntityType> received = new AtomicReference<>();
        EntityType value = EntityType.PIG;

        try (CommandTester tester = tester(received, value)) {
            tester.execute("summon minecraft:pig", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(value);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<EntityType> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, EntityType.PIG)) {
            assertThatThrownBy(() -> tester.execute("summon commandlib:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
