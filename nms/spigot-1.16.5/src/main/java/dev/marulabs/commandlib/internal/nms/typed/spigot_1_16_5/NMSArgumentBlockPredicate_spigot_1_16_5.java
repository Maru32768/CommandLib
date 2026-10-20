package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPredicate;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.server.v1_16_R3.ArgumentBlockPredicate;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.ShapeDetectorBlock;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

public class NMSArgumentBlockPredicate_spigot_1_16_5 extends NMSArgumentBlockPredicate {
    public NMSArgumentBlockPredicate_spigot_1_16_5() {
        super(null, ArgumentBlockPredicate.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentBlockPredicate.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Predicate<Block> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            Predicate<ShapeDetectorBlock> predicate = ArgumentBlockPredicate.a(context, name);
            return block -> predicate.test(new ShapeDetectorBlock(((CraftWorld) block.getWorld()).getHandle(),
                                                                  new BlockPosition(block.getX(),
                                                                                    block.getY(),
                                                                                    block.getZ()),
                                                                  true));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
