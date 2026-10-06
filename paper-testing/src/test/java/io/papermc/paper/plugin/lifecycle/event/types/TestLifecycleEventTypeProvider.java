package io.papermc.paper.plugin.lifecycle.event.types;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner;
import org.mockito.Mockito;

/**
 * Lets tests touch {@link LifecycleEvents} without a server. Paper loads the provider through
 * {@link java.util.ServiceLoader}, and the provider interface is package-private, so this lives in Paper's package.
 */
@SuppressWarnings({"unchecked", "UnstableApiUsage"})
public final class TestLifecycleEventTypeProvider implements LifecycleEventTypeProvider {
    @Override
    public <O extends LifecycleEventOwner, E extends LifecycleEvent> LifecycleEventType.Monitorable<O, E> monitor(String name,
                                                                                                                   Class<? extends O> ownerType) {
        return Mockito.mock(LifecycleEventType.Monitorable.class);
    }

    @Override
    public <O extends LifecycleEventOwner, E extends LifecycleEvent> LifecycleEventType.Prioritizable<O, E> prioritized(String name,
                                                                                                                         Class<? extends O> ownerType) {
        return Mockito.mock(LifecycleEventType.Prioritizable.class);
    }
}
