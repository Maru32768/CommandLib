package dev.marulabs.commandlib.internal.nms.server;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public abstract class NMSDedicatedServer extends NMSClass {
    public static NMSDedicatedServer create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDedicatedServer.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDedicatedServer(@Nullable Object handle, Class<?> serverClass) {
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
     * The server object that {@link #getDataPackResources()} wraps, which the server replaces when it loads the data
     * packs again, for an identity check without making a wrapper.
     */
    public abstract Object getDataPackResourcesHandle();

    /**
     * Compiles the functions of the current data packs again with the given dispatcher, so their commands may use
     * what was added to it after the data packs were loaded, and hands them to the server's function manager.
     *
     * @param reloaded whether the data packs were loaded again since the last compilation, so the function library
     *                 holds the server's own compilation, whose #minecraft:load tag lacks the functions that use
     *                 commands it did not have yet
     */
    public abstract void recompileFunctions(NMSCommandDispatcher dispatcher, boolean reloaded);

    /**
     * Whether the server is running, which is false once it begins to stop, before it disables the plugins.
     */
    public abstract boolean isRunning();

    /**
     * Runs {@code replaceLibrary}, a call of ServerFunctionManager#replaceLibrary, without the run of the #load functions
     * that it schedules, as after a data reload, so a compilation outside a data reload does not run them again. A run
     * still pending from a data reload stays. The flag, postReload, is the manager's only boolean field from 1.17, and
     * 1.16.5 also has isInFunction, which the call leaves alone, so every boolean field keeps its value; its name is
     * obfuscated on Spigot.
     *
     * <p>A data reload that a plugin task runs, such as {@code Bukkit.dispatchCommand(console, "minecraft:reload")},
     * is applied after this tick's compilation check and before the server ticks functions, so the server runs the
     * #load functions of the reload with its own compilation. That compilation drops a tag listing a function it could
     * not compile, so none of the tag's functions ran. When {@code loadMissed}, the tag was missing from the server's
     * compilation of a data reload and is present in CommandLib's, so the run that replaceLibrary schedules stays, and
     * the #load functions run once, with the commands.
     */
    protected static void keepPendingLoad(Object functionManager, Runnable replaceLibrary, boolean loadMissed) {
        if (loadMissed) {
            replaceLibrary.run();
            return;
        }
        List<Field> flags = new ArrayList<>();
        for (Class<?> c = functionManager.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    flags.add(field);
                }
            }
        }
        try {
            boolean[] values = new boolean[flags.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = flags.get(i)
                                 .getBoolean(functionManager);
            }
            replaceLibrary.run();
            for (int i = 0; i < values.length; i++) {
                flags.get(i)
                     .setBoolean(functionManager, values[i]);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The function library keeps the dispatcher it compiles functions with in a private final field, found by its
     * type since its name is obfuscated on Spigot.
     */
    protected static void setFunctionDispatcher(Object library,
                                                Class<?> libraryClass,
                                                NMSCommandDispatcher dispatcher) {
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
