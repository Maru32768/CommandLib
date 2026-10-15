package dev.marulabs.commandlib.nms.spigot_26_1_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.commands.arguments.item.ItemInput;

public class NMSArgumentPredicateItemStack_spigot_26_1_2 extends NMSArgumentPredicateItemStack {
    public NMSArgumentPredicateItemStack_spigot_26_1_2(Object handle) {
        super(handle, ItemInput.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ItemInput) requireHandle()).createItemStack(amount));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
