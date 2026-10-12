package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentItemStack;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.minecraft.server.v1_16_R3.ArgumentItemStack;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentItemStack_spigot_1_16_5 extends NMSArgumentItemStack {
    public NMSArgumentItemStack_spigot_1_16_5() {
        super(null, ArgumentItemStack.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentItemStack.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSArgumentPredicateItemStack parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return NMSArgumentPredicateItemStack.create(ArgumentItemStack.a(context, name));
    }
}
