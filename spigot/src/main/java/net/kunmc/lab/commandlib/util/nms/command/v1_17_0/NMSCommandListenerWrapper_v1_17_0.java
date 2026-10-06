package net.kunmc.lab.commandlib.util.nms.command.v1_17_0;

import net.kunmc.lab.commandlib.util.nms.NMSReflection;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMSCommandListenerWrapper_v1_17_0 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_v1_17_0(Object handle) {
        super(handle, "commands.CommandListenerWrapper");
    }

    public CommandSender getBukkitSender() {
        return ((CommandSender) invokeMethod("getBukkitSender"));
    }

    public Entity getBukkitEntity() {
        Object entity = invokeGetter("world.entity.Entity");
        if (entity == null) {
            return null;
        }

        return NMSEntity.create(entity)
                        .getBukkitEntity();
    }

    public World getBukkitWorld() {
        try {
            Object nmsWorld = invokeGetter("server.level.WorldServer");
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
        NMSVec3D pos = NMSVec3D.create(invokeGetter("world.phys.Vec3D"));
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world, pos.x(), pos.y(), pos.z()) : null;
    }

    /**
     * Invokes the no-argument getter returning the given Minecraft class. The obfuscated getter names move between
     * releases (the level getter is getWorld in 1.17, e in 1.18 and f in 1.19), while each type has one plain getter;
     * the "entity or fail" variant declares an exception and is not chosen.
     */
    private Object invokeGetter(String returnClassName) {
        Class<?> returnType = NMSReflection.findMinecraftClass(returnClassName);
        return invokeFoundMethod(findMethodByReturnType(false, returnType));
    }
}
