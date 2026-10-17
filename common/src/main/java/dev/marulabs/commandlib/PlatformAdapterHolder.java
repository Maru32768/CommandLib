package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

/**
 * Holds the platform's adapter, which every node creator and argument error asks for.
 */
final class PlatformAdapterHolder {
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
            return Class.forName("dev.marulabs.commandlib.PlatformAdapterImpl")
                        .asSubclass(PlatformAdapter.class)
                        .getConstructor()
                        .newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("Could not find PlatformAdapter", e);
        }
    }
}
