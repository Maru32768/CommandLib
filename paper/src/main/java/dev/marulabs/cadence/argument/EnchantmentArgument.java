package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;

@SuppressWarnings("UnstableApiUsage")
public class EnchantmentArgument extends Argument<Enchantment, EnchantmentArgument> {
    public EnchantmentArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.ENCHANTMENT));
    }

    @Override
    public Enchantment cast(Object parsedArgument) {
        return (Enchantment) parsedArgument;
    }

    @Override
    protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Enchantment.class);
    }
}
