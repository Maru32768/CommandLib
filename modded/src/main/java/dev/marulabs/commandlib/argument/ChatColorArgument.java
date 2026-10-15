package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ColorArgument;

/**
 * A chat color in the vanilla {@code color} syntax, which the client validates and suggests: a color name such as
 * {@code dark_red}, or {@code reset}. Formats such as {@code bold} are rejected.
 */
public class ChatColorArgument extends Argument<ChatFormatting, ChatColorArgument> {
    public ChatColorArgument(String name) {
        super(name, ColorArgument.color());
    }

    @Override
    public ChatFormatting cast(Object parsedArgument) {
        return (ChatFormatting) parsedArgument;
    }

    @Override
    protected ChatFormatting parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ColorArgument.getColor(ctx.getHandle(), name());
    }
}
