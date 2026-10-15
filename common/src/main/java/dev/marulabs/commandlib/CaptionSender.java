package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Sends a {@link Caption}, set with {@link Captions#sender(CaptionSender)}. The default sends {@code message} with
 * {@code send}.
 * <p>
 * For example, to prefix every caption, or to send the captions as MiniMessage texts on Paper:
 *
 * <pre>{@code
 * captions.sender((ctx, caption, message, send) -> send.accept(RichText.text("[MyPlugin] ").append(message)));
 *
 * captions.sender((ctx, caption, message, send) -> ((CommandContext) ctx).sendFailure(MiniMessage.miniMessage()
 *                                                                                                .deserialize(message.plainText())));
 * }</pre>
 *
 * Messages sent here while a command fails are part of its failure, as those of {@code ctx.sendFailure} are.
 */
@FunctionalInterface
public interface CaptionSender {
    /**
     * Sends {@code caption}.
     *
     * @param ctx     the context to send to
     * @param caption the caption, to treat some captions apart
     * @param message the caption's text for the sender's locale with its placeholders replaced, followed by the button
     *                for the confirmation request; a translatable text for a caption sent as a vanilla translation
     *                key
     * @param send    sends a message the way CommandLib does for this caption, such as in red for a failure
     */
    void send(@NotNull CommonCommandContext<?, ?> ctx,
              @NotNull Caption caption,
              @NotNull RichText message,
              @NotNull Consumer<RichText> send);
}
