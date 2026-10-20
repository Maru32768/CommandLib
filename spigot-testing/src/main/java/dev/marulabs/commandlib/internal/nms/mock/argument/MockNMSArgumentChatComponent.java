package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentChatComponent extends NMSArgumentChatComponent {
    public MockNMSArgumentChatComponent() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // Reads JSON text with Bungee, so the SNBT-only syntax of 1.21.5 (unquoted keys) is not supported.
        return reader -> {
            int start = reader.getCursor();
            String json = MockArgumentTypes.readValue(reader);
            try {
                BaseComponent[] components = ComponentSerializer.parse(json);
                // Vanilla reads a JSON array as its first element with the others appended.
                return components.length == 1 ? components[0] : new TextComponent(components);
            } catch (RuntimeException e) {
                // Gson reports malformed JSON and values of the wrong shape with different runtime exceptions.
                reader.setCursor(start);
                throw MockArgumentTypes.syntaxError("Invalid chat component: " + e.getMessage(), reader);
            }
        };
    }

    @Override
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, BaseComponent.class);
    }
}
