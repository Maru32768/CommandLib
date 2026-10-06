package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;

import java.util.Arrays;
import java.util.Collection;

class RawWordArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("@p", "@../config/value", "hello");
    private static volatile boolean registered = false;

    static RawWordArgumentType rawWord() {
        return new RawWordArgumentType();
    }

    static void ensureRegistered() {
        if (registered) {
            return;
        }
        synchronized (RawWordArgumentType.class) {
            if (registered) {
                return;
            }
            // Mark as registered only after success. Otherwise a failed registration would let commands using this
            // type reach the client command tree with an argument type the server cannot serialize.
            NMSArgumentTypeRegistrar.create()
                                    .registerAsGreedyString(RawWordArgumentType.class, RawWordArgumentType::rawWord);
            registered = true;
        }
    }

    static boolean isRegistered() {
        return registered;
    }

    static void resetRegistrationForTest() {
        registered = false;
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
}
