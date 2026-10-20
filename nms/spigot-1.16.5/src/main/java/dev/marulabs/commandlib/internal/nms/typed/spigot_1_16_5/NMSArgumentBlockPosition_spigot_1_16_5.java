package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPosition;
import net.minecraft.server.v1_16_R3.ArgumentPosition;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.IVectorPosition;
import org.bukkit.util.BlockVector;

public class NMSArgumentBlockPosition_spigot_1_16_5 extends NMSArgumentBlockPosition {
    public NMSArgumentBlockPosition_spigot_1_16_5() {
        super(null, ArgumentPosition.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentPosition.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BlockVector parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        BlockPosition pos = context.getArgument(name, IVectorPosition.class)
                                .c(context.getSource());
        return new BlockVector(pos.getX(), pos.getY(), pos.getZ());
    }
}
