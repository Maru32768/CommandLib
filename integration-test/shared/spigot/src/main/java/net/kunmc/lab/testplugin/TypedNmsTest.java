package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Checks that the typed NMS classes bundled for this server version are the ones NMSClassRegistry selects. The registry
 * falls back to the reflection implementations when a typed class does not link or does not match the plugin's
 * mappings, so the other tests pass either way; this test fails when that fallback hides a broken typed class.
 */
public class TypedNmsTest extends TestBase {
    public TypedNmsTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return typedNmsClassesAreSelected();
    }

    public List<String> typedNmsClassesAreSelected() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                try {
                    putResult(key, String.valueOf(findFallbacks()), "[]");
                } catch (ReflectiveOperationException | RuntimeException e) {
                    putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
                }
            });
        }});

        return List.of(buildCommand(command, name));
    }

    /**
     * Returns the wrappers that have a typed class for this version on the classpath, in a package whose MappingProbe
     * accepts this server, but resolve to a reflection implementation. Only wrappers already initialized by the earlier test commands are registered, which covers
     * the wrappers the tests use.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<String> findFallbacks() throws ReflectiveOperationException {
        MinecraftVersion version = new MinecraftVersion(BukkitUtil.getMinecraftVersion());
        Field registrationsField = NMSClassRegistry.class.getDeclaredField("TYPED_REGISTRATIONS");
        registrationsField.setAccessible(true);
        Map<Class<? extends NMSClass>, Collection<?>> registrations = (Map) registrationsField.get(null);

        List<String> fallbacks = new ArrayList<>();
        for (Map.Entry<Class<? extends NMSClass>, Collection<?>> entry : registrations.entrySet()) {
            if (!hasTypedClassInRange(entry.getValue(), version)) {
                continue;
            }
            Class<?> found = NMSClassRegistry.findClass(entry.getKey());
            if (!found.getName()
                      .contains(".commandlib.nms.")) {
                fallbacks.add(entry.getKey()
                                   .getSimpleName() + " -> " + found.getSimpleName());
            }
        }
        fallbacks.sort(String::compareTo);
        return fallbacks;
    }

    private static boolean hasTypedClassInRange(Collection<?> registrations,
                                                MinecraftVersion version) throws ReflectiveOperationException {
        for (Object registration : registrations) {
            MinecraftVersion lower = (MinecraftVersion) field(registration, "lowerVersion");
            MinecraftVersion upper = (MinecraftVersion) field(registration, "upperVersion");
            if (!version.isWithin(lower, upper)) {
                continue;
            }
            String className = (String) field(registration, "className");
            try {
                Class.forName(className, false, TypedNmsTest.class.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                // The module does not implement this wrapper.
                continue;
            }
            if (probeMatches(className.substring(0, className.lastIndexOf('.')))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A package with a MappingProbe is meant for this server only when the probe matches, as NMSClassRegistry checks.
     */
    private static boolean probeMatches(String packageName) throws ReflectiveOperationException {
        Class<?> probe;
        try {
            probe = Class.forName(packageName + ".MappingProbe", true, TypedNmsTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return true;
        } catch (LinkageError e) {
            return false;
        }
        Method matches = probe.getDeclaredMethod("matches");
        matches.setAccessible(true);
        try {
            return Boolean.TRUE.equals(matches.invoke(null));
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass()
                            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
