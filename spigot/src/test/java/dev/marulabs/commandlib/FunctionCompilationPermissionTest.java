package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FunctionCompilationPermissionTest {
    @Test
    void allowsASourceWithoutBukkitSenderAsFunctionsAreCompiledWithOne() {
        try (MockedStatic<NMSClassRegistry> registry = mockStatic(NMSClassRegistry.class)) {
            registry.when(() -> NMSClassRegistry.findClass(NMSCommandListenerWrapper.class))
                    .thenReturn(SenderlessSource.class);

            assertThat(new PlatformAdapterImpl().hasPermission(new Object(), "plugin.command.foo")).isTrue();
        }
    }

    @Test
    void checksTheBukkitSenderOtherwise() {
        try (MockedStatic<NMSClassRegistry> registry = mockStatic(NMSClassRegistry.class)) {
            registry.when(() -> NMSClassRegistry.findClass(NMSCommandListenerWrapper.class))
                    .thenReturn(DeniedSource.class);

            assertThat(new PlatformAdapterImpl().hasPermission(new Object(), "plugin.command.foo")).isFalse();
        }
    }

    /**
     * Stands in for CraftBukkit's CommandSource.NULL, the source of data pack function compilation.
     */
    public static class SenderlessSource extends NMSCommandListenerWrapper {
        public SenderlessSource(Object handle) {
            super(handle, Object.class);
        }

        @Override
        public CommandSender getBukkitSender() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Entity getBukkitEntity() {
            return null;
        }

        @Override
        public World getBukkitWorld() {
            return null;
        }

        @Override
        protected dev.marulabs.commandlib.util.nms.world.NMSVec3D getPosition() {
            return null;
        }

        @Override
        protected dev.marulabs.commandlib.util.nms.world.NMSVec2D getRotation() {
            return null;
        }
    }

    public static final class DeniedSource extends SenderlessSource {
        public DeniedSource(Object handle) {
            super(handle);
        }

        @Override
        public CommandSender getBukkitSender() {
            return mock(CommandSender.class);
        }
    }
}
