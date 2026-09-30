package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public class EffectArgument extends Argument<MobEffect, EffectArgument> {
    public EffectArgument(String name) {
        super(name, ResourceLocationArgument.id());
    }

    @Override
    public MobEffect cast(Object parsedArgument) {
        return ((MobEffect) parsedArgument);
    }

    @Override
    protected MobEffect parseImpl(CommandContext ctx) throws ArgumentParseException {
        return BuiltInRegistries.MOB_EFFECT.get(ResourceLocationArgument.getId(ctx.getHandle(), name()));
    }
}
