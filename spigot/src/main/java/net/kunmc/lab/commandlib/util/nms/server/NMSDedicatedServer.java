package net.kunmc.lab.commandlib.util.nms.server;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

import java.lang.reflect.Field;

public abstract class NMSDedicatedServer extends NMSClass {
    public static NMSDedicatedServer create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDedicatedServer.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDedicatedServer(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * The dispatcher of the current data pack resources, which CraftBukkit replaces on every command sync.
     */
    public abstract NMSCommandDispatcher getCommandDispatcher();

    /**
     * The dispatcher of the initial data pack resources, MinecraftServer#vanillaCommandDispatcher. CraftBukkit
     * assigns it once and never replaces it; "execute run" redirects to its root.
     */
    public abstract NMSCommandDispatcher getVanillaCommandDispatcher();

    public abstract NMSDataPackResources getDataPackResources();

    /**
     * Compiles the functions of the current data packs again with the given dispatcher, so their commands may use
     * what was added to it after the data packs were loaded, and hands them to the server's function manager.
     */
    public abstract void recompileFunctions(NMSCommandDispatcher dispatcher);

    /**
     * The function library keeps the dispatcher it compiles functions with in a private final field, found by its
     * type since its name is obfuscated on Spigot.
     */
    protected static void setFunctionDispatcher(Object library, Class<?> libraryClass, NMSCommandDispatcher dispatcher) {
        for (Field field : libraryClass.getDeclaredFields()) {
            if (field.getType() == CommandDispatcher.class) {
                try {
                    field.setAccessible(true);
                    field.set(library, dispatcher.getBrigadier());
                    return;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(libraryClass.getName() + " has no CommandDispatcher field");
    }
}
