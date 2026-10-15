package dev.marulabs.commandlib.nms.spigot_1_20_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNBTTag;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;

public class NMSArgumentNBTTag_spigot_1_20_3 extends NMSArgumentNBTTag {
    public NMSArgumentNBTTag_spigot_1_20_3() {
        super(null, CompoundTagArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return CompoundTagArgument.compoundTag();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return CompoundTagArgument.getCompoundTag(context, name).toString();
    }
}
