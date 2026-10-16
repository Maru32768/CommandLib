package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.SignedMessageResolver;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The rest of the input as a chat message, like {@code /say}: Paper's {@code ArgumentTypes.signedMessage()}. Entity
 * selectors in it, such as {@code @p}, are replaced by the names of the entities they select when the sender may use
 * selectors (permission level 2). It consumes the rest of the input, so it must be the last argument of its branch.
 *
 * <p>From 1.19 clients sign the message. The value is the resolved component without the signature, so sending it
 * sends a system message, not a signed chat message. Paper resolves the signed message only
 * asynchronously, after chat decoration; to send it signed, read Paper's resolver from the Brigadier context:
 * {@code ctx.getHandle().getArgument(name, SignedMessageResolver.class).resolveSignedMessage(name, ctx.getHandle())}.
 */
@SuppressWarnings("UnstableApiUsage")
public class MessageArgument extends Argument<Component, MessageArgument> {
    public MessageArgument(String name) {
        super(name, ArgumentTypes.signedMessage());
    }

    @Override
    public Component cast(Object parsedArgument) {
        return (Component) parsedArgument;
    }

    @Override
    protected Component parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        SignedMessageResolver resolver = ctx.getHandle()
                                            .getArgument(name(), SignedMessageResolver.class);
        Message resolved = resolveSelectors(resolver,
                                            ctx.getHandle()
                                               .getSource());
        return resolved == null ? Component.text(resolver.content()) : MessageComponentSerializer.message()
                                                                                                 .deserialize(resolved);
    }

    /**
     * Resolves the selectors as vanilla's {@code MessageArgument.getMessage} does. Paper's resolver keeps the vanilla
     * message, which has no API, so it is read by its Mojang names: the record accessor {@code message()} and the
     * message's {@code resolveComponent(CommandSourceStack)}, both present from Paper 1.21 to 26.3. A Paper resolver
     * without them fails the parse rather than passing the selectors on unresolved. Returns null for a resolver that is
     * not Paper's (such as a test double), whose content is then used as written.
     */
    @Nullable
    static Message resolveSelectors(SignedMessageResolver resolver, Object source) throws CommandSyntaxException {
        if (!resolver.getClass()
                     .getName()
                     .startsWith("io.papermc.paper.")) {
            return null;
        }
        Object message;
        try {
            message = resolver.getClass()
                              .getMethod("message")
                              .invoke(resolver);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot resolve the selectors of a message: " + resolver.getClass()
                                                                                                    .getName()
                    + " has no message()", e);
        }
        for (Method method : message.getClass()
                                    .getDeclaredMethods()) {
            if (method.getName()
                      .equals("resolveComponent")
                    && method.getParameterCount() == 1) {
                try {
                    method.setAccessible(true);
                    return (Message) method.invoke(message, source);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof CommandSyntaxException) {
                        throw (CommandSyntaxException) e.getCause();
                    }
                    throw new IllegalStateException(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(message.getClass()
                                               .getName()
                + " has no resolveComponent method");
    }
}
