package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardSlot;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the slot names of a server from 1.20.2: {@code list}, {@code sidebar}, {@code below_name} and
 * {@code sidebar.team.<color>}.
 */
@ApiStatus.Internal
public class MockNMSArgumentScoreboardSlot extends NMSArgumentScoreboardSlot {
    private static final List<String> SLOTS = slots();

    public MockNMSArgumentScoreboardSlot() {
        super(null, Object.class);
    }

    private static List<String> slots() {
        List<String> slots = new ArrayList<>(List.of("list", "sidebar", "below_name"));
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor()) {
                slots.add("sidebar.team." + color.name()
                                                 .toLowerCase(Locale.ROOT));
            }
        }
        return slots;
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.names("Unknown display slot", SLOTS);
    }

    @Override
    protected String parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}
