package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentItemPredicate;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * An item predicate in the vanilla {@code item_predicate} syntax of {@code /clear}: an item ({@code stone}), an item
 * tag ({@code #minecraft:logs}) or, from 1.20.5, {@code *}, with NBT ({@code stick{custom:1b}}) before 1.20.5 and
 * component tests ({@code *[damage>0]}) from then. The client parses it and suggests items and tags.
 *
 * <p>The value tests an item as the server does. An unknown item fails while parsing; an unknown tag fails while
 * parsing from 1.19 and when the command runs before.
 */
public class ItemPredicateArgument extends Argument<Predicate<ItemStack>, ItemPredicateArgument> {
    public ItemPredicateArgument(String name) {
        super(name,
              NMSArgumentItemPredicate.create()
                                      .argument());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<ItemStack> cast(Object parsedArgument) {
        return (Predicate<ItemStack>) parsedArgument;
    }

    @Override
    protected Predicate<ItemStack> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentItemPredicate.create()
                                       .parse(ctx.getHandle(), name());
    }
}
