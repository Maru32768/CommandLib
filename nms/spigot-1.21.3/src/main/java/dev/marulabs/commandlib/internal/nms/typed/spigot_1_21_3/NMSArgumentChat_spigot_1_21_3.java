package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChat;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import org.bukkit.craftbukkit.v1_21_R2.util.CraftChatMessage;

public class NMSArgumentChat_spigot_1_21_3 extends NMSArgumentChat {
    public NMSArgumentChat_spigot_1_21_3() {
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
