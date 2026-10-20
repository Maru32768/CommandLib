package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_3;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import net.minecraft.commands.arguments.item.ItemInput;

public class NMSArgumentPredicateItemStack_spigot_1_19_3 extends NMSArgumentPredicateItemStack {
    public NMSArgumentPredicateItemStack_spigot_1_19_3(Object handle) {
        super(handle, ItemInput.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ItemInput) requireHandle()).createItemStack(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
