package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChatFormat;
import org.bukkit.ChatColor;

/**
 * A chat color in the vanilla {@code color} syntax, which the client validates and suggests: a color name such as
 * {@code dark_red}, or {@code reset} up to 26.1, where vanilla's color argument still has it. Formats such as
 * {@code bold} are rejected.
 */
public class ChatColorArgument extends Argument<ChatColor, ChatColorArgument> {
    public ChatColorArgument(String name) {
        super(name,
              NMSArgumentChatFormat.create()
                                   .argument());
    }

    @Override
    public ChatColor cast(Object parsedArgument) {
        return (ChatColor) parsedArgument;
    }

    @Override
    protected ChatColor parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentChatFormat.create()
                                    .parse(ctx.getHandle(), name());
    }
}
