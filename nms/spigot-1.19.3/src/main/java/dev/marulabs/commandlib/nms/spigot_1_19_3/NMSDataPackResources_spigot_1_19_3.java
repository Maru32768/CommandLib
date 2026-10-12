package dev.marulabs.commandlib.nms.spigot_1_19_3;

import java.lang.reflect.Field;
import dev.marulabs.commandlib.util.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.ReloadableServerResources;

public class NMSDataPackResources_spigot_1_19_3 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_19_3(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        // ReloadableServerResources keeps the build context in a private field.
        for (Field field : ReloadableServerResources.class.getDeclaredFields()) {
            if (CommandBuildContext.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return NMSCommandBuildContext.create(field.get(getHandle()));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("ReloadableServerResources has no CommandBuildContext field");
    }
}
