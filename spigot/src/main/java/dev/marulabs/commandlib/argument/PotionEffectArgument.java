package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentMobEffect;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftPotionEffectType;
import org.bukkit.potion.PotionEffect;

public class PotionEffectArgument extends Argument<PotionEffect, PotionEffectArgument> {
    public PotionEffectArgument(String name) {
        super(name,
              NMSArgumentMobEffect.create()
                                  .argument());
    }

    @Override
    public PotionEffect cast(Object parsedArgument) {
        return ((PotionEffect) parsedArgument);
    }

    @Override
    protected PotionEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSCraftPotionEffectType.create()
                                       .createInstance(NMSArgumentMobEffect.create()
                                                                           .parse(ctx.getHandle(), name()))
                                       .createEffect(1, 0);
    }
}
