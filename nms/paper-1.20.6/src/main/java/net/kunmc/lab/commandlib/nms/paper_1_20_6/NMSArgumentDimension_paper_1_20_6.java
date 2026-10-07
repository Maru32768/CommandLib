package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentDimension;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import org.bukkit.World;

public class NMSArgumentDimension_paper_1_20_6 extends NMSArgumentDimension {
    public NMSArgumentDimension_paper_1_20_6() {
        super(null, "commands.arguments.DimensionArgument");
    }

    @Override
    public ArgumentType<?> argument() {
        return DimensionArgument.dimension();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected World parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return DimensionArgument.getDimension(context, name).getWorld();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
