package net.kunmc.lab.commandlib.nms.registry_unlinkable;

import java.util.List;

/**
 * The class list of a module whose class does not link.
 */
public final class TypedNmsClasses {
    private TypedNmsClasses() {
    }

    public static List<Class<?>> classes() {
        return List.of(UnlinkableTypedLookUp.class);
    }
}
