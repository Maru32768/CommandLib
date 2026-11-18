package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentChat;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * The rest of the input as a chat message, like {@code /say}: the vanilla {@code message} argument. Entity selectors
 * in it, such as {@code @p}, are replaced by the names of the entities they select when the sender may use selectors
 * (permission level 2). It consumes the rest of the input, so it must be the last argument of its branch.
 *
 * <p>From 1.19 clients sign the message. The value is the resolved component without the signature, so sending it
 * sends a system message, not a signed chat message.
 */
public class MessageArgument extends Argument<BaseComponent, MessageArgument> {
    public MessageArgument(String name) {
        super(name,
              NMSArgumentChat.create()
                             .argument());
    }

    @Override
    public BaseComponent cast(Object parsedArgument) {
        return (BaseComponent) parsedArgument;
    }

    @Override
    protected BaseComponent parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentChat.create()
                              .parse(ctx.getHandle(), name());
    }
}
