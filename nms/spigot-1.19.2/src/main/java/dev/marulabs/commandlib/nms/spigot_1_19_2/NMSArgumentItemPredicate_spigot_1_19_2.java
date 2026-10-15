package dev.marulabs.commandlib.nms.spigot_1_19_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentItemPredicate;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NMSArgumentItemPredicate_spigot_1_19_2 extends NMSArgumentItemPredicate {
    public NMSArgumentItemPredicate_spigot_1_19_2() {
        super(null, ItemPredicateArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ItemPredicateArgument.itemPredicate(BuildContexts.current());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Predicate<ItemStack> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        Predicate<net.minecraft.world.item.ItemStack> predicate = ItemPredicateArgument.getItemPredicate(context, name);
        return item -> predicate.test(CraftItemStack.asNMSCopy(item));
    }
}
