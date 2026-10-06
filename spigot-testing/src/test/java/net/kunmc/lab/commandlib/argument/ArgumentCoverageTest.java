package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommonArgument;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that every concrete Argument subclass in the argument package has a corresponding test class.
 * Fails automatically when a new Argument is added without a test.
 *
 * <p>Add to EXCLUDED_ARGUMENTS when a class intentionally has no standalone test
 * (e.g. tested indirectly, or not a user-facing argument type).
 */
class ArgumentCoverageTest {
    private static final String ARGUMENT_PACKAGE = "net.kunmc.lab.commandlib.argument";
    private static final String TEST_PACKAGE = "net.kunmc.lab.commandlib.argument";

    /**
     * Argument classes that intentionally have no standalone *Test class.
     * Document the reason when adding an entry here.
     */
    private static final Set<Class<?>> EXCLUDED_ARGUMENTS = Set.of();

    @Test
    void scan_includes_arguments_derived_from_common_implementations() throws Exception {
        assertThat(findConcreteArgumentClasses()).contains(IntegerArgument.class,
                                                           StringArgument.class,
                                                           LiteralArgument.class,
                                                           EnumArgument.class);
    }

    @Test
    void all_argument_classes_have_a_test() {
        List<Class<?>> argumentClasses = new ArrayList<>();
        assertThatCode(() -> argumentClasses.addAll(findConcreteArgumentClasses())).as("Package scan for %s failed",
                                                                                       ARGUMENT_PACKAGE)
                                                                                   .doesNotThrowAnyException();
        assertThat(argumentClasses).as("No Argument classes found in %s", ARGUMENT_PACKAGE)
                                   .isNotEmpty();

        List<String> missing = new ArrayList<>();
        for (Class<?> argClass : argumentClasses) {
            String testClassName = TEST_PACKAGE + "." + argClass.getSimpleName() + "Test";
            try {
                Class.forName(testClassName,
                              false,
                              Thread.currentThread()
                                    .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                missing.add(argClass.getSimpleName());
            }
        }

        assertThat(missing).as("These Argument classes have no corresponding *Test class in %s", TEST_PACKAGE)
                           .isEmpty();
    }

    private List<Class<?>> findConcreteArgumentClasses() throws Exception {
        String packagePath = ARGUMENT_PACKAGE.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread()
                                        .getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        List<Class<?>> result = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File dir = new File(resource.toURI());
                File[] files = dir.listFiles();
                if (files == null) {
                    continue;
                }
                for (File file : files) {
                    String filename = file.getName();
                    if (!filename.endsWith(".class")) {
                        continue;
                    }
                    String className = ARGUMENT_PACKAGE + "." + filename.replace(".class", "");
                    addIfConcreteArgument(className, classLoader, result);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConn.getJarFile()) {
                    jarFile.stream()
                           .map(JarEntry::getName)
                           .filter(name -> name.startsWith(packagePath + "/") && name.endsWith(".class") && !name.substring(
                                                                                                                         packagePath.length() + 1)
                                                                                                                 .contains(
                                                                                                                         "/"))
                           .forEach(name -> {
                               String className = name.replace('/', '.')
                                                      .replace(".class", "");
                               addIfConcreteArgument(className, classLoader, result);
                           });
                }
            }
        }
        return result;
    }

    private static Object location(Class<?> cls) {
        return cls.getProtectionDomain()
                  .getCodeSource()
                  .getLocation();
    }

    private void addIfConcreteArgument(String className, ClassLoader classLoader, List<Class<?>> result) {
        try {
            Class<?> cls = Class.forName(className, false, classLoader);
            // Many public arguments extend the common implementations rather than the platform Argument class, so
            // the scan has to start from CommonArgument to include them.
            boolean isPublicTopLevel = Modifier.isPublic(cls.getModifiers()) && cls.getEnclosingClass() == null;
            // The common module uses the same package name; only the Spigot module's own classes need tests here.
            boolean isSpigotClass = Objects.equals(location(cls), location(IntegerArgument.class));
            if (isPublicTopLevel && isSpigotClass && !Modifier.isAbstract(cls.getModifiers()) && CommonArgument.class.isAssignableFrom(
                    cls) && !EXCLUDED_ARGUMENTS.contains(cls)) {
                result.add(cls);
            }
        } catch (ClassNotFoundException ignored) {
        }
    }
}
