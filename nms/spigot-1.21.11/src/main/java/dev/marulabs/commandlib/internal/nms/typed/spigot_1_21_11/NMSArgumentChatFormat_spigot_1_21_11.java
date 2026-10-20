package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_11;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatFormat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_21_R7.util.CraftChatMessage;

public class NMSArgumentChatFormat_spigot_1_21_11 extends NMSArgumentChatFormat {
    public NMSArgumentChatFormat_spigot_1_21_11() {
        super(null, ColorArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ColorArgument.color();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ChatColor parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return CraftChatMessage.getColor(ColorArgument.getColor(context, name));
    }
}
