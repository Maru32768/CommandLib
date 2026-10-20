package dev.marulabs.commandlib.internal.nms.hybrid;

/**
 * A typed class that only uses the working method of {@link TargetWithBrokenMember}.
 */
public class BrokenOwnerCaller {
    public Object run() {
        return TargetWithBrokenMember.value();
    }
}
