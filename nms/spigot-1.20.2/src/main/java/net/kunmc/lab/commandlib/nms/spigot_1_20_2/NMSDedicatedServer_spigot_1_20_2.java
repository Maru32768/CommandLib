package net.kunmc.lab.commandlib.nms.spigot_1_20_2;

import java.util.concurrent.CompletableFuture;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;

public class NMSDedicatedServer_spigot_1_20_2 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_20_2(Object handle) {
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
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).resources.managers());
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {
        DedicatedServer server = (DedicatedServer) getHandle();
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
        server.getFunctions()
              .replaceLibrary(library);
    }
}
