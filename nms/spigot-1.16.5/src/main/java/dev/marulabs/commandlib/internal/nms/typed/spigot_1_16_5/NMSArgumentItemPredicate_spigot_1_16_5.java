package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentItemPredicate;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.server.v1_16_R3.ArgumentItemPredicate;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.ItemStack;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;

public class NMSArgumentItemPredicate_spigot_1_16_5 extends NMSArgumentItemPredicate {
    public NMSArgumentItemPredicate_spigot_1_16_5() {
        super(null, ArgumentItemPredicate.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentItemPredicate.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Predicate<org.bukkit.inventory.ItemStack> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            Predicate<ItemStack> predicate = ArgumentItemPredicate.a(context, name);
            return item -> predicate.test(CraftItemStack.asNMSCopy(item));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
