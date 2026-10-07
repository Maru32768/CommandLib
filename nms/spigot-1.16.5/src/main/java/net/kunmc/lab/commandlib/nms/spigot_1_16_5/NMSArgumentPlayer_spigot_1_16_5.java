package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPlayer;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.server.v1_16_R3.ArgumentEntity;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.entity.Player;

public class NMSArgumentPlayer_spigot_1_16_5 extends NMSArgumentPlayer {
    public NMSArgumentPlayer_spigot_1_16_5() {
        super(null, "ArgumentEntity");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentEntity.c();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Player parseImpl(CommandContext<?> ctx, String name) {
        try {
            return ArgumentEntity.e((CommandContext<CommandListenerWrapper>) ctx, name)
                                 .getBukkitEntity();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
