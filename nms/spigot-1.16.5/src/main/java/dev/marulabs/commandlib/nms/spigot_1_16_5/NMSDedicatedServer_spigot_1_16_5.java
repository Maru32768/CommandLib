package dev.marulabs.commandlib.nms.spigot_1_16_5;

import java.util.concurrent.CompletableFuture;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import net.minecraft.server.v1_16_R3.CustomFunctionManager;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.GameProfilerDisabled;
import net.minecraft.server.v1_16_R3.IReloadListener;

public class NMSDedicatedServer_spigot_1_16_5 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_16_5(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommandDispatcher());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).vanillaCommandDispatcher);
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).dataPackResources);
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {
        DedicatedServer server = (DedicatedServer) getHandle();
        CustomFunctionManager library = server.dataPackResources.a();
        setFunctionDispatcher(library, CustomFunctionManager.class, dispatcher);
        library.a(new IReloadListener.a() {
                      @Override
                      public <T> CompletableFuture<T> a(T value) {
                          return CompletableFuture.completedFuture(value);
                      }
                  },
                  server.dataPackResources.h(),
                  GameProfilerDisabled.a,
                  GameProfilerDisabled.a,
                  Runnable::run,
                  Runnable::run)
               .join();
        server.getFunctionData()
              .a(library);
    }
}
