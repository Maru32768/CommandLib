package dev.marulabs.commandlib.internal;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Holds the platform's adapter, which every node creator and argument error asks for.
 */
final class PlatformAdapterHolder {
    // A fixed name rather than ServiceLoader, which broke on plugin reloads. Shading relocates the string with the
    // package.
    private static final String IMPLEMENTATION = "dev.marulabs.commandlib.PlatformAdapterImpl";
    // A race only creates another adapter, which works as the first one does: adapters keep no state.
    private static volatile @Nullable PlatformAdapter<?, ?, ?> instance;

    private PlatformAdapterHolder() {
    }

    static PlatformAdapter<?, ?, ?> get() {
        PlatformAdapter<?, ?, ?> adapter = instance;
        if (adapter == null) {
            adapter = load();
            instance = adapter;
        }
        return adapter;
    }

    private static PlatformAdapter<?, ?, ?> load() {
        try {
            // The implementation is package-private, so it stays out of the public API. It is in this jar or one shaded
            // with it, so the same class loader and module allow the access.
            Constructor<?> constructor = Class.forName(IMPLEMENTATION)
                                              .getDeclaredConstructor();
            constructor.setAccessible(true);
            return (PlatformAdapter<?, ?, ?>) constructor.newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("Could not find PlatformAdapter", e);
        }
    }
}
