package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.StringRange;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The testers send a failure's messages one by one; the server sends this one message instead.
class FailureMessageTest {
    @Test
    void a_failure_joins_its_messages_one_per_line() {
        NMSCommandListenerWrapper wrapper = Mockito.mock(NMSCommandListenerWrapper.class);
        // The message keeps the plain text of the component, which the server would turn into vanilla's component.
        Mockito.when(wrapper.toMessage(Mockito.any()))
               .thenAnswer(invocation -> new LiteralMessage(invocation.<BaseComponent>getArgument(0)
                                                                      .toPlainText()));
        CommandContext ctx = new CommandContext(new com.mojang.brigadier.context.CommandContext<>(new Object(),
                                                                                                  "test",
                                                                                                  Map.of(),
                                                                                                  null,
                                                                                                  null,
                                                                                                  List.of(),
                                                                                                  StringRange.at(0),
                                                                                                  null,
                                                                                                  null,
                                                                                                  false));

        try (MockedStatic<NMSCommandListenerWrapper> wrappers = Mockito.mockStatic(NMSCommandListenerWrapper.class)) {
            wrappers.when(() -> NMSCommandListenerWrapper.create(Mockito.any()))
                    .thenReturn(wrapper);

            assertThat(ctx.toFailureMessage(List.of(new TextComponent("first"), new TextComponent("second")))
                          .getString()).isEqualTo("first\nsecond");
        }
    }
}
