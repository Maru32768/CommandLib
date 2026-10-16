package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeArgumentTest {
    // 1.21.2 dropped the generic. prefix of the attribute keys.
    //? if >=1.21.2 {
    /*private static final String MAX_HEALTH = "max_health";
    *///?} else
    private static final String MAX_HEALTH = "generic.max_health";

    @Test
    void attribute_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("attribute") {{
            argument(new AttributeArgument("attribute")).execute((attribute, ctx) -> {
                //? if >=1.20.5 {
                /*ctx.sendMessage(String.valueOf(attribute.value() == Attributes.MAX_HEALTH.value()));
                *///?} else
                ctx.sendMessage(String.valueOf(attribute == Attributes.MAX_HEALTH));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("attribute minecraft:" + MAX_HEALTH, sender);
            String unknown = RegistryArgumentTestSupport.failure(tester, "attribute commandlib:missing", sender);
            suggestions = tester.suggest("attribute " + MAX_HEALTH.replace("health", "h"), sender);
            messages = sender.getSentMessageTexts();
            assertThat(unknown).contains("commandlib:missing");
        }

        assertThat(messages).startsWith("true");
        assertThat(suggestions).containsExactly("minecraft:" + MAX_HEALTH);
    }
}
