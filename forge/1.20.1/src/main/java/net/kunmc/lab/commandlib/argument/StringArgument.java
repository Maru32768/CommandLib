package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.commandlib.CommandContext;

public class StringArgument extends CommonStringArgument<CommandContext, StringArgument> {
    public StringArgument(String name) {
        super(name);
    }

    public StringArgument(String name, Type type) {
        super(name, type);
    }

    public static class Type extends CommonStringArgument.Type {
        public static final Type WORD = new Type(StringArgumentType.word());
        public static final Type PHRASE_QUOTED = new Type(StringArgumentType.string());
        public static final Type PHRASE = new Type(StringArgumentType.greedyString());

        private Type(StringArgumentType type) {
            super(type);
        }
    }
}
