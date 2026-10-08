package net.kunmc.lab.commandlib.nms.spigot_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentDimension;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import org.bukkit.World;

public class NMSArgumentDimension_spigot_1_21_1 extends NMSArgumentDimension {
    public NMSArgumentDimension_spigot_1_21_1() {
        super(null, "commands.arguments.ArgumentDimension");
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
