package dev.marulabs.commandlib.nms.registry_mismatched;

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
