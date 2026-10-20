package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentArgument extends Argument<Enchantment, EnchantmentArgument> {
    public EnchantmentArgument(String name) {
        super(name,
              NMSArgumentEnchantment.create()
                                    .argument());
    }

    @Override
    public Enchantment cast(Object parsedArgument) {
        return ((Enchantment) parsedArgument);
    }

    @Override
    protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSCraftEnchantment.create()
                                  .createInstance(NMSArgumentEnchantment.create()
                                                                        .parse(ctx.getHandle(), name()));
    }
}
