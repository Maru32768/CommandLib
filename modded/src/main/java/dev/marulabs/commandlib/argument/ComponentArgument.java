package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.network.chat.Component;

/**
 * A vanilla text component, such as {@code {"text":"Hello","color":"red"}} (JSON up to 1.21.4, SNBT from 1.21.5,
 * which also reads that JSON). Selector, score and NBT components are returned as written, not resolved for a viewer.
 *
 * <p>Up to 1.20.2 vanilla's reader also consumes the character after the component, so an argument after it fails to
 * parse there. Put it last in its branch, as {@code /tellraw} does.
 */
public class ComponentArgument extends Argument<Component, ComponentArgument> {
    public ComponentArgument(String name) {
        //? if >=1.20.5 {
        /*super(name, net.minecraft.commands.arguments.ComponentArgument.textComponent(BuiltInCommandBuildContext.INSTANCE));
        *///?} else
        super(name, net.minecraft.commands.arguments.ComponentArgument.textComponent());
    }

    @Override
    public Component cast(Object parsedArgument) {
        return (Component) parsedArgument;
    }

    @Override
    protected Component parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        //? if >=1.21.5 {
        /*return net.minecraft.commands.arguments.ComponentArgument.getRawComponent(ctx.getHandle(), name());
        *///?} else
        return net.minecraft.commands.arguments.ComponentArgument.getComponent(ctx.getHandle(), name());
    }
}
