package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.command.arguments.PotionArgument;
import net.minecraft.potion.Effect;

public class EffectArgument extends Argument<Effect, EffectArgument> {
    public EffectArgument(String name) {
        super(name, PotionArgument.mobEffect());
    }

    @Override
    public Effect cast(Object parsedArgument) {
        return ((Effect) parsedArgument);
    }

    @Override
    protected Effect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return PotionArgument.getMobEffect(ctx.getHandle(), name());
    }
}
