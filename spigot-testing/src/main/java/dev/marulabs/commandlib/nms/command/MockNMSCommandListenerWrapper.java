package dev.marulabs.commandlib.nms.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.util.nms.world.NMSVec2D;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class MockNMSCommandListenerWrapper extends NMSCommandListenerWrapper {
    public MockNMSCommandListenerWrapper(Object handle) {
        super(handle, Object.class);
    }

    @Override
    public CommandSender getBukkitSender() {
        return CommandTester.getCurrentCommandSender();
    }

    @Override
    @Nullable
    public Entity getBukkitEntity() {
        CommandSender sender = CommandTester.getCurrentCommandSender();
        return sender instanceof Entity ? (Entity) sender : null;
    }

    @Override
    @Nullable
    public World getBukkitWorld() {
        CommandSender sender = CommandTester.getCurrentCommandSender();
        return sender instanceof Entity ? ((Entity) sender).getWorld() : null;
    }

    // The mock has no gamerules or operators, so the sender gets every feedback message.
    @Override
    public void sendSuccess(BaseComponent component, boolean broadcastToOps) {
        getBukkitSender().spigot()
                         .sendMessage(component);
    }

    @Override
    public Message toMessage(BaseComponent component) {
        return new LiteralMessage(component.toPlainText());
    }

    // The mock takes the location from the sender entity in getBukkitLocation instead.
    @Override
    @Nullable
    protected NMSVec3D getPosition() {
        return null;
    }

    @Override
    @Nullable
    protected NMSVec2D getRotation() {
        return null;
    }

    @Override
    @Nullable
    public Location getBukkitLocation() {
        CommandSender sender = CommandTester.getCurrentCommandSender();
        return sender instanceof Entity ? ((Entity) sender).getLocation() : null;
    }
}
