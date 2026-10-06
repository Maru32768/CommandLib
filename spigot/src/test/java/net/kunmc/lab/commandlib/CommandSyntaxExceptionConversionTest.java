package net.kunmc.lab.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.nms.NMSReflection;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommandSyntaxExceptionConversionTest {
    @Test
    void translatable_message_is_sent_as_translatable_failure() {
        CommandSyntaxException e = new SimpleCommandExceptionType(new FakeChatMessage("argument.entity.notfound.player",
                                                                                      "Alex")).create();

        CommandContext ctx = convertAndSend(e);

        ArgumentCaptor<BaseComponent> captor = ArgumentCaptor.forClass(BaseComponent.class);
        verify(ctx).sendFailure(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TranslatableComponent.class);
        TranslatableComponent component = (TranslatableComponent) captor.getValue();
        assertThat(component.getTranslate()).isEqualTo("argument.entity.notfound.player");
        assertThat(component.getWith()).extracting(x -> x.toPlainText())
                                        .containsExactly("Alex");
    }

    @Test
    void plain_brigadier_message_falls_back_to_message_text() {
        CommandSyntaxException e = new SimpleCommandExceptionType(new LiteralMessage("custom failure")).create();

        CommandContext ctx = convertAndSend(e);

        verify(ctx).sendFailure("custom failure");
        verify(ctx, never()).sendFailure(any(BaseComponent.class));
    }

    @Test
    void plain_brigadier_message_is_not_reported_as_conversion_failure() {
        List<LogRecord> records = captureWarnings(() -> convertAndSend(new SimpleCommandExceptionType(new LiteralMessage(
                "custom failure")).create()));

        assertThat(records).isEmpty();
    }

    @Test
    void broken_translatable_conversion_is_reported_and_falls_back_to_text() {
        CommandSyntaxException e = new SimpleCommandExceptionType(new BrokenChatMessage()).create();

        CommandContext[] ctx = new CommandContext[1];
        List<LogRecord> records = captureWarnings(() -> ctx[0] = convertAndSend(e));

        verify(ctx[0]).sendFailure("broken.key");
        assertThat(records).hasSize(1);
        assertThat(records.get(0)
                          .getThrown()).isNotNull();
    }

    private static List<LogRecord> captureWarnings(Runnable runnable) {
        Logger logger = Logger.getLogger(PlatformAdapterImpl.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        boolean useParentHandlers = logger.getUseParentHandlers();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        try {
            runnable.run();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(useParentHandlers);
        }
        return records;
    }

    private static CommandContext convertAndSend(CommandSyntaxException e) {
        CommandContext ctx = mock(CommandContext.class);
        try (MockedStatic<BukkitUtil> bukkitUtil = mockStatic(BukkitUtil.class);
             MockedStatic<NMSReflection> reflection = mockStatic(NMSReflection.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn("1.16.5");
            reflection.when(() -> NMSReflection.findMinecraftClass(anyString(), any(String[].class)))
                      .thenReturn(e.getRawMessage()
                                   .getClass() == BrokenChatMessage.class ? BrokenChatMessage.class : FakeChatMessage.class);

            ArgumentParseException converted = new PlatformAdapterImpl().convertCommandSyntaxException(e);
            converted.sendMessage(ctx);
        }
        return ctx;
    }

    /**
     * A {@code ChatMessage} stand-in whose arguments getter fails, as a broken NMS mapping would.
     */
    public static final class BrokenChatMessage implements Message {
        public String getKey() {
            return "broken.key";
        }

        public Object[] getArgs() {
            throw new IllegalStateException("broken mapping");
        }

        @Override
        public String getString() {
            return "broken.key";
        }
    }

    /**
     * Stands in for the 1.16 {@code ChatMessage} component, which exposes its key and arguments.
     */
    public static final class FakeChatMessage implements Message {
        private final String key;
        private final Object[] args;

        FakeChatMessage(String key, Object... args) {
            this.key = key;
            this.args = args;
        }

        public String getKey() {
            return key;
        }

        public Object[] getArgs() {
            return args;
        }

        @Override
        public String getString() {
            return key;
        }
    }
}
