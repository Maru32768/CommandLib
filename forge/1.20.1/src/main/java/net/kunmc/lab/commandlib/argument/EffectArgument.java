package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public class EffectArgument extends Argument<MobEffect, EffectArgument> {
    public EffectArgument(String name) {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.MOB_EFFECT));
    }

    @Override
    public MobEffect cast(Object parsedArgument) {
        return ((MobEffect) parsedArgument);
    }

    @Override
    protected MobEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getMobEffect(ctx.getHandle(), name())
                               .value();
    }
}
