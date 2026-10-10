package net.kunmc.lab.commandlib.util.nms.command.v1_20_5;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class NMSCommandListenerWrapper_v1_20_5 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_v1_20_5(Object handle) {
        super(handle, "commands.CommandSourceStack");
    }

    public CommandSender getBukkitSender() {
        return ((CommandSender) invokeMethod("getBukkitSender"));
    }

    public Entity getBukkitEntity() {
        return ((Entity) invokeMethod("getBukkitEntity"));
    }

    public World getBukkitWorld() {
        return ((World) invokeMethod("getBukkitWorld"));
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
