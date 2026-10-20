package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentDimension;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import org.bukkit.World;

public class NMSArgumentDimension_spigot_1_19_1 extends NMSArgumentDimension {
    public NMSArgumentDimension_spigot_1_19_1() {
        super(null, DimensionArgument.class);
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
