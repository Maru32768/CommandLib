package dev.marulabs.commandlib.nms.paper_1_21_1;

import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;

public class NMSDedicatedServer_paper_1_21_1 extends NMSDedicatedServer {
    public NMSDedicatedServer_paper_1_21_1(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).getCommands());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        DedicatedServer server = (DedicatedServer) requireHandle();
        try {
            return NMSCommandDispatcher.create(MinecraftServer.class.getField("vanillaCommandDispatcher")
                                                                    .get(server));
        } catch (NoSuchFieldException e) {
            return NMSCommandDispatcher.create(server.getCommands());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {
        DedicatedServer server = (DedicatedServer) requireHandle();
        ServerFunctionLibrary library = server.resources.managers().getFunctionLibrary();
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload(new PreparableReloadListener.PreparationBarrier() {
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {
                               return CompletableFuture.completedFuture(value);
                           }
                       },
                       server.resources.resourceManager(),
                       InactiveProfiler.INSTANCE,
                       InactiveProfiler.INSTANCE,
                       Runnable::run,
                       Runnable::run)
               .join();
        keepPendingLoad(server.getFunctions(), () -> server.getFunctions().replaceLibrary(library));
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) requireHandle()).isRunning();
    }
}
