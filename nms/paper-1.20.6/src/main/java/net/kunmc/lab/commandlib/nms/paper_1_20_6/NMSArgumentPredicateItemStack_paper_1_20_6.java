package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.commands.arguments.item.ItemInput;

public class NMSArgumentPredicateItemStack_paper_1_20_6 extends NMSArgumentPredicateItemStack {
    public NMSArgumentPredicateItemStack_paper_1_20_6(Object handle) {
        super(handle, "commands.arguments.item.ItemInput");
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ItemInput) getHandle()).createItemStack(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
