package net.kunmc.lab.commandlib.nms.spigot_1_17_1;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class NMSCommandListenerWrapper_spigot_1_17_1 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_spigot_1_17_1(Object handle) {
        super(handle, "commands.CommandListenerWrapper");
    }

    @Override
    public CommandSender getBukkitSender() {
        return source().getBukkitSender();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    public World getBukkitWorld() {
        ServerLevel level = source().getLevel();
        return level != null ? level.getWorld() : null;
    }

    @Override
    public Location getBukkitLocation() {
        Vec3 pos = source().getPosition();
        Vec2 rotation = source().getRotation();
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world,
                                                           pos.x,
                                                           pos.y,
                                                           pos.z,
                                                           rotation != null ? rotation.y : 0.0F,
                                                           rotation != null ? rotation.x : 0.0F) : null;
    }

    private CommandSourceStack source() {
        return (CommandSourceStack) getHandle();
    }
}
