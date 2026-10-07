package net.kunmc.lab.commandlib.nms.spigot_1_19_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;

public class NMSArgumentNamespacedKey_spigot_1_19_4 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_spigot_1_19_4() {
        super(null, "commands.arguments.ArgumentMinecraftKeyRegistered");
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
