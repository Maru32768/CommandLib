package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.predicate.ItemStackPredicate;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * An item predicate in the vanilla {@code item_predicate} syntax of {@code /clear}: an item ({@code stone}), an item
 * tag ({@code #minecraft:logs}) or {@code *}, with component tests ({@code *[damage>0]}). The client parses it and
 * suggests items and tags.
 *
 * <p>The value tests an item as the server does. An unknown item or tag fails while parsing.
 */
@SuppressWarnings("UnstableApiUsage")
public class ItemPredicateArgument extends Argument<Predicate<ItemStack>, ItemPredicateArgument> {
    public ItemPredicateArgument(String name) {
        super(name, ArgumentTypes.itemPredicate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<ItemStack> cast(Object parsedArgument) {
        return (Predicate<ItemStack>) parsedArgument;
    }

    @Override
    protected Predicate<ItemStack> parseImpl(CommandContext ctx) {
        return ctx.getHandle()
                  .getArgument(name(), ItemStackPredicate.class);
    }
}
