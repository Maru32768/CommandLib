package dev.marulabs.commandlib.nms.spigot_26_1_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChatFormat;
import org.bukkit.ChatColor;

public class NMSArgumentChatFormat_spigot_26_1_2 extends NMSArgumentChatFormat {
    public NMSArgumentChatFormat_spigot_26_1_2() {
        super(null, colorArgumentClass());
    }

    @Override
    public ArgumentType<?> argument() {
        return colorArgument();
    }

    @Override
    protected ChatColor parseImpl(CommandContext<?> ctx, String name) {
        return chatColor(ctx.getArgument(name, Enum.class));
    }
}
