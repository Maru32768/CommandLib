package net.kunmc.lab.commandlib.util.nms.argument.v1_17_0;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.NMSReflection;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentNamespacedKey;

public class NMSArgumentNamespacedKey_v1_17_0 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_v1_17_0() {
        super(null, "commands.arguments.ArgumentMinecraftKeyRegistered");
    }

    @Override
    public ArgumentType<?> argument() {
        return (ArgumentType<?>) invokeStaticMethod("a");
    }

    @Override
    protected String parseImpl(CommandContext<?> ctx, String name) {
        // Every getter of this class takes (CommandContext, String) and the obfuscated name of the MinecraftKey
        // getter moved from "f" (1.17) to "e" (1.18+), so it is selected by its return type instead.
        Class<?> keyClass = NMSReflection.findMinecraftClass("resources.MinecraftKey");
        return invokeFoundMethod(findMethodByReturnType(true, keyClass, CommandContext.class, String.class), ctx, name)
                .toString();
    }
}
