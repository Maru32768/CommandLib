package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.network.chat.Component;

/**
 * The rest of the input as a chat message, like {@code /say}: the vanilla {@code message} argument. Entity selectors
 * in it, such as {@code @p}, are replaced by the names of the entities they select when the sender may use selectors
 * (permission level 2). It consumes the rest of the input, so it must be the last argument of its branch.
 *
 * <p>From 1.19 clients sign the message. The value is the resolved component without the signature, so sending it
 * sends a system message, not a signed chat message. For the signed message on 1.19.3 and later, call
 * vanilla's {@code MessageArgument.resolveChatMessage(ctx.getHandle(), name, callback)}, which may call back later.
 */
public class MessageArgument extends Argument<Component, MessageArgument> {
    public MessageArgument(String name) {
        super(name, net.minecraft.commands.arguments.MessageArgument.message());
    }

    @Override
    public Component cast(Object parsedArgument) {
        return (Component) parsedArgument;
    }

    @Override
    protected Component parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return net.minecraft.commands.arguments.MessageArgument.getMessage(ctx.getHandle(), name());
    }
}
