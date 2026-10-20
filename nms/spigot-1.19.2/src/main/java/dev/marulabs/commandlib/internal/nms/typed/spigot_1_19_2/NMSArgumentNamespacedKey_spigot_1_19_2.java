package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;

public class NMSArgumentNamespacedKey_spigot_1_19_2 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_spigot_1_19_2() {
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
