package dev.marulabs.commandlib.nms.spigot_26_1_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChat;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import org.bukkit.craftbukkit.util.CraftChatMessage;

public class NMSArgumentChat_spigot_26_1_2 extends NMSArgumentChat {
    public NMSArgumentChat_spigot_26_1_2() {
        super(null, MessageArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MessageArgument.message();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return ComponentSerializer.parse(CraftChatMessage.toJSON(MessageArgument.getMessage(context, name)))[0];
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
