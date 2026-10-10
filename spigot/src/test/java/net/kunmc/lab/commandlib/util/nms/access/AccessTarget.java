package net.kunmc.lab.commandlib.util.nms.access;

/**
 * Lives in a package of its own, so its private members are not accessible from the classes that call it.
 */
public class AccessTarget {
    public static void publicMethod() {
    }

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
