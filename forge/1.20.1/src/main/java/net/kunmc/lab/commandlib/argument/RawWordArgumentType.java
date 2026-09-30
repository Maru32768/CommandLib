package net.kunmc.lab.commandlib.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.Collection;

class RawWordArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("@p", "@../config/value", "hello");

    static RawWordArgumentType rawWord() {
        return new RawWordArgumentType();
    }

    static void ensureRegistered() {
        synchronized (ArgumentTypeInfos.class) {
            if (!ArgumentTypeInfos.isClassRecognized(RawWordArgumentType.class)) {
                ArgumentTypeInfos.registerByClass(RawWordArgumentType.class, new Info());
            }
        }
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

    /**
     * Advertises this argument to clients as {@code brigadier:string} GREEDY_PHRASE.
     * {@link #unpack} returns vanilla's string template, whose {@code type()} is the registered
     * {@code brigadier:string} info, so the command tree packet only contains vanilla argument IDs.
     * The remaining methods are never reached because no template of this info is ever created.
     */
    private static final class Info implements ArgumentTypeInfo<RawWordArgumentType, ArgumentTypeInfo.Template<RawWordArgumentType>> {
        @Override
        public void serializeToNetwork(Template<RawWordArgumentType> template, FriendlyByteBuf buf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Template<RawWordArgumentType> deserializeFromNetwork(FriendlyByteBuf buf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void serializeToJson(Template<RawWordArgumentType> template, JsonObject json) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Template<RawWordArgumentType> unpack(RawWordArgumentType argumentType) {
            return (Template) ArgumentTypeInfos.unpack(StringArgumentType.greedyString());
        }
    }
}
