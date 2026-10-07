package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Location;
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
    public Location getBukkitLocation() {
        Vec3D pos = wrapper().getPosition();
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world, pos.x, pos.y, pos.z) : null;
    }

    private CommandListenerWrapper wrapper() {
        return (CommandListenerWrapper) getHandle();
    }
}
