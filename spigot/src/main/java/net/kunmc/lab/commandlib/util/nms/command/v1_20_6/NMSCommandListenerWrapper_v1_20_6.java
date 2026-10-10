package net.kunmc.lab.commandlib.util.nms.command.v1_20_6;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class NMSCommandListenerWrapper_v1_20_6 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_v1_20_6(Object handle) {
        super(handle, "commands.CommandSourceStack");
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
            Object level = getValue("level");
            if (level == null) {
                return null;
            }
            return ((World) ReflectionUtil.getMethodIncludingSuperclasses(level.getClass(), "getWorld")
                                          .invoke(level));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected NMSVec3D getPosition() {
        Object position = invokeMethod("getPosition");
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    protected NMSVec2D getRotation() {
        Object rotation = invokeMethod("getRotation");
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }
}
