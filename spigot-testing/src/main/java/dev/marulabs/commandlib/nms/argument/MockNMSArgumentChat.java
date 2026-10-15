package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class MockNMSArgumentChat extends NMSArgumentChat {
    public MockNMSArgumentChat() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return StringArgumentType.greedyString();
    }

    @Override
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        // Selectors in the message are kept as written.
        return new TextComponent(ctx.getArgument(name, String.class));
    }
}
