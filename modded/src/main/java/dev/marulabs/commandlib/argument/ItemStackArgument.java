package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.world.item.ItemStack;

public class ItemStackArgument extends Argument<ItemStack, ItemStackArgument> {
    public ItemStackArgument(String name) {
        //? if >=1.19 {
        super(name, ItemArgument.item(BuiltInCommandBuildContext.INSTANCE));
        //?} else
        /*super(name, ItemArgument.item());*/
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
