package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.brigadier.VanillaArgumentTypes;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;
import java.util.Map;

/**
 * A chat color in the vanilla {@code color} syntax, which the client validates and suggests: a color name such as
 * {@code dark_red}. Formats such as {@code bold} are rejected while parsing. {@code reset} is rejected while parsing
 * from 26.2, and up to 26.1, where the client accepts it, fails with {@code reset is not a color}, since
 * {@link NamedTextColor} has no reset.
 */
public class ChatColorArgument extends Argument<NamedTextColor, ChatColorArgument> {
    public ChatColorArgument(String name) {
        // Not Paper's namedColor(), which throws a NullPointerException while parsing reset.
        super(name, VanillaArgumentTypes.color());
    }

    @Override
    public NamedTextColor cast(Object parsedArgument) {
        return (NamedTextColor) parsedArgument;
    }

    @Override
    protected NamedTextColor parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String format = ((Enum<?>) ctx.getHandle()
                                      .getArgument(name(), Object.class)).name()
                                                                         .toLowerCase(Locale.ROOT);
        NamedTextColor color = NamedTextColor.NAMES.value(format);
        if (color == null) {
            throw new ArgumentParseException(Caption.NOT_A_COLOR, Map.of("value", format));
        }
        return color;
    }
}
