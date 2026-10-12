package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentItemStack;
import dev.marulabs.commandlib.util.nms.world.NMSCraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ItemStackArgument extends Argument<ItemStack, ItemStackArgument> {
    public ItemStackArgument(String name) {
        super(name,
              NMSArgumentItemStack.create()
                                  .argument());
    }

    @Override
    public ItemStack cast(Object parsedArgument) {
        return ((ItemStack) parsedArgument);
    }

    @Override
    protected ItemStack parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSCraftItemStack.create()
                                .asCraftMirror(NMSArgumentItemStack.create()
                                                                   .parse(ctx.getHandle(), name())
                                                                   .createItemStack(1, false));
    }
}
