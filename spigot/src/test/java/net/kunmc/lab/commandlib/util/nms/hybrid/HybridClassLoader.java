package net.kunmc.lab.commandlib.util.nms.hybrid;

import java.io.IOException;
import java.io.InputStream;

/**
 * Defines the classes of this package itself, except {@link MissingAtRuntime}, which it does not find. The test class
 * path keeps every class, so test discovery, which reflects over all of them, still works.
 */
public class HybridClassLoader extends ClassLoader {
    private static final String PACKAGE = HybridClassLoader.class.getPackage()
                                                                  .getName() + ".";

    public HybridClassLoader() {
        super(HybridClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(PACKAGE) || name.equals(HybridClassLoader.class.getName())) {
            return super.loadClass(name, resolve);
        }
        if (name.equals(MissingAtRuntime.class.getName())) {
            throw new ClassNotFoundException(name);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }
            try (InputStream in = getResourceAsStream(name.replace('.', '/') + ".class")) {
                byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
