package net.kunmc.lab.commandlib.util.nms.access;

/**
 * A superclass in another package than its subclass, with a protected constructor that {@code super(...)} may call and
 * a package-private one that it may not.
 */
public class ConstructorTarget {
    protected ConstructorTarget(int value) {
    }

    ConstructorTarget(char value) {
    }
}
