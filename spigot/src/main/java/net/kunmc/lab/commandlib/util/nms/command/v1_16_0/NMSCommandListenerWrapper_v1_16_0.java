package net.kunmc.lab.commandlib.util.nms.command.v1_16_0;

import net.kunmc.lab.commandlib.util.nms.NMSReflection;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMSCommandListenerWrapper_v1_16_0 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_v1_16_0(Object handle) {
        super(handle, "CommandListenerWrapper");
    }

    public CommandSender getBukkitSender() {
        return ((CommandSender) invokeMethod("getBukkitSender"));
    }

    public Entity getBukkitEntity() {
        Object entity = invokeMethod("getEntity");
        if (entity == null) {
            return null;
        }

        return NMSEntity.create(entity)
                        .getBukkitEntity();
    }

    public World getBukkitWorld() {
        try {
            Object nmsWorld = invokeMethod("getWorld");
            if (nmsWorld == null) {
                return null;
            }
            Method method = ReflectionUtil.getMethodIncludingSuperclasses(nmsWorld.getClass(), "getWorld");
            return ((World) method.invoke(nmsWorld));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Location getBukkitLocation() {
        NMSVec3D pos = NMSVec3D.create(invokeMethod("getPosition"));
        Object rotation = invokeFoundMethod(findMethodByReturnType(false, NMSReflection.findMinecraftClass("Vec2F")));
        NMSVec2D rot = rotation != null ? NMSVec2D.create(rotation) : null;
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world,
                                                           pos.x(),
                                                           pos.y(),
                                                           pos.z(),
                                                           rot != null ? rot.y() : 0.0F,
                                                           rot != null ? rot.x() : 0.0F) : null;
    }
}
