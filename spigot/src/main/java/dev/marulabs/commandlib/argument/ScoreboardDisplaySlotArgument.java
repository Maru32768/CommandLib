package dev.marulabs.commandlib.argument;

import org.bukkit.scoreboard.DisplaySlot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A scoreboard display slot by its vanilla name: {@code list}, {@code sidebar}, {@code below_name} (also the pre-1.20.2
 * {@code belowName}), or {@code sidebar.team.<color>} where the server's {@link DisplaySlot} has team slots.
 */
public class ScoreboardDisplaySlotArgument extends ObjectArgument<DisplaySlot> {
    private static final Map<String, DisplaySlot> SLOTS = slots();

    public ScoreboardDisplaySlotArgument(String name) {
        super(name, SLOTS);
    }

    private static Map<String, DisplaySlot> slots() {
        Map<String, DisplaySlot> slots = new LinkedHashMap<>();
        for (DisplaySlot slot : DisplaySlot.values()) {
            slots.put(vanillaName(slot.name()), slot);
            if (slot == DisplaySlot.BELOW_NAME) {
                slots.put("belowName", slot);
            }
        }
        return Collections.unmodifiableMap(slots);
    }

    static String vanillaName(String enumName) {
        String name = enumName.toLowerCase(Locale.ROOT);
        switch (name) {
            case "player_list":
                return "list";
            case "below_name":
            case "sidebar":
                return name;
            default:
                // Team slots are SIDEBAR_<COLOR> in Spigot and SIDEBAR_TEAM_<COLOR> in Paper.
                String color = name.substring("sidebar_".length());
                return "sidebar.team." + (color.startsWith("team_") ? color.substring("team_".length()) : color);
        }
    }
}
