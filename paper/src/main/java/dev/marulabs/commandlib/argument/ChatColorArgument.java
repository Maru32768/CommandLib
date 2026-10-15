package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A chat color by its vanilla name, such as {@code dark_red}, read by Paper's {@link ArgumentTypes#namedColor()}.
 */
@SuppressWarnings("UnstableApiUsage")
public class ChatColorArgument extends Argument<NamedTextColor, ChatColorArgument> {
    public ChatColorArgument(String name) {
        super(name, ArgumentTypes.namedColor());
    }

    @Override
    public NamedTextColor cast(Object parsedArgument) {
        return (NamedTextColor) parsedArgument;
    }

    @Override
    protected NamedTextColor parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), NamedTextColor.class);
    }
}
