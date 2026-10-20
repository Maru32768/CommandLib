package dev.marulabs.commandlib.internal.nms.typed.registry_typed;

import java.util.List;

/**
 * Stands in for the class list of a typed NMS module.
 */
public final class TypedNmsClasses {
    private TypedNmsClasses() {
    }

    public static List<Class<?>> classes() {
        return List.of(TypedLookUpImpl.class);
    }
}
