package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentEnchantment;
import dev.marulabs.cadence.internal.nms.world.NMSCraftEnchantment;
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
