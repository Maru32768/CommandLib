package net.kunmc.lab.commandlib.nms.spigot_1_20_1;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class NMSCommandListenerWrapper_spigot_1_20_1 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_spigot_1_20_1(Object handle) {
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
    protected NMSVec3D getPosition() {
        Vec3 position = source().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    protected NMSVec2D getRotation() {
        Vec2 rotation = source().getRotation();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    private CommandSourceStack source() {
        return (CommandSourceStack) getHandle();
    }
}
