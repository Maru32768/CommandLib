package net.kunmc.lab.commandlib.util.nms;

import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.exception.UnregisteredNMSClassException;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
    /** Why a typed class that is on the classpath failed to load, such as a class file for a newer Java release. */
    private static final Map<String, String> TYPED_LOAD_FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> MAPPING_PROBES = new ConcurrentHashMap<>();
    /**
     * The implementation each look-up class resolved to, with the server version it was resolved for. Registrations
     * happen in the wrappers' static initializers, so a registration clears the entry of its look-up class.
     */
    private static final Map<Class<? extends NMSClass>, Resolution> RESOLVED = new ConcurrentHashMap<>();
    private static final String TYPED_PACKAGE_PREFIX = "net.kunmc.lab.commandlib.nms.";

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
        RESOLVED.remove(lookUpClass);
    }

    /**
     * Registers an implementation that a typed NMS module compiles against a real server jar (see
     * {@code docs/agents/nms-build.md}). The class is looked up by name only on a server within the range, because it
     * may target a newer Java release than the server runs. A typed implementation takes precedence over the
     * reflection implementations, which are used when the class is not on the classpath or refers to a class or
     * member the server lacks.
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
        RESOLVED.remove(lookUpClass);
    }

    /**
     * Registers the typed implementations of the look-up class in every typed NMS module, named
     * {@code net.kunmc.lab.commandlib.nms.<module>.<LookUpClass>_<module>}, for the module's versions or the narrower
     * versions its table entry gives the wrapper. A module without the class falls back to the reflection
     * implementations.
     */
    public static <T extends NMSClass> void registerTyped(Class<T> lookUpClass) {
        Objects.requireNonNull(lookUpClass);
        for (TypedModule module : TypedNmsModules.MODULES) {
            MinecraftVersion[] versions = module.wrapperVersions.getOrDefault(lookUpClass.getSimpleName(),
                                                                              new MinecraftVersion[]{module.lowerVersion, module.upperVersion});
            String className = TYPED_PACKAGE_PREFIX + module.packageName + "." + lookUpClass.getSimpleName() + "_"
                    + module.moduleName;
            TYPED_REGISTRATIONS.computeIfAbsent(lookUpClass, x -> new ConcurrentLinkedDeque<>())
                               .addFirst(new TypedRegistration(className, versions[0], versions[1]));
        }
        RESOLVED.remove(lookUpClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Class<? extends NMSClass>> T findClass(T clazz) {
        if (!CLASS_TO_DEQUE_MAP.containsKey(clazz) && !TYPED_REGISTRATIONS.containsKey(clazz)) {
            throw new UnregisteredNMSClassException(clazz + " is unregistered.");
        }
        String version = BukkitUtil.getMinecraftVersion();
        Resolution resolution = RESOLVED.get(clazz);
        if (resolution == null || !resolution.version.equals(version)) {
            resolution = new Resolution(version, resolve(clazz, new MinecraftVersion(version)));
            RESOLVED.put(clazz, resolution);
        }
        return (T) resolution.clazz;
    }

    private static Class<?> resolve(Class<? extends NMSClass> clazz, MinecraftVersion version) {
        Deque<RegisteredClass> deque = CLASS_TO_DEQUE_MAP.get(clazz);
        Deque<TypedRegistration> typedDeque = TYPED_REGISTRATIONS.get(clazz);
        if (typedDeque != null) {
            for (TypedRegistration registration : typedDeque) {
                Optional<Class<?>> typed = applicableTypedClass(registration, clazz, version);
                if (typed.isPresent() && TypedClassLinkage.isLinkable(typed.get())) {
                    return typed.get();
                }
            }
        }

        if (deque != null) {
            for (RegisteredClass registeredClass : deque) {
                if (version.isWithin(registeredClass.lowerVersion, registeredClass.upperVersion)) {
                    return registeredClass.clazz;
                }
            }
        }

        throw new UnregisteredNMSClassException(clazz + " is unregistered.");
    }

    /**
     * The typed class of the registration when the server version is in its range, the class is on the classpath and
     * its package matches the mappings the plugin was loaded with. Whether it links is checked separately.
     */
    private static Optional<Class<?>> applicableTypedClass(TypedRegistration registration,
                                                           Class<? extends NMSClass> lookUpClass,
                                                           MinecraftVersion version) {
        if (!version.isWithin(registration.lowerVersion, registration.upperVersion)) {
            return Optional.empty();
        }
        return loadTypedClass(registration.className, lookUpClass).filter(lookUpClass::isAssignableFrom)
                                                                  .filter(NMSClassRegistry::matchesMappings);
    }

    /**
     * Returns the look-up classes that have a typed class meant for this server, but resolve to another
     * implementation, each as {@code <look-up class> -> <resolved class> (<why each typed class is not used>)}. A typed
     * class is meant for the server when the version is in its range, it is on the classpath and its package matches
     * the plugin's mappings; one that fails to load or does not extend the look-up class counts too. Only wrappers
     * initialized so far are registered. The integration tests use it, because the fallback to reflection hides a
     * typed class that does not link from every other test.
     */
    public static List<String> typedFallbacks() {
        MinecraftVersion version = new MinecraftVersion(BukkitUtil.getMinecraftVersion());
        List<String> fallbacks = new ArrayList<>();
        TYPED_REGISTRATIONS.forEach((lookUpClass, registrations) -> {
            List<String> reasons = new ArrayList<>();
            for (TypedRegistration registration : registrations) {
                if (!version.isWithin(registration.lowerVersion, registration.upperVersion)) {
                    continue;
                }
                Optional<Class<?>> typed = loadTypedClass(registration.className, lookUpClass);
                String name = registration.className.substring(registration.className.lastIndexOf('.') + 1);
                String loadFailure = TYPED_LOAD_FAILURES.get(registration.className);
                if (loadFailure != null) {
                    reasons.add(name + ": " + loadFailure);
                } else if (typed.isPresent() && !lookUpClass.isAssignableFrom(typed.get())) {
                    reasons.add(name + ": does not extend " + lookUpClass.getName());
                } else if (typed.isPresent() && matchesMappings(typed.get())) {
                    reasons.add(name + ": " + TypedClassLinkage.failure(typed.get()));
                }
            }
            if (reasons.isEmpty()) {
                return;
            }
            Class<?> resolved = findClass(lookUpClass);
            if (!resolved.getName()
                         .startsWith(TYPED_PACKAGE_PREFIX)) {
                fallbacks.add(lookUpClass.getSimpleName() + " -> " + resolved.getSimpleName() + " ("
                                      + String.join("; ", reasons) + ")");
            }
        });
        fallbacks.sort(String::compareTo);
        return fallbacks;
    }

    /**
     * The typed class, or empty when it is not on the classpath or fails to load. A load failure is kept for
     * {@link #typedFallbacks()}.
     */
    private static Optional<Class<?>> loadTypedClass(String className, Class<?> lookUpClass) {
        return TYPED_CLASSES.computeIfAbsent(className, x -> {
            try {
                return Optional.of(Class.forName(x, false, lookUpClass.getClassLoader()));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            } catch (LinkageError e) {
                TYPED_LOAD_FAILURES.put(x, e.toString());
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

    /**
     * A typed NMS module: the package under {@code net.kunmc.lab.commandlib.nms}, the module name its classes carry as
     * a suffix, the Minecraft versions its server jar matches, and narrower versions for wrappers whose class needs a
     * later release. {@link TypedNmsModules} lists them.
     */
    static final class TypedModule {
        private final String packageName;
        private final String moduleName;
        private final MinecraftVersion lowerVersion;
        private final MinecraftVersion upperVersion;
        private final Map<String, MinecraftVersion[]> wrapperVersions = new HashMap<>();

        TypedModule(String moduleName, String lowerVersion, String upperVersion) {
            this(moduleName, moduleName, lowerVersion, upperVersion);
        }

        TypedModule(String packageName, String moduleName, String lowerVersion, String upperVersion) {
            this.packageName = packageName;
            this.moduleName = moduleName;
            this.lowerVersion = new MinecraftVersion(lowerVersion);
            this.upperVersion = new MinecraftVersion(upperVersion);
        }

        /**
         * Narrows the versions of the wrapper with the given simple name, such as {@code NMSDataPackResources}.
         */
        TypedModule withWrapperVersions(String wrapper, String lowerVersion, String upperVersion) {
            wrapperVersions.put(wrapper,
                                new MinecraftVersion[]{new MinecraftVersion(lowerVersion), new MinecraftVersion(upperVersion)});
            return this;
        }
    }

    private static final class Resolution {
        private final String version;
        private final Class<?> clazz;

        private Resolution(String version, Class<?> clazz) {
            this.version = version;
            this.clazz = clazz;
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
