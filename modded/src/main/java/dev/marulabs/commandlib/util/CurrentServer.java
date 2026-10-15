package dev.marulabs.commandlib.util;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
//? if fabric {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
*///?} elif neoforge {
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} elif >=1.17 {
import net.minecraftforge.server.ServerLifecycleHooks;
//?} else
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/

/**
 * The running server, for CommandLib's own code. Not part of the public API.
 */
public final class CurrentServer {
    //? if fabric {
    /*// Fabric has no global server accessor, so remember the server from the lifecycle events. They are registered
    // when this class initializes: from CommandLib's mod entrypoint, or from CommandLib.register when the jar is not
    // loaded as a mod.
    @Nullable
    private static volatile MinecraftServer server;

    static {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }
    *///?}

    private CurrentServer() {
    }

    /**
     * Returns the running server, or {@code null} when no server runs.
     */
    @Nullable
    public static MinecraftServer get() {
        //? if fabric {
        /*return server;
        *///?} else
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * Starts following the server on Fabric. It is the entrypoint of CommandLib's {@code fabric.mod.json}, and does
     * nothing on Forge and NeoForge.
     */
    public static void listen() {
        // Calling a static method initializes the class, which registers the Fabric listeners.
    }
}
