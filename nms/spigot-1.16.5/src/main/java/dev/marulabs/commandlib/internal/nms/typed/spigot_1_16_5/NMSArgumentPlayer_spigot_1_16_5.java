package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPlayer;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.server.v1_16_R3.ArgumentEntity;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.entity.Player;

public class NMSArgumentPlayer_spigot_1_16_5 extends NMSArgumentPlayer {
    public NMSArgumentPlayer_spigot_1_16_5() {
        super(null, ArgumentEntity.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentEntity.c();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Player parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return ArgumentEntity.e(context, name).getBukkitEntity();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
