package dev.marulabs.cadence.internal;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
//? if fabric {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
*///?} elif neoforge {
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} elif >=1.17 {
import net.minecraftforge.server.ServerLifecycleHooks;
//?} else
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/

/**
 * The running server, for Cadence's own code. Not part of the public API.
 */
@ApiStatus.Internal
public final class CurrentServer {
    //? if fabric {
    /*// Fabric has no global server accessor. The events are registered when this class initializes, which can be after
    // the server started, so get() falls back to the game instance.
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
        /*MinecraftServer current = server;
        if (current != null) {
            return current;
        }
        Object game = FabricLoader.getInstance()
                                  .getGameInstance();
        if (game == null || game instanceof MinecraftServer) {
            return (MinecraftServer) game;
        }
        return Client.integratedServer(game);
        *///?} else
        return ServerLifecycleHooks.getCurrentServer();
    }

    //? if fabric {
    /*// A dedicated server has no Minecraft class, so it is referenced only from here, which loads on a client alone.
    private static final class Client {
        @Nullable
        static MinecraftServer integratedServer(Object game) {
            return ((Minecraft) game).getSingleplayerServer();
        }
    }
    *///?}
}
