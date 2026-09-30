package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.world.item.ItemStack;

public class ItemStackArgument extends Argument<ItemStack, ItemStackArgument> {
    public ItemStackArgument(String name) {
        super(name, ItemArgument.item(BuiltInCommandBuildContext.INSTANCE));
    }

    @Override
    public ItemStack cast(Object parsedArgument) {
        return ((ItemStack) parsedArgument);
    }

    @Override
    protected ItemStack parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ItemArgument.getItem(ctx.getHandle(), name())
                           .createItemStack(1, false);
    }
}
