package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;

public class NMSArgumentNamespacedKey_paper_1_20_6 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_paper_1_20_6() {
        super(null, "commands.arguments.ResourceLocationArgument");
    }

    @Override
    public ArgumentType<?> argument() {
        return ResourceLocationArgument.id();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return ResourceLocationArgument.getId(context, name).toString();
    }
}
