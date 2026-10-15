package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChatFormat;
import net.minecraft.server.v1_16_R3.ArgumentChatFormat;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;

public class NMSArgumentChatFormat_spigot_1_16_5 extends NMSArgumentChatFormat {
    public NMSArgumentChatFormat_spigot_1_16_5() {
        super(null, ArgumentChatFormat.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentChatFormat.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ChatColor parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return CraftChatMessage.getColor(ArgumentChatFormat.a(context, name));
    }
}
