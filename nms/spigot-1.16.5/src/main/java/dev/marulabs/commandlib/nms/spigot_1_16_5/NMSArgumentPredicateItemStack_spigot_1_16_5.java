package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.server.v1_16_R3.ArgumentPredicateItemStack;

public class NMSArgumentPredicateItemStack_spigot_1_16_5 extends NMSArgumentPredicateItemStack {
    public NMSArgumentPredicateItemStack_spigot_1_16_5(Object handle) {
        super(handle, ArgumentPredicateItemStack.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ArgumentPredicateItemStack) getHandle()).a(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
