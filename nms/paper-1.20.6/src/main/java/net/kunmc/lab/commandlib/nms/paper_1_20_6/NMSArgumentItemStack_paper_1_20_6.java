package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentItemStack;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;

public class NMSArgumentItemStack_paper_1_20_6 extends NMSArgumentItemStack {
    public NMSArgumentItemStack_paper_1_20_6() {
        super(null, "commands.arguments.item.ItemArgument");
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
