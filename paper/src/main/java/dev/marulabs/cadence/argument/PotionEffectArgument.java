package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("UnstableApiUsage")
public class PotionEffectArgument extends Argument<PotionEffect, PotionEffectArgument> {
    public PotionEffectArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.MOB_EFFECT));
    }

    @Override
    public PotionEffect cast(Object parsedArgument) {
        return (PotionEffect) parsedArgument;
    }

    @Override
    protected PotionEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        PotionEffectType type = ctx.getHandle()
                                   .getArgument(name(), PotionEffectType.class);
        return type.createEffect(1, 0);
    }
}
