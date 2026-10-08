package net.kunmc.lab.commandlib.util.nms.unlinkable;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistryTest;
import org.jetbrains.annotations.NotNull;

/**
 * A typed class that refers to a class the runtime lacks, like a typed NMS class on a server whose jar differs from
 * the one it was compiled against. The annotations jar is on the test compile classpath only.
 */
public class UnlinkableTypedLookUp extends NMSClassRegistryTest.TypedLookUp {
    public Class<?> missing() {
        return NotNull.class;
    }
}
