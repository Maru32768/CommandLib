package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentItemStack;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.minecraft.server.v1_16_R3.ArgumentItemStack;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentItemStack_spigot_1_16_5 extends NMSArgumentItemStack {
    public NMSArgumentItemStack_spigot_1_16_5() {
        super(null, "ArgumentItemStack");
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
