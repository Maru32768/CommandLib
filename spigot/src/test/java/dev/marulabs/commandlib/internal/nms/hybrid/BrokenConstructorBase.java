package dev.marulabs.commandlib.internal.nms.hybrid;

/**
 * A superclass with a protected constructor next to one whose signature names a class the runtime lacks.
 */
public class BrokenConstructorBase {
    protected BrokenConstructorBase() {
    }

    public BrokenConstructorBase(MissingAtRuntime missing) {
    }
}
