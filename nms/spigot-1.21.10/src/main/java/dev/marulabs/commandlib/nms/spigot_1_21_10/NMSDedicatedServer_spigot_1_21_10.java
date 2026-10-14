package dev.marulabs.commandlib.nms.spigot_1_21_10;

import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public class NMSDedicatedServer_spigot_1_21_10 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_21_10(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommands());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).vanillaCommandDispatcher);
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(getDataPackResourcesHandle());
    }

    @Override
    public Object getDataPackResourcesHandle() {
        return ((DedicatedServer) getHandle()).resources.managers();
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {
        DedicatedServer server = (DedicatedServer) getHandle();
        ServerFunctionLibrary library = server.resources.managers().getFunctionLibrary();
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload(new PreparableReloadListener.SharedState(server.resources.resourceManager()),
                       Runnable::run,
                       new PreparableReloadListener.PreparationBarrier() {
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {
                               return CompletableFuture.completedFuture(value);
                           }
                       },
                       Runnable::run)
               .join();
        keepPendingLoad(server.getFunctions(), () -> server.getFunctions().replaceLibrary(library));
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) getHandle()).isRunning();
    }
}
