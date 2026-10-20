package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_1;

import dev.marulabs.commandlib.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.internal.nms.server.NMSDataPackResources;
import java.lang.reflect.Field;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.ReloadableServerResources;

public class NMSDataPackResources_spigot_1_20_1 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_20_1(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        // ReloadableServerResources keeps the build context in a private field.
        for (Field field : ReloadableServerResources.class.getDeclaredFields()) {
            if (CommandBuildContext.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return NMSCommandBuildContext.create(field.get(requireHandle()));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("ReloadableServerResources has no CommandBuildContext field");
    }
}
