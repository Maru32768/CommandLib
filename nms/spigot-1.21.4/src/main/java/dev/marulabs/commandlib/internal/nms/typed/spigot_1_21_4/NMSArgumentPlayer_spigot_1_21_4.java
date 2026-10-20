package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPlayer;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import org.bukkit.entity.Player;

public class NMSArgumentPlayer_spigot_1_21_4 extends NMSArgumentPlayer {
    public NMSArgumentPlayer_spigot_1_21_4() {
        super(null, EntityArgument.class);
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
