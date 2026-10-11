package net.kunmc.lab.commandlib.util.nms.command;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public abstract class NMSCommandListenerWrapper extends NMSClass {
    public static NMSCommandListenerWrapper create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandListenerWrapper(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
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
     * position.
     */
    public Location getBukkitLocation() {
        World world = getBukkitWorld();
        NMSVec3D position = getPosition();
        if (world == null || position == null) {
            return null;
        }
        NMSVec2D rotation = getRotation();
        float yaw = rotation != null ? rotation.y() : 0.0F;
        float pitch = rotation != null ? rotation.x() : 0.0F;
        return new Location(world, position.x(), position.y(), position.z(), yaw, pitch);
    }
}
