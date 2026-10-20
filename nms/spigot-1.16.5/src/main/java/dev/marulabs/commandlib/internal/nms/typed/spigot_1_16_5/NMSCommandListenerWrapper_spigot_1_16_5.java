package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.internal.nms.world.NMSVec2D;
import dev.marulabs.commandlib.internal.nms.world.NMSVec3D;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.Vec2F;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.jetbrains.annotations.Nullable;

public class NMSCommandListenerWrapper_spigot_1_16_5 extends NMSCommandListenerWrapper {
    public NMSCommandListenerWrapper_spigot_1_16_5(Object handle) {
        super(handle, CommandListenerWrapper.class);
    }

    @Override
    public CommandSender getBukkitSender() {
        return wrapper().getBukkitSender();
    }

    @Override
    @Nullable
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = wrapper().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    @Nullable
    public World getBukkitWorld() {
        WorldServer world = wrapper().getWorld();
        return world != null ? world.getWorld() : null;
    }

    @Override
    @Nullable
    protected NMSVec3D getPosition() {
        Vec3D position = wrapper().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    @Nullable
    protected NMSVec2D getRotation() {
        Vec2F rotation = wrapper().i();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    @Override
    public void sendSuccess(BaseComponent component, boolean broadcastToOps) {
        wrapper().sendMessage(component(component), broadcastToOps);
    }

    @Override
    public Message toMessage(BaseComponent component) {
        return component(component);
    }

    private static IChatBaseComponent component(BaseComponent component) {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(component));
    }

    private CommandListenerWrapper wrapper() {
        return (CommandListenerWrapper) requireHandle();
    }
}
