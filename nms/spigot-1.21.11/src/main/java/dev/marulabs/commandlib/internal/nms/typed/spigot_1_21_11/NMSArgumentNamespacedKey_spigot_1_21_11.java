package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_11;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;

public class NMSArgumentNamespacedKey_spigot_1_21_11 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_spigot_1_21_11() {
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
