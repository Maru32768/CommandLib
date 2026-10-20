package dev.marulabs.commandlib.internal.nms.command;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSVanillaCommandWrapper extends NMSClass {
    public static NMSVanillaCommandWrapper create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVanillaCommandWrapper.class))
                             .newInstance();
    }

    public NMSVanillaCommandWrapper(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command);
}
