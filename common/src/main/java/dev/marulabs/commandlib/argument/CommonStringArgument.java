package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;

public class CommonStringArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonStringArgument<C, SELF>> extends CommonArgument<String, C, SELF> {
    public CommonStringArgument(String name) {
        super(name, Type.PHRASE_QUOTED.type);
    }

    public CommonStringArgument(String name, Type type) {
        super(name, type.type);
    }

    @Override
    public final String cast(Object parsedArgument) {
        return ((String) parsedArgument);
    }

    @Override
    protected final String parseImpl(C ctx) {
        return StringArgumentType.getString(ctx.getHandle(), name());
    }

    public static class Type {
        public static final Type WORD = new Type(StringArgumentType.word());
        public static final Type PHRASE_QUOTED = new Type(StringArgumentType.string());
        public static final Type PHRASE = new Type(StringArgumentType.greedyString());

        final StringArgumentType type;

        protected Type(StringArgumentType type) {
            this.type = type;
        }
    }
}
