package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;

/**
 * A vanilla text component, such as {@code {"text":"Hello","color":"red"}} (JSON up to 1.21.4, SNBT from 1.21.5,
 * which also reads that JSON), read by Paper's {@code ArgumentTypes.component()}. Selector, score and NBT components
 * are returned as written, not resolved for a viewer.
 */
@SuppressWarnings("UnstableApiUsage")
public class ComponentArgument extends Argument<Component, ComponentArgument> {
    public ComponentArgument(String name) {
        super(name, ArgumentTypes.component());
    }

    @Override
    public Component cast(Object parsedArgument) {
        return (Component) parsedArgument;
    }

    @Override
    protected Component parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Component.class);
    }
}
