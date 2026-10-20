package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatFormat;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads the colors and {@code reset}, as vanilla's color argument does. Like vanilla's {@code ChatFormatting.getByName},
 * it ignores case and characters other than letters, so {@code DARK_RED} and {@code darkred} read as {@code dark_red}.
 */
@ApiStatus.Internal
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
        ArgumentType<String> names = MockArgumentTypes.names("Invalid color", COLORS);
        return new ArgumentType<String>() {
            @Override
            public String parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String input = reader.readUnquotedString();
                String key = clean(input);
                for (String color : COLORS) {
                    if (clean(color).equals(key)) {
                        return color;
                    }
                }
                reader.setCursor(start);
                throw MockArgumentTypes.syntaxError("Invalid color '" + input + "'", reader);
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                return names.listSuggestions(context, builder);
            }
        };
    }

    private static String clean(String name) {
        return name.toLowerCase(Locale.ROOT)
                   .replaceAll("[^a-z]", "");
    }

    @Override
    protected ChatColor parseImpl(CommandContext<?> ctx, String name) {
        return ChatColor.valueOf(ctx.getArgument(name, String.class)
                                    .toUpperCase(Locale.ROOT));
    }
}
