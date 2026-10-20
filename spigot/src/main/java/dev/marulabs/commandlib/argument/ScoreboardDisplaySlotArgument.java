package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardSlot;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A scoreboard display slot in the vanilla {@code scoreboard_slot} syntax, which the client validates and suggests:
 * {@code list}, {@code sidebar}, {@code below_name} ({@code belowName} before 1.20.2) or {@code sidebar.team.<color>}.
 * A team slot fails when the command runs on a server whose {@link DisplaySlot} has no team slots.
 */
public class ScoreboardDisplaySlotArgument extends Argument<DisplaySlot, ScoreboardDisplaySlotArgument> {
    private static final Map<String, DisplaySlot> SLOTS = slots();

    public ScoreboardDisplaySlotArgument(String name) {
        super(name,
              NMSArgumentScoreboardSlot.create()
                                       .argument());
    }

    @Override
    public DisplaySlot cast(Object parsedArgument) {
        return (DisplaySlot) parsedArgument;
    }

    @Override
    protected DisplaySlot parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String slotName = NMSArgumentScoreboardSlot.create()
                                                   .parse(ctx.getHandle(), name());
        DisplaySlot slot = SLOTS.get(slotName);
        if (slot == null) {
            throw new ArgumentParseException(Caption.UNKNOWN_DISPLAY_SLOT, Map.of("value", slotName));
        }
        return slot;
    }

    private static Map<String, DisplaySlot> slots() {
        Map<String, DisplaySlot> slots = new LinkedHashMap<>();
        for (DisplaySlot slot : DisplaySlot.values()) {
            String name = vanillaName(slot.name());
            if (name != null) {
                slots.put(name, slot);
            }
            if (slot == DisplaySlot.BELOW_NAME) {
                slots.put("belowName", slot);
            }
        }
        return Collections.unmodifiableMap(slots);
    }

    // Null for a constant outside the known pattern, which a newer Bukkit or a fork may add.
    static @Nullable String vanillaName(String enumName) {
        String name = enumName.toLowerCase(Locale.ROOT);
        switch (name) {
            case "player_list":
                return "list";
            case "below_name":
            case "sidebar":
                return name;
            default:
                // Team slots are SIDEBAR_<COLOR> in Spigot and SIDEBAR_TEAM_<COLOR> in Paper.
                if (!name.startsWith("sidebar_")) {
                    return null;
                }
                String color = name.substring("sidebar_".length());
                return "sidebar.team." + (color.startsWith("team_") ? color.substring("team_".length()) : color);
        }
    }
}
