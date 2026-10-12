package dev.marulabs.commandlib.util.nms.exception;

/**
 * Thrown when no typed NMS module provides a usable class of a wrapper for the running server.
 */
public class NMSClassNotFoundException extends RuntimeException {
    public NMSClassNotFoundException(String message) {
        super(message);
    }
}
