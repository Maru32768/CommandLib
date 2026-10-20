package dev.marulabs.commandlib.internal.nms.access;

/**
 * Lives in a package of its own, so its private members are not accessible from the classes that call it.
 */
public class AccessTarget {
    public static void publicMethod() {
    }

    // Called only from the bytecode TypedClassLinkageTest rewrites, which the linkage check must reject.
    @SuppressWarnings("UnusedMethod")
    private static void hiddenMethod() {
    }

    /**
     * Returns a class that callers cannot access, which their class files still name in {@code InnerClasses}.
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public static Hidden hidden() {
        return new Hidden();
    }

    static class Hidden {
    }
}
