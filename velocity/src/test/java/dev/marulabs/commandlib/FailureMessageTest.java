package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The testers send a failure's messages one by one; the proxy sends this one message instead.
class FailureMessageTest {
    @Test
    void a_failure_joins_its_messages_one_per_line() {
        CommandSource source = permission -> Tristate.TRUE;
        CommandContext ctx = new CommandContext(new com.mojang.brigadier.context.CommandContext<>(source,
                                                                                                  "test",
                                                                                                  Map.of(),
                                                                                                  null,
                                                                                                  null,
                                                                                                  List.of(),
                                                                                                  StringRange.at(0),
                                                                                                  null,
                                                                                                  null,
                                                                                                  false));

        Message message = ctx.toFailureMessage(List.of(Component.text("first"), Component.text("second")));

        assertThat(message).isInstanceOf(VelocityBrigadierMessage.class);
        assertThat(PlainTextComponentSerializer.plainText()
                                               .serialize(((VelocityBrigadierMessage) message).asComponent())).isEqualTo("first\nsecond");
    }
}
