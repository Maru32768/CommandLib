package net.kunmc.lab.commandlib.util.nms.hybrid;

/**
 * A server class with a method whose signature names a class the runtime lacks, next to a method that works.
 */
public class TargetWithBrokenMember {
    public static int value() {
        return 1;
    }

    public static void broken(MissingAtRuntime missing) {
    }
}
