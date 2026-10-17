package io.papermc.paper.commandlibtest;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.SignedMessageResolver;
import net.kyori.adventure.chat.SignedMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Resolvers in a Paper package, which MessageArgument takes for Paper's own: one shaped like Paper's (a public
 * {@code message()} whose value has a private {@code resolveComponent}, as on 26.x), one whose
 * {@code resolveComponent} fails, and one that is not shaped like Paper's.
 */
public final class PaperResolvers {
    private PaperResolvers() {
    }

    public static SignedMessageResolver resolving(String resolved) {
        return new Resolving(resolved);
    }

    public static SignedMessageResolver failing(CommandSyntaxException failure) {
        return new Failing(failure);
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

    public static final class Failing extends Base {
        private final CommandSyntaxException failure;

        Failing(CommandSyntaxException failure) {
            this.failure = failure;
        }

        public FailingMessage message() {
            return new FailingMessage(failure);
        }
    }

    public static final class FailingMessage {
        private final CommandSyntaxException failure;

        FailingMessage(CommandSyntaxException failure) {
            this.failure = failure;
        }

        @SuppressWarnings("unused")
        private Message resolveComponent(Object source) throws CommandSyntaxException {
            throw failure;
        }
    }
}
