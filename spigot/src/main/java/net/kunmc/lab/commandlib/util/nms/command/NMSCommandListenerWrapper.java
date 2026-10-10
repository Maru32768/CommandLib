package net.kunmc.lab.commandlib.util.nms.command;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.v1_16_0.NMSCommandListenerWrapper_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_17_0.NMSCommandListenerWrapper_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_5.NMSCommandListenerWrapper_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_6.NMSCommandListenerWrapper_v1_20_6;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public abstract class NMSCommandListenerWrapper extends MinecraftClass {
    public static NMSCommandListenerWrapper create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandListenerWrapper(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract CommandSender getBukkitSender();

    public abstract Entity getBukkitEntity();

    public abstract World getBukkitWorld();

    /**
     * The position of the source, or null without one.
     */
    protected abstract NMSVec3D getPosition();

    /**
     * The rotation of the source, x being the pitch and y the yaw, or null without one.
     */
    protected abstract NMSVec2D getRotation();

    /**
     * The position of the source in its world, with its rotation as the yaw and pitch, or null without a world or a
     * position. The reflection implementations find the rotation by type and obfuscated field names, so a server where
     * that fails still gets the position, without the rotation.
     */
    public Location getBukkitLocation() {
        World world = getBukkitWorld();
        NMSVec3D position = getPosition();
        if (world == null || position == null) {
            return null;
        }
        float yaw = 0.0F;
        float pitch = 0.0F;
        try {
            NMSVec2D rotation = getRotation();
            if (rotation != null) {
                yaw = rotation.y();
                pitch = rotation.x();
            }
        } catch (RuntimeException e) {
            yaw = 0.0F;
            pitch = 0.0F;
        }
        return new Location(world, position.x(), position.y(), position.z(), yaw, pitch);
    }

    static {
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_17_0.class,
                                  "1.17.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_20_5.class,
                                  "1.20.5",
                                  "1.20.5");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_20_6.class,
                                  "1.20.6",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class);
    }
}
