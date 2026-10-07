package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTile;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;

public class NMSArgumentTile_paper_1_20_6 extends NMSArgumentTile {
    public NMSArgumentTile_paper_1_20_6() {
        super(null, "commands.arguments.blocks.BlockStateArgument");
    }

    @Override
    public ArgumentType<?> argument() {
        return BlockStateArgument.block(BuildContexts.current());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSArgumentTileLocation parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSArgumentTileLocation.create(BlockStateArgument.getBlock(context, name));
    }
}
