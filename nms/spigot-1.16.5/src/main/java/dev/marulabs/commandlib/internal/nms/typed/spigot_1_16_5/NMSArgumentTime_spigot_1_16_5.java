package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTime;
import net.minecraft.server.v1_16_R3.ArgumentTime;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentTime_spigot_1_16_5 extends NMSArgumentTime {
    public NMSArgumentTime_spigot_1_16_5() {
        super(null, ArgumentTime.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentTime.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Integer parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return context.getArgument(name, Integer.class);
    }

    @Override
    public ArgumentType<?> argument(int minimum) {
        return ArgumentTime.a();
    }
}
