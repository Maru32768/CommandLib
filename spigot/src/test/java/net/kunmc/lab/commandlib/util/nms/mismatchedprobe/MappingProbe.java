package net.kunmc.lab.commandlib.util.nms.mismatchedprobe;

/**
 * A probe for a package whose classes do not match the mappings the plugin runs with.
 */
final class MappingProbe {
    private MappingProbe() {
    }

    static boolean matches() {
        return false;
    }
}
