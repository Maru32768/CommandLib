package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.Vec2F;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class NMSCommandListenerWrapper_spigot_1_16_5 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_spigot_1_16_5(Object handle) {
        super(handle, "CommandListenerWrapper");
    }

    @Override
    public CommandSender getBukkitSender() {
        return wrapper().getBukkitSender();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = wrapper().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    public World getBukkitWorld() {
        WorldServer world = wrapper().getWorld();
        return world != null ? world.getWorld() : null;
    }

    @Override
    protected NMSVec3D getPosition() {
        Vec3D position = wrapper().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    protected NMSVec2D getRotation() {
        Vec2F rotation = wrapper().i();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    private CommandListenerWrapper wrapper() {
        return (CommandListenerWrapper) getHandle();
    }
}
