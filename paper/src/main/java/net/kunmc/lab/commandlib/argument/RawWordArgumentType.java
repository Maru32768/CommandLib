package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("UnstableApiUsage")
class RawWordArgumentType implements CustomArgumentType<String, String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("@p", "@../config/value", "hello");

    static RawWordArgumentType rawWord() {
        return new RawWordArgumentType();
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString()
                     .substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        // A word would make clients flag selectors such as @p as invalid input. A greedy phrase is accepted by the
        // client, while the server still reads a single token through parse().
        return StringArgumentType.greedyString();
    }
}
