package dev.marulabs.commandlib.util.nms;

import dev.marulabs.commandlib.util.bukkit.BukkitUtil;
import dev.marulabs.commandlib.util.bukkit.MinecraftVersion;
import dev.marulabs.commandlib.util.nms.exception.NMSClassNotFoundException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the implementation of each NMS wrapper in the typed NMS modules bundled into the spigot jar (see
 * {@code docs/agents/nms-build.md}). {@link TypedNmsModules} lists each module's package and the Minecraft versions
 * its server jar matches, and each module lists its classes in {@code <package>.TypedNmsClasses}. A class implements
 * the wrapper it extends.
 *
 * <p>A module's classes are loaded only on a server inside its versions, because they may target a newer Java release
 * than older servers run. Of the modules for the server's version, the first one whose package matches the plugin's
 * mappings and whose class links on the server provides the implementation.
 */
public final class NMSClassRegistry {
    private static final String TYPED_PACKAGE_PREFIX = "dev.marulabs.commandlib.nms.";
    private static final NMSClassRegistry DEFAULT = new NMSClassRegistry(TypedNmsModules.MODULES, isPaper());
    /**
     * The last Paper release the spigot jar has modules for. Later Paper releases need the paper artifact.
     */
    private static final String LAST_PAPER_VERSION = "1.21.1";

    private final List<TypedModule> modules;
    private final boolean paper;
    private final Map<String, ModuleClasses> moduleClasses = new ConcurrentHashMap<>();
    private final Map<String, Boolean> mappingProbes = new ConcurrentHashMap<>();
    /**
     * The implementation each wrapper resolved to, with the server version it was resolved for.
     */
    private final Map<Class<?>, Resolution> resolved = new ConcurrentHashMap<>();

    NMSClassRegistry(List<TypedModule> modules) {
        this(modules, false);
    }

    NMSClassRegistry(List<TypedModule> modules, boolean paper) {
        this.modules = List.copyOf(modules);
        this.paper = paper;
    }

    private static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager",
                          false,
                          NMSClassRegistry.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns the implementation of the wrapper for the running server.
     *
     * @throws NMSClassNotFoundException when no typed NMS module provides a usable implementation
     */
    public static <T extends Class<? extends NMSClass>> T findClass(T lookUpClass) {
        return DEFAULT.find(lookUpClass, BukkitUtil.getMinecraftVersion());
    }

    /**
     * Returns why each wrapper that a typed NMS module for the running server implements has no usable implementation,
     * as {@code <wrapper> has no ...}, or why no module matches. Empty when every wrapper resolves. The integration
     * tests use it to check every class of the modules, including the wrappers no test reaches.
     */
    public static List<String> unavailableClasses() {
        return DEFAULT.unavailable(BukkitUtil.getMinecraftVersion());
    }

    /**
     * Returns each typed NMS module for the running server's version, in the order they are tried, as
     * {@code <module>: <status>}: {@code in use} when it provided an implementation so far, {@code available}, or why
     * it cannot be used. Empty when no module supports the version. Runtime diagnostics report it.
     */
    public static List<String> moduleStatus() {
        return DEFAULT.moduleStatus(BukkitUtil.getMinecraftVersion());
    }

