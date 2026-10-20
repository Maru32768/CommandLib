package dev.marulabs.commandlib.internal.nms.exception;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class NMSClassNotAssignableException extends RuntimeException {
    public NMSClassNotAssignableException(Object handle, Class<?> serverClass) {
        super(String.format("serverClass(%s) is not assignable from handle's class(%s).",
                            serverClass,
                            handle.getClass()));
    }
}
