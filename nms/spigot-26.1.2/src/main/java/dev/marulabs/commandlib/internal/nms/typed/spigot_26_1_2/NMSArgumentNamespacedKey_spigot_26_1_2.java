package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;

public class NMSArgumentNamespacedKey_spigot_26_1_2 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_spigot_26_1_2() {
        super(null, IdentifierArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return IdentifierArgument.id();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return IdentifierArgument.getId(context, name).toString();
    }
}
