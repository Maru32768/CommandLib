package net.kunmc.lab.commandlib.util.nms;

import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.exception.UnregisteredNMSClassException;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NMSClassRegistry {
    private static final Map<Class<? extends NMSClass>, Deque<RegisteredClass>> CLASS_TO_DEQUE_MAP = new ConcurrentHashMap<>();
    private static final Map<Class<? extends NMSClass>, Deque<TypedRegistration>> TYPED_REGISTRATIONS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Class<?>>> TYPED_CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> MAPPING_PROBES = new ConcurrentHashMap<>();

    public static <T extends NMSClass> void register(Class<T> lookUpClass,
                                                     Class<? extends T> targetClass,
                                                     String lowerVersion,
                                                     String upperVersion) {
        Objects.requireNonNull(lookUpClass);
        Objects.requireNonNull(targetClass);

        CLASS_TO_DEQUE_MAP.computeIfAbsent(lookUpClass, x -> new ConcurrentLinkedDeque<>())
                          .addFirst(new RegisteredClass(targetClass,
                                                        new MinecraftVersion(lowerVersion),
                                                        new MinecraftVersion(upperVersion)));
    }

    /**
     * Registers an implementation that a typed NMS module compiles against a real server jar (see
     * {@code docs/agents/nms-build.md}). The class is looked up by name only on a server within the range, because it
     * may target a newer Java release than the server runs. A typed implementation takes precedence over the
     * reflection implementations, which are used when the class is not on the classpath.
     */
    public static <T extends NMSClass> void registerTyped(Class<T> lookUpClass,
                                                          String targetClassName,
                                                          String lowerVersion,
                                                          String upperVersion) {
        Objects.requireNonNull(lookUpClass);
        Objects.requireNonNull(targetClassName);

        TYPED_REGISTRATIONS.computeIfAbsent(lookUpClass, x -> new ConcurrentLinkedDeque<>())
                           .addFirst(new TypedRegistration(targetClassName,
                                                           new MinecraftVersion(lowerVersion),
                                                           new MinecraftVersion(upperVersion)));
    }

    public static <T extends Class<? extends NMSClass>> T findClass(T clazz) {
        Deque<RegisteredClass> deque = CLASS_TO_DEQUE_MAP.get(clazz);
        Deque<TypedRegistration> typedDeque = TYPED_REGISTRATIONS.get(clazz);
        if (deque == null && typedDeque == null) {
            throw new UnregisteredNMSClassException(clazz + " is unregistered.");
        }

        MinecraftVersion version = new MinecraftVersion(BukkitUtil.getMinecraftVersion());
        if (typedDeque != null) {
            for (TypedRegistration registration : typedDeque) {
                if (!version.isWithin(registration.lowerVersion, registration.upperVersion)) {
                    continue;
                }
                Optional<Class<?>> found = loadTypedClass(registration.className, clazz);
                if (found.isPresent() && clazz.isAssignableFrom(found.get()) && matchesMappings(found.get())) {
                    return ((T) found.get());
                }
            }
        }

        if (deque != null) {
            for (RegisteredClass registeredClass : deque) {
                if (version.isWithin(registeredClass.lowerVersion, registeredClass.upperVersion)) {
                    return ((T) registeredClass.clazz);
                }
            }
        }

        throw new UnregisteredNMSClassException(clazz + " is unregistered.");
    }

    private static Optional<Class<?>> loadTypedClass(String className, Class<?> lookUpClass) {
        return TYPED_CLASSES.computeIfAbsent(className, x -> {
            try {
                return Optional.of(Class.forName(x, false, lookUpClass.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                return Optional.empty();
            }
        });
    }

    /**
     * Paper remaps a plugin from Spigot names to Mojang names from 1.20.5 unless the plugin declares Mojang mappings,
     * so a typed module for those versions ships one package per mapping. Each package has a {@code MappingProbe}
     * whose static {@code matches()} tells whether its classes match the mapping the server loaded this plugin with.
     * Packages without a probe always match.
     */
    private static boolean matchesMappings(Class<?> typedClass) {
        String packageName = typedClass.getName()
                                       .substring(0,
                                                  typedClass.getName()
                                                            .lastIndexOf('.'));
        return MAPPING_PROBES.computeIfAbsent(packageName, x -> {
            Class<?> probe;
            try {
                probe = Class.forName(x + ".MappingProbe", true, typedClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                return true;
            } catch (LinkageError e) {
                return false;
            }
            try {
                Method matches = probe.getDeclaredMethod("matches");
                matches.setAccessible(true);
                return Boolean.TRUE.equals(matches.invoke(null));
            } catch (ReflectiveOperationException | LinkageError e) {
                return false;
            }
        });
    }

    /**
     * Returns the registered implementations of each look-up class, most recently registered first. Exposed for
     * tests that check the version ranges for gaps and overlaps.
     */
    static Map<Class<? extends NMSClass>, List<RegisteredClass>> registrations() {
        Map<Class<? extends NMSClass>, List<RegisteredClass>> result = new HashMap<>();
        CLASS_TO_DEQUE_MAP.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return result;
    }

    static class RegisteredClass {
        public final Class<? extends NMSClass> clazz;
        public final MinecraftVersion lowerVersion;
        public final MinecraftVersion upperVersion;

        private RegisteredClass(Class<? extends NMSClass> clazz,
                                MinecraftVersion lowerVersion,
                                MinecraftVersion upperVersion) {
            this.clazz = clazz;
            this.lowerVersion = lowerVersion;
            this.upperVersion = upperVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RegisteredClass that = (RegisteredClass) o;

            if (!Objects.equals(lowerVersion, that.lowerVersion)) {
                return false;
            }
            if (!Objects.equals(upperVersion, that.upperVersion)) {
                return false;
            }
            return Objects.equals(clazz, that.clazz);
        }

        @Override
        public int hashCode() {
            int result = lowerVersion != null ? lowerVersion.hashCode() : 0;
            result = 31 * result + (upperVersion != null ? upperVersion.hashCode() : 0);
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            return result;
        }
    }

    private static class TypedRegistration {
        private final String className;
        private final MinecraftVersion lowerVersion;
        private final MinecraftVersion upperVersion;

        private TypedRegistration(String className, MinecraftVersion lowerVersion, MinecraftVersion upperVersion) {
            this.className = className;
            this.lowerVersion = lowerVersion;
            this.upperVersion = upperVersion;
        }
    }
}
