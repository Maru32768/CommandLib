package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentItemStack;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPredicateItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;

public class NMSArgumentItemStack_paper_1_21_1 extends NMSArgumentItemStack {
    public NMSArgumentItemStack_paper_1_21_1() {
        super(null, ItemArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ItemArgument.item(BuildContexts.current());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSArgumentPredicateItemStack parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSArgumentPredicateItemStack.create(ItemArgument.getItem(context, name));
    }
}
