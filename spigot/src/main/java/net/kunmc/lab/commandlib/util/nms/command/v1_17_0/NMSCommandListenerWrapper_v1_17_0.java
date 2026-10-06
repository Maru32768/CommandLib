package net.kunmc.lab.commandlib.util.nms.command.v1_17_0;

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
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NMSCommandListenerWrapper_v1_17_0 extends NMSCommandListenerWrapper {
    private static final Map<Class<?>, Map<String, Method>> GETTERS = new ConcurrentHashMap<>();

    public NMSCommandListenerWrapper_v1_17_0(Object handle) {
        super(handle, "commands.CommandListenerWrapper");
    }

    public CommandSender getBukkitSender() {
        return ((CommandSender) invokeMethod("getBukkitSender"));
    }

    public Entity getBukkitEntity() {
        Object entity = invokeGetter("Entity");
        if (entity == null) {
            return null;
        }

        return NMSEntity.create(entity)
                        .getBukkitEntity();
    }

    public World getBukkitWorld() {
        try {
            Object nmsWorld = invokeGetter("WorldServer");
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
        NMSVec3D pos = NMSVec3D.create(invokeGetter("Vec3D"));
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world, pos.x(), pos.y(), pos.z()) : null;
    }

    /**
     * Invokes the no-argument getter returning the given Minecraft type. The obfuscated getter names move between
     * releases (the level getter is getWorld in 1.17, e in 1.18 and f in 1.19), while each type has one plain getter.
     * Getters declaring exceptions, such as the "entity or fail" variant, are skipped, and only net.minecraft return
     * types match so that Paper's Bukkit-side getters (such as getBukkitEntity returning org.bukkit.entity.Entity) are
     * never chosen.
     */
    private Object invokeGetter(String returnTypeSimpleName) {
        Method method = GETTERS.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
                               .computeIfAbsent(returnTypeSimpleName, this::findGetter);
        try {
            return method.invoke(getHandle());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Method findGetter(String returnTypeSimpleName) {
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers()) || method.getExceptionTypes().length != 0) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType.getName()
                          .startsWith("net.minecraft.") && returnType.getSimpleName()
                                                                     .equals(returnTypeSimpleName)) {
                return method;
            }
        }
        throw new IllegalStateException("No getter returning " + returnTypeSimpleName + " in " + clazz.getName());
    }
}
