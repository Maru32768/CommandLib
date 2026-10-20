package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPosition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import org.bukkit.util.BlockVector;

public class NMSArgumentBlockPosition_spigot_1_20_1 extends NMSArgumentBlockPosition {
    public NMSArgumentBlockPosition_spigot_1_20_1() {
        super(null, BlockPosArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return BlockPosArgument.blockPos();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BlockVector parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        BlockPos pos = context.getArgument(name, Coordinates.class)
                           .getBlockPos(context.getSource());
        return new BlockVector(pos.getX(), pos.getY(), pos.getZ());
    }
}
