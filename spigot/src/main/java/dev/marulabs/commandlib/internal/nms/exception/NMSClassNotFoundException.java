package dev.marulabs.commandlib.internal.nms.exception;

import org.jetbrains.annotations.ApiStatus;

/**
 * Thrown when no typed NMS module provides a usable class of a wrapper for the running server.
 */
@ApiStatus.Internal
public class NMSClassNotFoundException extends RuntimeException {
    public NMSClassNotFoundException(String message) {
        super(message);
    }
}
