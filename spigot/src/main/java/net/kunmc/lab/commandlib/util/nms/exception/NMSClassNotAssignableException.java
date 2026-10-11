package net.kunmc.lab.commandlib.util.nms.exception;

public class NMSClassNotAssignableException extends RuntimeException {
    public NMSClassNotAssignableException(Object handle, Class<?> serverClass) {
        super(String.format("serverClass(%s) is not assignable from handle's class(%s).",
                            serverClass,
                            handle.getClass()));
    }
}
