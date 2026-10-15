package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChatFormat;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads the colors and {@code reset}, as vanilla's color argument does.
 */
public class MockNMSArgumentChatFormat extends NMSArgumentChatFormat {
    private static final List<String> COLORS = Stream.of(ChatColor.values())
                                                     .filter(x -> x.isColor() || x == ChatColor.RESET)
                                                     .map(x -> x.name()
                                                                .toLowerCase(Locale.ROOT))
                                                     .collect(Collectors.toList());

    public MockNMSArgumentChatFormat() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.names("Invalid color", COLORS);
    }

    @Override
    protected ChatColor parseImpl(CommandContext<?> ctx, String name) {
        return ChatColor.valueOf(ctx.getArgument(name, String.class)
                                    .toUpperCase(Locale.ROOT));
    }
}
