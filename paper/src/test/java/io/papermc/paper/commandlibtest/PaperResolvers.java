package io.papermc.paper.commandlibtest;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.SignedMessageResolver;
import net.kyori.adventure.chat.SignedMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Resolvers in a Paper package, which MessageArgument takes for Paper's own: one shaped like Paper's (a public
 * {@code message()} whose value has a private {@code resolveComponent}, as on 26.x) and one that is not.
 */
public final class PaperResolvers {
    private PaperResolvers() {
    }

    public static SignedMessageResolver resolving(String resolved) {
        return new Resolving(resolved);
    }

    public static SignedMessageResolver withoutMessage() {
        return new Base();
    }

    public static class Base implements SignedMessageResolver {
        @Override
        public String content() {
            return "hello, @p!";
        }

        @Override
        public CompletableFuture<SignedMessage> resolveSignedMessage(String argumentName,
                                                                     CommandContext<CommandSourceStack> context) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class Resolving extends Base {
        private final String resolved;

        Resolving(String resolved) {
            this.resolved = resolved;
        }

        public VanillaMessage message() {
            return new VanillaMessage(resolved);
        }
    }

    public static final class VanillaMessage {
        private final String resolved;

        VanillaMessage(String resolved) {
            this.resolved = resolved;
        }

        @SuppressWarnings("unused")
        private Message resolveComponent(Object source) {
            return () -> resolved;
        }
    }
}
