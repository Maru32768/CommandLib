package dev.marulabs.commandlib.argument;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A chat color by its vanilla name, such as {@code dark_red}, or {@code reset}. Formats such as {@code bold} are rejected,
 * as by vanilla's color argument.
 */
public class ChatColorArgument extends ObjectArgument<ChatColor> {
    private static final Map<String, ChatColor> COLORS = colors();

    public ChatColorArgument(String name) {
        super(name, COLORS);
    }

    private static Map<String, ChatColor> colors() {
        Map<String, ChatColor> colors = new LinkedHashMap<>();
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor() || color == ChatColor.RESET) {
                colors.put(color.name()
                                .toLowerCase(Locale.ROOT),
                           color);
            }
        }
        return Collections.unmodifiableMap(colors);
    }
}
