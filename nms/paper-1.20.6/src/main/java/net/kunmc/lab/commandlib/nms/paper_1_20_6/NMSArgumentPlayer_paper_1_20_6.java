package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPlayer;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import org.bukkit.entity.Player;

public class NMSArgumentPlayer_paper_1_20_6 extends NMSArgumentPlayer {
    public NMSArgumentPlayer_paper_1_20_6() {
        super(null, "commands.arguments.EntityArgument");
    }

    @Override
    public ArgumentType<?> argument() {
        return EntityArgument.player();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Player parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return EntityArgument.getPlayer(context, name).getBukkitEntity();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
