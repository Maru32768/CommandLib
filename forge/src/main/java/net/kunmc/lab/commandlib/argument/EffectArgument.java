package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.commands.arguments.MobEffectArgument;*/
import net.minecraft.world.effect.MobEffect;

public class EffectArgument extends Argument<MobEffect, EffectArgument> {
    public EffectArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.MOB_EFFECT));
        //?} else
        /*super(name, MobEffectArgument.effect());*/
    }

    @Override
    public MobEffect cast(Object parsedArgument) {
        return ((MobEffect) parsedArgument);
    }

    @Override
    protected MobEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        //? if >=1.19.3 {
        return ResourceArgument.getMobEffect(ctx.getHandle(), name())
                               .value();
        //?} else
        /*return MobEffectArgument.getEffect(ctx.getHandle(), name());*/
    }
}
