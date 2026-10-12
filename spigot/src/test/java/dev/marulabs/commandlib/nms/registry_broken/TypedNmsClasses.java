package dev.marulabs.commandlib.nms.registry_broken;

import java.util.List;

/**
 * The class list of a module whose class fails to load, like one built for a newer Java release than the server
 * runs. BrokenTypedLookUp is a test resource with the class file name but other content, so test discovery, which
 * only scans the compiled test classes, does not load it.
 */
public final class TypedNmsClasses {
    private TypedNmsClasses() {
    }

    public static List<Class<?>> classes() throws ClassNotFoundException {
        return List.of(Class.forName(TypedNmsClasses.class.getPackageName() + ".BrokenTypedLookUp"));
    }
}
