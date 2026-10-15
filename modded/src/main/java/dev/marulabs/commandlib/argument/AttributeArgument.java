package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
//?} else {
/*import dev.marulabs.commandlib.Caption;
import net.minecraft.core.Registry;
*///?}
   //? if >=1.20.5
/*import net.minecraft.core.Holder;*/
import net.minecraft.world.entity.ai.attributes.Attribute;

// On 1.20.5+ the parsed value is the registry holder, which LivingEntity#getAttribute and AttributeModifier take there.
//? if >=1.20.5 {
/*public class AttributeArgument extends Argument<Holder<Attribute>, AttributeArgument> {
*///?} else
public class AttributeArgument extends Argument<Attribute, AttributeArgument> {
    public AttributeArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.ATTRIBUTE));
        //?} else {
        /*super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registry.ATTRIBUTE_REGISTRY));
        *///?}
    }

    //? if >=1.20.5 {
    /*@Override
    @SuppressWarnings("unchecked")
    public Holder<Attribute> cast(Object parsedArgument) {
        return ((Holder<Attribute>) parsedArgument);
    }

    @Override
    protected Holder<Attribute> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getAttribute(ctx.getHandle(), name());
    }
    *///?} elif >=1.19.3 {
    @Override
    public Attribute cast(Object parsedArgument) {
        return ((Attribute) parsedArgument);
    }

    @Override
    protected Attribute parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getAttribute(ctx.getHandle(), name())
                               .value();
    }
    //?} else {
    /*@Override
    public Attribute cast(Object parsedArgument) {
        return ((Attribute) parsedArgument);
    }

    @Override
    protected Attribute parseImpl(CommandContext ctx) throws ArgumentParseException {
        return RegistryEntries.get(ctx, name(), Registry.ATTRIBUTE_REGISTRY, Caption.UNKNOWN_ATTRIBUTE);
    }
    *///?}
}
