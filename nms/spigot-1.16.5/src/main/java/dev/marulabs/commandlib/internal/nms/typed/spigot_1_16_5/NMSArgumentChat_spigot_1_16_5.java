package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChat;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R3.ArgumentChat;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;

public class NMSArgumentChat_spigot_1_16_5 extends NMSArgumentChat {
    public NMSArgumentChat_spigot_1_16_5() {
        super(null, ArgumentChat.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentChat.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return ComponentSerializer.parse(CraftChatMessage.toJSON(ArgumentChat.a(context, name)))[0];
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
