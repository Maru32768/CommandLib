package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentItemPredicate;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NMSArgumentItemPredicate_spigot_1_18_1 extends NMSArgumentItemPredicate {
    public NMSArgumentItemPredicate_spigot_1_18_1() {
        super(null, ItemPredicateArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ItemPredicateArgument.itemPredicate();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Predicate<ItemStack> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            Predicate<net.minecraft.world.item.ItemStack> predicate = ItemPredicateArgument.getItemPredicate(context, name);
            return item -> predicate.test(CraftItemStack.asNMSCopy(item));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
