package dev.marulabs.commandlib.util.nms.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

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
 *
 * <p>The registries keep that map in a private field, so the typed classes pass the registry class and the classes
 * the map's entries use, and the helpers here find the map by its content: {@link #registerInArgumentRegistry} up to
 * 1.18.2 and {@link #registerInArgumentTypeInfos} from 1.19.
 */
public abstract class NMSArgumentTypeRegistrar extends NMSClass {
    public NMSArgumentTypeRegistrar(Class<?> registryClass) {
        super(null, registryClass);
    }

    public static NMSArgumentTypeRegistrar create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTypeRegistrar.class))
                             .newInstance();
    }

    /**
     * Registers {@code argumentTypeClass} in the NMS Class→Entry map so it is
     * serialized to clients as {@code brigadier:string GREEDY_PHRASE}.
     */
    public abstract <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass,
                                                                            Supplier<T> factory);

    /**
     * Registers the type in the {@code ArgumentRegistry} (Mojang name {@code ArgumentTypes}) of 1.16 to 1.18.2, with a
     * serializer that writes {@code GREEDY_PHRASE}.
     *
     * @param serializerInterface the {@code ArgumentSerializer} interface
     * @param brigadierStringKey  the {@code MinecraftKey} / {@code ResourceLocation} {@code brigadier:string}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final <T extends ArgumentType<?>> void registerInArgumentRegistry(Class<T> argumentTypeClass,
                                                                                Supplier<T> factory,
                                                                                Class<?> serializerInterface,
                                                                                Object brigadierStringKey) {
        Map classToEntryMap = findClassToEntryMap();
        if (classToEntryMap == null) {
            throw new IllegalStateException("Could not find NMS argument-type Class→Entry map in " + serverClass.getName());
        }
        if (classToEntryMap.containsKey(argumentTypeClass)) {
            return;
        }

        Object greedySerializer = Proxy.newProxyInstance(argumentTypeClass.getClassLoader(),
                                                         new Class[]{serializerInterface},
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

        Object entry = createEntry(argumentTypeClass, greedySerializer, brigadierStringKey);
        classToEntryMap.put(argumentTypeClass, entry);
    }

    @SuppressWarnings({"rawtypes"})
    private Map findClassToEntryMap() {
        for (Field f : serverClass.getDeclaredFields()) {
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
        Class<?>[] innerClasses = serverClass.getDeclaredClasses();
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

    /**
     * Registers the type in the {@code ArgumentTypeInfos} of 1.19 and later. Its map holds {@code ArgumentTypeInfo}
     * instances directly, so the type gets a proxy whose {@code unpack()} returns the {@code GREEDY_PHRASE} template of
     * the string serializer. The template's own {@code type()} points back to the vanilla serializer, so the registry
     * lookup and the network write proceed through the vanilla path with no unknown key.
     *
     * @param argumentTypeInfoInterface the {@code ArgumentTypeInfo} interface
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected final void registerInArgumentTypeInfos(Class<?> argumentTypeClass, Class<?> argumentTypeInfoInterface) {
        Map byClassMap = findClassToEntryMap();
        if (byClassMap == null) {
            throw new IllegalStateException("Could not find NMS argument-type Class→ArgumentTypeInfo map in " + serverClass.getName());
        }
        if (byClassMap.containsKey(argumentTypeClass)) {
            return;
        }

        // The string ArgumentTypeInfo already in the map, and its inner Template class.
        Object stringSerializer = byClassMap.get(StringArgumentType.class);
        Class<?>[] innerClasses = stringSerializer.getClass()
                                                  .getDeclaredClasses();
        if (innerClasses.length == 0) {
            throw new IllegalStateException("Could not find Template inner class in " + stringSerializer.getClass()
                                                                                                        .getName());
        }
        Class<?> templateClass = innerClasses[0];

        // Inner-class constructor takes the outer instance as first parameter.
        Constructor<?> templateCtor = null;
        for (Constructor<?> c : templateClass.getDeclaredConstructors()) {
            if (c.getParameterCount() == 2 && c.getParameterTypes()[1] == StringArgumentType.StringType.class) {
                templateCtor = c;
                break;
            }
        }
        if (templateCtor == null) {
            throw new IllegalStateException("Could not find Template(outer, StringType) constructor in " + templateClass.getName());
        }
        templateCtor.setAccessible(true);
        Object greedyTemplate;
        try {
            greedyTemplate = templateCtor.newInstance(stringSerializer, StringArgumentType.StringType.GREEDY_PHRASE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GREEDY_PHRASE template", e);
        }

        // Match unpack() by signature (1 arg of ArgumentType), not method name, because servers with obfuscated names
        // give other methods of the interface the same name.
        Object proxy = Proxy.newProxyInstance(argumentTypeClass.getClassLoader(),
                                              new Class[]{argumentTypeInfoInterface},
                                              (p, method, args) -> {
                                                  if (args != null && args.length == 1 && args[0] instanceof ArgumentType) {
                                                      return greedyTemplate;
                                                  }
                                                  return null;
                                              });

        byClassMap.put(argumentTypeClass, proxy);
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
}
