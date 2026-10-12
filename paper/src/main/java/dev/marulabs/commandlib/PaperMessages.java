package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Converts Adventure {@link Component} objects to Brigadier {@link Message} objects
 * for use as suggestion tooltips.
 *
 * <pre>{@code
 * argument(new StringArgument("key")
 *     .addSuggestionAction(sb -> sb.suggest("difficulty",
 *         PaperMessages.toMessage(Component.text("The world difficulty").color(NamedTextColor.GRAY)))));
 * }</pre>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PaperMessages {
    private PaperMessages() {
    }

    public static @NotNull Message toMessage(@NotNull Component component) {
        return MessageComponentSerializer.message()
                                         .serialize(component);
    }
}
