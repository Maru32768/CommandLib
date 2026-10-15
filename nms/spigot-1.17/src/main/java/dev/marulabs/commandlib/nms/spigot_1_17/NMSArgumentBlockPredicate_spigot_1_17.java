package dev.marulabs.commandlib.nms.spigot_1_17;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentBlockPredicate;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;

public class NMSArgumentBlockPredicate_spigot_1_17 extends NMSArgumentBlockPredicate {
    public NMSArgumentBlockPredicate_spigot_1_17() {
        super(null, BlockPredicateArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return BlockPredicateArgument.blockPredicate();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Predicate<Block> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            Predicate<BlockInWorld> predicate = BlockPredicateArgument.getBlockPredicate(context, name);
            return block -> predicate.test(new BlockInWorld(((CraftWorld) block.getWorld()).getHandle(),
                                                            new BlockPos(block.getX(), block.getY(), block.getZ()),
                                                            true));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
