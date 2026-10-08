package net.kunmc.lab.commandlib.nms.spigot_1_19_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentItemStack;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;

public class NMSArgumentItemStack_spigot_1_19_1 extends NMSArgumentItemStack {
    public NMSArgumentItemStack_spigot_1_19_1() {
        super(null, "commands.arguments.item.ArgumentItemStack");
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
