package dev.marulabs.commandlib.internal.nms.typed.spigot_1_17_1;

import dev.marulabs.commandlib.internal.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.internal.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.internal.nms.server.NMSDedicatedServer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;

public class NMSDedicatedServer_spigot_1_17_1 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_17_1(Object handle) {
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
        return ((DedicatedServer) requireHandle()).resources;
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher, boolean reloaded) {
        DedicatedServer server = (DedicatedServer) requireHandle();
        ServerFunctionLibrary library = server.resources.getFunctionLibrary();
        boolean loadMissing = reloaded && loadTagEmpty(library);
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload(new PreparableReloadListener.PreparationBarrier() {
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {
                               return CompletableFuture.completedFuture(value);
                           }
                       },
                       server.resources.getResourceManager(),
                       InactiveProfiler.INSTANCE,
                       InactiveProfiler.INSTANCE,
                       Runnable::run,
                       Runnable::run)
               .join();
        keepPendingLoad(server.getFunctions(),
                        () -> server.getFunctions().replaceLibrary(library),
                        loadMissing && !loadTagEmpty(library));
    }

    private static boolean loadTagEmpty(ServerFunctionLibrary library) {
        return library.getTag(new ResourceLocation("minecraft", "load")).getValues().isEmpty();
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) requireHandle()).isRunning();
    }
}
