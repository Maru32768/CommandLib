package net.kunmc.lab.commandlib.util.nms.hybrid.sub;

import net.kunmc.lab.commandlib.util.nms.hybrid.BrokenConstructorBase;

/**
 * A typed class in another package than its superclass, calling the superclass's protected constructor.
 */
public class BrokenConstructorSub extends BrokenConstructorBase {
    public BrokenConstructorSub() {
        super();
    }
}
