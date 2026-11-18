package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attribute;

@SuppressWarnings("UnstableApiUsage")
public class AttributeArgument extends Argument<Attribute, AttributeArgument> {
    public AttributeArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.ATTRIBUTE));
    }

    @Override
    public Attribute cast(Object parsedArgument) {
        return (Attribute) parsedArgument;
    }

    @Override
    protected Attribute parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Attribute.class);
    }
}
