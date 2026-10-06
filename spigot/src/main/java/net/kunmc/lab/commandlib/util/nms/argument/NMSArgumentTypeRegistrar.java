package net.kunmc.lab.commandlib.util.nms.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentTypeRegistrar_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentTypeRegistrar_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_0.NMSArgumentTypeRegistrar_v1_19_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers a custom {@link ArgumentType} implementation into the NMS argument-type
 * registry so that the server serializes it to clients as {@code brigadier:string GREEDY_PHRASE}.
 *
 * <p>Only the Class→Entry map is updated, not the Key→Entry map, so the client never
 * sees an unknown key; it deserializes the node using the standard brigadier:string path.
 */
public abstract class NMSArgumentTypeRegistrar extends MinecraftClass {
    public NMSArgumentTypeRegistrar(String className) {
        super(null, className);  // clazz = the NMS registry class for this version
    }

    public static NMSArgumentTypeRegistrar create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTypeRegistrar.class))
                             .newInstance();
    }

    /**
     * Returns the NMS serializer interface class for this version.
     */
    protected abstract Class<?> serializerInterface();

    /**
     * Creates an NMS MinecraftKey / ResourceLocation for "brigadier:string".
     */
    protected abstract Object brigadierStringKey();

    /**
     * Registers {@code argumentTypeClass} in the NMS Class→Entry map so it is
     * serialized to clients as {@code brigadier:string GREEDY_PHRASE}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        Map classToEntryMap = findClassToEntryMap();
        if (classToEntryMap == null) {
            throw new IllegalStateException("Could not find NMS argument-type Class→Entry map in " + clazz.getName());
        }
        if (classToEntryMap.containsKey(argumentTypeClass)) {
            return;
        }

        Class<?> serializerIface = serializerInterface();
        Object key = brigadierStringKey();

        Object greedySerializer = Proxy.newProxyInstance(argumentTypeClass.getClassLoader(),
                                                         new Class[]{serializerIface},
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
                                                                 return factory.get();
                                                             }
                                                             return null;
                                                         });

        Object entry = createEntry(argumentTypeClass, greedySerializer, key);
        classToEntryMap.put(argumentTypeClass, entry);
    }

    @SuppressWarnings({"rawtypes"})
    private Map findClassToEntryMap() {
        for (Field f : clazz.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(f.getType())) {
                continue;
            }
            f.setAccessible(true);
            try {
                Map m = (Map) f.get(null);
                if (m != null && m.containsKey(StringArgumentType.class)) {
                    return m;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    private Object createEntry(Class<?> argumentTypeClass, Object serializer, Object key) {
        Class<?>[] innerClasses = clazz.getDeclaredClasses();
        if (innerClasses.length == 0) {
            throw new IllegalStateException("NMS registry class has no inner entry class");
        }
        Class<?> entryClass = innerClasses[0];

        // The entry constructor differs between versions: (Class, serializer, key[, extra]) up to 1.17 and
        // (serializer, key) in 1.18. Arguments are assigned by parameter type so every shape is supported.
        for (Constructor<?> ctor : entryClass.getDeclaredConstructors()) {
            Object[] args = entryArguments(ctor.getParameterTypes(), argumentTypeClass, serializer, key);
            if (args == null) {
                continue;
            }
            ctor.setAccessible(true);
            try {
                return ctor.newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("No suitable entry constructor found in " + entryClass.getName());
    }

    private static Object[] entryArguments(Class<?>[] parameterTypes,
                                           Class<?> argumentTypeClass,
                                           Object serializer,
                                           Object key) {
        Object[] args = new Object[parameterTypes.length];
        boolean serializerAssigned = false;
        boolean keyAssigned = false;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == Class.class) {
                args[i] = argumentTypeClass;
            } else if (!serializerAssigned && type.isInstance(serializer)) {
                args[i] = serializer;
                serializerAssigned = true;
            } else if (!keyAssigned && type.isInstance(key)) {
                args[i] = key;
                keyAssigned = true;
            } else if (type.isPrimitive()) {
                return null;
            }
        }
        return serializerAssigned && keyAssigned ? args : null;
    }

    private static void writeGreedyPhrase(Object packetSerializer) throws Exception {
        for (Method m : packetSerializer.getClass()
                                        .getMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Enum.class) {
                m.invoke(packetSerializer, StringArgumentType.StringType.GREEDY_PHRASE);
                return;
            }
        }
    }

    static {
        NMSClassRegistry.register(NMSArgumentTypeRegistrar.class,
                                  NMSArgumentTypeRegistrar_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSArgumentTypeRegistrar.class,
                                  NMSArgumentTypeRegistrar_v1_17_0.class,
                                  "1.17.0",
                                  "1.18.9");
        NMSClassRegistry.register(NMSArgumentTypeRegistrar.class,
                                  NMSArgumentTypeRegistrar_v1_19_0.class,
                                  "1.19.0",
                                  "9.9.9");
    }
}
