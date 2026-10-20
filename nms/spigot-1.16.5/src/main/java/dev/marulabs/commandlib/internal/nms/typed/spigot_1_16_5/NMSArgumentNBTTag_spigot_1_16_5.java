package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNBTTag;
import net.minecraft.server.v1_16_R3.ArgumentNBTTag;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentNBTTag_spigot_1_16_5 extends NMSArgumentNBTTag {
    public NMSArgumentNBTTag_spigot_1_16_5() {
        super(null, ArgumentNBTTag.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentNBTTag.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return ArgumentNBTTag.a(context, name).toString();
    }
}
