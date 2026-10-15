package dev.marulabs.commandlib.nms.spigot_1_21_5;

import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public class NMSDedicatedServer_spigot_1_21_5 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_21_5(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).getCommands());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).vanillaCommandDispatcher);
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(getDataPackResourcesHandle());
    }

    @Override
    public Object getDataPackResourcesHandle() {
        return ((DedicatedServer) requireHandle()).resources.managers();
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher, boolean reloaded) {
        DedicatedServer server = (DedicatedServer) requireHandle();
        ServerFunctionLibrary library = server.resources.managers().getFunctionLibrary();
        boolean loadMissing = reloaded && loadTagEmpty(library);
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload(new PreparableReloadListener.PreparationBarrier() {
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {
                               return CompletableFuture.completedFuture(value);
                           }
                       },
                       server.resources.resourceManager(),
                       Runnable::run,
                       Runnable::run)
               .join();
        keepPendingLoad(server.getFunctions(),
                        () -> server.getFunctions().replaceLibrary(library),
                        loadMissing && !loadTagEmpty(library));
    }

    private static boolean loadTagEmpty(ServerFunctionLibrary library) {
        return library.getTag(ResourceLocation.withDefaultNamespace("load")).isEmpty();
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) requireHandle()).isRunning();
    }
}
