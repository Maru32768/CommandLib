package dev.marulabs.commandlib.nms.spigot_1_17;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.util.nms.world.NMSVec2D;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.jetbrains.annotations.Nullable;

public class NMSCommandListenerWrapper_spigot_1_17 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_spigot_1_17(Object handle) {
        super(handle, CommandSourceStack.class);
    }

    @Override
    public CommandSender getBukkitSender() {
        return source().getBukkitSender();
    }

    @Override
    @Nullable
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    @Nullable
    public World getBukkitWorld() {
        ServerLevel level = source().getLevel();
        return level != null ? level.getWorld() : null;
    }

    @Override
    @Nullable
    protected NMSVec3D getPosition() {
        Vec3 position = source().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    @Nullable
    protected NMSVec2D getRotation() {
        Vec2 rotation = source().getRotation();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    @Override
    public void sendSuccess(BaseComponent component, boolean broadcastToOps) {
        Component message = component(component);
        source().sendSuccess(message, broadcastToOps);
    }

    @Override
    public Message toMessage(BaseComponent component) {
        return component(component);
    }

    private static Component component(BaseComponent component) {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(component));
    }

    private CommandSourceStack source() {
        return (CommandSourceStack) requireHandle();
    }
}
