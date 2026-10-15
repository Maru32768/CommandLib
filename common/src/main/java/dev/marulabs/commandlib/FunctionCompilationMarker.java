package dev.marulabs.commandlib;

/**
 * Lets the copies of CommandLib that plugins shade (each relocated to its own package) compile the data pack functions
 * once for a change instead of once per copy. Every copy counts a change it makes, or an event after which functions
 * need compiling again, and records a compilation it does, in system properties, which every copy sees. A copy skips
 * its compilation when one was done for the same data pack resources after the last change.
 *
 * <p>A copy without this class, or one that reads another format, never skips, so copies of different versions compile
 * at worst once each, as before. The property names are not shaped like a package, so relocation leaves them alone.
 * Main thread only.
 */
final class FunctionCompilationMarker {
    static final String CHANGES = "commandlib.functionCompilation.changes";
    static final String COMPILED = "commandlib.functionCompilation.compiled";

    private FunctionCompilationMarker() {
    }

    /**
     * Records a change after which the functions must be compiled again.
     */
    static void changed() {
        System.setProperty(CHANGES, Long.toString(changes() + 1));
    }

    /**
     * Whether a copy compiled the functions for these resources after the last change.
     */
    static boolean compiled(Object resources) {
        return state(resources).equals(System.getProperty(COMPILED));
    }

    /**
     * Whether a copy compiled the functions for other data pack resources last, so the server loaded the data packs
     * again since. False before the first compilation.
     */
    static boolean reloaded(Object resources) {
        String compiled = System.getProperty(COMPILED);
        return compiled != null && !compiled.startsWith(System.identityHashCode(resources) + ":");
    }

    static void markCompiled(Object resources) {
        System.setProperty(COMPILED, state(resources));
    }

    // shortcut: two resources objects with the same identity hash (about 1 in 2^31 per reload) would skip a compilation;
    // share an object instead of a string if that ever matters.
    private static String state(Object resources) {
        return System.identityHashCode(resources) + ":" + changes();
    }

    private static long changes() {
        try {
            return Long.parseLong(System.getProperty(CHANGES, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
