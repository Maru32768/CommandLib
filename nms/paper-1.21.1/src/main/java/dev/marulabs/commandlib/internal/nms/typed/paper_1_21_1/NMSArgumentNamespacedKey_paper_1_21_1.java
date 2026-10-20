package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;

public class NMSArgumentNamespacedKey_paper_1_21_1 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_paper_1_21_1() {
        super(null, ResourceLocationArgument.class);
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
