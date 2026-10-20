package dev.marulabs.commandlib.util.nms;

import dev.marulabs.commandlib.util.nms.exception.NMSClassNotAssignableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A wrapper of a server class. The typed NMS modules extend each wrapper and pass the server class they compile against,
 * so the build remaps it along with the rest of the bytecode.
 */
public abstract class NMSClass {
    private final @Nullable Object handle;
    protected final Class<?> serverClass;

    public NMSClass(@Nullable Object handle, @NotNull Class<?> serverClass) {
        this.handle = handle;
        this.serverClass = Objects.requireNonNull(serverClass);

        if (handle != null && !serverClass.isAssignableFrom(handle.getClass())) {
            throw new NMSClassNotAssignableException(handle, serverClass);
        }
    }

    /**
     * The wrapped server object, or null for a wrapper that only uses the static members of its server class, such as
     * an argument type factory, and for the test mocks.
     */
    @Nullable
    public final Object getHandle() {
        return handle;
    }

    /**
     * The wrapped server object of a wrapper that wraps one.
     *
     * @throws IllegalStateException if the wrapper has no handle
     */
    public final Object requireHandle() {
        if (handle == null) {
            throw new IllegalStateException(getClass().getName() + " has no handle");
        }
        return handle;
    }
}
