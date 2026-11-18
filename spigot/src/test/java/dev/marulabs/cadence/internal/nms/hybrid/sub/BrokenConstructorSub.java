package dev.marulabs.cadence.internal.nms.hybrid.sub;

import dev.marulabs.cadence.internal.nms.hybrid.BrokenConstructorBase;

/**
 * A typed class in another package than its superclass, calling the superclass's protected constructor.
 */
public class BrokenConstructorSub extends BrokenConstructorBase {
    public BrokenConstructorSub() {
        super();
    }
}
