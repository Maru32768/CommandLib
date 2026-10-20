package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatFormat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.util.CraftChatMessage;

public class NMSArgumentChatFormat_paper_1_21_1 extends NMSArgumentChatFormat {
    public NMSArgumentChatFormat_paper_1_21_1() {
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