    List<String> moduleStatus(String version) {
        Set<String> inUse = new HashSet<>();
        for (Resolution resolution : resolved.values()) {
            String name = resolution.clazz.getName();
            if (resolution.version.equals(version) && name.startsWith(TYPED_PACKAGE_PREFIX)) {
                String rest = name.substring(TYPED_PACKAGE_PREFIX.length());
                inUse.add(rest.substring(0, Math.max(rest.indexOf('.'), 0)));
            }
        }
        List<String> status = new ArrayList<>();
        for (TypedModule module : modulesFor(new MinecraftVersion(version))) {
            String state;
            String failure = matchesMappings(module) ? classes(module).failure : null;
            if (!matchesMappings(module)) {
                state = "its mappings do not match the plugin";
            } else if (failure != null) {
                state = failure;
            } else if (inUse.contains(module.packageName)) {
                state = "in use";
            } else {
                state = "available";
            }
            status.add(module.packageName + ": " + state);
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    <T extends Class<? extends NMSClass>> T find(T lookUpClass, String version) {
        return (T) resolution(lookUpClass, version);
    }

    List<String> unavailable(String version) {
        MinecraftVersion minecraftVersion = new MinecraftVersion(version);
        List<String> problems = new ArrayList<>();
        Set<Class<?>> lookUpClasses = new TreeSet<>(Comparator.comparing(Class::getName));
        boolean matched = false;
        for (TypedModule module : modulesFor(minecraftVersion)) {
            if (!matchesMappings(module)) {
                continue;
            }
            matched = true;
            ModuleClasses classes = classes(module);
            if (classes.failure != null) {
                problems.add(module.packageName + ": " + classes.failure);
            } else {
                lookUpClasses.addAll(classes.byLookUpClass.keySet());
            }
        }
        if (!matched) {
            problems.add("No typed NMS module matches Minecraft " + version);
        }
        for (Class<?> lookUpClass : lookUpClasses) {
            try {
                resolution(lookUpClass, version);
            } catch (NMSClassNotFoundException e) {
                problems.add(e.getMessage());
            }
        }
        return problems;
    }

    private Class<?> resolution(Class<?> lookUpClass, String version) {
        Resolution resolution = resolved.get(lookUpClass);
        if (resolution == null || !resolution.version.equals(version)) {
            resolution = new Resolution(version, resolve(lookUpClass, version));
            resolved.put(lookUpClass, resolution);
        }
        return resolution.clazz;
    }

    private Class<?> resolve(Class<?> lookUpClass, String version) {
        List<TypedModule> candidates = modulesFor(new MinecraftVersion(version));
        if (candidates.isEmpty()) {
            throw new NMSClassNotFoundException(String.format("%s has no implementation for Minecraft %s: no typed NMS "
                    + "module supports this version", lookUpClass.getSimpleName(), version) + paperHint(version));
        }

        List<String> reasons = new ArrayList<>();
        for (TypedModule module : candidates) {
            if (!matchesMappings(module)) {
                reasons.add(module.packageName + ": its mappings do not match the plugin");
                continue;
            }
            ModuleClasses classes = classes(module);
            if (classes.failure != null) {
                reasons.add(module.packageName + ": " + classes.failure);
                continue;
            }
            Class<?> typed = classes.byLookUpClass.get(lookUpClass);
            if (typed == null) {
                reasons.add(module.packageName + ": no implementation");
            } else if (!TypedClassLinkage.isLinkable(typed)) {
                reasons.add(module.packageName + ": " + TypedClassLinkage.failure(typed));
            } else {
                return typed;
            }
        }
        throw new NMSClassNotFoundException(String.format("%s has no implementation for Minecraft %s (%s)",
                                                          lookUpClass.getSimpleName(),
                                                          version,
                                                          String.join("; ", reasons))
                + paperHint(version));
    }

    private String paperHint(String version) {
        if (!paper || !new MinecraftVersion(LAST_PAPER_VERSION).isLessThan(new MinecraftVersion(version))) {
            return "";
        }
        return ". The spigot artifact of CommandLib supports Paper up to " + LAST_PAPER_VERSION
                + "; use the paper artifact on Paper " + version;
    }

    private List<TypedModule> modulesFor(MinecraftVersion version) {
        List<TypedModule> result = new ArrayList<>();
        for (TypedModule module : modules) {
            if (version.isWithin(module.lowerVersion, module.upperVersion)) {
                result.add(module);
            }
        }
        return result;
    }

    /**
     * The classes {@code <package>.TypedNmsClasses} lists, by the wrapper each extends. Listing them loads them, but
     * neither links nor initializes them, so a class that refers to members the server lacks still loads.
     */
    private ModuleClasses classes(TypedModule module) {
        return moduleClasses.computeIfAbsent(module.packageName, x -> {
            String listName = TYPED_PACKAGE_PREFIX + x + ".TypedNmsClasses";
            try {
                Class<?> list = Class.forName(listName, true, NMSClassRegistry.class.getClassLoader());
                Map<Class<?>, Class<?>> byLookUpClass = new HashMap<>();
                for (Object typed : (List<?>) list.getMethod("classes")
                                                  .invoke(null)) {
                    byLookUpClass.put(((Class<?>) typed).getSuperclass(), (Class<?>) typed);
                }
                return new ModuleClasses(byLookUpClass, null);
            } catch (ClassNotFoundException e) {
                return new ModuleClasses(Map.of(), "the module is not in this jar");
            } catch (InvocationTargetException e) {
                return new ModuleClasses(Map.of(), String.valueOf(e.getCause()));
            } catch (ReflectiveOperationException | LinkageError e) {
                return new ModuleClasses(Map.of(), e.toString());
            }
        });
    }

    /**
     * Paper remaps a plugin from Spigot names to Mojang names from 1.20.5 unless the plugin declares Mojang mappings,
     * so a typed module for those versions ships one package per mapping. Each package has a {@code MappingProbe}
     * whose static {@code matches()} tells whether its classes match the mapping the server loaded this plugin with.
     * Packages without a probe always match.
     */
    private boolean matchesMappings(TypedModule module) {
        return mappingProbes.computeIfAbsent(module.packageName, x -> {
            Class<?> probe;
            try {
                probe = Class.forName(TYPED_PACKAGE_PREFIX + x + ".MappingProbe",
                                      true,
                                      NMSClassRegistry.class.getClassLoader());
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
     * A typed NMS module: the package under {@code dev.marulabs.commandlib.nms} and the Minecraft versions its server
     * jar matches. {@link TypedNmsModules} lists them.
     */
    static final class TypedModule {
        private final String packageName;
        private final MinecraftVersion lowerVersion;
        private final MinecraftVersion upperVersion;

        TypedModule(String packageName, String lowerVersion, String upperVersion) {
            this.packageName = packageName;
            this.lowerVersion = new MinecraftVersion(lowerVersion);
            this.upperVersion = new MinecraftVersion(upperVersion);
        }
    }

    private static final class ModuleClasses {
        private final Map<Class<?>, Class<?>> byLookUpClass;
        private final @Nullable String failure;

        private ModuleClasses(Map<Class<?>, Class<?>> byLookUpClass, @Nullable String failure) {
            this.byLookUpClass = byLookUpClass;
            this.failure = failure;
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
}
