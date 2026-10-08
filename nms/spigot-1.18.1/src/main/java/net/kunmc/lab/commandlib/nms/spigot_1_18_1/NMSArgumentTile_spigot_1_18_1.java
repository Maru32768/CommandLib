package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTile;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;

public class NMSArgumentTile_spigot_1_18_1 extends NMSArgumentTile {
    public NMSArgumentTile_spigot_1_18_1() {
        super(null, "commands.arguments.blocks.ArgumentTile");
    }

    @Override
    public ArgumentType<?> argument() {
        return BlockStateArgument.block();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSArgumentTileLocation parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSArgumentTileLocation.create(BlockStateArgument.getBlock(context, name));
    }
}
