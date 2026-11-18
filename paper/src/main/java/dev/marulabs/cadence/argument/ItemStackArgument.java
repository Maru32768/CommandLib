package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class ItemStackArgument extends Argument<ItemStack, ItemStackArgument> {
    public ItemStackArgument(String name) {
        super(name, ArgumentTypes.itemStack());
    }

    @Override
    public ItemStack cast(Object parsedArgument) {
        return (ItemStack) parsedArgument;
    }

    @Override
    protected ItemStack parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), ItemStack.class);
    }
}
