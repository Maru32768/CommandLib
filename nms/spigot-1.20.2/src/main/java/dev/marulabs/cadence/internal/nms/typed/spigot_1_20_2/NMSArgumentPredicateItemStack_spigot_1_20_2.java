package dev.marulabs.cadence.internal.nms.typed.spigot_1_20_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.cadence.internal.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.cadence.internal.nms.world.NMSItemStack;
import net.minecraft.commands.arguments.item.ItemInput;

public class NMSArgumentPredicateItemStack_spigot_1_20_2 extends NMSArgumentPredicateItemStack {
    public NMSArgumentPredicateItemStack_spigot_1_20_2(Object handle) {
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
