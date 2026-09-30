package net.kunmc.lab.commandlib.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.arguments.ArgumentTypes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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
            registered = true;
            tryRegisterWithArgumentTypes();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tryRegisterWithArgumentTypes() {
        try {
            Class<?> argTypesClass = ArgumentTypes.class;

            // Find the Class→Entry map: the static Map field whose keys include StringArgumentType.class.
            // We use content-based detection instead of field names since MCP/SRG names may vary.
            Map classToEntryMap = null;
            for (Field f : argTypesClass.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(null);
                if (val instanceof Map && ((Map) val).containsKey(StringArgumentType.class)) {
                    classToEntryMap = (Map) val;
                    break;
                }
            }
            if (classToEntryMap == null || classToEntryMap.containsKey(RawWordArgumentType.class)) {
                return;
            }

            // Find the serializer interface from the register(String, Class, IArgumentSerializer) method.
            Class<?> serializerInterface = null;
            for (Method m : argTypesClass.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 3 && params[0] == String.class && params[1] == Class.class && params[2].isInterface()) {
                    serializerInterface = params[2];
                    break;
                }
            }
            if (serializerInterface == null) {
                return;
            }

            // Find MinecraftKey (or ResourceLocation) class to construct "brigadier:string".
            // The Entry inner class holds an instance of it; detect by constructor parameter type.
            Class<?> entryClass = argTypesClass.getDeclaredClasses()[0];
            Constructor<?> entryCtor = null;
            Class<?> keyClass = null;
            for (Constructor<?> c : entryClass.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                // Expect (Class, IArgumentSerializer, MinecraftKey/ResourceLocation[, Object])
                if (p.length >= 3) {
                    entryCtor = c;
                    keyClass = p[2];
                    break;
                }
            }
            if (entryCtor == null || keyClass == null) {
                return;
            }

            // Tell the client this is brigadier:string GREEDY_PHRASE so it accepts any characters.
            Object brigadierStringKey = keyClass.getConstructor(String.class, String.class)
                                                .newInstance("brigadier", "string");

            final Class<?> finalSerializerInterface = serializerInterface;
            Object greedySerializer = Proxy.newProxyInstance(RawWordArgumentType.class.getClassLoader(),
                                                             new Class[]{finalSerializerInterface},
                                                             (proxy, method, args) -> {
                                                                 if (args == null || args.length == 0) {
                                                                     return null;
                                                                 }
                                                                 if (args.length == 2) {
                                                                     Object second = args[1];
                                                                     if (second instanceof JsonObject) {
                                                                         ((JsonObject) second).addProperty("type",
                                                                                                           "greedy");
                                                                     } else if (second != null) {
                                                                         writeGreedyPhrase(second);
                                                                     }
                                                                 } else {
                                                                     return rawWord();
                                                                 }
                                                                 return null;
                                                             });

            entryCtor.setAccessible(true);
            Object entry;
            if (entryCtor.getParameterCount() == 4) {
                entry = entryCtor.newInstance(RawWordArgumentType.class, greedySerializer, brigadierStringKey, null);
            } else {
                entry = entryCtor.newInstance(RawWordArgumentType.class, greedySerializer, brigadierStringKey);
            }
            classToEntryMap.put(RawWordArgumentType.class, entry);
        } catch (Exception ignored) {
            // Commands still execute correctly server-side; clients see no tab-completion for this node.
        }
    }

    private static void writeGreedyPhrase(Object packetBuffer) throws Exception {
        for (Method m : packetBuffer.getClass()
                                    .getMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Enum.class) {
                m.invoke(packetBuffer, StringArgumentType.StringType.GREEDY_PHRASE);
                return;
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
}
