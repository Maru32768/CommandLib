package net.kunmc.lab.commandlib.nms.spigot_1_19_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPlayer;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import org.bukkit.entity.Player;

public class NMSArgumentPlayer_spigot_1_19_3 extends NMSArgumentPlayer {
    public NMSArgumentPlayer_spigot_1_19_3() {
        super(null, "commands.arguments.ArgumentEntity");
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
