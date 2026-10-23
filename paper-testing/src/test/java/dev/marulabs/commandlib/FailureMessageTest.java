package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.StringRange;
import dev.marulabs.commandlib.internal.ContextState;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The testers send a failure's messages one by one; the server sends this one message instead.
@SuppressWarnings("UnstableApiUsage")
class FailureMessageTest {
    @Test
    void a_failure_joins_its_messages_one_per_line() {
        MessageComponentSerializer serializer = Mockito.mock(MessageComponentSerializer.class);
        // The message keeps the plain text of the component, which the server would turn into vanilla's component.
        Mockito.when(serializer.serialize(Mockito.any()))
               .thenAnswer(invocation -> new LiteralMessage(PlainTextComponentSerializer.plainText()
                                                                                        .serialize(invocation.getArgument(0))));
        CommandContext ctx = new CommandContext(new com.mojang.brigadier.context.CommandContext<>(Mockito.mock(CommandSourceStack.class),
                                                                                                  "test",
                                                                                                  Map.of(),
                                                                                                  null,
                                                                                                  null,
                                                                                                  List.of(),
                                                                                                  StringRange.at(0),
                                                                                                  null,
                                                                                                  null,
                                                                                                  false),
                                                ContextState.EMPTY);

        try (MockedStatic<MessageComponentSerializer> serializers = Mockito.mockStatic(MessageComponentSerializer.class)) {
            serializers.when(MessageComponentSerializer::message)
                       .thenReturn(serializer);

            assertThat(ctx.toFailureMessage(List.of(Component.text("first"), Component.text("second")))
                          .getString()).isEqualTo("first\nsecond");
        }
    }
}
