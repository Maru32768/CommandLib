package dev.marulabs.commandlib.util.nms.command;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSVec2D;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public abstract class NMSCommandListenerWrapper extends NMSClass {
    public static NMSCommandListenerWrapper create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandListenerWrapper(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract CommandSender getBukkitSender();

    @Nullable
    public abstract Entity getBukkitEntity();

    @Nullable
    public abstract World getBukkitWorld();

    /**
     * Sends {@code component} through the source as command feedback, as vanilla commands do: the source gets it unless
     * it rejects feedback or is silent, and with {@code broadcastToOps} the operators and the server log get it as the
     * gamerules allow.
     */
    public abstract void sendSuccess(BaseComponent component, boolean broadcastToOps);

    /**
     * Converts {@code component} into the server's chat component, which a Brigadier exception carries as its message.
     */
    public abstract Message toMessage(BaseComponent component);

    /**
     * The position of the source, or null without one.
     */
    @Nullable
    protected abstract NMSVec3D getPosition();

    /**
     * The rotation of the source, x being the pitch and y the yaw, or null without one.
     */
    @Nullable
    protected abstract NMSVec2D getRotation();

    /**
     * The position of the source in its world, with its rotation as the yaw and pitch, or null without a world or a
     * position.
     */
    @Nullable
    public Location getBukkitLocation() {
        World world = getBukkitWorld();
        NMSVec3D position = getPosition();
        if (world == null || position == null) {
            return null;
        }
        NMSVec2D rotation = getRotation();
        float yaw = rotation != null ? rotation.y() : 0.0F;
        float pitch = rotation != null ? rotation.x() : 0.0F;
        return new Location(world, position.x(), position.y(), position.z(), yaw, pitch);
    }
}
