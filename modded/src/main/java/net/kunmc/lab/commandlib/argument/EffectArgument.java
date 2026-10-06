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
//? if >=1.20.5
/*import net.minecraft.core.Holder;*/
import net.minecraft.world.effect.MobEffect;

// On 1.20.5+ the parsed value is the registry holder, which MobEffectInstance and LivingEntity take there.
//? if >=1.20.5 {
/*public class EffectArgument extends Argument<Holder<MobEffect>, EffectArgument> {
*///?} else
public class EffectArgument extends Argument<MobEffect, EffectArgument> {
    public EffectArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.MOB_EFFECT));
        //?} else
        /*super(name, MobEffectArgument.effect());*/
    }

    //? if >=1.20.5 {
    /*@Override
    @SuppressWarnings("unchecked")
    public Holder<MobEffect> cast(Object parsedArgument) {
        return ((Holder<MobEffect>) parsedArgument);
    }

    @Override
    protected Holder<MobEffect> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getMobEffect(ctx.getHandle(), name());
    }
    *///?} elif >=1.19.3 {
    @Override
    public MobEffect cast(Object parsedArgument) {
        return ((MobEffect) parsedArgument);
    }

    @Override
    protected MobEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getMobEffect(ctx.getHandle(), name())
                               .value();
    }
    //?} else {
    /*@Override
    public MobEffect cast(Object parsedArgument) {
        return ((MobEffect) parsedArgument);
    }

    @Override
    protected MobEffect parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return MobEffectArgument.getEffect(ctx.getHandle(), name());
    }
    *///?}
}
