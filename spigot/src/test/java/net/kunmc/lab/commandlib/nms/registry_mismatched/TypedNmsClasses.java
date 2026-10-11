package net.kunmc.lab.commandlib.nms.registry_mismatched;

import java.util.List;

/**
 * The class list of a module whose mappings do not match.
 */
public final class TypedNmsClasses {
    private TypedNmsClasses() {
    }

    public static List<Class<?>> classes() {
        return List.of(MismatchedTypedLookUp.class);
    }
}
